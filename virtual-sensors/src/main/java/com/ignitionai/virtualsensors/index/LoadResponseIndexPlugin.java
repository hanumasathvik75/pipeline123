package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Air-path / throttle response lag: how long engine_rpm takes to start rising after a genuine
 * throttle tip-in. Detects tip-in events as throttle_position rising faster than a configured
 * rate, then finds when engine_rpm's own rate of change first exceeds a configured response rate
 * within the look-ahead window. All thresholds live in config/diagnostic-index-config.json under
 * "load_response". A raw scan tool has no notion of "response time" between two channels — this
 * only exists by relating two signals over time, which is exactly what a single-PID reading
 * cannot do.
 */
public final class LoadResponseIndexPlugin implements DiagnosticIndexPlugin {
    private static final String ID = "load_response";

    @Override public String getIndexId() { return ID; }
    @Override public String getDisplayName() { return "Load Response"; }
    @Override public String getPluginVersion() { return "tip-in-to-rpm-lag.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("throttle_position", "engine_rpm"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        DiagnosticIndexConfig cfg = DiagnosticIndexConfig.get();
        double tipInRatePctPerS = cfg.number(ID, "tip_in_rate_pct_per_s");
        double minTipInDeltaPct = cfg.number(ID, "min_tip_in_delta_pct");
        double rpmResponseRatePerS = cfg.number(ID, "rpm_response_rate_per_s");
        long lookAheadMs = (long) cfg.number(ID, "look_ahead_ms");
        double maxLagSeconds = cfg.number(ID, "max_lag_seconds");
        double minEvents = cfg.number(ID, "min_events");

        List<SeriesUtil.Sample> throttle = SeriesUtil.series(session, "throttle_position");
        List<SeriesUtil.Sample> rpm = SeriesUtil.series(session, "engine_rpm");
        if (throttle.size() < 10 || rpm.size() < 10) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Fewer than 10 valid throttle_position or engine_rpm samples", getPluginVersion());
        }
        List<Double> lags = new java.util.ArrayList<>();
        for (int i = 1; i < throttle.size(); i++) {
            double dt = (throttle.get(i).t - throttle.get(i - 1).t) / 1000.0;
            if (dt <= 0) continue;
            double dThrottle = throttle.get(i).v - throttle.get(i - 1).v;
            double ratePerS = dThrottle / dt;
            if (ratePerS < tipInRatePctPerS || dThrottle < minTipInDeltaPct) continue;
            long eventT = throttle.get(i).t;
            Double lag = firstRpmResponseLagSeconds(rpm, eventT, lookAheadMs, rpmResponseRatePerS);
            if (lag != null) lags.add(lag);
        }
        if (lags.size() < minEvents) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Fewer than " + (int) minEvents + " clean throttle tip-in events detected in this session", getPluginVersion());
        }
        double meanLag = SeriesUtil.mean(lags);
        double health = 100.0 * (1.0 - SeriesUtil.clamp01(Math.max(0, meanLag - 0.4) / (maxLagSeconds - 0.4 + 0.01)));
        double confidence = 100.0 * SeriesUtil.clamp01(lags.size() / 8.0) * 0.85;
        DiagnosticIndexResult.State state = meanLag > maxLagSeconds ? DiagnosticIndexResult.State.FLAGGED : DiagnosticIndexResult.State.NORMAL;

        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        evidence.put("response_signal", "engine_rpm");
        evidence.put("events_detected", String.valueOf(lags.size()));
        evidence.put("estimated_lag_seconds", String.format(Locale.ROOT, "%.3f", meanLag));
        evidence.put("max_lag_seconds", String.format(Locale.ROOT, "%.1f", maxLagSeconds));
        return DiagnosticIndexResult.of(getIndexId(), getDisplayName(), health, confidence, state,
            getRequiredSignalIds(), evidence, getPluginVersion());
    }

    private Double firstRpmResponseLagSeconds(List<SeriesUtil.Sample> rpm, long eventT, long lookAheadMs, double rpmResponseRatePerS) {
        int start = -1;
        for (int i = 0; i < rpm.size(); i++) if (rpm.get(i).t >= eventT) { start = i; break; }
        if (start < 0) return null;
        double baseline = rpm.get(start).v;
        for (int i = start + 1; i < rpm.size(); i++) {
            long dtMs = rpm.get(i).t - eventT;
            if (dtMs > lookAheadMs) break;
            double elapsedS = (rpm.get(i).t - rpm.get(start).t) / 1000.0;
            if (elapsedS <= 0) continue;
            double rate = (rpm.get(i).v - baseline) / elapsedS;
            if (rate > rpmResponseRatePerS) return dtMs / 1000.0;
        }
        return null;
    }
}
