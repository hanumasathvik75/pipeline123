package ai.ignition.android.obd;

import ai.ignition.android.util.RedactionUtil;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Requests vehicle VIN using Mode 09 PID 02, reassembles multi-frame payloads,
 * validates against ISO 3779 (17 alphanumeric chars, strictly excluding I, O, Q), and redacts for logs.
 */
public class VinParser {

    private static final Duration VIN_TIMEOUT = Duration.ofSeconds(4);
    private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");

    public enum VinStatus {
        VALID,
        INCOMPLETE,
        UNSUPPORTED,
        NO_RESPONSE,
        MALFORMED
    }

    public static class VinResult {
        public final String rawVin;
        public final String redactedVin;
        public final String rawResponse;
        public final VinStatus status;
        public final String notes;

        public VinResult(String rawVin, String rawResponse, VinStatus status, String notes) {
            this.rawVin = rawVin;
            this.rawResponse = rawResponse;
            this.status = status;
            this.notes = notes;
            this.redactedVin = (rawVin != null && !rawVin.isBlank()) ? RedactionUtil.redactVin(rawVin) : "UNAVAILABLE";
        }

        @Override
        public String toString() {
            return "VIN Status: " + status + " (" + redactedVin + ") - " + notes;
        }
    }

    /**
     * Executes Mode 09 PID 02 request on the transport and decodes the response.
     */
    public static VinResult requestVin(ObdTransport transport) {
        try {
            String raw = transport.exchange("0902", VIN_TIMEOUT);
            return decodeVin(raw);
        } catch (IOException e) {
            return new VinResult(null, "ERROR", VinStatus.NO_RESPONSE, "IO error requesting VIN: " + e.getMessage());
        }
    }

    /**
     * Decodes raw multi-line or single-line ELM327 response to 0902 into a validated VIN.
     */
    public static VinResult decodeVin(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new VinResult(null, rawResponse, VinStatus.NO_RESPONSE, "Empty response from ECU");
        }

        if (rawResponse.contains("NO DATA") || rawResponse.contains("7F 09") || rawResponse.contains("7F09") || rawResponse.contains("?")) {
            return new VinResult(null, rawResponse, VinStatus.UNSUPPORTED, "Mode 09 PID 02 is not supported by vehicle ECU");
        }

        // Clean out line numbers, headers, and delimiters
        StringBuilder hexPayload = new StringBuilder();
        String[] lines = rawResponse.split("[\r\n]+");

        for (String line : lines) {
            String cleanLine = line.trim().replaceAll("\\s+", "");
            if (cleanLine.isEmpty()) continue;

            // Ignore 3-hex-digit ISO-TP CAN length header lines like "014", "013", "010"
            if (cleanLine.matches("^[0-9A-Fa-f]{3}$") && !cleanLine.startsWith("49")) {
                continue;
            }

            // Remove frame sequence index prefix (e.g. "0:", "1:", "2:") at start of line or after complete bytes
            cleanLine = cleanLine.replaceAll("(?<=^|[\r\n\\s]|[0-9A-Fa-f]{2})[0-9A-Fa-f]:", "");

            // If line contains positive response 490201 or 4902, strip up to the data payload
            int headerIdx = cleanLine.indexOf("4902");
            if (headerIdx != -1) {
                int skip = 4;
                if (cleanLine.startsWith("490201", headerIdx)) {
                    skip = 6;
                }
                cleanLine = cleanLine.substring(headerIdx + skip);
            }

            hexPayload.append(cleanLine);
        }

        String hex = hexPayload.toString().replaceAll("[^0-9A-Fa-f]", "");

        // Convert hex pairs into ASCII characters
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i + 2 <= hex.length(); i += 2) {
            try {
                int charCode = Integer.parseInt(hex.substring(i, i + 2), 16);
                if (charCode >= 32 && charCode <= 126) {
                    ascii.append((char) charCode);
                }
            } catch (NumberFormatException ignored) {}
        }

        String candidate = ascii.toString().trim().toUpperCase(Locale.ROOT);

        // Find 17-character sequence matching ISO 3779
        if (candidate.length() >= 17) {
            for (int i = 0; i <= candidate.length() - 17; i++) {
                String sub = candidate.substring(i, i + 17);
                if (VIN_PATTERN.matcher(sub).matches()) {
                    return new VinResult(sub, rawResponse, VinStatus.VALID, "Valid 17-character ISO 3779 VIN confirmed");
                }
            }

            // Check if failure is due to forbidden letters I, O, Q
            if (candidate.length() == 17 && (candidate.contains("I") || candidate.contains("O") || candidate.contains("Q"))) {
                return new VinResult(candidate, rawResponse, VinStatus.MALFORMED,
                        "VIN contains forbidden characters per ISO 3779 (letters I, O, or Q are illegal)");
            }

            return new VinResult(candidate, rawResponse, VinStatus.MALFORMED,
                    "Decoded string of length " + candidate.length() + " does not meet ISO 3779 alphanumeric criteria");
        }

        if (!candidate.isEmpty()) {
            return new VinResult(candidate, rawResponse, VinStatus.INCOMPLETE,
                    "Incomplete VIN payload: expected 17 characters, found " + candidate.length() + " ('" + candidate + "')");
        }

        return new VinResult(null, rawResponse, VinStatus.MALFORMED,
                "No printable VIN payload decoded from raw response: " + rawResponse);
    }
}
