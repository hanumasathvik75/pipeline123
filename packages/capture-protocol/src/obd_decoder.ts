import { DecodedObservation } from '@ignitionai/contracts';
import { validateDiagnosticRequest } from './allowlist';

export interface RawFrameInput {
  ecuId: string;
  service: number;
  pid: number;
  payloadBytes: number[];
  monotonicStartMs: number;
  monotonicEndMs: number;
  utcAnchorIso: string;
  isSimulated?: boolean;
}

export function decodeStandardPid(input: RawFrameInput, sequence: number): DecodedObservation {
  const { ecuId, service, pid, payloadBytes, monotonicStartMs, monotonicEndMs, utcAnchorIso, isSimulated } = input;

  const validation = validateDiagnosticRequest(service, pid);
  if (!validation.isAllowed) {
    throw new Error(`Security Exception: Prohibited diagnostic request blocked. ${validation.reason}`);
  }

  const rawHex = payloadBytes.map(b => b.toString(16).padStart(2, '0')).join('');

  if (service !== 0x01) {
    // Basic fallback for non-01 services
    return {
      sequence,
      ecuId,
      service,
      pid,
      signalId: `SVC_${service.toString(16).toUpperCase()}_PID_${pid.toString(16).toUpperCase()}`,
      value: 0,
      unit: 'raw',
      monotonicStartMs,
      monotonicEndMs,
      utcAnchorIso,
      rawHex,
      isValid: true,
      isSimulated: isSimulated ?? false,
    };
  }

  const A = payloadBytes[0] ?? 0;
  const B = payloadBytes[1] ?? 0;

  switch (pid) {
    case 0x05: // Engine Coolant Temperature
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: 'ECT',
        value: A - 40,
        unit: 'degC',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: payloadBytes.length >= 1,
        isSimulated: isSimulated ?? false,
      };

    case 0x06: // Short Term Fuel Trim - Bank 1
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: 'STFT1',
        value: Number(((A / 1.28) - 100).toFixed(2)),
        unit: '%',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: payloadBytes.length >= 1,
        isSimulated: isSimulated ?? false,
      };

    case 0x07: // Long Term Fuel Trim - Bank 1
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: 'LTFT1',
        value: Number(((A / 1.28) - 100).toFixed(2)),
        unit: '%',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: payloadBytes.length >= 1,
        isSimulated: isSimulated ?? false,
      };

    case 0x0B: // Intake Manifold Absolute Pressure
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: 'MAP',
        value: A,
        unit: 'kPa',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: payloadBytes.length >= 1,
        isSimulated: isSimulated ?? false,
      };

    case 0x0C: // Engine RPM
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: 'RPM',
        value: Number((((A * 256) + B) / 4).toFixed(1)),
        unit: 'rpm',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: payloadBytes.length >= 2,
        isSimulated: isSimulated ?? false,
      };

    case 0x0D: // Vehicle Speed
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: 'VSS',
        value: A,
        unit: 'km/h',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: payloadBytes.length >= 1,
        isSimulated: isSimulated ?? false,
      };

    case 0x0E: // Timing Advance
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: 'SPARK',
        value: Number(((A / 2) - 64).toFixed(1)),
        unit: 'deg',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: payloadBytes.length >= 1,
        isSimulated: isSimulated ?? false,
      };

    case 0x0F: // Intake Air Temperature
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: 'IAT',
        value: A - 40,
        unit: 'degC',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: payloadBytes.length >= 1,
        isSimulated: isSimulated ?? false,
      };

    case 0x11: // Throttle Position
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: 'TP',
        value: Number(((A * 100) / 255).toFixed(1)),
        unit: '%',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: payloadBytes.length >= 1,
        isSimulated: isSimulated ?? false,
      };

    case 0x42: // Control Module Voltage (ECU / Bus Voltage)
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: 'VPWR',
        value: Number((((A * 256) + B) / 1000).toFixed(2)),
        unit: 'V',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: payloadBytes.length >= 2,
        isSimulated: isSimulated ?? false,
      };

    default:
      return {
        sequence,
        ecuId,
        service,
        pid,
        signalId: `PID_${pid.toString(16).toUpperCase()}`,
        value: A,
        unit: 'raw',
        monotonicStartMs,
        monotonicEndMs,
        utcAnchorIso,
        rawHex,
        isValid: true,
        isSimulated: isSimulated ?? false,
      };
  }
}
