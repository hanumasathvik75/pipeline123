import { CaptureChunk, CaptureManifest, DecodedObservation } from '@ignitionai/contracts';
import { createHash } from 'node:crypto';

export interface IngestionSession {
  captureId: string;
  tenantId: string;
  siteId: string;
  bayId: string;
  operatorId: string;
  vehicleProfileId: string;
  isSimulated: boolean;
  status: 'active' | 'finalized' | 'quarantined';
  uploadedChunks: Map<number, CaptureChunk>;
  manifest?: CaptureManifest;
}

export class IngestionService {
  private sessions = new Map<string, IngestionSession>();

  public initCapture(session: Omit<IngestionSession, 'status' | 'uploadedChunks'>): IngestionSession {
    if (this.sessions.has(session.captureId)) {
      return this.sessions.get(session.captureId)!;
    }

    const newSession: IngestionSession = {
      ...session,
      status: 'active',
      uploadedChunks: new Map(),
    };
    this.sessions.set(session.captureId, newSession);
    return newSession;
  }

  public uploadChunk(captureId: string, chunk: CaptureChunk): { status: 'acknowledged' | 'quarantined'; message?: string } {
    const session = this.sessions.get(captureId);
    if (!session) {
      throw new Error(`InvalidCapture: Capture ${captureId} does not exist or has not been initialized.`);
    }

    if (session.status !== 'active') {
      throw new Error(`IllegalState: Capture ${captureId} is already ${session.status}.`);
    }

    // 1. Verify chunk SHA-256 digest
    const decodedBytes = Buffer.from(chunk.payloadBase64, 'base64').toString('utf8');
    const computedHash = createHash('sha256').update(decodedBytes).digest('hex');

    if (computedHash !== chunk.sha256Digest) {
      session.status = 'quarantined';
      return {
        status: 'quarantined',
        message: `DigestMismatch: Expected ${chunk.sha256Digest} but calculated ${computedHash}. Chunk quarantined.`,
      };
    }

    // 2. Idempotent Handling: If identical ordinal exists with matching hash, acknowledge safely
    const existing = session.uploadedChunks.get(chunk.ordinal);
    if (existing) {
      if (existing.sha256Digest === chunk.sha256Digest) {
        return { status: 'acknowledged', message: 'Duplicate chunk acknowledged idempotently.' };
      } else {
        session.status = 'quarantined';
        return {
          status: 'quarantined',
          message: `ConflictingBytes: Chunk ${chunk.ordinal} already committed with different digest. Quarantined.`,
        };
      }
    }

    session.uploadedChunks.set(chunk.ordinal, chunk);
    return { status: 'acknowledged' };
  }

  public finalizeCapture(captureId: string, manifest: CaptureManifest): { status: 'finalized' | 'rejected'; reason?: string } {
    const session = this.sessions.get(captureId);
    if (!session) {
      return { status: 'rejected', reason: 'Capture session not found.' };
    }

    if (session.uploadedChunks.size !== manifest.totalChunks) {
      return {
        status: 'rejected',
        reason: `MissingChunks: Manifest expects ${manifest.totalChunks} chunks but received ${session.uploadedChunks.size}.`,
      };
    }

    // Audit chunk continuity and gaps
    for (let ord = 1; ord <= manifest.totalChunks; ord++) {
      if (!session.uploadedChunks.has(ord)) {
        return {
          status: 'rejected',
          reason: `GapDetected: Ordinal ${ord} is missing from capture stream.`,
        };
      }
    }

    session.status = 'finalized';
    session.manifest = manifest;
    return { status: 'finalized' };
  }

  public getSession(captureId: string): IngestionSession | undefined {
    return this.sessions.get(captureId);
  }

  public reassembleObservations(captureId: string): DecodedObservation[] {
    const session = this.sessions.get(captureId);
    if (!session || session.status !== 'finalized') {
      throw new Error(`Cannot reassemble observations: capture ${captureId} is not finalized.`);
    }

    const observations: DecodedObservation[] = [];
    for (let ord = 1; ord <= session.uploadedChunks.size; ord++) {
      const chunk = session.uploadedChunks.get(ord)!;
      const jsonStr = Buffer.from(chunk.payloadBase64, 'base64').toString('utf8');
      const obsList: DecodedObservation[] = JSON.parse(jsonStr);
      observations.push(...obsList);
    }

    return observations;
  }
}
