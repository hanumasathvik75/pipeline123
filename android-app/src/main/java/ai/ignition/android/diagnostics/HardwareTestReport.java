package ai.ignition.android.diagnostics;

import android.os.Build;
import ai.ignition.android.obd.DtcParser;
import ai.ignition.android.obd.Elm327Initializer;
import ai.ignition.android.obd.LivePidDecoder;
import ai.ignition.android.obd.ObdProtocolInfo;
import ai.ignition.android.obd.SupportedPidDiscovery;
import ai.ignition.android.obd.TransportType;
import ai.ignition.android.obd.VinParser;

import java.time.Instant;
import java.util.List;

/**
 * Builds a reproducible, privacy-safe Markdown report of the physical hardware verification run.
 */
public class HardwareTestReport {

    public enum VerificationOutcome {
        PASS("✅ PASS"),
        PARTIAL("⚠️ PARTIAL"),
        FAIL("❌ FAIL");

        public final String display;
        VerificationOutcome(String display) { this.display = display; }
    }

    public final Instant timestamp;
    public final String appVersion;
    public final String deviceDescription;
    public final TransportType transportType;
    public final String transportEvidence;
    public final Elm327Initializer.InitResult initResult;
    public final ObdProtocolInfo protocolInfo;
    public final SupportedPidDiscovery.DiscoveryReport pidReport;
    public final List<LivePidDecoder.PidReading> liveReadings;
    public final VinParser.VinResult vinResult;
    public final List<DtcParser.DtcReport> dtcReports;
    public final VerificationOutcome outcome;
    public final String outcomeReason;
    public final boolean passed;
    public final String summary;

    public HardwareTestReport(String appVersion, String deviceDescription,
                              TransportType transportType, String transportEvidence,
                              Elm327Initializer.InitResult initResult,
                              ObdProtocolInfo protocolInfo,
                              SupportedPidDiscovery.DiscoveryReport pidReport,
                              List<LivePidDecoder.PidReading> liveReadings,
                              VinParser.VinResult vinResult,
                              List<DtcParser.DtcReport> dtcReports,
                              VerificationOutcome outcome,
                              String outcomeReason,
                              String summary) {
        this.timestamp = Instant.now();
        this.appVersion = appVersion;
        this.deviceDescription = deviceDescription;
        this.transportType = transportType;
        this.transportEvidence = transportEvidence;
        this.initResult = initResult;
        this.protocolInfo = protocolInfo;
        this.pidReport = pidReport;
        this.liveReadings = liveReadings != null ? List.copyOf(liveReadings) : List.of();
        this.vinResult = vinResult;
        this.dtcReports = dtcReports != null ? List.copyOf(dtcReports) : List.of();
        this.outcome = outcome;
        this.outcomeReason = outcomeReason;
        this.passed = outcome == VerificationOutcome.PASS;
        this.summary = summary;
    }

