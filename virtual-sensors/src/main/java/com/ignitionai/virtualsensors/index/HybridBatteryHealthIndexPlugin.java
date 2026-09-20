package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Hybrid/EV traction battery health, from SAE J1979 Mode 01 PID 0x5B ("Hybrid Battery Pack
 * Remaining Life") -- the one HV-battery figure genuinely standardized over OBD-II. It is a
 * single aggregate percentage, not the per-cell/SoC-estimator detail the bms-diagnostics module
 * computes from real BMS CAN exports; that level of detail is manufacturer-proprietary CAN
 * traffic, not reachable from a generic OBD-II port. Most non-hybrid/EV vehicles never implement
 * this PID at all -- that is reported as UNSUPPORTED, not a low score, since "no PID" and "bad
 * battery" are different facts a technician needs told apart.
 */
public final class HybridBatteryHealthIndexPlugin implements DiagnosticIndexPlugin {
    private static final String ID = "hybrid_battery_health";
    private static final String SIGNAL = "hybrid_battery_pack_life";

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Hybrid Battery Health"; }
    @Override public String getPluginVersion() { return "j1979-pid5b.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of(SIGNAL); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        if (!SeriesUtil.hasSignal(session, SIGNAL)) {
            return DiagnosticIndexResult.unsupported(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Vehicle did not report PID 0x5B (Hybrid Battery Pack Remaining Life) -- likely a non-hybrid/EV "
                    + "vehicle, or this PID isn't implemented on this ECU. Not scored as unhealthy: absence of the "
                    + "PID is a different fact from a degraded battery.", getPluginVersion());
        }
        DiagnosticIndexConfig cfg = DiagnosticIndexConfig.get();
        double flagBelowPct = cfg.number(ID, "flag_below_pct");

        List<SeriesUtil.Sample> life = SeriesUtil.series(session, SIGNAL);
        if (life.isEmpty()) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "PID 0x5B was observed but no reading passed quality validation", getPluginVersion());
        }

        double meanLife = life.stream().mapToDouble(s -> s.v).average().orElse(0);
        double latestLife = life.get(life.size() - 1).v;
        boolean flagged = meanLife < flagBelowPct;
        // A handful of polls per session is realistic (this PID is polled far less often than the
        // core engine PIDs), so confidence ramps up over a small sample count rather than the
        // hundreds used for continuously-streamed signals like voltage.
        double confidence = 100.0 * SeriesUtil.clamp01(life.size() / 5.0);

        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        evidence.put("mean_remaining_life_pct", String.format(Locale.ROOT, "%.1f", meanLife));
        evidence.put("latest_remaining_life_pct", String.format(Locale.ROOT, "%.1f", latestLife));
        evidence.put("flag_below_pct", String.valueOf(flagBelowPct));
        evidence.put("samples", String.valueOf(life.size()));
        return DiagnosticIndexResult.of(getIndexId(), getDisplayName(), meanLife, confidence,
            flagged ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL,
            getRequiredSignalIds(), evidence, getPluginVersion());
    }
}
