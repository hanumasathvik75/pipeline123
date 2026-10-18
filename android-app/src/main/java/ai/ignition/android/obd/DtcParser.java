package ai.ignition.android.obd;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Executes read-only queries for Diagnostic Trouble Codes (DTCs):
 * - Mode 03: Stored/confirmed emission-related DTCs
 * - Mode 07: Pending DTCs
 * - Mode 0A: Permanent DTCs
 *
 * Enforces safety guardrail: Mode 04 (clear codes) and Mode 08 (control) are strictly prohibited.
 */
public class DtcParser {

    private static final Duration DTC_TIMEOUT = Duration.ofSeconds(4);

    public enum DtcResultStatus {
        NO_CODES_DETECTED,
        CODES_CONFIRMED,
        UNSUPPORTED_MODE,
        MALFORMED_RESPONSE,
        COMMUNICATION_ERROR
    }

    public static class DtcReport {
        public final String mode;
        public final List<String> codes;
        public final String rawResponse;
        public final DtcResultStatus status;
        public final String notes;

        public DtcReport(String mode, List<String> codes, String rawResponse, DtcResultStatus status, String notes) {
            this.mode = mode;
            this.codes = Collections.unmodifiableList(codes);
            this.rawResponse = rawResponse;
            this.status = status;
            this.notes = notes;
        }

        @Override
        public String toString() {
            return "Mode " + mode + ": " + status + " -> " + codes + " [Raw='" + rawResponse + "']";
        }
    }

    /**
     * Executes queries for all three standard read-only DTC modes: 03, 07, and 0A.
     */
    public static List<DtcReport> readAllDtcModes(ObdTransport transport) {
        List<DtcReport> results = new ArrayList<>();
        results.add(readMode(transport, "03", "43", "Stored Confirmed"));
        results.add(readMode(transport, "07", "47", "Pending"));
        results.add(readMode(transport, "0A", "4A", "Permanent"));
        return results;
    }

    public static DtcReport readMode(ObdTransport transport, String mode, String expectedHeader, String label) {
        try {
            String raw = transport.exchange(mode, DTC_TIMEOUT);
            return decodeDtcResponse(mode, expectedHeader, raw);
        } catch (IOException e) {
            return new DtcReport(mode, List.of(), "ERROR", DtcResultStatus.COMMUNICATION_ERROR,
                    label + " DTC query failed: " + e.getMessage());
        }
    }

    /**
     * Decodes an ELM327 hex response for a DTC mode.
     */
    public static DtcReport decodeDtcResponse(String mode, String expectedHeader, String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new DtcReport(mode, List.of(), rawResponse, DtcResultStatus.COMMUNICATION_ERROR, "Empty response");
        }

        if (rawResponse.contains("NO DATA")
                || rawResponse.contains("43 00 00") || rawResponse.contains("430000") || rawResponse.contains("43 00")
                || rawResponse.contains("47 00 00") || rawResponse.contains("470000") || rawResponse.contains("47 00")
                || rawResponse.contains("4A 00 00") || rawResponse.contains("4A0000") || rawResponse.contains("4A 00")) {
            return new DtcReport(mode, List.of(), rawResponse, DtcResultStatus.NO_CODES_DETECTED, "No DTCs present in Mode " + mode + " (clean)");
        }

        if (rawResponse.contains("7F " + mode) || rawResponse.contains("7F" + mode) || rawResponse.contains("?")) {
            return new DtcReport(mode, List.of(), rawResponse, DtcResultStatus.UNSUPPORTED_MODE, "Mode " + mode + " is not supported by vehicle");
        }

        // Clean out CAN length headers (e.g. "014") and CAN frame sequence numbers (e.g. "0:", "1:", "01:")
        String cleaned = rawResponse.replaceAll("(?m)^[0-9A-Fa-f]{3}$", "")
                                    .replaceAll("(?:^|[\r\n\s]+)[0-9A-Fa-f]{1,2}:", " ");

        String clean = cleaned.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        int headerIdx = clean.indexOf(expectedHeader);
        if (headerIdx == -1) {
            return new DtcReport(mode, List.of(), rawResponse, DtcResultStatus.MALFORMED_RESPONSE,
                    "Expected header " + expectedHeader + " not found in response");
        }

        String payload = clean.substring(headerIdx + expectedHeader.length());
        List<String> codes = new ArrayList<>();

        // If payload is empty or zero-count
        if (payload.isEmpty() || payload.equals("00") || payload.equals("0000")) {
            return new DtcReport(mode, List.of(), rawResponse, DtcResultStatus.NO_CODES_DETECTED, "No DTCs reported");
        }

        // In CAN multi-frame (or responses with count byte), check if first byte indicates DTC count
        boolean isMultiFrame = rawResponse.contains("0:") || rawResponse.contains("1:");
        int maxCodes = -1;
        if (isMultiFrame && payload.length() >= 2) {
            try {
                maxCodes = Integer.parseInt(payload.substring(0, 2), 16);
                payload = payload.substring(2);
            } catch (NumberFormatException ignored) {}
        } else if (payload.length() >= 6 && (payload.length() % 4 == 2)) {
            payload = payload.substring(2);
        }

        for (int i = 0; i + 4 <= payload.length(); i += 4) {
            if (maxCodes >= 0 && codes.size() >= maxCodes) {
                break;
            }
            String hexPair = payload.substring(i, i + 4);
            try {
                int packed = Integer.parseInt(hexPair, 16);
                if (packed == 0) continue; // Skip 0000 padding

                char prefix = "PCBU".charAt((packed >> 14) & 3);
                String code = prefix + String.format(Locale.ROOT, "%04X", packed & 0x3FFF);
                if (!codes.contains(code)) {
                    codes.add(code);
                }
            } catch (NumberFormatException ignored) {}
        }

        if (codes.isEmpty()) {
            return new DtcReport(mode, List.of(), rawResponse, DtcResultStatus.NO_CODES_DETECTED, "Zero active DTCs reported");
        }

        return new DtcReport(mode, codes, rawResponse, DtcResultStatus.CODES_CONFIRMED,
                codes.size() + " code(s) detected in Mode " + mode);
    }
}
