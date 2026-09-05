import { decodeStandardPid } from '@ignitionai/capture-protocol';
import { 
  VehicleStateMachine, 
  PetrolFuelControlModule, 
  ThermalResponseModule, 
  SupplyVoltageModule 
} from '@ignitionai/core';
import { 
  CaptureView, 
  DiagnosticContext, 
  DecodedObservation, 
  ModuleResult, 
  PowertrainHealthScore 
} from '@ignitionai/contracts';
import { generateScenario, ScenarioType } from './scenario_generator';

export interface SimulationReport {
  scenario: ScenarioType;
  watermark: 'SIMULATED / NOT VALIDATED';
  vehicleProfile: string;
  totalFrames: number;
  totalDecodedObservations: number;
  healthScore: PowertrainHealthScore;
  moduleResults: ModuleResult[];
}

export function runSimulationReplay(scenarioType: ScenarioType): SimulationReport {
  const scenario = generateScenario(scenarioType);
  const stateMachine = new VehicleStateMachine();

  const decodedObservations: DecodedObservation[] = [];
  let seq = 1;

  let currentRpm = 0;
  let currentSpeed = 0;

  // 1. Decode raw frames through capture-protocol
  for (const raw of scenario.rawFrames) {
    const obs = decodeStandardPid(raw, seq++);
    decodedObservations.push(obs);

    if (obs.signalId === 'RPM') currentRpm = obs.value;
    if (obs.signalId === 'VSS') currentSpeed = obs.value;

    stateMachine.classify({
      rpm: currentRpm,
      speed: currentSpeed,
      throttle: 15,
      dtSeconds: 0.5,
    });
  }

  const captureView: CaptureView = {
    captureId: `sim-cap-${Date.now()}-${scenarioType}`,
    vehicleProfileId: scenario.vehicleProfileId,
    isSimulated: true,
    totalDurationMs: scenario.totalDurationMs,
    observations: decodedObservations,
    stateTimeline: [],
    dtcList: [],
  };

  const context: DiagnosticContext = {
    vehicleProfileId: scenario.vehicleProfileId,
    fuelType: 'petrol',
    isSimulated: true,
  };

  // 2. Run diagnostic modules
  const modules = [
    new PetrolFuelControlModule(),
    new ThermalResponseModule(),
    new SupplyVoltageModule(),
  ];

  const results: ModuleResult[] = modules.map(m => m.evaluate(captureView, context));

  // 3. Compute powertrain condition score (Strictly watermarked SIMULATED)
  const candidateFindings = results.filter(r => r.findingLevel === 'candidate');
  const coveragePercent = results.length > 0 
    ? (results.filter(r => r.status === 'ok').length / results.length) * 100 
    : 0;

  // Unvalidated prototype score formula for simulator
  let penalty = 0;
  for (const f of candidateFindings) {
    penalty += 25; // 25 point penalty per candidate fault
  }
  const rawScore = Math.max(0, 100 - penalty);

  const healthScore: PowertrainHealthScore = {
    score: rawScore,
    status: 'available',
    isSimulated: true,
    watermark: 'SIMULATED / NOT VALIDATED',
    testedScope: ['Fuel Control', 'Thermal Response', 'Supply Voltage'],
    coveragePercentage: coveragePercent,
    unresolvedConditions: candidateFindings.flatMap(f => f.candidateComponents),
    criticalWarnings: candidateFindings.length > 0 
      ? ['Advisory candidates identified under simulated conditions. Refer to discriminating workshop test plan.'] 
      : [],
  };

  return {
    scenario: scenarioType,
    watermark: 'SIMULATED / NOT VALIDATED',
    vehicleProfile: scenario.vehicleProfileId,
    totalFrames: scenario.rawFrames.length,
    totalDecodedObservations: decodedObservations.length,
    healthScore,
    moduleResults: results,
  };
}

// Direct CLI entrypoint
if (require.main === module) {
  const scenarios: ScenarioType[] = [
    'healthy_baseline',
    'air_intake_leak',
    'coolant_thermostat_stuck_open',
    'alternator_voltage_sag',
  ];

  console.log('===============================================================');
  console.log('  IGNITIONAI SOFTWARE SIMULATOR: REPLAY ENGINE');
  console.log('  Notice: ALL OUTPUTS ARE WATERMARKED SIMULATED / NOT VALIDATED');
  console.log('===============================================================\n');

  for (const sc of scenarios) {
    const report = runSimulationReplay(sc);
    console.log(`[SCENARIO: ${report.scenario.toUpperCase()}]`);
    console.log(`  Watermark:      ${report.watermark}`);
    console.log(`  Profile:        ${report.vehicleProfile}`);
    console.log(`  Observations:   ${report.totalDecodedObservations}`);
    console.log(`  Sim Condition:  ${report.healthScore.score}/100`);
    console.log(`  Candidates:     ${report.healthScore.unresolvedConditions.join(', ') || 'None (Healthy)'}`);
    for (const res of report.moduleResults) {
      if (res.findingLevel === 'candidate') {
        console.log(`    -> Finding [${res.moduleId}]:`);
        console.log(`       Candidates:   ${res.candidateComponents.join(', ')}`);
        console.log(`       Next Action:  ${res.requiredNextTests[0]}`);
      }
    }
    console.log('---------------------------------------------------------------');
  }
}
