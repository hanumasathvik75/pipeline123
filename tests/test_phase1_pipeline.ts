import assert from 'node:assert';
import { decodeStandardPid, validateDiagnosticRequest } from '../packages/capture-protocol/src';
import { 
  computeCombinedFuelTrim, 
  computeMedian, 
  computeMAD, 
  VehicleStateMachine 
} from '../packages/core/src';
import { runSimulationReplay } from '../simulator/software/src/replay_runner';

console.log('--- STARTING PHASE 1 AUTOMATED VERIFICATION ---');

// 1. Test Command Allowlist & Security
console.log('1. Testing Command Allowlist & Security Restrictions...');
const allowlistRead = validateDiagnosticRequest(0x01, 0x0C);
assert.strictEqual(allowlistRead.isAllowed, true, 'Service 0x01 should be allowed');

const prohibitedClear = validateDiagnosticRequest(0x04);
assert.strictEqual(prohibitedClear.isAllowed, false, 'Service 0x04 (Clear DTCs) must be strictly prohibited');
assert.ok(prohibitedClear.reason?.includes('SECURITY VIOLATION'), 'Should report security violation');

const prohibitedControl = validateDiagnosticRequest(0x08);
assert.strictEqual(prohibitedControl.isAllowed, false, 'Service 0x08 (Actuator Control) must be strictly prohibited');
console.log('  -> Allowlist Security Verified.');

// 2. Test Byte-Level Standard OBD Decoders
console.log('2. Testing Byte-Level Standard OBD Decoders...');
const now = new Date().toISOString();

// RPM: ((A * 256) + B) / 4 -> A=0x0B, B=0xB8 -> (2816 + 184) / 4 = 3000 / 4 = 750 rpm
const rpmObs = decodeStandardPid({
  ecuId: '0x7E8',
  service: 0x01,
  pid: 0x0C,
  payloadBytes: [0x0B, 0xB8],
  monotonicStartMs: 100,
  monotonicEndMs: 110,
  utcAnchorIso: now,
}, 1);
assert.strictEqual(rpmObs.signalId, 'RPM');
assert.strictEqual(rpmObs.value, 750);
assert.strictEqual(rpmObs.unit, 'rpm');

// ECT: A - 40 -> A=125 -> 85 degC
const ectObs = decodeStandardPid({
  ecuId: '0x7E8',
  service: 0x01,
  pid: 0x05,
  payloadBytes: [125],
  monotonicStartMs: 120,
  monotonicEndMs: 130,
  utcAnchorIso: now,
}, 2);
assert.strictEqual(ectObs.signalId, 'ECT');
assert.strictEqual(ectObs.value, 85);
assert.strictEqual(ectObs.unit, 'degC');

// STFT: (A / 1.28) - 100 -> A=128 -> (128 / 1.28) - 100 = 0%
const stftObs = decodeStandardPid({
  ecuId: '0x7E8',
  service: 0x01,
  pid: 0x06,
  payloadBytes: [128],
  monotonicStartMs: 140,
  monotonicEndMs: 150,
  utcAnchorIso: now,
}, 3);
assert.strictEqual(stftObs.signalId, 'STFT1');
assert.strictEqual(stftObs.value, 0);

// VPWR: ((A * 256) + B) / 1000 -> A=0x37, B=0x14 -> (14080 + 20) / 1000 = 14.10 V
const vpwrObs = decodeStandardPid({
  ecuId: '0x7E8',
  service: 0x01,
  pid: 0x42,
  payloadBytes: [0x37, 0x14],
  monotonicStartMs: 160,
  monotonicEndMs: 170,
  utcAnchorIso: now,
}, 4);
assert.strictEqual(vpwrObs.signalId, 'VPWR');
assert.strictEqual(vpwrObs.value, 14.1);
console.log('  -> Byte Decoders Verified.');

// 3. Test Robust Metrics & State Machine
console.log('3. Testing Robust Metrics & State Machine...');
// STFT = 5%, LTFT = 10% -> 100 * ((1.05 * 1.10) - 1) = 15.5%
const combinedTrim = computeCombinedFuelTrim(5.0, 10.0);
assert.strictEqual(combinedTrim, 15.5);

const med = computeMedian([1, 2, 10, 12, 14]);
assert.strictEqual(med, 10);
const mad = computeMAD([1, 2, 10, 12, 14], med);
assert.strictEqual(mad, 4);

const sm = new VehicleStateMachine();
assert.strictEqual(sm.classify({ rpm: 0, speed: 0, throttle: 0, dtSeconds: 1.0 }), 'key_on_engine_off');
assert.strictEqual(sm.classify({ rpm: 750, speed: 0, throttle: 13, dtSeconds: 1.5 }), 'idle');
assert.strictEqual(sm.classify({ rpm: 2200, speed: 45, throttle: 20, dtSeconds: 1.5 }), 'cruise');
console.log('  -> Robust Metrics & State Machine Verified.');

// 4. Test End-to-End Seeded Simulator Replays
console.log('4. Testing Seeded Simulator Replays & Watermarking...');

// 4a. Healthy baseline
const healthyReport = runSimulationReplay('healthy_baseline');
assert.strictEqual(healthyReport.watermark, 'SIMULATED / NOT VALIDATED');
assert.strictEqual(healthyReport.healthScore.score, 100);
assert.strictEqual(healthyReport.healthScore.unresolvedConditions.length, 0);

// 4b. Air intake leak
const leakReport = runSimulationReplay('air_intake_leak');
assert.strictEqual(leakReport.watermark, 'SIMULATED / NOT VALIDATED');
assert.ok(leakReport.healthScore.score! < 100, 'Health score should be penalized for leak');
assert.ok(leakReport.healthScore.unresolvedConditions.includes('intake_manifold_gasket'));

// 4c. Thermostat stuck open
const thermoReport = runSimulationReplay('coolant_thermostat_stuck_open');
assert.ok(thermoReport.healthScore.unresolvedConditions.includes('engine_coolant_thermostat'));

// 4d. Alternator voltage sag
const voltReport = runSimulationReplay('alternator_voltage_sag');
assert.ok(voltReport.healthScore.unresolvedConditions.includes('alternator_assembly'));

console.log('  -> Seeded Replays & Watermarks Verified.');
console.log('\n*** ALL PHASE 0 & PHASE 1 VERIFICATIONS PASSED SUCCESSFULLY! ***');
