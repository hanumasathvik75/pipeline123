package com.ignitionai.virtualsensors.index;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Session-level composite finding produced by a {@link DiagnosticIndexPlugin}.
 * This is the contract rendered in the certificate's "IgnitionAI Diagnostic Index" section —
 * the differentiated, evidence-first layer that a raw single-PID OBD scan does not produce.
 */
public final class DiagnosticIndexResult {
    public enum State { NORMAL, FLAGGED, INSUFFICIENT_DATA, UNSUPPORTED }

    private final String indexId;
    private final String displayName;
    private final Double healthPct;
    private final Double confidencePct;
    private final State state;
    private final List<String> contributingPids;
    private final Map<String, String> evidence;
    private final String reason;
    private final String pluginVersion;

    private DiagnosticIndexResult(String indexId, String displayName, Double healthPct, Double confidencePct,
            State state, List<String> contributingPids, Map<String, String> evidence, String reason, String pluginVersion) {
        this.indexId = indexId;
        this.displayName = displayName;
        this.healthPct = healthPct;
        this.confidencePct = confidencePct;
        this.state = state;
        this.contributingPids = List.copyOf(contributingPids);
        this.evidence = Map.copyOf(evidence);
        this.reason = reason;
        this.pluginVersion = pluginVersion;
    }

    public static DiagnosticIndexResult of(String indexId, String displayName, double healthPct, double confidencePct,
            State state, List<String> contributingPids, LinkedHashMap<String, String> evidence, String pluginVersion) {
        return new DiagnosticIndexResult(indexId, displayName, healthPct, confidencePct, state, contributingPids, evidence, null, pluginVersion);
    }

    public static DiagnosticIndexResult unsupported(String indexId, String displayName, List<String> contributingPids, String reason, String pluginVersion) {
        return new DiagnosticIndexResult(indexId, displayName, null, null, State.UNSUPPORTED, contributingPids, Map.of(), reason, pluginVersion);
    }

    public static DiagnosticIndexResult insufficientData(String indexId, String displayName, List<String> contributingPids, String reason, String pluginVersion) {
        return new DiagnosticIndexResult(indexId, displayName, null, null, State.INSUFFICIENT_DATA, contributingPids, Map.of(), reason, pluginVersion);
    }

    public String getIndexId() { return indexId; }
    public String getDisplayName() { return displayName; }
    public Double getHealthPct() { return healthPct; }
    public Double getConfidencePct() { return confidencePct; }
    public State getState() { return state; }
    public List<String> getContributingPids() { return contributingPids; }
    public Map<String, String> getEvidence() { return evidence; }
    public String getReason() { return reason; }
    public String getPluginVersion() { return pluginVersion; }
}
