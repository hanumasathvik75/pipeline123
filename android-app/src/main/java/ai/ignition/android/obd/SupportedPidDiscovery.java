package ai.ignition.android.obd;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Executes supported PID discovery by querying 0100 and chaining 0120, 0140 only when indicated.
 */
public class SupportedPidDiscovery {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    public static class DiscoveryReport {
        public final List<String> allSupportedPids;
        public final String raw0100;
        public final List<String> rawResponses;
        public final boolean success;
        public final String notes;

        public DiscoveryReport(List<String> allSupportedPids, String raw0100,
                               List<String> rawResponses, boolean success, String notes) {
            this.allSupportedPids = Collections.unmodifiableList(allSupportedPids);
            this.raw0100 = raw0100;
            this.rawResponses = Collections.unmodifiableList(rawResponses);
            this.success = success;
            this.notes = notes;
        }

        public boolean isPidSupported(String pidHex) {
            String clean = pidHex.replace("01", "").toUpperCase();
            return allSupportedPids.contains(clean);
        }
    }

    public static DiscoveryReport discover(ObdTransport transport) {
        List<String> supportedPids = new ArrayList<>();
        List<String> rawResponses = new ArrayList<>();
        String raw0100 = "";

        try {
            // 1. Query 0100
            raw0100 = transport.exchange("0100", TIMEOUT);
            rawResponses.add("0100: " + raw0100);

            PidBitmapDecoder.BitmapResult r0 = PidBitmapDecoder.decode(raw0100, 0x00);
            if (!r0.valid) {
                return new DiscoveryReport(List.of(), raw0100, rawResponses, false,
                        "Failed to decode Mode 01 PID 00 bitmap: " + r0.errorMessage);
            }
            supportedPids.addAll(r0.supportedPids);

            // 2. Chaining: Query 0120 if bit 32 is set
            if (r0.nextRangeSupported) {
                String raw0120 = transport.exchange("0120", TIMEOUT);
                rawResponses.add("0120: " + raw0120);
                PidBitmapDecoder.BitmapResult r1 = PidBitmapDecoder.decode(raw0120, 0x20);
                if (r1.valid) {
                    supportedPids.addAll(r1.supportedPids);

                    // 3. Query 0140 if bit 32 is set in range 1
                    if (r1.nextRangeSupported) {
                        String raw0140 = transport.exchange("0140", TIMEOUT);
                        rawResponses.add("0140: " + raw0140);
                        PidBitmapDecoder.BitmapResult r2 = PidBitmapDecoder.decode(raw0140, 0x40);
                        if (r2.valid) {
                            supportedPids.addAll(r2.supportedPids);
                        }
                    }
                }
            }

            return new DiscoveryReport(supportedPids, raw0100, rawResponses, true,
                    "Successfully discovered " + supportedPids.size() + " supported PIDs");
        } catch (IOException e) {
            return new DiscoveryReport(supportedPids, raw0100, rawResponses, false,
                    "Communication error during PID discovery: " + e.getMessage());
        }
    }
}
