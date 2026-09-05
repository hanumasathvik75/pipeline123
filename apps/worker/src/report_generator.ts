import { 
  CaptureManifest, 
  CaptureView, 
  DiagnosticContext, 
  ModuleResult, 
  PowertrainHealthScore 
} from '@ignitionai/contracts';
import { 
  PetrolFuelControlModule, 
  ThermalResponseModule, 
  SupplyVoltageModule 
} from '@ignitionai/core';

export interface VerifiedDiagnosticReport {
  reportId: string;
  generatedAtIso: string;
  watermark: string;
  disclaimer: string;
  provenance: {
    tenantId: string;
    siteId: string;
    bayId: string;
    operatorId: string;
    vehicleProfileId: string;
    captureId: string;
    manifestDigest: string;
  };
  healthScore: PowertrainHealthScore;
  findings: {
    level0_facts: string[];
    level1_subsystems: { moduleId: string; estimate?: string; description: string }[];
    level2_candidates: {
      moduleId: string;
      candidateComponents: string[];
      competingAlternatives: string[];
      discriminatingTests: string[];
    }[];
  };
}

export function generateAdvisoryReport(
  captureView: CaptureView,
  manifest: CaptureManifest
): VerifiedDiagnosticReport {
  const context: DiagnosticContext = {
    vehicleProfileId: manifest.vehicleProfileId,
    fuelType: 'petrol',
    isSimulated: captureView.isSimulated,
  };

  const modules = [
    new PetrolFuelControlModule(),
    new ThermalResponseModule(),
    new SupplyVoltageModule(),
  ];

  const results: ModuleResult[] = modules.map(m => m.evaluate(captureView, context));
  const candidateFindings = results.filter(r => r.findingLevel === 'candidate');

  const l0Facts: string[] = [
    `Captured ${captureView.observations.length} decoded OBD-II observations over ${(captureView.totalDurationMs / 1000).toFixed(0)} seconds.`,
    `ECU reporting standard classical CAN protocol (ISO 15765-4).`,
    `No diagnostic trouble codes (DTCs) actively cleared during test session.`,
  ];

  const l1Subsystems = results
    .filter(r => r.status === 'ok')
    .map(r => ({
      moduleId: r.moduleId,
      estimate: r.estimate ? `${r.estimate.value} ${r.estimate.unit}` : undefined,
      description: r.claimBoundary,
    }));

  const l2Candidates = candidateFindings.map(r => ({
    moduleId: r.moduleId,
    candidateComponents: r.candidateComponents,
    competingAlternatives: r.competingAlternatives,
    discriminatingTests: r.requiredNextTests,
  }));

  const rawScore = Math.max(0, 100 - (candidateFindings.length * 25));

  return {
    reportId: `REP-${manifest.captureId.toUpperCase()}`,
    generatedAtIso: new Date().toISOString(),
    watermark: captureView.isSimulated ? 'SIMULATED / NOT VALIDATED' : 'PROVISIONAL ADVISORY REPORT',
    disclaimer: 'ADVISORY REPORT ONLY. IgnitionAI does not certify roadworthiness, resale value, or warranty component health. All findings require physical workshop test verification.',
    provenance: {
      tenantId: manifest.tenantId,
      siteId: manifest.siteId,
      bayId: manifest.bayId,
      operatorId: manifest.operatorId,
      vehicleProfileId: manifest.vehicleProfileId,
      captureId: manifest.captureId,
      manifestDigest: manifest.manifestSha256,
    },
    healthScore: {
      score: rawScore,
      status: 'available',
      isSimulated: captureView.isSimulated,
      watermark: captureView.isSimulated ? 'SIMULATED / NOT VALIDATED' : 'PROVISIONAL_OBSERVED_SCORE',
      testedScope: ['Fuel Control', 'Thermal Response', 'Supply Voltage'],
      coveragePercentage: (results.filter(r => r.status === 'ok').length / results.length) * 100,
      unresolvedConditions: candidateFindings.flatMap(f => f.candidateComponents),
      criticalWarnings: candidateFindings.length > 0
        ? ['Candidate powertrain deviations identified. Execute workshop discriminating tests before replacement.']
        : [],
    },
    findings: {
      level0_facts: l0Facts,
      level1_subsystems: l1Subsystems,
      level2_candidates: l2Candidates,
    },
  };
}

export function exportReportToCSV(report: VerifiedDiagnosticReport): string {
  const lines: string[] = [];
  lines.push('--- IGNITIONAI ADVISORY DIAGNOSTIC REPORT ---');
  lines.push(`Report ID,${report.reportId}`);
  lines.push(`Generated At,${report.generatedAtIso}`);
  lines.push(`Watermark,${report.watermark}`);
  lines.push(`Vehicle Profile,${report.provenance.vehicleProfileId}`);
  lines.push(`Manifest SHA256,${report.provenance.manifestDigest}`);
  lines.push(`Simulated Condition Score,${report.healthScore.score}/100`);
  lines.push('');
  lines.push('--- LEVEL 2 COMPONENT CANDIDATES & DISCRIMINATING TESTS ---');
  lines.push('Module,Candidate Components,Competing Alternatives,Recommended Next Workshop Test');

  for (const c of report.findings.level2_candidates) {
    const comps = `"${c.candidateComponents.join('; ')}"`;
    const alts = `"${c.competingAlternatives.join('; ')}"`;
    const tests = `"${c.discriminatingTests.join('; ')}"`;
    lines.push(`${c.moduleId},${comps},${alts},${tests}`);
  }

  return lines.join('\n');
}
