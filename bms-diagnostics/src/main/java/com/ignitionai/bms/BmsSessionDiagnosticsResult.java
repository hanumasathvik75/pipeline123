package com.ignitionai.bms;

/** Real, single-session BMS diagnostics -- every field here is computed from one session's own
 * telemetry and needs no multi-day history, unlike degradation trend. */
public final class BmsSessionDiagnosticsResult {
    public final String sessionId;
    public final int readingCount;
    public final Double meanVoltageImbalanceV, maxVoltageImbalanceV;
    public final Double meanSocDisagreementPct, maxSocDisagreementPct;
    public final Double maxThermalSpreadC, maxCellTempC, maxFetTempC;
    public final Double thermalDeratingCorrelation;
    public final boolean cellImbalanceFlagged, socDisagreementFlagged, thermalSpreadFlagged;
    public final Double representativeSohPct;
    public final double confidence;

    public BmsSessionDiagnosticsResult(String sessionId, int readingCount,
            Double meanVoltageImbalanceV, Double maxVoltageImbalanceV,
            Double meanSocDisagreementPct, Double maxSocDisagreementPct,
            Double maxThermalSpreadC, Double maxCellTempC, Double maxFetTempC,
            Double thermalDeratingCorrelation,
            boolean cellImbalanceFlagged, boolean socDisagreementFlagged, boolean thermalSpreadFlagged,
            Double representativeSohPct, double confidence) {
        this.sessionId = sessionId;
        this.readingCount = readingCount;
        this.meanVoltageImbalanceV = meanVoltageImbalanceV;
        this.maxVoltageImbalanceV = maxVoltageImbalanceV;
        this.meanSocDisagreementPct = meanSocDisagreementPct;
        this.maxSocDisagreementPct = maxSocDisagreementPct;
        this.maxThermalSpreadC = maxThermalSpreadC;
        this.maxCellTempC = maxCellTempC;
        this.maxFetTempC = maxFetTempC;
        this.thermalDeratingCorrelation = thermalDeratingCorrelation;
        this.cellImbalanceFlagged = cellImbalanceFlagged;
        this.socDisagreementFlagged = socDisagreementFlagged;
        this.thermalSpreadFlagged = thermalSpreadFlagged;
        this.representativeSohPct = representativeSohPct;
        this.confidence = confidence;
    }
}
