package ai.ignition.android.obd;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.Assert.*;

/**
 * Regression and failure-case test suite for ClassicSppTransport and Elm327Initializer
 * using simulated stream test doubles.
 *
 * NOTE: These are automated software test doubles, NOT physical hardware evidence.
 */
public class TransportRegressionTest {

    @Test
    public void testPromptTermination() throws Exception {
        // Simulated ELM327 responding with "41 00 BE 3E B8 13>"
        ByteArrayInputStream in = new ByteArrayInputStream("41 00 BE 3E B8 13\r\n>".getBytes(StandardCharsets.US_ASCII));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ClassicSppTransport transport = new ClassicSppTransport(in, out, "Mock Dongle");
        String response = transport.exchange("0100", Duration.ofSeconds(2));

        assertEquals("41 00 BE 3E B8 13", response);
        // Verify sent command terminated with \r
        assertEquals("0100\r", out.toString(StandardCharsets.US_ASCII));
    }

    @Test
    public void testMissingPromptTimeout() {
        // Stream without '>' prompt
        ByteArrayInputStream in = new ByteArrayInputStream("41 00 BE 3E B8 13\r\n".getBytes(StandardCharsets.US_ASCII));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ClassicSppTransport transport = new ClassicSppTransport(in, out, "Mock Dongle");
        try {
            transport.exchange("0100", Duration.ofMillis(200));
            fail("Expected IOException due to missing prompt / timeout");
        } catch (IOException e) {
            assertTrue("Expected timeout or prompt failure message",
                    e.getMessage().contains("Timed out") || e.getMessage().contains("prompt") || e.getMessage().contains("end of stream"));
        }
    }

    @Test
    public void testEofDisconnectMidCommand() {
        // Empty stream simulating immediate disconnect
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ClassicSppTransport transport = new ClassicSppTransport(in, out, "Mock Dongle");
        try {
            transport.exchange("0100", Duration.ofSeconds(1));
            fail("Expected IOException due to unexpected EOF");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("end of stream") || e.getMessage().contains("disconnected"));
        }
    }

    @Test
    public void testBufferOverflowLimit() {
        // Oversized stream without prompt exceeding 16384 bytes
        byte[] oversized = new byte[17000];
        java.util.Arrays.fill(oversized, (byte) 'A');
        ByteArrayInputStream in = new ByteArrayInputStream(oversized);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ClassicSppTransport transport = new ClassicSppTransport(in, out, "Mock Dongle");
        try {
            transport.exchange("0100", Duration.ofSeconds(2));
            fail("Expected IOException due to buffer overflow");
        } catch (IOException e) {
            assertTrue("Expected buffer limit error", e.getMessage().contains("maximum buffer limit"));
        }
    }

    @Test
    public void testProhibitedMode04Rejection() {
        ByteArrayInputStream in = new ByteArrayInputStream(">".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ClassicSppTransport transport = new ClassicSppTransport(in, out, "Mock Dongle");

        try {
            transport.exchange("04", Duration.ofSeconds(1));
            fail("Expected IllegalArgumentException for Mode 04 clearing command");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Mode 04 code clearing commands are strictly prohibited"));
        } catch (IOException e) {
            fail("Expected IllegalArgumentException, not IOException");
        }
    }

    @Test
    public void testProhibitedMode08Rejection() {
        ByteArrayInputStream in = new ByteArrayInputStream(">".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ClassicSppTransport transport = new ClassicSppTransport(in, out, "Mock Dongle");

        try {
            transport.exchange("080102", Duration.ofSeconds(1));
            fail("Expected IllegalArgumentException for Mode 08 actuator command");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Mode 08 actuator/control commands are strictly prohibited"));
        } catch (IOException e) {
            fail("Expected IllegalArgumentException, not IOException");
        }
    }

    @Test
    public void testElmInitializationErrorResponses() {
        // Adapter returns "?" to ATZ and ATI
        String mockRejection = "?\r\n>?\r\n>OK\r\n>OK\r\n>OK\r\n>";
        ByteArrayInputStream in = new ByteArrayInputStream(mockRejection.getBytes(StandardCharsets.US_ASCII));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ClassicSppTransport transport = new ClassicSppTransport(in, out, "Faulty Adapter");

        Elm327Initializer.InitResult res = Elm327Initializer.initialize(transport);
        assertFalse("Init should fail when adapter rejects AT commands with '?'", res.success);
        assertNotNull(res.errorMessage);
    }

    @Test
    public void testMultiLinePreservedWithNewlines() throws Exception {
        // Simulated multi-line response like Mode 09 PID 02 on CAN
        String rawCan = "0: 49 02 01 49 47 4E\r1: 34 4E 54 30 52 30 30\r2: 30 31\r>";
        ByteArrayInputStream in = new ByteArrayInputStream(rawCan.getBytes(StandardCharsets.US_ASCII));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ClassicSppTransport transport = new ClassicSppTransport(in, out, "Mock Dongle");
        String response = transport.exchange("0902", Duration.ofSeconds(2));

        String[] lines = response.split("\n");
        assertEquals("Multi-line response should preserve 3 separate lines", 3, lines.length);
        assertEquals("0: 49 02 01 49 47 4E", lines[0]);
        assertEquals("1: 34 4E 54 30 52 30 30", lines[1]);
        assertEquals("2: 30 31", lines[2]);
    }
}
