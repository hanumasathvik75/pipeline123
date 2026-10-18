package ai.ignition.android.obd;

import org.junit.Test;
import static org.junit.Assert.*;

public class PidBitmapDecoderTest {

    @Test
    public void testStandardBitmapDecoding() {
        // "41 00 BE 3E B8 13"
        // BE = 10111110 (PIDs 01, 03, 04, 05, 06, 07)
        // 3E = 00111110 (PIDs 0B, 0C, 0D, 0E, 0F)
        // B8 = 10111000 (PIDs 11, 13, 14, 15)
        // 13 = 00010011 (PIDs 1C, 1F, 20 -> next range supported!)
        String raw = "41 00 BE 3E B8 13";
        PidBitmapDecoder.BitmapResult result = PidBitmapDecoder.decode(raw, 0x00);

        assertTrue("Bitmap should be valid", result.valid);
        assertTrue("RPM (0C) should be supported", result.supportedPids.contains("0C"));
        assertTrue("Speed (0D) should be supported", result.supportedPids.contains("0D"));
        assertTrue("Coolant (05) should be supported", result.supportedPids.contains("05"));
        assertTrue("Load (04) should be supported", result.supportedPids.contains("04"));
        assertTrue("Next range PID 20 should indicate next range is supported", result.nextRangeSupported);
    }

    @Test
    public void testSpacesOffBitmapDecoding() {
        String raw = "4100BE3EB810"; // LSB is 0 -> nextRangeSupported is false
        PidBitmapDecoder.BitmapResult result = PidBitmapDecoder.decode(raw, 0x00);

        assertTrue("Bitmap should be valid", result.valid);
        assertTrue("Coolant (05) supported", result.supportedPids.contains("05"));
        assertFalse("Next range should NOT be supported", result.nextRangeSupported);
    }

    @Test
    public void testChainedRange0120() {
        // Base PID 0x20
        String raw = "41 20 80 00 00 01";
        // 80 = PID 21 (0x21)
        // 01 = PID 40 (0x40 -> next range supported!)
        PidBitmapDecoder.BitmapResult result = PidBitmapDecoder.decode(raw, 0x20);

        assertTrue(result.valid);
        assertTrue("PID 21 should be supported", result.supportedPids.contains("21"));
        assertTrue("Next range PID 40 should be supported", result.nextRangeSupported);
    }

    @Test
    public void testMalformedHeaderOrTruncated() {
        // Missing positive response header
        PidBitmapDecoder.BitmapResult r1 = PidBitmapDecoder.decode("7F 01 12", 0x00);
        assertFalse(r1.valid);

        // Truncated payload
        PidBitmapDecoder.BitmapResult r2 = PidBitmapDecoder.decode("41 00 BE 3E", 0x00);
        assertFalse(r2.valid);
    }
}
