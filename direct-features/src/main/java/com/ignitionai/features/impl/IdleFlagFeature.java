package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.OperatingRegime;

import java.util.List;

/** Drive-state flag: 1.0 while the vehicle context classifies the current operating regime as
 * WARM_IDLE, 0.0 otherwise. Reuses the same regime classification IdleRpmStabilityFeature already
 * depends on rather than re-deriving an idle threshold from RPM directly. */
public class IdleFlagFeature implements Feature {

    @Override
    public String getFeatureId() { return "idle_flag"; }

    @Override
    public String getName() { return "Idle (Flag)"; }

    @Override
    public List<String> getInputs() { return List.of("engine_rpm"); }

    @Override
    public String getUnits() { return "boolean"; }

    @Override
    public String getFormula() { return "operating_regime == WARM_IDLE"; }

    @Override
    public String getWindow() { return "instantaneous"; }

    @Override
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No context timestamp");

        double flag = context.getOperatingConditions().getOperatingRegime() == OperatingRegime.WARM_IDLE ? 1.0 : 0.0;

        return new AnalyticalObservation(getFeatureId(), flag, getUnits(), ts, QualityState.AVAILABLE,
                List.of("CTX-REGIME-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
