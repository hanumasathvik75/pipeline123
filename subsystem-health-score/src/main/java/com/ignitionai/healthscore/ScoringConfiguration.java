package com.ignitionai.healthscore;

public class ScoringConfiguration {
    private final double degradationWeight;
    private final double eventRiskWeight;
    private final double persistenceWeight;
    private final double recurrenceWeight;
    private final String calculationVersion;

    public ScoringConfiguration(double degradationWeight, double eventRiskWeight, 
                                double persistenceWeight, double recurrenceWeight, String calculationVersion) {
        this.degradationWeight = degradationWeight;
        this.eventRiskWeight = eventRiskWeight;
        this.persistenceWeight = persistenceWeight;
        this.recurrenceWeight = recurrenceWeight;
        this.calculationVersion = calculationVersion;
    }

    public static ScoringConfiguration getDefault() {
        return new ScoringConfiguration(0.5, 30.0, 10.0, 10.0, "1.1");
    }

    public double getDegradationWeight() { return degradationWeight; }
    public double getEventRiskWeight() { return eventRiskWeight; }
    public double getPersistenceWeight() { return persistenceWeight; }
    public double getRecurrenceWeight() { return recurrenceWeight; }
    public String getCalculationVersion() { return calculationVersion; }
}
