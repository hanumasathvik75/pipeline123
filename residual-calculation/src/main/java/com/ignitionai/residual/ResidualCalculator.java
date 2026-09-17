package com.ignitionai.residual;

public class ResidualCalculator {
    public static Double calculateResidual(Double observedValue, Double expectedValue) {
        if (observedValue == null || expectedValue == null) {
            return null;
        }
        return observedValue - expectedValue;
    }
    
    public static Double calculateNormalizedResidual(Double residual, Double uncertainty) {
        if (residual == null || uncertainty == null || uncertainty == 0.0) {
            return null;
        }
        return residual / uncertainty;
    }
}
