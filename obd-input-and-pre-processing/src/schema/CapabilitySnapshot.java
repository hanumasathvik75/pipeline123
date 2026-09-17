package com.ignitionai.obdinput.schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CapabilitySnapshot {
    private final List<String> supportedSignals;
    private final List<String> supportedDtcServices;
    private final List<String> ecuIds;
    private final Map<String, Double> requestedRateHz;
    private final Map<String, Double> observedRateHz;
    private final CapabilitySource capabilitySource;
    private final Instant observedAt;

    private CapabilitySnapshot(Builder builder) {
        this.supportedSignals = builder.supportedSignals != null ? List.copyOf(builder.supportedSignals) : List.of();
        this.supportedDtcServices = builder.supportedDtcServices != null ? List.copyOf(builder.supportedDtcServices) : List.of();
        this.ecuIds = builder.ecuIds != null ? List.copyOf(builder.ecuIds) : List.of();
        this.requestedRateHz = builder.requestedRateHz != null ? Map.copyOf(builder.requestedRateHz) : Map.of();
        this.observedRateHz = builder.observedRateHz != null ? Map.copyOf(builder.observedRateHz) : Map.of();
        this.capabilitySource = Objects.requireNonNull(builder.capabilitySource, "capabilitySource is required");
        this.observedAt = Objects.requireNonNull(builder.observedAt, "observedAt is required");
    }

    public List<String> getSupportedSignals() { return supportedSignals; }
    public List<String> getSupportedDtcServices() { return supportedDtcServices; }
    public List<String> getEcuIds() { return ecuIds; }
    public Map<String, Double> getRequestedRateHz() { return requestedRateHz; }
    public Map<String, Double> getObservedRateHz() { return observedRateHz; }
    public CapabilitySource getCapabilitySource() { return capabilitySource; }
    public Instant getObservedAt() { return observedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private List<String> supportedSignals;
        private List<String> supportedDtcServices;
        private List<String> ecuIds;
        private Map<String, Double> requestedRateHz;
        private Map<String, Double> observedRateHz;
        private CapabilitySource capabilitySource;
        private Instant observedAt;

        public Builder supportedSignals(List<String> v) { this.supportedSignals = v; return this; }
        public Builder supportedDtcServices(List<String> v) { this.supportedDtcServices = v; return this; }
        public Builder ecuIds(List<String> v) { this.ecuIds = v; return this; }
        public Builder requestedRateHz(Map<String, Double> v) { this.requestedRateHz = v; return this; }
        public Builder observedRateHz(Map<String, Double> v) { this.observedRateHz = v; return this; }
        public Builder capabilitySource(CapabilitySource v) { this.capabilitySource = v; return this; }
        public Builder observedAt(Instant v) { this.observedAt = v; return this; }

        public CapabilitySnapshot build() { return new CapabilitySnapshot(this); }
    }
}
