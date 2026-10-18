package ai.ignition.android.recording;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

public class PayloadReaderTest {

    private File tempDir;
    private File sampleJsonl;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("payload_reader_test").toFile();
        sampleJsonl = new File(tempDir, "test_session.jsonl");

        try (FileOutputStream fos = new FileOutputStream(sampleJsonl)) {
            // Line 1: SESSION_STARTED
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"s-001\",\"sequence\":1,\"timestamp\":\"2026-10-17T06:00:00Z\",\"event_type\":\"SESSION_STARTED\"}\n".getBytes(StandardCharsets.UTF_8));
            // Line 2: Valid RPM observation
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"s-001\",\"sequence\":2,\"timestamp\":\"2026-10-17T06:00:01Z\",\"event_type\":\"OBD_OBSERVATION\",\"signal_name\":\"Engine RPM\",\"value\":2450.0,\"unit\":\"rpm\",\"status\":\"VALID\"}\n".getBytes(StandardCharsets.UTF_8));
            // Line 3: Observation with NO_DATA status and explicit null value
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"s-001\",\"sequence\":3,\"timestamp\":\"2026-10-17T06:00:02Z\",\"event_type\":\"OBD_OBSERVATION\",\"signal_name\":\"Fuel Pressure\",\"value\":null,\"unit\":\"kPa\",\"status\":\"NO_DATA\"}\n".getBytes(StandardCharsets.UTF_8));
            // Line 4: Observation with UNSUPPORTED status and explicit null value
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"s-001\",\"sequence\":4,\"timestamp\":\"2026-10-17T06:00:03Z\",\"event_type\":\"OBD_OBSERVATION\",\"signal_name\":\"Engine Oil Temp\",\"value\":null,\"unit\":\"°C\",\"status\":\"UNSUPPORTED\"}\n".getBytes(StandardCharsets.UTF_8));
            // Line 5: Observation with ERROR status
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"s-001\",\"sequence\":5,\"timestamp\":\"2026-10-17T06:00:04Z\",\"event_type\":\"OBD_OBSERVATION\",\"signal_name\":\"Intake Pressure\",\"value\":null,\"unit\":\"kPa\",\"status\":\"ERROR\"}\n".getBytes(StandardCharsets.UTF_8));
            // Line 6: Connection lost event
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"s-001\",\"sequence\":6,\"timestamp\":\"2026-10-17T06:00:05Z\",\"event_type\":\"CONNECTION_LOST\",\"error_category\":\"TRANSPORT_DISCONNECT\",\"error_message\":\"Bluetooth dropped\"}\n".getBytes(StandardCharsets.UTF_8));
            // Line 7: Malformed non-JSON line
            fos.write("CORRUPTED_RAW_TEXT_NOT_JSON\n".getBytes(StandardCharsets.UTF_8));
            // Line 8: SESSION_FINALIZED
            fos.write("{\"schema_version\":\"1.0-local\",\"session_id\":\"s-001\",\"sequence\":7,\"timestamp\":\"2026-10-17T06:00:10Z\",\"event_type\":\"SESSION_FINALIZED\"}\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    @After
    public void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            tempDir.delete();
        }
    }

    @Test
    public void testReadPage_pagedBoundsAndCounts() throws Exception {
        assertEquals(8, PayloadReader.countLines(sampleJsonl));

        // Read Page 0 with page size 3
        PayloadReader.PayloadPage page0 = PayloadReader.readPage(sampleJsonl, 0, 3);
        assertEquals(0, page0.getPageIndex());
        assertEquals(3, page0.getPageSize());
        assertEquals(8, page0.getTotalLines());
        assertEquals(3, page0.getTotalPages());
        assertEquals(3, page0.getRecords().size());
        assertTrue(page0.hasNextPage());
        assertFalse(page0.hasPreviousPage());

        assertEquals(1, page0.getRecords().get(0).getLineNumber());
        assertEquals(2, page0.getRecords().get(1).getLineNumber());
        assertEquals(3, page0.getRecords().get(2).getLineNumber());

        // Read Page 2 (last page, contains remaining 2 lines: lines 7 and 8)
        PayloadReader.PayloadPage page2 = PayloadReader.readPage(sampleJsonl, 2, 3);
        assertEquals(2, page2.getPageIndex());
        assertEquals(2, page2.getRecords().size());
        assertFalse(page2.hasNextPage());
        assertTrue(page2.hasPreviousPage());

        assertEquals(7, page2.getRecords().get(0).getLineNumber());
        assertEquals(8, page2.getRecords().get(1).getLineNumber());
    }

    @Test
    public void testExplicitNullAndStatusPreservation() throws Exception {
        PayloadReader.PayloadPage all = PayloadReader.readPage(sampleJsonl, 0, 10);
        List<PayloadReader.PayloadRecord> records = all.getRecords();

        // Record 2: VALID with value 2450.0
        PayloadReader.PayloadRecord rValid = records.get(1);
        assertFalse(rValid.isMalformed());
        assertNotNull(rValid.getEvent());
        assertEquals(Double.valueOf(2450.0), rValid.getEvent().getValue());
        assertEquals(ObservationStatus.VALID, rValid.getEvent().getStatus());
        assertTrue(rValid.getFormattedDisplay().contains("2450.0 rpm"));
        assertTrue(rValid.getFormattedDisplay().contains("[Status: VALID]"));

        // Record 3: NO_DATA with explicit null
        PayloadReader.PayloadRecord rNoData = records.get(2);
        assertFalse(rNoData.isMalformed());
        assertNotNull(rNoData.getEvent());
        assertNull("NO_DATA value must remain strictly null, not fabricated 0.0", rNoData.getEvent().getValue());
        assertEquals(ObservationStatus.NO_DATA, rNoData.getEvent().getStatus());
        assertTrue(rNoData.getFormattedDisplay().contains("null"));
        assertTrue(rNoData.getFormattedDisplay().contains("[Status: NO_DATA]"));

        // Record 4: UNSUPPORTED with explicit null
        PayloadReader.PayloadRecord rUnsupported = records.get(3);
        assertFalse(rUnsupported.isMalformed());
        assertNull("UNSUPPORTED value must remain null", rUnsupported.getEvent().getValue());
        assertEquals(ObservationStatus.UNSUPPORTED, rUnsupported.getEvent().getStatus());
        assertTrue(rUnsupported.getFormattedDisplay().contains("[Status: UNSUPPORTED]"));

        // Record 5: ERROR with explicit null
        PayloadReader.PayloadRecord rError = records.get(4);
        assertFalse(rError.isMalformed());
        assertNull("ERROR value must remain null", rError.getEvent().getValue());
        assertEquals(ObservationStatus.ERROR, rError.getEvent().getStatus());
        assertTrue(rError.getFormattedDisplay().contains("[Status: ERROR]"));
    }

    @Test
    public void testMalformedLineHandling_doesNotCrashAndCapturesError() throws Exception {
        PayloadReader.PayloadPage all = PayloadReader.readPage(sampleJsonl, 0, 10);
        List<PayloadReader.PayloadRecord> records = all.getRecords();

        // Line 7 is malformed
        PayloadReader.PayloadRecord rMalformed = records.get(6);
        assertEquals(7, rMalformed.getLineNumber());
        assertTrue(rMalformed.isMalformed());
        assertNull(rMalformed.getEvent());
        assertNotNull(rMalformed.getParseError());
        assertEquals("CORRUPTED_RAW_TEXT_NOT_JSON", rMalformed.getRawJson());
        assertTrue(rMalformed.getFormattedDisplay().contains("⚠️ MALFORMED"));
    }

    @Test
    public void testEmptyAndMissingFiles() throws Exception {
        File emptyFile = new File(tempDir, "empty.jsonl");
        assertTrue(emptyFile.createNewFile());

        PayloadReader.PayloadPage emptyPage = PayloadReader.readPage(emptyFile, 0, 50);
        assertEquals(0, emptyPage.getTotalLines());
        assertEquals(0, emptyPage.getRecords().size());
        assertEquals(1, emptyPage.getTotalPages());

        File missingFile = new File(tempDir, "non_existent.jsonl");
        PayloadReader.PayloadPage missingPage = PayloadReader.readPage(missingFile, 0, 50);
        assertEquals(0, missingPage.getTotalLines());
        assertEquals(0, missingPage.getRecords().size());
    }
}
