package com.ignitionai.obdinput.schema;

import java.time.Instant;
import java.util.Objects;

public final class SensorReading {
    private final String signalId;
    private final String pidOrDid;
    private final String ecuId;
    private final Double value;
    private final String unit;
    private final Instant measuredAt;
    private final MeasurementTimeBasis measurementTimeBasis;
    private final long monotonicMs;
    private final Instant receivedAt;
    private final Quality quality;
    private final SignalProvenance provenance;

    private SensorReading(Builder builder) {
        this.signalId = Objects.requireNonNull(builder.signalId, "signalId is required");
        this.pidOrDid = builder.pidOrDid;
        this.ecuId = builder.ecuId;
        this.value = Objects.requireNonNull(builder.value, "value is required");
        this.unit = Objects.requireNonNull(builder.unit, "unit is required");
        this.measuredAt = builder.measuredAt;
        this.measurementTimeBasis = Objects.requireNonNull(builder.measurementTimeBasis, "measurementTimeBasis is required");
        this.monotonicMs = builder.monotonicMs;
        this.receivedAt = Objects.requireNonNull(builder.receivedAt, "receivedAt is required");
        this.quality = Objects.requireNonNull(builder.quality, "quality is required");
        this.provenance = Objects.requireNonNull(builder.provenance, "provenance is required");
    }

    public String getSignalId() { return signalId; }
    public String getPidOrDid() { return pidOrDid; }
    public String getEcuId() { return ecuId; }
    public Double getValue() { return value; }
    public String getUnit() { return unit; }
    public Instant getMeasuredAt() { return measuredAt; }
    public MeasurementTimeBasis getMeasurementTimeBasis() { return measurementTimeBasis; }
    public long getMonotonicMs() { return monotonicMs; }
    public Instant getReceivedAt() { return receivedAt; }
    public Quality getQuality() { return quality; }
    public SignalProvenance getProvenance() { return provenance; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String signalId;
        private String pidOrDid;
        private String ecuId;
        private Double value;
        private String unit;
        private Instant measuredAt;
        private MeasurementTimeBasis measurementTimeBasis;
        private long monotonicMs;
        private Instant receivedAt;
        private Quality quality;
        private SignalProvenance provenance;

        public Builder signalId(String v) { this.signalId = v; return this; }
        public Builder pidOrDid(String v) { this.pidOrDid = v; return this; }
        public Builder ecuId(String v) { this.ecuId = v; return this; }
        public Builder value(Double v) { this.value = v; return this; }
        public Builder unit(String v) { this.unit = v; return this; }
        public Builder measuredAt(Instant v) { this.measuredAt = v; return this; }
        public Builder measurementTimeBasis(MeasurementTimeBasis v) { this.measurementTimeBasis = v; return this; }
        public Builder monotonicMs(long v) { this.monotonicMs = v; return this; }
        public Builder receivedAt(Instant v) { this.receivedAt = v; return this; }
        public Builder quality(Quality v) { this.quality = v; return this; }
        public Builder provenance(SignalProvenance v) { this.provenance = v; return this; }

        public SensorReading build() { return new SensorReading(this); }
    }
}
