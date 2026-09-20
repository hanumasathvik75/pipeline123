package com.ignitionai.severity;

import com.ignitionai.obdinput.storage.MiniJson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SeverityConfiguration {
    private final double criticalDegradationScoreThreshold;
    private final double criticalEventRiskThreshold;

    private final double degradedDegradationScoreThreshold;
    private final double degradedEventRiskThreshold;

    private final double watchDegradationScoreThreshold;
    private final double watchEventRiskThreshold;

    private static SeverityConfiguration DEFAULT;

    public SeverityConfiguration(double criticalDegradationScoreThreshold, double criticalEventRiskThreshold,
                                 double degradedDegradationScoreThreshold, double degradedEventRiskThreshold,
                                 double watchDegradationScoreThreshold, double watchEventRiskThreshold) {
        this.criticalDegradationScoreThreshold = criticalDegradationScoreThreshold;
        this.criticalEventRiskThreshold = criticalEventRiskThreshold;
        this.degradedDegradationScoreThreshold = degradedDegradationScoreThreshold;
        this.degradedEventRiskThreshold = degradedEventRiskThreshold;
        this.watchDegradationScoreThreshold = watchDegradationScoreThreshold;
        this.watchEventRiskThreshold = watchEventRiskThreshold;
    }

    /** Loads thresholds from config/severity-config.json (classpath resource) so they can be
     * reviewed and revised without recompiling; see that file's "rationale" for why these
     * particular values were chosen and their calibration status. */
    public static synchronized SeverityConfiguration getDefault() {
        if (DEFAULT != null) return DEFAULT;
        try (InputStream in = SeverityConfiguration.class.getClassLoader().getResourceAsStream("config/severity-config.json")) {
            if (in == null) throw new IllegalStateException("config/severity-config.json not found on classpath");
            @SuppressWarnings("unchecked")
            Map<String, Object> cfg = (Map<String, Object>) MiniJson.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            DEFAULT = new SeverityConfiguration(
                ((Number) cfg.get("critical_degradation_score_threshold")).doubleValue(),
                ((Number) cfg.get("critical_event_risk_threshold")).doubleValue(),
                ((Number) cfg.get("degraded_degradation_score_threshold")).doubleValue(),
                ((Number) cfg.get("degraded_event_risk_threshold")).doubleValue(),
                ((Number) cfg.get("watch_degradation_score_threshold")).doubleValue(),
                ((Number) cfg.get("watch_event_risk_threshold")).doubleValue());
            return DEFAULT;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load config/severity-config.json", e);
        }
    }

    public double getCriticalDegradationScoreThreshold() { return criticalDegradationScoreThreshold; }
    public double getCriticalEventRiskThreshold() { return criticalEventRiskThreshold; }
    public double getDegradedDegradationScoreThreshold() { return degradedDegradationScoreThreshold; }
    public double getDegradedEventRiskThreshold() { return degradedEventRiskThreshold; }
    public double getWatchDegradationScoreThreshold() { return watchDegradationScoreThreshold; }
    public double getWatchEventRiskThreshold() { return watchEventRiskThreshold; }
}
