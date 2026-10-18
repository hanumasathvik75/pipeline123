package ai.ignition.android.obd;

import org.junit.Test;
import static org.junit.Assert.*;

public class VinParserTest {

    @Test
    public void testMultiFrameVinReassembly() {
        // Multi-frame CAN format for VIN "1HGCR2F84HA123456"
        String rawCanResponse =
                "0: 49 02 01 31 48 47\r\n" +
                "1: 43 52 32 46 38 34 48\r\n" +
                "2: 41 31 32 33 34 35 36";

        VinParser.VinResult result = VinParser.decodeVin(rawCanResponse);

        assertEquals(VinParser.VinStatus.VALID, result.status);
        assertEquals("1HGCR2F84HA123456", result.rawVin);
        assertEquals("1HG**********3456", result.redactedVin);
    }

    @Test
    public void testSingleLineVin() {
        String raw = "49 02 01 31 48 47 43 52 32 46 38 34 48 41 31 32 33 34 35 36";
        VinParser.VinResult result = VinParser.decodeVin(raw);

        assertEquals(VinParser.VinStatus.VALID, result.status);
        assertEquals("1HGCR2F84HA123456", result.rawVin);
    }

    @Test
    public void testForbiddenCharactersI_O_Q_Rejected() {
        // String with letter 'I' (ASCII 0x49): "1HGCR2F84IA123456"
        String rawWithI = "49 02 01 31 48 47 43 52 32 46 38 34 49 41 31 32 33 34 35 36";
        VinParser.VinResult r1 = VinParser.decodeVin(rawWithI);
        assertEquals(VinParser.VinStatus.MALFORMED, r1.status);
        assertTrue(r1.notes.contains("forbidden characters"));

        // String with letter 'O' (ASCII 0x4F): "1HGCR2F84OA123456"
        String rawWithO = "49 02 01 31 48 47 43 52 32 46 38 34 4F 41 31 32 33 34 35 36";
        VinParser.VinResult r2 = VinParser.decodeVin(rawWithO);
        assertEquals(VinParser.VinStatus.MALFORMED, r2.status);
        assertTrue(r2.notes.contains("forbidden characters"));

        // String with letter 'Q' (ASCII 0x51): "1HGCR2F84QA123456"
        String rawWithQ = "49 02 01 31 48 47 43 52 32 46 38 34 51 41 31 32 33 34 35 36";
        VinParser.VinResult r3 = VinParser.decodeVin(rawWithQ);
        assertEquals(VinParser.VinStatus.MALFORMED, r3.status);
        assertTrue(r3.notes.contains("forbidden characters"));
    }

    @Test
    public void testUnsupportedVin() {
        VinParser.VinResult result = VinParser.decodeVin("7F 09 12");
        assertEquals(VinParser.VinStatus.UNSUPPORTED, result.status);
        assertNull(result.rawVin);
    }

    @Test
    public void testIncompleteVin_12Chars_ReportedAsIncomplete() {
        // Physical test case where 12 characters ("IGN4NT0R0001") were received instead of 17
        String raw = "0: 49 02 01 49 47 4E\r\n" +
                     "1: 34 4E 54 30 52 30 30\r\n" +
                     "2: 30 31";

        VinParser.VinResult result = VinParser.decodeVin(raw);

        assertEquals(VinParser.VinStatus.INCOMPLETE, result.status);
        assertEquals("IGN4NT0R0001", result.rawVin);
        assertEquals("IGN***001", result.redactedVin);
        assertTrue(result.notes.contains("Incomplete VIN payload: expected 17 characters, found 12"));
    }

    @Test
    public void testMultiFrameVinWithIsoTpLengthHeader() {
        // ELM327 CAN auto protocol with "014" length header on line 1
        String rawWithLengthHeader =
                "014\r\n" +
                "0: 49 02 01 31 48 47\r\n" +
                "1: 43 52 32 46 38 34 48\r\n" +
                "2: 41 31 32 33 34 35 36";

        VinParser.VinResult result = VinParser.decodeVin(rawWithLengthHeader);

        assertEquals(VinParser.VinStatus.VALID, result.status);
        assertEquals("1HGCR2F84HA123456", result.rawVin);
    }

    @Test
    public void testCollapsedMultiFrameVinWithoutNewlines() {
        // Stream where newlines were collapsed: "0:4902013148471:435232463834482:41313233343536"
        String collapsed = "0:4902013148471:435232463834482:41313233343536";

        VinParser.VinResult result = VinParser.decodeVin(collapsed);

        assertEquals(VinParser.VinStatus.VALID, result.status);
        assertEquals("1HGCR2F84HA123456", result.rawVin);
    }

    @Test
    public void testVinResponseWithout01CountByte() {
        // Response format with "4902" header directly followed by VIN bytes without "01" item count
        String rawWithout01 =
                "0: 49 02 31 48 47 43 52\r\n" +
                "1: 32 46 38 34 48 41 31\r\n" +
                "2: 32 33 34 35 36 00 00";

        VinParser.VinResult result = VinParser.decodeVin(rawWithout01);

        assertEquals(VinParser.VinStatus.VALID, result.status);
        assertEquals("1HGCR2F84HA123456", result.rawVin);
    }

    @Test
    public void testMalformedVinWithNonAlphanumeric() {
        // 17 characters with non-alphanumeric punctuation
        String raw = "49 02 01 31 48 47 21 21 21 21 21 21 21 21 21 21 21 21 21 21";
        VinParser.VinResult result = VinParser.decodeVin(raw);
        assertEquals(VinParser.VinStatus.MALFORMED, result.status);
    }
}
