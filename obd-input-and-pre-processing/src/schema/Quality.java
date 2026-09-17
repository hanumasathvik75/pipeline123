package com.ignitionai.obdinput.schema;

import java.util.Objects;

public class Quality {
    private final QualityStatus status;
    private final String rawQuality;
    private final Integer ageMs;
    private final boolean interpolated;

    public Quality(QualityStatus status, String rawQuality, Integer ageMs) {
        this.status = Objects.requireNonNull(status, "status is required");
        this.rawQuality = rawQuality;
        this.ageMs = ageMs;
        this.interpolated = false; // contract explicitly prohibits interpolation here
    }

    public QualityStatus getStatus() { return status; }
    public String getRawQuality() { return rawQuality; }
    public Integer getAgeMs() { return ageMs; }
    public boolean isInterpolated() { return interpolated; }
}
