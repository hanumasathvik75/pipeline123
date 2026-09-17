package com.ignitionai.obdinput.schema;

public class SequenceGapEvent implements QualityEvent {
    private final String sessionId;
    private final long timestampMs;
    private final long expectedSequence;
    private final long receivedSequence;
    private final long gapSize;

    public SequenceGapEvent(String sessionId, long timestampMs, long expectedSequence, long receivedSequence) {
        this.sessionId = sessionId;
        this.timestampMs = timestampMs;
        this.expectedSequence = expectedSequence;
        this.receivedSequence = receivedSequence;
        this.gapSize = receivedSequence - expectedSequence;
    }

    @Override
    public String getSessionId() { return sessionId; }
    
    @Override
    public long getTimestampMs() { return timestampMs; }
    
    @Override
    public String getEventType() { return "sequence_gap"; }

    public long getExpectedSequence() { return expectedSequence; }
    public long getReceivedSequence() { return receivedSequence; }
    public long getGapSize() { return gapSize; }
}
