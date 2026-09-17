package com.ignitionai.obdinput.preprocessor;

import java.util.Set;
import java.util.List;

public class SessionInspectionReport {
    private final String sessionId;
    private final double minimumSignalCoverage;
    private final List<String> missingRequiredSignals;
    private final String inspectionAcceptanceStatus; // e.g. "ACCEPTED", "LIMITED_COVERAGE", "INSUFFICIENT_DATA"

    public SessionInspectionReport(String sessionId, double minimumSignalCoverage, List<String> missingRequiredSignals, String inspectionAcceptanceStatus) {
        this.sessionId = sessionId;
        this.minimumSignalCoverage = minimumSignalCoverage;
        this.missingRequiredSignals = missingRequiredSignals;
        this.inspectionAcceptanceStatus = inspectionAcceptanceStatus;
    }

    public String getSessionId() { return sessionId; }
    public double getMinimumSignalCoverage() { return minimumSignalCoverage; }
    public List<String> getMissingRequiredSignals() { return missingRequiredSignals; }
    public String getInspectionAcceptanceStatus() { return inspectionAcceptanceStatus; }
}
