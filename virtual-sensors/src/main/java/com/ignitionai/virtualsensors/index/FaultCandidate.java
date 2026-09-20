package com.ignitionai.virtualsensors.index;

import java.util.Map;

/**
 * A ranked root-cause candidate produced by {@link FaultCandidateRanker}'s probabilistic fault
 * graph. likelihoodPct is a posterior probability from a naive-Bayes-style combination of every
 * corroborating piece of evidence this session found (flagged Diagnostic Index results and DTCs)
 * — not a calibrated failure probability, since the underlying association strengths are domain
 * reasoning, not fitted to real repair-outcome data. It orders candidates for a technician, it
 * does not replace physical diagnosis.
 */
public final class FaultCandidate {
    private final String name;
    private final double likelihoodPct;
    private final String rootCauseHypothesis;
    private final String corroboration;
    private final String sourceIndexId;
    private final Map<String, String> supportingEvidence;

    public FaultCandidate(String name, double likelihoodPct, String rootCauseHypothesis, String corroboration,
            String sourceIndexId, Map<String, String> supportingEvidence) {
        this.name = name;
        this.likelihoodPct = likelihoodPct;
        this.rootCauseHypothesis = rootCauseHypothesis;
        this.corroboration = corroboration;
        this.sourceIndexId = sourceIndexId;
        this.supportingEvidence = Map.copyOf(supportingEvidence);
    }

    public String getName() { return name; }
    public double getLikelihoodPct() { return likelihoodPct; }
    public String getRootCauseHypothesis() { return rootCauseHypothesis; }
    public String getCorroboration() { return corroboration; }
    public String getSourceIndexId() { return sourceIndexId; }
    public Map<String, String> getSupportingEvidence() { return supportingEvidence; }
}
