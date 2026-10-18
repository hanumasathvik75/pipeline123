package ai.ignition.android.obd;

import org.junit.Test;
import static org.junit.Assert.*;

public class DtcParserTest {

    @Test
    public void testMode03SingleDtc() {
        // "43 03 01 00 00 00 00" -> P0301 (Cylinder 1 Misfire) with trailing 0000 padding
        // 03 01 -> binary 0000 0011 0000 0001 -> high 2 bits: 00 = 'P', hex = 0301 -> P0301
        String raw = "43 03 01 00 00 00 00";
        DtcParser.DtcReport report = DtcParser.decodeDtcResponse("03", "43", raw);

        assertEquals(DtcParser.DtcResultStatus.CODES_CONFIRMED, report.status);
        assertEquals(1, report.codes.size());
        assertEquals("P0301", report.codes.get(0));
    }

    @Test
    public void testMultipleDtcsWithPrefixes() {
        // 01 33 -> 0000 0001 0011 0011 -> P0133
        // 41 00 -> 0100 0001 0000 0000 -> 01 = 'C' -> C0100
        // C1 20 -> 1100 0001 0010 0000 -> 11 = 'U' -> U0120
        String raw = "43 01 33 41 00 C1 20";
        DtcParser.DtcReport report = DtcParser.decodeDtcResponse("03", "43", raw);

        assertEquals(DtcParser.DtcResultStatus.CODES_CONFIRMED, report.status);
        assertEquals(3, report.codes.size());
        assertTrue(report.codes.contains("P0133"));
        assertTrue(report.codes.contains("C0100"));
        assertTrue(report.codes.contains("U0120"));
    }

    @Test
    public void testCleanVehicleNoDtcs() {
        DtcParser.DtcReport r1 = DtcParser.decodeDtcResponse("03", "43", "43 00 00 00 00 00 00");
        assertEquals(DtcParser.DtcResultStatus.NO_CODES_DETECTED, r1.status);
        assertTrue(r1.codes.isEmpty());

        DtcParser.DtcReport r2 = DtcParser.decodeDtcResponse("07", "47", "NO DATA");
        assertEquals(DtcParser.DtcResultStatus.NO_CODES_DETECTED, r2.status);
    }

    @Test
    public void testUnsupportedMode() {
        DtcParser.DtcReport r = DtcParser.decodeDtcResponse("0A", "4A", "7F 0A 11");
        assertEquals(DtcParser.DtcResultStatus.UNSUPPORTED_MODE, r.status);
    }

    @Test
    public void testP0172InMode03AndMode07() {
        // Physical device test observed P0172 in Mode 03 and Mode 07
        // 01 72 -> high 2 bits: 00 = 'P', hex = 0172 -> P0172
        DtcParser.DtcReport r3 = DtcParser.decodeDtcResponse("03", "43", "43 01 01 72");
        assertEquals(DtcParser.DtcResultStatus.CODES_CONFIRMED, r3.status);
        assertEquals(1, r3.codes.size());
        assertEquals("P0172", r3.codes.get(0));

        DtcParser.DtcReport r7 = DtcParser.decodeDtcResponse("07", "47", "47 01 01 72");
        assertEquals(DtcParser.DtcResultStatus.CODES_CONFIRMED, r7.status);
        assertEquals(1, r7.codes.size());
        assertEquals("P0172", r7.codes.get(0));
    }

    @Test
    public void testMultiFrameCanDtcWithSequenceHeaders() {
        // Multi-frame CAN format where sequence prefixes "0:", "1:" are present
        // 01 72 -> P0172, 03 01 -> P0301, 04 20 -> P0420
        String raw = "0: 43 03 01 72 03 01\r\n1: 04 20 00 00 00 00 00";
        DtcParser.DtcReport report = DtcParser.decodeDtcResponse("03", "43", raw);

        assertEquals(DtcParser.DtcResultStatus.CODES_CONFIRMED, report.status);
        assertEquals(3, report.codes.size());
        assertTrue(report.codes.contains("P0172"));
        assertTrue(report.codes.contains("P0301"));
        assertTrue(report.codes.contains("P0420"));
    }

    @Test
    public void testMode0ANoDataClean() {
        DtcParser.DtcReport r = DtcParser.decodeDtcResponse("0A", "4A", "NO DATA");
        assertEquals(DtcParser.DtcResultStatus.NO_CODES_DETECTED, r.status);
        assertTrue(r.codes.isEmpty());
    }
}
