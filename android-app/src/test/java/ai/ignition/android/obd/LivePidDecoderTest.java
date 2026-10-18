package ai.ignition.android.obd;

import org.junit.Test;
import static org.junit.Assert.*;

public class LivePidDecoderTest {

    @Test
    public void testRpmDecoding() {
        // "41 0C 1A F8"
        // RPM = (0x1A * 256 + 0xF8) / 4 = (26 * 256 + 248) / 4 = 6904 / 4 = 1726.0 rpm
        String raw = "41 0C 1A F8";
        LivePidDecoder.PidReading r = LivePidDecoder.decodeRaw(raw, "010C", "Engine RPM", "rpm", (clean, idx) -> {
            int a = Integer.parseInt(clean.substring(idx + 4, idx + 6), 16);
            int b = Integer.parseInt(clean.substring(idx + 6, idx + 8), 16);
            return ((a * 256.0) + b) / 4.0;
        });

        assertTrue(r.valid);
        assertEquals(1726.0, r.value, 0.01);
        assertEquals("rpm", r.unit);
    }

    @Test
    public void testCoolantDecoding() {
        // "41 05 5A" -> 0x5A = 90 -> 90 - 40 = 50 °C
        String raw = "41 05 5A";
        LivePidDecoder.PidReading r = LivePidDecoder.decodeRaw(raw, "0105", "Coolant Temperature", "°C", (clean, idx) -> {
            int a = Integer.parseInt(clean.substring(idx + 4, idx + 6), 16);
            return (double) (a - 40);
        });

        assertTrue(r.valid);
        assertEquals(50.0, r.value, 0.01);
    }

    @Test
    public void testSpeedDecoding() {
        // "41 0D 32" -> 0x32 = 50 km/h
        String raw = "41 0D 32";
        LivePidDecoder.PidReading r = LivePidDecoder.decodeRaw(raw, "010D", "Vehicle Speed", "km/h", (clean, idx) -> {
            return (double) Integer.parseInt(clean.substring(idx + 4, idx + 6), 16);
        });

        assertTrue(r.valid);
        assertEquals(50.0, r.value, 0.01);
    }

    @Test
    public void testEngineLoadDecoding() {
        // "41 04 66" -> 0x66 = 102 -> (102 * 100) / 255 = 40.0%
        String raw = "41 04 66";
        LivePidDecoder.PidReading r = LivePidDecoder.decodeRaw(raw, "0104", "Engine Load", "%", (clean, idx) -> {
            int a = Integer.parseInt(clean.substring(idx + 4, idx + 6), 16);
            return (a * 100.0) / 255.0;
        });

        assertTrue(r.valid);
        assertEquals(40.0, r.value, 0.01);
    }

    @Test
    public void testNoDataHandling() {
        LivePidDecoder.PidReading r = LivePidDecoder.decodeRaw("NO DATA", "010C", "Engine RPM", "rpm", (clean, idx) -> 0.0);
        assertFalse(r.valid);
        assertNull(r.value);
    }
}
