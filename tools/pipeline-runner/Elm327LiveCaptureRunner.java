package tools.pipeline_runner;

import com.ignitionai.application.InspectionService;
import com.ignitionai.application.SessionFiles;
import com.ignitionai.obdgenerator.generator.Elm327FrameEncoder;
import com.ignitionai.obdinput.schema.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures a real OBD-II session over TCP from a genuinely independent third-party ELM327
 * protocol emulator (Ircama/ELM327-emulator, github.com/Ircama/ELM327-emulator -- not written or
 * maintained by IgnitionAI) instead of our own {@link com.ignitionai.obdgenerator.generator.ObdGenerator}.
 * This is the honest answer to "you could be gaming your own system with your own simulator": the
 * byte-level protocol responses this class decodes come from someone else's code, over a real
 * socket, using the real AT-command/Mode-01/Mode-03 protocol -- the same wire format a genuine
 * ELM327 dongle uses. Everything downstream (SessionFiles, InspectionService) is the unmodified
 * production pipeline; nothing here has any special-cased "if this came from the emulator" path.
 *
 * Usage: start the emulator separately (pip install elm327-emulator; python -m elm -n 35000 -s car),
 * then: java -cp out tools.pipeline_runner.Elm327LiveCaptureRunner [host] [port] [durationMs]
 *
 * {@link #captureToFile(String, int, long)} is the reusable entry point -- the native desktop app's
 * "Capture live ELM327" action calls it directly instead of shelling out to this CLI, so the exact
 * same socket/decode/save path backs both the command line and the UI button.
 */
public final class Elm327LiveCaptureRunner {
    private static final Pattern DATA_RESPONSE = Pattern.compile("41\\s+([0-9A-Fa-f]{2})((?:\\s+[0-9A-Fa-f]{2})*)");
    private static final Pattern DTC_RESPONSE = Pattern.compile("43\\s+([0-9A-Fa-f]{2})((?:\\s+[0-9A-Fa-f]{2})*)");

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 35000;
        long durationMs = args.length > 2 ? Long.parseLong(args[2]) : 15000;

        System.out.println("Connecting to third-party ELM327 emulator at " + host + ":" + port + " ...");
        Path sessionFile = captureToFile(host, port, durationMs);
        System.out.println("Saved session (open via 'Open OBD session' in the main app): " + sessionFile.toAbsolutePath());

        List<ObdMessage> messages = SessionFiles.read(sessionFile);
        System.out.println("Captured " + messages.size() + " messages from the live third-party emulator.");
        System.out.println("\nFeeding the captured session through the real, unmodified InspectionService...");
        var result = new InspectionService(new java.io.File("virtual-sensors/src/main/resources/sensors")).assess(messages);
        System.out.println("VHI: " + result.certificate.getHealthAssessment().getVehicleHealthIndex()
            + "  band=" + result.certificate.getHealthAssessment().getHealthBand()
            + "  dtcs=" + result.certificate.getDtcCodes()
            + "  diagnosticIndexCount=" + result.certificate.getDiagnosticIndex().size());
    }

    /** Connects to a live ELM327 endpoint, captures durationMs worth of real telemetry, and saves it
     * as a session file under out/captures. Returns the path so a caller (CLI or UI) can load it. */
    public static Path captureToFile(String host, int port, long durationMs) throws IOException {
        String adapterId = host + ":" + port;
        Socket socket = new Socket(host, port);
        socket.setSoTimeout(5000);
        List<ObdMessage> messages;
        try {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            send(out, in, "ATZ");
            send(out, in, "ATE0");
            send(out, in, "ATL0");
            send(out, in, "ATSP0");
            messages = capture(out, in, durationMs, adapterId);
        } finally {
            try { socket.shutdownOutput(); } catch (IOException ignored) { }
            try { Thread.sleep(200); } catch (InterruptedException ignored) { }
            socket.close();
        }
        Path sessionFile = Path.of("out/captures", "elm327-live-" + System.currentTimeMillis() + ".jsonl");
        Files.createDirectories(sessionFile.getParent());
        SessionFiles.write(sessionFile, messages);
        return sessionFile;
    }

    private static List<ObdMessage> capture(OutputStream out, InputStream in, long durationMs, String adapterId) throws IOException {
        List<ObdMessage> messages = new ArrayList<>();
        String vehicleId = "ELM327-LIVE";
        String sessionId = "SESSION-" + UUID.randomUUID();
        Instant sessionStart = Instant.now();
        long startNanos = System.nanoTime();
        long sequence = 1, endAtMs = System.currentTimeMillis() + durationMs;
        int cycle = 0;

        List<String> signals = new ArrayList<>(java.util.Arrays.asList(
            "engine_rpm", "vehicle_speed", "coolant_temperature", "calculated_engine_load", "throttle_position",
            "manifold_absolute_pressure", "mass_airflow_rate", "timing_advance", "intake_air_temperature",
            "barometric_pressure", "control_module_voltage", "short_term_fuel_trim_b1", "long_term_fuel_trim_b1", "oil_temperature",
            "hybrid_battery_pack_life"));

        while (System.currentTimeMillis() < endAtMs) {
            long monotonicMs = (System.nanoTime() - startNanos) / 1_000_000L;
            Instant now = Instant.now();
            List<SensorReading> readings = new ArrayList<>();
            for (String signal : signals) {
                Integer pid = Elm327FrameEncoder.pidFor(signal);
                String response = send(out, in, String.format("01%02X", pid));
                int[] bytes = parseDataBytes(response, pid);
                if (bytes == null) continue;
                double value = Elm327FrameEncoder.decodeValue(signal, bytes);
                readings.add(SensorReading.builder()
                    .signalId(signal).value(value).unit(Elm327FrameEncoder.unitFor(signal))
                    .measurementTimeBasis(MeasurementTimeBasis.ADAPTER).monotonicMs(monotonicMs).receivedAt(now)
                    .quality(new Quality(QualityStatus.VALID, null, 0))
                    .provenance(new SignalProvenance("elm327-emulator-live", adapterId))
                    .build());
            }
            List<DtcObservation> dtcs = new ArrayList<>();
            boolean dtcPoll = cycle % 5 == 0;
            if (dtcPoll) {
                String dtcResponse = send(out, in, "03");
                dtcs = parseDtcs(dtcResponse, monotonicMs, now, adapterId);
            }
            if (!readings.isEmpty() || dtcPoll) {
                ObdMessage.Builder builder = ObdMessage.builder()
                    .messageId("MSG-" + sequence).sessionId(sessionId).sequence(sequence++)
                    .messageType(MessageType.TELEMETRY).sourceType(SourceType.VEHICLE)
                    .emittedAt(now).receivedAt(now)
                    .vehicleRef(new VehicleRef(vehicleId, null, IdentityStatus.UNKNOWN))
                    .sensorReadings(readings)
                    .rawProvenance(new Provenance("elm327-emulator-live", "4.0.0", adapterId, 0L));
                if (dtcPoll) { builder.dtcObservations(dtcs); builder.dtcSnapshotCompleteness(DtcSnapshotCompleteness.COMPLETE); }
                messages.add(builder.build());
            }
            cycle++;
        }
        return messages;
    }

    private static int[] parseDataBytes(String response, int expectedPid) {
        Matcher m = DATA_RESPONSE.matcher(response);
        while (m.find()) {
            int pid = Integer.parseInt(m.group(1), 16);
            if (pid != expectedPid) continue;
            String[] rest = m.group(2).trim().split("\\s+");
            if (rest.length == 0 || rest[0].isEmpty()) return null;
            int[] bytes = new int[rest.length];
            for (int i = 0; i < rest.length; i++) bytes[i] = Integer.parseInt(rest[i], 16);
            return bytes;
        }
        return null;
    }

    private static List<DtcObservation> parseDtcs(String response, long monotonicMs, Instant now, String adapterId) {
        List<DtcObservation> out = new ArrayList<>();
        Matcher m = DTC_RESPONSE.matcher(response);
        if (!m.find()) return out;
        String[] rest = m.group(2).trim().split("\\s+");
        if (rest.length == 0 || rest[0].isEmpty()) return out;
        for (int i = 0; i + 1 < rest.length; i += 2) {
            int high = Integer.parseInt(rest[i], 16), low = Integer.parseInt(rest[i + 1], 16);
            if (high == 0 && low == 0) continue;
            String code = Elm327FrameEncoder.decodeDtc(high, low);
            out.add(DtcObservation.builder().code(code).status(DtcStatus.CONFIRMED).observationType(ObservationType.SNAPSHOT)
                .observedAt(now).monotonicMs(monotonicMs).provenance(new DtcProvenance("elm327-emulator-live", adapterId)).build());
        }
        return out;
    }

    private static String send(OutputStream out, InputStream in, String cmd) throws IOException {
        out.write((cmd + "\r").getBytes());
        out.flush();
        StringBuilder response = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            char c = (char) b;
            if (c == '>') break;
            response.append(c);
        }
        return response.toString();
    }
}
