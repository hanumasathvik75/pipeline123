package com.ignitionai.obdinput.schema;

import java.util.Objects;

public final class VehicleRef {
    private final String vehicleId;
    private final String vin;
    private final IdentityStatus identityStatus;

    public VehicleRef(String vehicleId, String vin, IdentityStatus identityStatus) {
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicleId is required");
        this.vin = vin;
        this.identityStatus = Objects.requireNonNull(identityStatus, "identityStatus is required");
    }

    public String getVehicleId() { return vehicleId; }
    public String getVin() { return vin; }
    public IdentityStatus getIdentityStatus() { return identityStatus; }
}
