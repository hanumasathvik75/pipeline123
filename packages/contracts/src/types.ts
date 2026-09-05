/**
 * IgnitionAI MVP Core Contracts & Schemas
 * Based on IgnitionAI_MVP_Implementation_Plan.md v0.2
 *
 * All contracts are pure TypeScript with zero DOM/Node/Native dependencies.
 */

export type DiagnosticStatus = 'ok' | 'unsupported' | 'insufficient_evidence' | 'invalid_input';

export type FindingLevel = 
  | 'fact'                 // L0: Correctly decoded observation / DTC
  | 'subsystem'            // L1: Qualified operating window + validated residual deviation
  | 'candidate'            // L2: Symptom signature + alternatives + vehicle applicability
  | 'reference_supported'  // L3: Discriminating workshop measurement recorded
  | 'post_repair';         // L4: Objective retest improvement under matched conditions

export type OperatingState = 
  | 'key_on_engine_off'
  | 'idle'
  | 'cruise'
  | 'acceleration'
  | 'deceleration'
  | 'unknown';

export interface SignalRequirement {
  signalId: string;
  minCadenceHz: number;
  maxLatencyMs: number;
  requiredStates?: OperatingState[];
  vehicleScope?: string[];
}

export interface DecodedObservation {
  sequence: number;
  ecuId: string;
  service: number;
  pid: number;
  signalId: string;
  value: number;
  unit: string;
  monotonicStartMs: number;
  monotonicEndMs: number;
  utcAnchorIso: string;
  rawHex: string;
  isValid: boolean;
  isSimulated: boolean;
  uncertainty?: number;
}

export interface CaptureView {
  captureId: string;
  vehicleProfileId: string;
  isSimulated: boolean;
  totalDurationMs: number;
  observations: DecodedObservation[];
  stateTimeline: { state: OperatingState; startMs: number; endMs: number }[];
  dtcList: string[];
}

export interface DiagnosticContext {
  vehicleProfileId: string;
  fuelType: 'petrol' | 'diesel' | 'hybrid';
  referenceTestsRecorded?: Record<string, any>;
  isSimulated: boolean;
}

export interface ModuleEstimate {
  value: number;
  unit: string;
  interval?: [number, number];
}

export interface ModuleResult {
  moduleId: string;
  status: DiagnosticStatus;
  findingLevel?: FindingLevel;
  estimate?: ModuleEstimate;
  candidateComponents: string[];
  competingAlternatives: string[];
  requiredNextTests: string[];
  evidenceRefs: string[];
  unmetRequirements: string[];
  claimBoundary: string;
  bundleDigest: string;
  isSimulated: boolean;
}

export interface DiagnosticModule {
  id: string;
  version: string;
  requirements: SignalRequirement[];
  evaluate(input: CaptureView, context: DiagnosticContext): ModuleResult;
}

export interface CaptureChunk {
  captureId: string;
  ordinal: number;
  bootId: string;
  byteCount: number;
  sha256Digest: string;
  payloadBase64: string;
  isSimulated: boolean;
}

export interface CaptureManifest {
  captureId: string;
  tenantId: string;
  siteId: string;
  bayId: string;
  operatorId: string;
  deviceId: string;
  vehicleVin?: string;
  vehicleProfileId: string;
  startedAtUtc: string;
  completedAtUtc: string;
  totalChunks: number;
  totalBytes: number;
  manifestSha256: string;
  isSimulated: boolean;
  watermark: 'SIMULATED / NOT VALIDATED' | 'VALIDATED_FIELD_CAPTURE';
  gapAccounting: {
    expectedSequences: number;
    receivedSequences: number;
    missingSequenceRanges: [number, number][];
  };
}

export interface PowertrainHealthScore {
  score: number | null; // Null if score unavailable due to unmet requirements or simulation policy
  status: 'available' | 'unavailable';
  reasonUnavailable?: string;
  isSimulated: boolean;
  watermark: string;
  testedScope: string[];
  coveragePercentage: number;
  unresolvedConditions: string[];
  criticalWarnings: string[];
}
