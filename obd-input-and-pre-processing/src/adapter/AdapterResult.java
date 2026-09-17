package com.ignitionai.obdinput.adapter;

import com.ignitionai.obdinput.schema.ObdMessage;

public class AdapterResult {
    private final ObdMessage normalizedMessage;
    private final String rawPayload;

    public AdapterResult(ObdMessage normalizedMessage, String rawPayload) {
        this.normalizedMessage = normalizedMessage;
        this.rawPayload = rawPayload;
    }

    public ObdMessage getNormalizedMessage() { return normalizedMessage; }
    public String getRawPayload() { return rawPayload; }
}
