package com.ignitionai.obdinput.schema;

import java.util.Objects;

public final class DtcProvenance {
    private final String service;
    private final String responseReference;

    public DtcProvenance(String service, String responseReference) {
        this.service = Objects.requireNonNull(service, "service is required");
        this.responseReference = Objects.requireNonNull(responseReference, "responseReference is required");
    }

    public String getService() { return service; }
    public String getResponseReference() { return responseReference; }
}
