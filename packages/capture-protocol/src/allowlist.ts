/**
 * Vehicle Command Safety & Service Allowlist
 * Section 9.4 of IgnitionAI MVP Implementation Plan
 */

export const ALLOWED_OBD_SERVICES = new Set<number>([
  0x01, // Current powertrain diagnostic data
  0x02, // Freeze frame data
  0x03, // Emission-related confirmed diagnostic trouble codes
  0x06, // On-board monitoring test results for specific monitored systems
  0x07, // Emission-related pending diagnostic trouble codes
  0x09, // Vehicle information (VIN, calibration IDs)
  0x0A, // Permanent diagnostic trouble codes
]);

export const PROHIBITED_OBD_SERVICES = new Set<number>([
  0x04, // Clear diagnostic information (STRICTLY FORBIDDEN)
  0x08, // Control operation of on-board component/system (STRICTLY FORBIDDEN)
  0x10, // Diagnostic session control (UDS)
  0x11, // ECU reset (UDS)
  0x27, // Security access (UDS)
  0x28, // Communication control (UDS)
  0x2E, // Write data by identifier (UDS)
  0x31, // Routine control (UDS)
  0x34, // Request download (UDS)
  0x36, // Transfer data (UDS)
  0x37, // Request transfer exit (UDS)
]);

export interface CommandValidationResult {
  isAllowed: boolean;
  reason?: string;
}

export function validateDiagnosticRequest(service: number, pidOrDid?: number): CommandValidationResult {
  if (PROHIBITED_OBD_SERVICES.has(service)) {
    return {
      isAllowed: false,
      reason: `SECURITY VIOLATION: Service 0x${service.toString(16).toUpperCase()} is strictly prohibited (actuator control, ECU reset, or code clearing).`,
    };
  }

  if (!ALLOWED_OBD_SERVICES.has(service)) {
    return {
      isAllowed: false,
      reason: `UNSUPPORTED: Service 0x${service.toString(16).toUpperCase()} is not in the validated read-only allowlist.`,
    };
  }

  return { isAllowed: true };
}
