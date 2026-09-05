import assert from 'node:assert';
import fs from 'node:fs';
import path from 'node:path';
import { generateScenario } from '../simulator/software/src/scenario_generator';
import { decodeStandardPid } from '../packages/capture-protocol/src';
import { createCaptureChunks } from '../packages/sync/src';
import { IngestionService } from '../apps/api/src';
import { generateAdvisoryReport, exportReportToCSV } from '../apps/worker/src';

console.log('--- STARTING PHASE 2 & PHASE 3 AUTOMATED VERIFICATION ---');

// ==========================================
// 1. Phase 2 Verification: Hardware & Bench Peer
// ==========================================
console.log('1. Verifying Phase 2 Hardware & Bench Peer Deliverables...');

const pinoutPath = path.resolve('hardware/dongle/pinout_and_protection_spec.md');
assert.ok(fs.existsSync(pinoutPath), 'Hardware pinout spec must exist');
const pinoutContent = fs.readFileSync(pinoutPath, 'utf8');
assert.ok(pinoutContent.includes('TCAN1042HGV-Q1'), 'Spec must reference AEC-Q100 CAN transceiver');
assert.ok(pinoutContent.includes('LM5164-Q1'), 'Spec must reference wide-input buck regulator');
assert.ok(pinoutContent.includes('SMBJ28CA'), 'Spec must specify TVS diode clamp');

const bomPath = path.resolve('hardware/dongle/BOM.md');
assert.ok(fs.existsSync(bomPath), 'BOM must exist');
const bomContent = fs.readFileSync(bomPath, 'utf8');
assert.ok(bomContent.includes('INR 8,000'), 'BOM must reflect the approved INR 8,000 provisional budget');

const benchPeerPath = path.resolve('simulator/bench-peer/src/main.cpp');
assert.ok(fs.existsSync(benchPeerPath), 'Bench peer C++ firmware must exist');
const benchPeerContent = fs.readFileSync(benchPeerPath, 'utf8');
assert.ok(benchPeerContent.includes('TWAI_TIMING_CONFIG_500KBITS'), 'Bench peer must configure 500kbps CAN');
assert.ok(benchPeerContent.includes('0x04'), 'Bench peer must handle and reject Service 0x04 clear codes');
console.log('  -> Phase 2 Hardware Specifications & Bench Peer Firmware Verified.');

// ==========================================
// 2. Phase 3 Verification: Outbox & Chunking
// ==========================================
console.log('2. Verifying Cryptographic Outbox & Chunking...');

const scenario = generateScenario('air_intake_leak');
const decodedObs = scenario.rawFrames.map((f, i) => decodeStandardPid(f, i + 1));

const { chunks, manifest } = createCaptureChunks(decodedObs, {
  captureId: 'cap-bench-test-001',
  bootId: 'dongle-sn-98214',
  tenantId: 'tenant-delhi-01',
  siteId: 'site-south-ex',
  bayId: 'bay-3',
  operatorId: 'tech-shashank',
  vehicleProfileId: 'VP-MS-K12M',
  isSimulated: true,
  maxRecordsPerChunk: 300,
});

assert.ok(chunks.length > 1, 'Should create multiple chunks for 3600 frames');
assert.strictEqual(manifest.totalChunks, chunks.length);
assert.strictEqual(manifest.isSimulated, true);
assert.strictEqual(manifest.watermark, 'SIMULATED / NOT VALIDATED');
console.log(`  -> Created ${chunks.length} chunks with valid SHA-256 digests.`);

// ==========================================
// 3. Phase 3 Verification: Cloud Ingestion API
// ==========================================
console.log('3. Verifying Cloud Ingestion API & Idempotency...');

const api = new IngestionService();
api.initCapture({
  captureId: manifest.captureId,
  tenantId: manifest.tenantId,
  siteId: manifest.siteId,
  bayId: manifest.bayId,
  operatorId: manifest.operatorId,
  vehicleProfileId: manifest.vehicleProfileId,
  isSimulated: true,
});

// Upload chunks sequentially
for (const chunk of chunks) {
  const res = api.uploadChunk(manifest.captureId, chunk);
  assert.strictEqual(res.status, 'acknowledged');
}

// Test Idempotent Retry: Uploading chunk 1 again must succeed without error
const retryRes = api.uploadChunk(manifest.captureId, chunks[0]);
assert.strictEqual(retryRes.status, 'acknowledged');
assert.ok(retryRes.message?.includes('idempotently'));

// Test Corrupted Payload Check: Chunk with tampered bytes must be quarantined
const tamperedChunk = {
  ...chunks[1],
  ordinal: 999, // New ordinal
  payloadBase64: Buffer.from('corrupted_payload').toString('base64'), // Changed payload without changing hash
};
const tamperRes = api.uploadChunk(manifest.captureId, tamperedChunk);
assert.strictEqual(tamperRes.status, 'quarantined');
assert.ok(tamperRes.message?.includes('DigestMismatch'));

// Finalize capture
const finalizeRes = api.finalizeCapture(manifest.captureId, manifest);
assert.strictEqual(finalizeRes.status, 'finalized');
console.log('  -> Ingestion Idempotency, Tamper Detection, & Finalization Verified.');

// ==========================================
// 4. Phase 3 Verification: Worker Replay & Facts-Only Report
// ==========================================
console.log('4. Verifying Worker Reassembly & Facts-Only Report Generation...');

const reassembledObs = api.reassembleObservations(manifest.captureId);
assert.strictEqual(reassembledObs.length, decodedObs.length);

const captureView = {
  captureId: manifest.captureId,
  vehicleProfileId: manifest.vehicleProfileId,
  isSimulated: true,
  totalDurationMs: scenario.totalDurationMs,
  observations: reassembledObs,
  stateTimeline: [],
  dtcList: [],
};

const report = generateAdvisoryReport(captureView, manifest);
assert.strictEqual(report.watermark, 'SIMULATED / NOT VALIDATED');
assert.ok(report.disclaimer.includes('ADVISORY REPORT ONLY'));
assert.strictEqual(report.provenance.tenantId, 'tenant-delhi-01');
assert.strictEqual(report.provenance.manifestDigest, manifest.manifestSha256);

// Verify candidate findings for air_intake_leak
const leakFinding = report.findings.level2_candidates.find(c => c.moduleId === 'fuel_control_petrol');
assert.ok(leakFinding, 'Report must contain fuel control candidate finding');
assert.ok(leakFinding.candidateComponents.includes('intake_manifold_gasket'));
assert.ok(leakFinding.discriminatingTests[0].includes('smoke test'));

// Verify CSV export
const csvOutput = exportReportToCSV(report);
assert.ok(csvOutput.includes('IGNITIONAI ADVISORY DIAGNOSTIC REPORT'));
assert.ok(csvOutput.includes('SIMULATED / NOT VALIDATED'));
assert.ok(csvOutput.includes('intake_manifold_gasket'));

console.log('  -> Report Generator, Evidence Leveling, & CSV Export Verified.');
console.log('\n*** ALL PHASE 2 & PHASE 3 VERIFICATIONS PASSED SUCCESSFULLY! ***');
