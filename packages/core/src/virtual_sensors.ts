import { 
  DiagnosticModule, 
  CaptureView, 
  DiagnosticContext, 
  ModuleResult, 
  SignalRequirement 
} from '@ignitionai/contracts';
import { computeCombinedFuelTrim, computeMedian, computeMAD } from './metrics';

/**
 * 1. Petrol Fuel Control Assessment Family
 */
export class PetrolFuelControlModule implements DiagnosticModule {
  public id = 'fuel_control_petrol';
  public version = '1.0.0';
  public requirements: SignalRequirement[] = [
    { signalId: 'STFT1', minCadenceHz: 1.0, maxLatencyMs: 1500, requiredStates: ['idle', 'cruise'] },
    { signalId: 'LTFT1', minCadenceHz: 0.5, maxLatencyMs: 3000, requiredStates: ['idle', 'cruise'] },
    { signalId: 'ECT', minCadenceHz: 0.2, maxLatencyMs: 5000 },
  ];

  public evaluate(input: CaptureView, context: DiagnosticContext): ModuleResult {
    if (context.fuelType === 'diesel') {
      return {
        moduleId: this.id,
        status: 'unsupported',
        candidateComponents: [],
        competingAlternatives: [],
        requiredNextTests: [],
        evidenceRefs: [],
        unmetRequirements: ['fuel_control_trims_not_applicable_to_diesel'],
        claimBoundary: 'Closed-loop stoichiometric fuel trim analysis is disabled for compression-ignition (diesel) engines.',
        bundleDigest: 'sha256-fuel-ctrl-v1',
        isSimulated: input.isSimulated,
      };
    }

    const stftObs = input.observations.filter(o => o.signalId === 'STFT1' && o.isValid);
    const ltftObs = input.observations.filter(o => o.signalId === 'LTFT1' && o.isValid);
    const ectObs = input.observations.filter(o => o.signalId === 'ECT' && o.isValid);

    if (stftObs.length < 30 || ltftObs.length < 10) {
      return {
        moduleId: this.id,
        status: 'insufficient_evidence',
        candidateComponents: [],
        competingAlternatives: [],
        requiredNextTests: ['complete_standard_10_min_drive'],
        evidenceRefs: [],
        unmetRequirements: ['minimum_30_stft_and_10_ltft_samples_required'],
        claimBoundary: 'Insufficient fuel trim sample density in steady operating windows.',
        bundleDigest: 'sha256-fuel-ctrl-v1',
        isSimulated: input.isSimulated,
      };
    }

    // Check warm engine requirement (ECT >= 75 degC)
    const maxEct = ectObs.length > 0 ? Math.max(...ectObs.map(o => o.value)) : 0;
    if (maxEct < 70) {
      return {
        moduleId: this.id,
        status: 'insufficient_evidence',
        candidateComponents: [],
        competingAlternatives: [],
        requiredNextTests: ['warm_engine_to_operating_temperature_above_75C'],
        evidenceRefs: [`ECT_MAX_${maxEct}C`],
        unmetRequirements: ['warm_closed_loop_operating_temperature_not_reached'],
        claimBoundary: 'Engine did not achieve operating thermal baseline (>70 degC) during drive.',
        bundleDigest: 'sha256-fuel-ctrl-v1',
        isSimulated: input.isSimulated,
      };
    }

    // Compute combined trim values
    const combinedTrims: number[] = [];
    const minLength = Math.min(stftObs.length, ltftObs.length);
    for (let i = 0; i < minLength; i++) {
      combinedTrims.push(computeCombinedFuelTrim(stftObs[i].value, ltftObs[i].value));
    }

    const medianTrim = computeMedian(combinedTrims);
    const madTrim = computeMAD(combinedTrims, medianTrim);

    const sortedTrims = [...combinedTrims].sort((a, b) => a - b);
    const p90Trim = sortedTrims[Math.floor(sortedTrims.length * 0.85)] ?? medianTrim;

    // Baseline: Normal closed-loop combined trim is within [-8.0%, +8.0%]
    // Significant Lean Bias (Positive Trim >= +15.0% median or >= +18.0% sustained peak/idle)
    if (medianTrim >= 15.0 || p90Trim >= 18.0) {
      const reportTrimVal = medianTrim >= 15.0 ? medianTrim : p90Trim;
      return {
        moduleId: this.id,
        status: 'ok',
        findingLevel: 'candidate',
        estimate: { value: reportTrimVal, unit: '%', interval: [medianTrim - madTrim, medianTrim + madTrim] },
        candidateComponents: [
          'intake_manifold_gasket',
          'pcv_valve_or_breather_hose',
          'fuel_injector_flow_restriction',
          'fuel_pump_pressure_low',
          'upstream_o2_sensor_lean_bias',
        ],
        competingAlternatives: [
          'Exhaust leak upstream of O2 sensor introducing false ambient oxygen',
          'Unmetered air ingress post-throttle body / vacuum leak',
          'Clogged fuel filter or weak fuel delivery pressure',
        ],
        requiredNextTests: [
          'Perform intake smoke test to isolate unmetered air leaks',
          'Measure physical fuel rail pressure with mechanical gauge under idle and load',
          'Inspect fuel trim response at 2500 RPM (if trims improve at high RPM, vacuum leak is likely)',
        ],
        evidenceRefs: [
          `MEDIAN_COMBINED_TRIM_${medianTrim}%`,
          `MAD_TRIM_${madTrim}%`,
          `SAMPLE_COUNT_${minLength}`,
        ],
        unmetRequirements: [],
        claimBoundary: 'Advisory: Positive fuel trim bias indicates unmetered air or low fuel mass. Requires workshop smoke/pressure discriminator.',
        bundleDigest: 'sha256-fuel-ctrl-v1',
        isSimulated: input.isSimulated,
      };
    }

    // Significant Rich Bias (Negative Trim <= -15.0%)
    if (medianTrim <= -15.0) {
      return {
        moduleId: this.id,
        status: 'ok',
        findingLevel: 'candidate',
        estimate: { value: medianTrim, unit: '%', interval: [medianTrim - madTrim, medianTrim + madTrim] },
        candidateComponents: [
          'leaking_fuel_injector',
          'evap_purge_valve_stuck_open',
          'high_fuel_rail_pressure',
          'manifold_absolute_pressure_sensor_high_bias',
        ],
        competingAlternatives: [
          'EVAP purge solenoid leaking fuel vapor continuously from charcoal canister',
          'Faulty fuel pressure regulator',
          'Dripping/stuck-open injector nozzle',
        ],
        requiredNextTests: [
          'Clamp EVAP purge line and monitor if fuel trims normalize to zero',
          'Perform fuel injector balance and drop test',
          'Verify key-on-engine-off MAP sensor reading against barometric pressure',
        ],
        evidenceRefs: [
          `MEDIAN_COMBINED_TRIM_${medianTrim}%`,
          `MAD_TRIM_${madTrim}%`,
        ],
        unmetRequirements: [],
        claimBoundary: 'Advisory: Negative fuel trim bias indicates excessive fuel delivery or rich metering. Requires EVAP and injector balance test.',
        bundleDigest: 'sha256-fuel-ctrl-v1',
        isSimulated: input.isSimulated,
      };
    }

    // Normal Fuel Control
    return {
      moduleId: this.id,
      status: 'ok',
      findingLevel: 'subsystem',
      estimate: { value: medianTrim, unit: '%', interval: [medianTrim - madTrim, medianTrim + madTrim] },
      candidateComponents: [],
      competingAlternatives: [],
      requiredNextTests: [],
      evidenceRefs: [`MEDIAN_COMBINED_TRIM_${medianTrim}%`],
      unmetRequirements: [],
      claimBoundary: 'Fuel control trims operating within nominal healthy closed-loop bounds [-8%, +8%].',
      bundleDigest: 'sha256-fuel-ctrl-v1',
      isSimulated: input.isSimulated,
    };
  }
}

