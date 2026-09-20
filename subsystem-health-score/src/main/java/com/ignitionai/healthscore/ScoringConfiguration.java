package com.ignitionai.healthscore;

import com.ignitionai.obdinput.storage.MiniJson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ScoringConfiguration {
    private final double degradationWeight;
    private final double eventRiskWeight;
    private final double persistenceWeight;
    private final double recurrenceWeight;
    private final String calculationVersion;

    private static ScoringConfiguration DEFAULT;

    public ScoringConfiguration(double degradationWeight, double eventRiskWeight,
                                double persistenceWeight, double recurrenceWeight, String calculationVersion) {
        this.degradationWeight = degradationWeight;
        this.eventRiskWeight = eventRiskWeight;
        this.persistenceWeight = persistenceWeight;
        this.recurrenceWeight = recurrenceWeight;
        this.calculationVersion = calculationVersion;
    }

    /** Loads weights from config/scoring-config.json (classpath resource) so they can be reviewed
     * and revised without recompiling; see that file's "rationale" for why these particular values
     * were chosen and their calibration status. */
    public static synchronized ScoringConfiguration getDefault() {
        if (DEFAULT != null) return DEFAULT;
        try (InputStream in = ScoringConfiguration.class.getClassLoader().getResourceAsStream("config/scoring-config.json")) {
            if (in == null) throw new IllegalStateException("config/scoring-config.json not found on classpath");
            @SuppressWarnings("unchecked")
            Map<String, Object> cfg = (Map<String, Object>) MiniJson.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            DEFAULT = new ScoringConfiguration(
                ((Number) cfg.get("degradation_weight")).doubleValue(),
                ((Number) cfg.get("event_risk_weight")).doubleValue(),
                ((Number) cfg.get("persistence_weight")).doubleValue(),
                ((Number) cfg.get("recurrence_weight")).doubleValue(),
                (String) cfg.get("calculation_version"));
            return DEFAULT;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load config/scoring-config.json", e);
        }
    }

    public double getDegradationWeight() { return degradationWeight; }
    public double getEventRiskWeight() { return eventRiskWeight; }
    public double getPersistenceWeight() { return persistenceWeight; }
    public double getRecurrenceWeight() { return recurrenceWeight; }
    public String getCalculationVersion() { return calculationVersion; }
}
