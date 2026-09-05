import { CaptureChunk, CaptureManifest, DecodedObservation } from '@ignitionai/contracts';
import { createHash } from 'node:crypto';

export interface ChunkingOptions {
  captureId: string;
  bootId: string;
  tenantId: string;
  siteId: string;
  bayId: string;
  operatorId: string;
  vehicleProfileId: string;
  isSimulated: boolean;
  maxRecordsPerChunk?: number;
}

export function createCaptureChunks(
  observations: DecodedObservation[],
  options: ChunkingOptions
): { chunks: CaptureChunk[]; manifest: CaptureManifest } {
  const {
    captureId,
    bootId,
    tenantId,
    siteId,
    bayId,
    operatorId,
    vehicleProfileId,
    isSimulated,
    maxRecordsPerChunk = 200,
  } = options;

  const chunks: CaptureChunk[] = [];
  let totalBytes = 0;
  let ordinal = 1;

  for (let i = 0; i < observations.length; i += maxRecordsPerChunk) {
    const slice = observations.slice(i, i + maxRecordsPerChunk);
    const jsonStr = JSON.stringify(slice);
    const byteCount = Buffer.byteLength(jsonStr, 'utf8');
    totalBytes += byteCount;

    const hash = createHash('sha256').update(jsonStr).digest('hex');
    const base64 = Buffer.from(jsonStr, 'utf8').toString('base64');

    chunks.push({
      captureId,
      ordinal: ordinal++,
      bootId,
      byteCount,
      sha256Digest: hash,
      payloadBase64: base64,
      isSimulated,
    });
  }

  // Calculate whole manifest hash
  const manifestData = chunks.map(c => `${c.ordinal}:${c.sha256Digest}`).join('|');
  const manifestHash = createHash('sha256').update(manifestData).digest('hex');

  const now = new Date().toISOString();
  const manifest: CaptureManifest = {
    captureId,
    tenantId,
    siteId,
    bayId,
    operatorId,
    deviceId: bootId,
    vehicleProfileId,
    startedAtUtc: observations[0]?.utcAnchorIso || now,
    completedAtUtc: now,
    totalChunks: chunks.length,
    totalBytes,
    manifestSha256: manifestHash,
    isSimulated,
    watermark: isSimulated ? 'SIMULATED / NOT VALIDATED' : 'VALIDATED_FIELD_CAPTURE',
    gapAccounting: {
      expectedSequences: observations.length,
      receivedSequences: observations.length,
      missingSequenceRanges: [],
    },
  };

  return { chunks, manifest };
}