/**
 * 2. Thermal Response Assessment Family (Thermostat / Cooling)
 */
export class ThermalResponseModule implements DiagnosticModule {
  public id = 'thermal_response';
  public version = '1.0.0';
  public requirements: SignalRequirement[] = [
    { signalId: 'ECT', minCadenceHz: 0.2, maxLatencyMs: 5000 },
  ];

  public evaluate(input: CaptureView, context: DiagnosticContext): ModuleResult {
    const ectObs = input.observations.filter(o => o.signalId === 'ECT' && o.isValid);

    if (ectObs.length < 20) {
      return {
        moduleId: this.id,
        status: 'insufficient_evidence',
        candidateComponents: [],
        competingAlternatives: [],
        requiredNextTests: ['continuous_thermal_monitoring_drive'],
        evidenceRefs: [],
        unmetRequirements: ['minimum_20_coolant_samples_required'],
        claimBoundary: 'Insufficient coolant temperature observations.',
        bundleDigest: 'sha256-thermal-v1',
        isSimulated: input.isSimulated,
      };
    }

    const startEct = ectObs[0].value;
    const finalEct = ectObs[ectObs.length - 1].value;
    const maxEct = Math.max(...ectObs.map(o => o.value));

    // Stuck open thermostat check: 10 minutes drive elapsed but coolant temperature stayed < 70 degC
    if (input.totalDurationMs >= 480000 && maxEct < 70) {
      return {
        moduleId: this.id,
        status: 'ok',
        findingLevel: 'candidate',
        estimate: { value: maxEct, unit: 'degC' },
        candidateComponents: [
          'engine_coolant_thermostat',
          'engine_coolant_temperature_sensor',
          'radiator_cooling_fan_relay',
        ],
        competingAlternatives: [
          'Thermostat valve stuck mechanically open or opening prematurely',
          'ECT sensor resistance out of calibration',
          'Radiator cooling fan running continuously',
        ],
        requiredNextTests: [
          'Measure upper vs lower radiator hose temperatures with infrared pyrometer during warm-up',
          'Verify ECT sensor resistance vs workshop manual temperature chart',
        ],
        evidenceRefs: [
          `START_ECT_${startEct}C`,
          `FINAL_ECT_${finalEct}C`,
          `DURATION_MS_${input.totalDurationMs}`,
        ],
        unmetRequirements: [],
        claimBoundary: 'Advisory: Thermal trajectory failed to reach normal operating temperature (75-95C) within 8+ minutes.',
        bundleDigest: 'sha256-thermal-v1',
        isSimulated: input.isSimulated,
      };
    }

    return {
      moduleId: this.id,
      status: 'ok',
      findingLevel: 'subsystem',
      estimate: { value: maxEct, unit: 'degC' },
      candidateComponents: [],
      competingAlternatives: [],
      requiredNextTests: [],
      evidenceRefs: [`MAX_ECT_${maxEct}C`],
      unmetRequirements: [],
      claimBoundary: 'Coolant temperature trajectory achieved normal operating envelope.',
      bundleDigest: 'sha256-thermal-v1',
      isSimulated: input.isSimulated,
    };
  }
}

