package com.ignitionai.obdinput.schema;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ObdMessage {
    private final String schemaVersion = "obd-input.v1";
    private final String messageId;
    private final String sessionId;
    private final long sequence;
    private final MessageType messageType;
    private final SourceType sourceType;
    private final Instant emittedAt;
    private final Instant receivedAt;
    private final VehicleRef vehicleRef;
    private final List<SensorReading> sensorReadings;
    private final List<DtcObservation> dtcObservations;
    private final CapabilitySnapshot capabilitySnapshot;
    private final Provenance rawProvenance;
    private final DtcSnapshotCompleteness dtcSnapshotCompleteness;

    private ObdMessage(Builder builder) {
        this.messageId = Objects.requireNonNull(builder.messageId, "messageId is required");
        this.sessionId = Objects.requireNonNull(builder.sessionId, "sessionId is required");
        this.sequence = builder.sequence;
        this.messageType = Objects.requireNonNull(builder.messageType, "messageType is required");
        this.sourceType = Objects.requireNonNull(builder.sourceType, "sourceType is required");
        this.emittedAt = Objects.requireNonNull(builder.emittedAt, "emittedAt is required");
        this.receivedAt = Objects.requireNonNull(builder.receivedAt, "receivedAt is required");
        this.vehicleRef = Objects.requireNonNull(builder.vehicleRef, "vehicleRef is required");
        this.sensorReadings = builder.sensorReadings != null ? List.copyOf(builder.sensorReadings) : List.of();
        this.dtcObservations = builder.dtcObservations != null ? List.copyOf(builder.dtcObservations) : List.of();
        this.capabilitySnapshot = builder.capabilitySnapshot;
        this.rawProvenance = Objects.requireNonNull(builder.rawProvenance, "rawProvenance is required");
        this.dtcSnapshotCompleteness = builder.dtcSnapshotCompleteness != null ? builder.dtcSnapshotCompleteness : DtcSnapshotCompleteness.UNKNOWN;
    }

    public String getSchemaVersion() { return schemaVersion; }
    public String getMessageId() { return messageId; }
    public String getSessionId() { return sessionId; }
    public long getSequence() { return sequence; }
    public MessageType getMessageType() { return messageType; }
    public SourceType getSourceType() { return sourceType; }
    public Instant getEmittedAt() { return emittedAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public VehicleRef getVehicleRef() { return vehicleRef; }
    public List<SensorReading> getSensorReadings() { return sensorReadings; }
    public List<DtcObservation> getDtcObservations() { return dtcObservations; }
    public CapabilitySnapshot getCapabilitySnapshot() { return capabilitySnapshot; }
    public Provenance getRawProvenance() { return rawProvenance; }
    public DtcSnapshotCompleteness getDtcSnapshotCompleteness() { return dtcSnapshotCompleteness; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String messageId;
        private String sessionId;
        private long sequence;
        private MessageType messageType;
        private SourceType sourceType;
        private Instant emittedAt;
        private Instant receivedAt;
        private VehicleRef vehicleRef;
        private List<SensorReading> sensorReadings;
        private List<DtcObservation> dtcObservations;
        private CapabilitySnapshot capabilitySnapshot;
        private Provenance rawProvenance;
        private DtcSnapshotCompleteness dtcSnapshotCompleteness;

        public Builder messageId(String v) { this.messageId = v; return this; }
        public Builder sessionId(String v) { this.sessionId = v; return this; }
        public Builder sequence(long v) { this.sequence = v; return this; }
        public Builder messageType(MessageType v) { this.messageType = v; return this; }
        public Builder sourceType(SourceType v) { this.sourceType = v; return this; }
        public Builder emittedAt(Instant v) { this.emittedAt = v; return this; }
        public Builder receivedAt(Instant v) { this.receivedAt = v; return this; }
        public Builder vehicleRef(VehicleRef v) { this.vehicleRef = v; return this; }
        public Builder sensorReadings(List<SensorReading> v) { this.sensorReadings = v; return this; }
        public Builder dtcObservations(List<DtcObservation> v) { this.dtcObservations = v; return this; }
        public Builder capabilitySnapshot(CapabilitySnapshot v) { this.capabilitySnapshot = v; return this; }
        public Builder rawProvenance(Provenance v) { this.rawProvenance = v; return this; }
        public Builder dtcSnapshotCompleteness(DtcSnapshotCompleteness v) { this.dtcSnapshotCompleteness = v; return this; }

        public ObdMessage build() { return new ObdMessage(this); }
    }
}
