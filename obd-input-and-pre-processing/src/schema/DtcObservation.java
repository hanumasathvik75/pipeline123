package com.ignitionai.obdinput.schema;

import java.time.Instant;
import java.util.Objects;

public final class DtcObservation {
    private final String code;
    private final String ecuId;
    private final DtcStatus status;
    private final ObservationType observationType;
    private final Instant observedAt;
    private final long monotonicMs;
    private final Instant firstSeenAt;
    private final Instant lastSeenAt;
    private final Integer occurrenceCount;
    private final String freezeFrameRef;
    private final Integer rawStatusByte;
    private final DtcProvenance provenance;

    private DtcObservation(Builder builder) {
        this.code = Objects.requireNonNull(builder.code, "code is required");
        this.ecuId = builder.ecuId;
        this.status = Objects.requireNonNull(builder.status, "status is required");
        this.observationType = Objects.requireNonNull(builder.observationType, "observationType is required");
        this.observedAt = Objects.requireNonNull(builder.observedAt, "observedAt is required");
        this.monotonicMs = builder.monotonicMs;
        this.firstSeenAt = builder.firstSeenAt;
        this.lastSeenAt = builder.lastSeenAt;
        this.occurrenceCount = builder.occurrenceCount;
        this.freezeFrameRef = builder.freezeFrameRef;
        this.rawStatusByte = builder.rawStatusByte;
        this.provenance = Objects.requireNonNull(builder.provenance, "provenance is required");
    }

    public String getCode() { return code; }
    public String getEcuId() { return ecuId; }
    public DtcStatus getStatus() { return status; }
    public ObservationType getObservationType() { return observationType; }
    public Instant getObservedAt() { return observedAt; }
    public long getMonotonicMs() { return monotonicMs; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Integer getOccurrenceCount() { return occurrenceCount; }
    public String getFreezeFrameRef() { return freezeFrameRef; }
    public Integer getRawStatusByte() { return rawStatusByte; }
    public DtcProvenance getProvenance() { return provenance; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String code;
        private String ecuId;
        private DtcStatus status;
        private ObservationType observationType;
        private Instant observedAt;
        private long monotonicMs;
        private Instant firstSeenAt;
        private Instant lastSeenAt;
        private Integer occurrenceCount;
        private String freezeFrameRef;
        private Integer rawStatusByte;
        private DtcProvenance provenance;

        public Builder code(String v) { this.code = v; return this; }
        public Builder ecuId(String v) { this.ecuId = v; return this; }
        public Builder status(DtcStatus v) { this.status = v; return this; }
        public Builder observationType(ObservationType v) { this.observationType = v; return this; }
        public Builder observedAt(Instant v) { this.observedAt = v; return this; }
        public Builder monotonicMs(long v) { this.monotonicMs = v; return this; }
        public Builder firstSeenAt(Instant v) { this.firstSeenAt = v; return this; }
        public Builder lastSeenAt(Instant v) { this.lastSeenAt = v; return this; }
        public Builder occurrenceCount(Integer v) { this.occurrenceCount = v; return this; }
        public Builder freezeFrameRef(String v) { this.freezeFrameRef = v; return this; }
        public Builder rawStatusByte(Integer v) { this.rawStatusByte = v; return this; }
        public Builder provenance(DtcProvenance v) { this.provenance = v; return this; }

        public DtcObservation build() { return new DtcObservation(this); }
    }
}
