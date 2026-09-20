package com.ignitionai.features;

import com.ignitionai.features.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class FeatureCatalogue {
    private Map<String, Feature> features = new HashMap<>();

    public FeatureCatalogue() {
        // Auto-register core features
        register(new CoolantWarmupRateFeature());
        register(new IdleRpmStabilityFeature());
        register(new MissingDataFractionFeature());
        register(new SpeedMpsFeature());
        register(new TimestampSecondsFeature());
        register(new WindowedStatisticFeature());
        register(new FuelTrimSummaryFeature());
        register(new VoltageStatisticsFeature());
        register(new RegimeDurationFeature());
        register(new EngineRunningFlagFeature());
        register(new IdleFlagFeature());
        register(new OilMinusCoolantFeature());
        register(new RpmRadPerSecFeature());
        register(new AccelerationMps2Feature());
        register(new CruiseFlagFeature());
        register(new AccelEventFlagFeature());
        register(new DecelEventFlagFeature());
        register(new CoolantMinusAmbientFeature());
        register(new OilMinusAmbientFeature());
        register(new BoostGaugeFeature());
        register(new PedalThrottleGapFeature());
        register(new ThrottleCommandGapFeature());
        register(new InstantFuelEconomyFeature());
        register(new FuelTrimImbalanceFeature());
        register(new SensorCoverageFeature());
    }

    public void register(Feature feature) {
        features.put(feature.getFeatureId(), feature);
    }

    public Feature getFeature(String featureId) {
        return features.get(featureId);
    }

    public List<Feature> getAllFeatures() {
        return new ArrayList<>(features.values());
    }
}
