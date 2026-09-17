package com.ignitionai.obdinput.schema;

public interface QualityEvent {
    String getSessionId();
    long getTimestampMs();
    String getEventType();
}
