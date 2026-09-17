package com.ignitionai.obdinput.schema;

import java.util.Objects;

public final class SignalProvenance {
    private final String requestService;
    private final String responseReference;

    public SignalProvenance(String requestService, String responseReference) {
        this.requestService = requestService;
        this.responseReference = Objects.requireNonNull(responseReference, "responseReference is required");
    }

    public String getRequestService() { return requestService; }
    public String getResponseReference() { return responseReference; }
}
