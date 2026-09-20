package com.ignitionai.degradation.risk;

import java.util.HashMap;
import java.util.Map;

/**
 * WARNING: baselineHazard/betaCoefficient below are INVENTED PLACEHOLDER NUMBERS, not a calibrated
 * survival model fitted to real failure data. They currently do nothing in production: every caller
 * passes isCalibrated=false to EventRiskEstimator.estimateRisk(), which short-circuits before these
 * values are ever used and returns RiskStatus.UNCALIBRATED instead. Do not flip isCalibrated to true
 * anywhere until these coefficients are actually fitted against real vehicle failure/repair outcome
 * data -- doing so would present a fabricated failure-probability percentage as if it were real.
 */
public class HazardModelConfig {
    private String endpointLabel;
    private double baselineHazard; // h0
    private double betaCoefficient; // Beta for latent state

    // Default configs for specific endpoints
    private static final Map<String, HazardModelConfig> endpoints = new HashMap<>();

    static {
        endpoints.put("critical_failure", new HazardModelConfig("critical_failure", 0.01, 2.5));
        endpoints.put("mild_failure", new HazardModelConfig("mild_failure", 0.05, 1.2));
    }

    public HazardModelConfig(String endpointLabel, double baselineHazard, double betaCoefficient) {
        this.endpointLabel = endpointLabel;
        this.baselineHazard = baselineHazard;
        this.betaCoefficient = betaCoefficient;
    }

    public static HazardModelConfig get(String endpointLabel) {
        return endpoints.get(endpointLabel);
    }

    public String getEndpointLabel() { return endpointLabel; }
    public double getBaselineHazard() { return baselineHazard; }
    public double getBetaCoefficient() { return betaCoefficient; }
}
