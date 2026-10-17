package ai.ignition.android.obd;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Requests and decodes supported live Mode 01 diagnostic signals (Engine RPM, Coolant Temp, Speed, Load)
 * with SAE J1979 standard formulas and plausibility validation.
 */
public class LivePidDecoder {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    public static class PidReading {
        public final String pid;
        public final String signalName;
        public final Double value;
        public final String unit;
        public final String rawResponse;
        public final boolean valid;
        public final String status;
        public final Instant timestamp;

        public PidReading(String pid, String signalName, Double value, String unit,
                          String rawResponse, boolean valid, String status) {
            this.pid = pid;
            this.signalName = signalName;
            this.value = value;
            this.unit = unit;
            this.rawResponse = rawResponse;
            this.valid = valid;
            this.status = status;
            this.timestamp = Instant.now();
        }

        @Override
        public String toString() {
            if (!valid) return signalName + " (" + pid + "): " + status + " [Raw='" + rawResponse + "']";
            return String.format(Locale.ROOT, "%s (%s): %.1f %s [Raw='%s']", signalName, pid, value, unit, rawResponse);
        }
    }

    /**
     * Queries and decodes Engine RPM (PID 010C).
     * Standard formula: ((A * 256) + B) / 4 rpm.
     */
    public static PidReading readRpm(ObdTransport transport) {
        return queryPid(transport, "010C", "Engine RPM", "rpm", (clean, idx) -> {
            if (clean.length() < idx + 8) return null;
            int a = Integer.parseInt(clean.substring(idx + 4, idx + 6), 16);
            int b = Integer.parseInt(clean.substring(idx + 6, idx + 8), 16);
            double rpm = ((a * 256.0) + b) / 4.0;
            return rpm >= 0.0 && rpm <= 12000.0 ? rpm : null;
        });
    }

    /**
     * Queries and decodes Engine Coolant Temperature (PID 0105).
     * Standard formula: A - 40 °C.
     */
    public static PidReading readCoolant(ObdTransport transport) {
        return queryPid(transport, "0105", "Coolant Temperature", "°C", (clean, idx) -> {
            if (clean.length() < idx + 6) return null;
            int a = Integer.parseInt(clean.substring(idx + 4, idx + 6), 16);
            double temp = a - 40.0;
            return temp >= -40.0 && temp <= 160.0 ? temp : null;
        });
    }

    /**
     * Queries and decodes Vehicle Speed (PID 010D).
     * Standard formula: A km/h.
     */
    public static PidReading readSpeed(ObdTransport transport) {
        return queryPid(transport, "010D", "Vehicle Speed", "km/h", (clean, idx) -> {
            if (clean.length() < idx + 6) return null;
            double speed = Integer.parseInt(clean.substring(idx + 4, idx + 6), 16);
            return speed >= 0.0 && speed <= 300.0 ? speed : null;
        });
    }

    /**
     * Queries and decodes Calculated Engine Load (PID 0104).
     * Standard formula: (A * 100) / 255 %.
     */
    public static PidReading readEngineLoad(ObdTransport transport) {
        return queryPid(transport, "0104", "Calculated Engine Load", "%", (clean, idx) -> {
            if (clean.length() < idx + 6) return null;
            int a = Integer.parseInt(clean.substring(idx + 4, idx + 6), 16);
            double load = (a * 100.0) / 255.0;
            return load >= 0.0 && load <= 100.0 ? load : null;
        });
    }

    @FunctionalInterface
    public interface PayloadParser {
        Double parse(String cleanHex, int headerIndex) throws Exception;
    }

    public static PidReading decodeRaw(String rawResponse, String pid, String signalName, String unit, PayloadParser parser) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new PidReading(pid, signalName, null, unit, rawResponse, false, "EMPTY_RESPONSE");
        }
        if (rawResponse.contains("NO DATA") || rawResponse.contains("UNABLE") || rawResponse.contains("?")) {
            return new PidReading(pid, signalName, null, unit, rawResponse, false, "NO_DATA_OR_ERROR");
        }

        String clean = rawResponse.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        String expectedHeader = "41" + pid.replace("01", "");
        int idx = clean.indexOf(expectedHeader);
        if (idx == -1) {
            return new PidReading(pid, signalName, null, unit, rawResponse, false, "MISSING_HEADER_" + expectedHeader);
        }

        try {
            Double val = parser.parse(clean, idx);
            if (val == null) {
                return new PidReading(pid, signalName, null, unit, rawResponse, false, "IMPLAUSIBLE_OR_SHORT_VALUE");
            }
            return new PidReading(pid, signalName, val, unit, rawResponse, true, "VALID");
        } catch (Exception e) {
            return new PidReading(pid, signalName, null, unit, rawResponse, false, "PARSE_ERROR: " + e.getMessage());
        }
    }

    private static PidReading queryPid(ObdTransport transport, String pid, String name, String unit, PayloadParser parser) {
        try {
            String raw = transport.exchange(pid, TIMEOUT);
            return decodeRaw(raw, pid, name, unit, parser);
        } catch (IOException e) {
            return new PidReading(pid, name, null, unit, "ERROR", false, "IO_ERROR: " + e.getMessage());
        }
    }
}