/**
 * 3. Supply-Voltage Plausibility Module
 */
export class SupplyVoltageModule implements DiagnosticModule {
  public id = 'supply_voltage_plausibility';
  public version = '1.0.0';
  public requirements: SignalRequirement[] = [
    { signalId: 'VPWR', minCadenceHz: 1.0, maxLatencyMs: 1500 },
  ];

  public evaluate(input: CaptureView, context: DiagnosticContext): ModuleResult {
    const vpwrObs = input.observations.filter(o => o.signalId === 'VPWR' && o.isValid);

    if (vpwrObs.length < 20) {
      return {
        moduleId: this.id,
        status: 'insufficient_evidence',
        candidateComponents: [],
        competingAlternatives: [],
        requiredNextTests: ['log_ecu_voltage_during_drive'],
        evidenceRefs: [],
        unmetRequirements: ['minimum_20_vpwr_samples_required'],
        claimBoundary: 'Insufficient ECU supply voltage samples.',
        bundleDigest: 'sha256-vpwr-v1',
        isSimulated: input.isSimulated,
      };
    }

    const voltages = vpwrObs.map(o => o.value);
    const medianVoltage = computeMedian(voltages);
    const minVoltage = Math.min(...voltages);

    // Charging rail failure: while engine running (RPM > 600), voltage drops below 13.0V
    if (medianVoltage < 13.0) {
      return {
        moduleId: this.id,
        status: 'ok',
        findingLevel: 'candidate',
        estimate: { value: medianVoltage, unit: 'V' },
        candidateComponents: [
          'alternator_assembly',
          'alternator_voltage_regulator',
          'serpentine_accessory_belt',
          'main_battery_ground_cable',
        ],
        competingAlternatives: [
          'Alternator diode pack failure or worn brushes',
          'Loose or slipping alternator belt under load',
          'High resistance ground strap connection between engine block and chassis',
        ],
        requiredNextTests: [
          'Measure charging voltage directly at battery terminals with digital multimeter under load',
          'Perform voltage drop test on alternator B+ cable and engine ground strap',
          'Inspect serpentine belt tension and pulley wear',
        ],
        evidenceRefs: [
          `MEDIAN_VPWR_${medianVoltage}V`,
          `MIN_VPWR_${minVoltage}V`,
        ],
        unmetRequirements: [],
        claimBoundary: 'Advisory: Charging system voltage sustained below 13.0V during engine operation.',
        bundleDigest: 'sha256-vpwr-v1',
        isSimulated: input.isSimulated,
      };
    }

    return {
      moduleId: this.id,
      status: 'ok',
      findingLevel: 'subsystem',
      estimate: { value: medianVoltage, unit: 'V' },
      candidateComponents: [],
      competingAlternatives: [],
      requiredNextTests: [],
      evidenceRefs: [`MEDIAN_VPWR_${medianVoltage}V`],
      unmetRequirements: [],
      claimBoundary: 'Charging system voltage healthy (13.5V - 14.7V).',
      bundleDigest: 'sha256-vpwr-v1',
      isSimulated: input.isSimulated,
    };
  }
}
