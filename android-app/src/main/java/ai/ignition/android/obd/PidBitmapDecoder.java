package ai.ignition.android.obd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Decodes SAE J1979 / ISO 15031-5 Mode 01 supported PID bitmaps (e.g. PIDs 0100, 0120, 0140).
 */
public class PidBitmapDecoder {

    public static class BitmapResult {
        public final int basePid;
        public final List<String> supportedPids; // e.g. ["01", "04", "05", "0C", "0D"]
        public final boolean nextRangeSupported; // bit 32 (PID base+0x20)
        public final String rawPayload;
        public final boolean valid;
        public final String errorMessage;

        public BitmapResult(int basePid, List<String> supportedPids, boolean nextRangeSupported,
                            String rawPayload, boolean valid, String errorMessage) {
            this.basePid = basePid;
            this.supportedPids = Collections.unmodifiableList(supportedPids);
            this.nextRangeSupported = nextRangeSupported;
            this.rawPayload = rawPayload;
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Decodes an ELM response string for a PID bitmap query (e.g. "41 00 BE 3E B8 13" or "4100BE3EB813").
     *
     * @param rawResponse The raw string returned by the adapter
     * @param basePid The base PID queried (e.g. 0x00 for 0100, 0x20 for 0120, 0x40 for 0140)
     * @return Decoded BitmapResult containing the list of supported PIDs in this 32-bit window
     */
    public static BitmapResult decode(String rawResponse, int basePid) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new BitmapResult(basePid, List.of(), false, rawResponse, false, "Empty response");
        }

        String clean = rawResponse.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        String expectedHeader = String.format(Locale.ROOT, "41%02X", basePid);

        int headerIndex = clean.indexOf(expectedHeader);
        if (headerIndex == -1) {
            return new BitmapResult(basePid, List.of(), false, rawResponse, false,
                    "Expected positive response header " + expectedHeader + " not found in: " + rawResponse);
        }

        int payloadStart = headerIndex + expectedHeader.length();
        if (clean.length() < payloadStart + 8) {
            return new BitmapResult(basePid, List.of(), false, rawResponse, false,
                    "Truncated payload: expected 8 hex characters for 4 bitmap bytes, got: " + clean.substring(payloadStart));
        }

        String hexBytes = clean.substring(payloadStart, payloadStart + 8);
        List<String> supported = new ArrayList<>();
        boolean nextRange = false;

        try {
            long bitmapValue = Long.parseLong(hexBytes, 16);

            // 32 bits evaluated from MSB (bit 31 -> PID base+1) down to LSB (bit 0 -> PID base+32)
            for (int bit = 31; bit >= 0; bit--) {
                boolean isSet = ((bitmapValue >> bit) & 1L) == 1L;
                int pidNumber = basePid + (32 - bit);
                String pidHex = String.format(Locale.ROOT, "%02X", pidNumber);

                if (isSet) {
                    if (bit == 0) {
                        // Bit 0 indicates support for the next 32-PID range (0x20, 0x40, etc.)
                        nextRange = true;
                    }
                    supported.add(pidHex);
                }
            }

            return new BitmapResult(basePid, supported, nextRange, hexBytes, true, null);
        } catch (NumberFormatException e) {
            return new BitmapResult(basePid, List.of(), false, rawResponse, false, "Malformed hex: " + hexBytes);
        }
    }
}
