package ai.ignition.android.recording;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

public class SessionExporterTest {

    private File tempDir;
    private File sessionDir;
    private File jsonlFile;
    private File manifestFile;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("ignition_export_test").toFile();
        sessionDir = new File(tempDir, "session_test_123");
        assertTrue(sessionDir.mkdirs());

        jsonlFile = new File(sessionDir, "session.jsonl");
        manifestFile = new File(sessionDir, "manifest.json");

        // Write sample JSONL content
        try (FileOutputStream fos = new FileOutputStream(jsonlFile)) {
            String record1 = "{\"schema_version\":\"1.0-local\",\"session_id\":\"session_test_123\",\"sequence\":1,\"timestamp\":\"2026-10-17T06:00:00Z\",\"event_type\":\"SESSION_STARTED\"}\n";
            String record2 = "{\"schema_version\":\"1.0-local\",\"session_id\":\"session_test_123\",\"sequence\":2,\"timestamp\":\"2026-10-17T06:00:01Z\",\"event_type\":\"OBD_OBSERVATION\",\"signal_name\":\"Engine RPM\",\"value\":2450.0,\"unit\":\"rpm\",\"status\":\"VALID\"}\n";
            fos.write(record1.getBytes(StandardCharsets.UTF_8));
            fos.write(record2.getBytes(StandardCharsets.UTF_8));
        }

        // Write sample manifest content
        try (FileOutputStream fos = new FileOutputStream(manifestFile)) {
            String manifestJson = "{\"schema_version\":\"1.0-local\",\"session_id\":\"session_test_123\",\"status\":\"COMPLETE\",\"record_count\":2,\"valid_record_count\":2,\"invalid_record_count\":0}\n";
            fos.write(manifestJson.getBytes(StandardCharsets.UTF_8));
        }
    }

    @After
    public void tearDown() {
        deleteRecursively(tempDir);
    }

    private void deleteRecursively(File f) {
        if (f != null && f.exists()) {
            if (f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) {
                    for (File c : children) deleteRecursively(c);
                }
            }
            f.delete();
        }
    }

    @Test
    public void testExportJsonl_streamsExactContent() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long bytesWritten = SessionExporter.exportJsonl(jsonlFile, out);

        assertEquals("Bytes written must match file length", jsonlFile.length(), bytesWritten);
        String exported = out.toString(StandardCharsets.UTF_8.name());
        assertTrue(exported.contains("SESSION_STARTED"));
        assertTrue(exported.contains("Engine RPM"));
        assertTrue(exported.contains("2450.0"));

        // Verify source file is untouched
        assertTrue("Source JSONL file must remain intact", jsonlFile.exists());
        assertEquals(bytesWritten, jsonlFile.length());
    }

    @Test
    public void testExportManifest_streamsExactContent() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long bytesWritten = SessionExporter.exportManifest(manifestFile, out);

        assertEquals("Bytes written must match manifest length", manifestFile.length(), bytesWritten);
        String exported = out.toString(StandardCharsets.UTF_8.name());
        assertTrue(exported.contains("\"status\":\"COMPLETE\""));
        assertTrue(exported.contains("session_test_123"));

        // Verify source file is untouched
        assertTrue("Source manifest file must remain intact", manifestFile.exists());
    }

    @Test
    public void testExportSessionZip_containsOriginalFilesWithCorrectContent() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long totalBytesPacked = SessionExporter.exportSessionZip(sessionDir, out);

        assertTrue("Packed bytes must be positive", totalBytesPacked > 0);
        byte[] zipData = out.toByteArray();
        assertTrue("ZIP archive data must be produced", zipData.length > 0);

        // Read and verify the created ZIP archive
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            byte[] buf = new byte[1024];
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream entryContent = new ByteArrayOutputStream();
                int read;
                while ((read = zis.read(buf)) != -1) {
                    entryContent.write(buf, 0, read);
                }
                entries.put(entry.getName(), entryContent.toString(StandardCharsets.UTF_8.name()));
                zis.closeEntry();
            }
        }

        assertTrue("ZIP must contain session.jsonl", entries.containsKey("session.jsonl"));
        assertTrue("ZIP must contain manifest.json", entries.containsKey("manifest.json"));

        assertTrue(entries.get("session.jsonl").contains("Engine RPM"));
        assertTrue(entries.get("manifest.json").contains("COMPLETE"));

        // Source files must remain intact
        assertTrue(jsonlFile.exists());
        assertTrue(manifestFile.exists());
    }

    @Test(expected = FileNotFoundException.class)
    public void testExportJsonl_missingFileThrows() throws Exception {
        File missing = new File(tempDir, "does_not_exist.jsonl");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SessionExporter.exportJsonl(missing, out);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportJsonl_nullDestinationThrows() throws Exception {
        SessionExporter.exportJsonl(jsonlFile, null);
    }

    @Test
    public void testExportStreaming_ioErrorPropagatesCleanly() {
        OutputStream failingStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Simulated disk full or storage ejection error");
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new IOException("Simulated storage write error");
            }
        };

        try {
            SessionExporter.exportJsonl(jsonlFile, failingStream);
            fail("Must throw IOException on destination write failure");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Simulated"));
        }

        // Original file must not be damaged
        assertTrue(jsonlFile.exists());
        assertTrue(jsonlFile.length() > 0);
    }

    @Test
    public void testExportSessionZip_largeStreamingIntegrity() throws Exception {
        // Create 2,000 JSONL lines to verify multi-chunk buffer streaming
        File largeDir = new File(tempDir, "large_session");
        assertTrue(largeDir.mkdirs());
        File largeJsonl = new File(largeDir, "session.jsonl");

        int lineCount = 2000;
        try (FileOutputStream fos = new FileOutputStream(largeJsonl)) {
            for (int i = 0; i < lineCount; i++) {
                String line = "{\"schema_version\":\"1.0-local\",\"sequence\":" + i + ",\"data\":\"telemetry-payload-sample-" + i + "\"}\n";
                fos.write(line.getBytes(StandardCharsets.UTF_8));
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long packed = SessionExporter.exportSessionZip(largeDir, out);
        assertEquals(largeJsonl.length(), packed);

        // Verify ZIP entry can be read back completely
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertEquals("session.jsonl", entry.getName());
            ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int r;
            while ((r = zis.read(buf)) != -1) {
                decompressed.write(buf, 0, r);
            }
            assertEquals(largeJsonl.length(), decompressed.size());
        }
    }
}
