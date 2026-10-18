package ai.ignition.android.diagnostics;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import ai.ignition.android.obd.AdapterIdentifier;
import ai.ignition.android.obd.DtcParser;
import ai.ignition.android.obd.Elm327Initializer;
import ai.ignition.android.obd.LivePidDecoder;
import ai.ignition.android.obd.ObdProtocolInfo;
import ai.ignition.android.obd.ObdTransport;
import ai.ignition.android.obd.SupportedPidDiscovery;
import ai.ignition.android.obd.TransportType;
import ai.ignition.android.obd.VinParser;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates the physical ELM327 verification workflow asynchronously on a worker thread.
 */
public class HardwareTestRunner {

    public interface TestCallback {
        void onProgress(String status);
        void onLog(String line);
        void onComplete(HardwareTestReport report, File savedReportFile);
        void onError(String error);
    }

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HardwareTestRunner(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Executes the verification pipeline on a background thread.
     */
    public void runHardwareTest(BluetoothDevice device, TestCallback callback) {
        executor.execute(() -> {
            ObdTransport transport = null;
            try {
                // Step 1: In-code transport identification
                callback.onProgress("Step 1/8: Identifying adapter transport…");
                AdapterIdentifier.IdentificationResult ident = AdapterIdentifier.identify(context, device);
                callback.onLog("Transport Identification: " + ident.transportType);
                callback.onLog("Transport Evidence:\n" + ident.evidence.trim());

                if (ident.transportType == TransportType.UNKNOWN_NEEDS_DOCUMENTATION || ident.transport == null) {
                    callback.onError("Cannot proceed: Adapter transport is unknown or lacks pairing documentation.\n" + ident.evidence);
                    return;
                }

                transport = ident.transport;

                // Step 2: Establish connection
                callback.onProgress("Step 2/8: Connecting to " + transport.getDeviceDescription() + "…");
                callback.onLog("Connecting via " + transport.getTransportType() + "…");
                transport.connect(Duration.ofSeconds(6));
                callback.onLog("Connected successfully to physical adapter!");

                // Step 3: Adapter initialization
                callback.onProgress("Step 3/8: Initializing ELM327 (ATZ, ATI, ATE0, ATL0, ATS0)…");
                Elm327Initializer.InitResult initResult = Elm327Initializer.initialize(transport);
                callback.onLog("ATI (Adapter Identity): " + initResult.adapterIdentity);
                callback.onLog("Init status: " + (initResult.success ? "OK" : initResult.errorMessage));

                // Step 4: Mode 01 PID 00 Supported PID Discovery
                callback.onProgress("Step 4/8: Querying supported PIDs (0100)…");
                SupportedPidDiscovery.DiscoveryReport pidReport = SupportedPidDiscovery.discover(transport);
                callback.onLog("Supported PIDs Count: " + pidReport.allSupportedPids.size());
                callback.onLog("Supported PID Set: " + String.join(", ", pidReport.allSupportedPids));

                // Step 5: Vehicle OBD Protocol
                callback.onProgress("Step 5/8: Querying vehicle OBD protocol (ATDP/ATDPN)…");
                ObdProtocolInfo protocolInfo = Elm327Initializer.queryProtocol(transport);
                callback.onLog("Vehicle Protocol: " + protocolInfo);

                // Step 6: Live PID sanity check
                callback.onProgress("Step 6/8: Reading supported live diagnostic signals…");
                List<LivePidDecoder.PidReading> liveReadings = new ArrayList<>();
                if (pidReport.isPidSupported("0C")) {
                    LivePidDecoder.PidReading rpm = LivePidDecoder.readRpm(transport);
                    liveReadings.add(rpm);
                    callback.onLog("  -> " + rpm);
                }
                if (pidReport.isPidSupported("05")) {
                    LivePidDecoder.PidReading coolant = LivePidDecoder.readCoolant(transport);
                    liveReadings.add(coolant);
                    callback.onLog("  -> " + coolant);
                }
                if (pidReport.isPidSupported("0D")) {
                    LivePidDecoder.PidReading speed = LivePidDecoder.readSpeed(transport);
                    liveReadings.add(speed);
                    callback.onLog("  -> " + speed);
                }
                if (pidReport.isPidSupported("04")) {
                    LivePidDecoder.PidReading load = LivePidDecoder.readEngineLoad(transport);
                    liveReadings.add(load);
                    callback.onLog("  -> " + load);
                }

                // Step 7: Mode 09 PID 02 VIN Query
                callback.onProgress("Step 7/8: Requesting vehicle VIN (Mode 09 PID 02)…");
                VinParser.VinResult vinResult = VinParser.requestVin(transport);
                callback.onLog("VIN Result: " + vinResult);

                // Step 8: Read-only DTCs (03, 07, 0A)
                callback.onProgress("Step 8/8: Querying fault codes (Modes 03, 07, 0A)…");
                List<DtcParser.DtcReport> dtcReports = DtcParser.readAllDtcModes(transport);
                for (DtcParser.DtcReport dtc : dtcReports) {
                    callback.onLog("  -> " + dtc);
                }

                // Determine milestone verification outcome based on explicit criteria
                boolean connOk = transport.isConnected();
                boolean initOk = initResult != null && initResult.success;
                boolean pidOk = pidReport != null && pidReport.success && !pidReport.allSupportedPids.isEmpty();
                boolean liveOk = !liveReadings.isEmpty();
                boolean vinValid = vinResult != null && vinResult.status == VinParser.VinStatus.VALID;
                boolean vinUnsupported = vinResult != null && vinResult.status == VinParser.VinStatus.UNSUPPORTED;
                boolean vinIncomplete = vinResult != null && vinResult.status == VinParser.VinStatus.INCOMPLETE;
                boolean dtcOk = !dtcReports.isEmpty();

                HardwareTestReport.VerificationOutcome outcome;
                String outcomeReason;

                if (!connOk || !initOk || !pidOk) {
                    outcome = HardwareTestReport.VerificationOutcome.FAIL;
                    outcomeReason = "Critical adapter communication failure (Connection, Init, or PID discovery failed)";
                } else if (vinValid && liveOk && dtcOk) {
                    outcome = HardwareTestReport.VerificationOutcome.PASS;
                    outcomeReason = "All mandatory checks passed including valid 17-char ISO 3779 VIN, live telemetry, and DTCs.";
                } else {
                    outcome = HardwareTestReport.VerificationOutcome.PARTIAL;
                    if (vinIncomplete) {
                        outcomeReason = "Adapter communication verified, but VIN is incomplete (" +
                                (vinResult.rawVin != null ? vinResult.rawVin.length() : 0) + " chars received: '" +
                                vinResult.redactedVin + "' - expected 17).";
                    } else if (vinUnsupported) {
                        outcomeReason = "Adapter communication verified, but Mode 09 PID 02 (VIN) is unsupported by vehicle ECU.";
                    } else if (!liveOk) {
                        outcomeReason = "Adapter communication verified, but no live telemetry signals were decoded.";
                    } else {
                        outcomeReason = "Adapter communication verified with partial diagnostic data.";
                    }
                }

                String summary = outcome == HardwareTestReport.VerificationOutcome.PASS
                        ? "Physical adapter communication and complete OBD-II query pipeline verified."
                        : "Verification outcome: " + outcome.display + " - " + outcomeReason;

                HardwareTestReport report = new HardwareTestReport(
                        "0.1.0-hardware-preview",
                        transport.getDeviceDescription(),
                        ident.transportType,
                        ident.evidence,
                        initResult,
                        protocolInfo,
                        pidReport,
                        liveReadings,
                        vinResult,
                        dtcReports,
                        outcome,
                        outcomeReason,
                        summary
                );

                // Persist markdown report to app-private storage
                File reportFolder = new File(context.getFilesDir(), "reports");
                reportFolder.mkdirs();
                File reportFile = new File(reportFolder, "elm327_report_" + System.currentTimeMillis() + ".md");
                try (FileOutputStream fos = new FileOutputStream(reportFile)) {
                    fos.write(report.toMarkdown().getBytes(StandardCharsets.UTF_8));
                }
                callback.onLog("\nReport saved to: " + reportFile.getAbsolutePath());

                callback.onComplete(report, reportFile);

            } catch (Exception e) {
                callback.onError("Verification aborted with exception: " + e.getMessage());
            } finally {
                if (transport != null) {
                    try {
                        transport.close();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