    /**
     * Formats the report as GitHub-Flavored Markdown.
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# IgnitionAI — ELM327 Hardware Verification Report\n\n");
        sb.append("**Generated At:** ").append(timestamp).append("\n");
        sb.append("**App Version:** ").append(appVersion).append("\n");
        sb.append("**Android Device:** ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
                .append(" (Android ").append(Build.VERSION.RELEASE).append(", SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("**Overall Milestone Outcome:** ").append(outcome.display).append("\n");
        sb.append("**Outcome Details:** ").append(outcomeReason).append("\n");
        sb.append("**Summary:** ").append(summary).append("\n\n");

        sb.append("### Verification Checklist\n\n");
        sb.append("| Category | Check | Status | Details |\n");
        sb.append("|---|---|---|---|\n");
        sb.append("| **1. Transport** | Classification | ")
                .append(transportType != TransportType.UNKNOWN_NEEDS_DOCUMENTATION ? "✅ PASS" : "❌ FAIL")
                .append(" | `").append(transportType).append("` |\n");
        sb.append("| **2. Connection** | Physical Transport | ")
                .append(initResult != null && initResult.success ? "✅ PASS" : "❌ FAIL")
                .append(" | `").append(deviceDescription).append("` |\n");
        sb.append("| **3. Adapter Init** | AT Commands (ATZ, ATI, etc.) | ")
                .append(initResult != null && initResult.success ? "✅ PASS" : "❌ FAIL")
                .append(" | ").append(initResult != null ? initResult.adapterIdentity : "None").append(" |\n");
        sb.append("| **4. Protocol** | Vehicle OBD Bus Sync | ")
                .append(protocolInfo != null && !protocolInfo.normalizedName.equals("UNSET") ? "✅ PASS" : "⚠️ UNSET")
                .append(" | ").append(protocolInfo != null ? protocolInfo.normalizedName : "Unset").append(" |\n");
        sb.append("| **5. PIDs** | Supported PID Discovery (0100) | ")
                .append(pidReport != null && pidReport.success ? "✅ PASS" : "❌ FAIL")
                .append(" | ").append(pidReport != null ? pidReport.allSupportedPids.size() + " supported PIDs" : "None").append(" |\n");
        sb.append("| **6. Telemetry** | Live Signals (RPM, Speed, etc.) | ")
                .append(!liveReadings.isEmpty() ? "✅ PASS" : "⚠️ NO DATA")
                .append(" | ").append(liveReadings.size()).append(" live signal(s) decoded |\n");
        sb.append("| **7. VIN** | Mode 09 PID 02 | ")
                .append(vinResult != null && vinResult.status == VinParser.VinStatus.VALID ? "✅ PASS" :
                        (vinResult != null && vinResult.status == VinParser.VinStatus.INCOMPLETE ? "⚠️ INCOMPLETE" :
                        (vinResult != null && vinResult.status == VinParser.VinStatus.UNSUPPORTED ? "ℹ️ UNSUPPORTED" : "❌ FAIL")))
                .append(" | ").append(vinResult != null ? vinResult.notes : "Not requested").append(" |\n");
        sb.append("| **8. DTCs** | Fault Codes (Modes 03, 07, 0A) | ")
                .append(!dtcReports.isEmpty() ? "✅ PASS" : "⚠️ NO DATA")
                .append(" | ").append(dtcReports.size()).append(" mode(s) queried |\n\n");

        sb.append("## 1. Transport Identification & Evidence\n\n");
        sb.append("* **Selected Device:** `").append(deviceDescription).append("`\n");
        sb.append("* **Confirmed Transport:** `").append(transportType).append("`\n");
        sb.append("```text\n").append(transportEvidence).append("```\n\n");

        sb.append("## 2. Adapter Initialization (AT Commands)\n\n");
        if (initResult != null) {
            sb.append("| Command | Purpose | Raw Response |\n");
            sb.append("|---|---|---|\n");
            sb.append("| `ATZ` | Adapter Reset | `").append(escape(initResult.resetResponse)).append("` |\n");
            sb.append("| `ATI` | Adapter Identity | `").append(escape(initResult.adapterIdentity)).append("` |\n");
            sb.append("| `ATE0` | Echo Off | `").append(escape(initResult.echoResponse)).append("` |\n");
            sb.append("| `ATL0` | Linefeed Off | `").append(escape(initResult.linefeedResponse)).append("` |\n");
            sb.append("| `ATS0` | Spaces Off | `").append(escape(initResult.spacesResponse)).append("` |\n");
            sb.append("\n* **Init Status:** ").append(initResult.success ? "Success" : "Failed: " + initResult.errorMessage).append("\n\n");
        } else {
            sb.append("_Initialization was not reached._\n\n");
        }

        sb.append("## 3. Vehicle OBD Protocol\n\n");
        if (protocolInfo != null) {
            sb.append("* **Protocol Name (ATDP):** `").append(protocolInfo.rawProtocolName).append("`\n");
            sb.append("* **Protocol Number (ATDPN):** `").append(protocolInfo.rawProtocolNumber).append("`\n");
            sb.append("* **Normalized Protocol:** **").append(protocolInfo.normalizedName).append("**\n\n");
        } else {
            sb.append("_Protocol was not queried._\n\n");
        }

        sb.append("## 4. Supported PID Discovery (Mode 01 PID 00)\n\n");
        if (pidReport != null) {
            sb.append("* **Raw 0100 Response:** `").append(escape(pidReport.raw0100)).append("`\n");
            sb.append("* **Supported PIDs Count:** ").append(pidReport.allSupportedPids.size()).append("\n");
            sb.append("* **Decoded Supported PID Set:** `").append(String.join(", ", pidReport.allSupportedPids)).append("`\n\n");
        } else {
            sb.append("_PID discovery was not performed._\n\n");
        }

        sb.append("## 5. Live PID Verification (Sanity Check)\n\n");
        if (!liveReadings.isEmpty()) {
            sb.append("| Signal | PID | Value | Unit | Status | Raw Response |\n");
            sb.append("|---|---|---|---|---|---|\n");
            for (LivePidDecoder.PidReading r : liveReadings) {
                sb.append("| ").append(r.signalName).append(" | `").append(r.pid).append("` | ")
                        .append(r.value != null ? String.format("%.1f", r.value) : "N/A").append(" | ")
                        .append(r.unit).append(" | ").append(r.status).append(" | `")
                        .append(escape(r.rawResponse)).append("` |\n");
            }
            sb.append("\n");
        } else {
            sb.append("_No live PIDs were read._\n\n");
        }

        sb.append("## 6. Vehicle Identification Number (Mode 09 PID 02)\n\n");
        if (vinResult != null) {
            sb.append("* **VIN Status:** `").append(vinResult.status).append("`\n");
            sb.append("* **Redacted VIN (ISO 3779):** `").append(vinResult.redactedVin).append("`\n");
            sb.append("* **Notes:** ").append(vinResult.notes).append("\n\n");
        } else {
            sb.append("_VIN was not requested._\n\n");
        }

        sb.append("## 7. Diagnostic Trouble Codes (DTCs)\n\n");
        if (!dtcReports.isEmpty()) {
            sb.append("| Mode | Purpose | Status | Codes Found | Raw Response |\n");
            sb.append("|---|---|---|---|---|\n");
            for (DtcParser.DtcReport d : dtcReports) {
                sb.append("| `").append(d.mode).append("` | ").append(getModeLabel(d.mode)).append(" | ")
                        .append(d.status).append(" | ")
                        .append(d.codes.isEmpty() ? "None" : String.join(", ", d.codes)).append(" | `")
                        .append(escape(d.rawResponse)).append("` |\n");
            }
            sb.append("\n");
        } else {
            sb.append("_No DTC queries were executed._\n\n");
        }

        sb.append("## 8. Safety Guardrails & Compliance\n\n");
        sb.append("- [x] Read-only operations only.\n");
        sb.append("- [x] Mode 04 code-clearing commands strictly blocked in transport layer.\n");
        sb.append("- [x] Mode 08 actuator/control commands strictly blocked in transport layer.\n");
        sb.append("- [x] Sensitive identifiers (MAC addresses, center 10 characters of VIN) redacted in logs.\n");

        return sb.toString();
    }

    private static String getModeLabel(String mode) {
        switch (mode) {
            case "03": return "Stored / Confirmed";
            case "07": return "Pending";
            case "0A": return "Permanent";
            default: return "Diagnostic Mode";
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\n", " ").replace("\r", " ").replace("|", "\\|");
    }
}
