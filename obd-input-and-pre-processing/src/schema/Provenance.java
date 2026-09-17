package com.ignitionai.obdinput.schema;

import java.util.Objects;

public final class Provenance {
    private final String adapterId;
    private final String adapterVersion;
    private final String rawRecordRef;
    private final Long simulationSeed;

    public Provenance(String adapterId, String adapterVersion, String rawRecordRef, Long simulationSeed) {
        this.adapterId = Objects.requireNonNull(adapterId, "adapterId is required");
        this.adapterVersion = Objects.requireNonNull(adapterVersion, "adapterVersion is required");
        this.rawRecordRef = Objects.requireNonNull(rawRecordRef, "rawRecordRef is required");
        this.simulationSeed = simulationSeed;
    }

    public String getAdapterId() { return adapterId; }
    public String getAdapterVersion() { return adapterVersion; }
    public String getRawRecordRef() { return rawRecordRef; }
    public Long getSimulationSeed() { return simulationSeed; }
}
