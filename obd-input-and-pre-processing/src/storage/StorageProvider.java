package com.ignitionai.obdinput.storage;

import com.ignitionai.obdinput.schema.ObdMessage;

public interface StorageProvider {
    void persistRawRecord(String sessionId, String rawPayload);
    void persistNormalizedMessage(ObdMessage message);
}
