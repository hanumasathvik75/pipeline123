package com.ignitionai.phase6;

import java.util.Arrays;
import java.util.Collections;

public class Phase6Fixtures {

    public static VehicleHealthAssessment getHealthyVehicle() {
        return new VehicleHealthAssessment(
            "VIN_HEALTHY_01", "VERIFIED", "SUV_AWD",
            "2026-09-18T10:00:00Z", "30d", 15000.0,
            92.5, VehicleHealthBand.A_EXCELLENT, SeverityLevel.NONE,
            0.95, 0.05, "SUFFICIENT",
            Arrays.asList(
                new SubsystemHealthAssessment("Engine", 95.0, SeverityLevel.NONE, 0.98, Arrays.asList("evt-01", "ts-02"), true),
                new SubsystemHealthAssessment("Transmission", 90.0, SeverityLevel.NONE, 0.90, Arrays.asList("evt-03"), true)
            ),
            "v2.1", "v1.0", "REL-20260918-001"
        );
    }

    public static VehicleHealthAssessment getDegradedVehicle() {
        return new VehicleHealthAssessment(
            "VIN_DEGRADED_01", "VERIFIED", "SEDAN_FWD",
            "2026-09-18T10:05:00Z", "30d", 85000.0,
            68.0, VehicleHealthBand.C_FAIR, SeverityLevel.MEDIUM,
            0.85, 0.15, "SUFFICIENT",
            Arrays.asList(
                new SubsystemHealthAssessment("Engine", 65.0, SeverityLevel.MEDIUM, 0.88, Arrays.asList("evt-11", "ts-12"), true),
                new SubsystemHealthAssessment("Transmission", 75.0, SeverityLevel.LOW, 0.82, Arrays.asList("evt-13"), true)
            ),
            "v2.1", "v1.0", "REL-20260918-001"
        );
    }

    public static VehicleHealthAssessment getCriticalSubsystemVehicle() {
        return new VehicleHealthAssessment(
            "VIN_CRITICAL_01", "VERIFIED", "TRUCK_4X4",
            "2026-09-18T10:10:00Z", "30d", 120000.0,
            45.0, VehicleHealthBand.E_CRITICAL, SeverityLevel.CRITICAL,
            0.99, 0.01, "SUFFICIENT",
            Arrays.asList(
                new SubsystemHealthAssessment("Engine", 25.0, SeverityLevel.CRITICAL, 0.99, Arrays.asList("evt-21", "ts-22"), true),
                new SubsystemHealthAssessment("Transmission", 80.0, SeverityLevel.NONE, 0.80, Arrays.asList("evt-23"), true)
            ),
            "v2.1", "v1.0", "REL-20260918-001"
        );
    }

    public static VehicleHealthAssessment getMissingDataVehicle() {
        return new VehicleHealthAssessment(
            "VIN_MISSING_01", "UNVERIFIED", "UNKNOWN",
            "2026-09-18T10:15:00Z", "30d", null,
            null, VehicleHealthBand.UNASSESSED, SeverityLevel.UNKNOWN,
            null, null, "INSUFFICIENT",
            null,
            "v2.1", "v1.0", "REL-20260918-001"
        );
    }

    public static VehicleHealthAssessment getLowConfidenceVehicle() {
        return new VehicleHealthAssessment(
            "VIN_LOWCONF_01", "VERIFIED", "SEDAN_FWD",
            "2026-09-18T10:20:00Z", "30d", 45000.0,
            75.0, VehicleHealthBand.B_GOOD, SeverityLevel.LOW,
            0.40, 0.60, "SPARSE",
            Arrays.asList(
                new SubsystemHealthAssessment("Engine", 75.0, SeverityLevel.LOW, 0.40, Arrays.asList("evt-41"), false)
            ),
            "v2.1", "v1.0", "REL-20260918-001"
        );
    }

    public static VehicleHealthAssessment getRecoveredVehicle() {
        return new VehicleHealthAssessment(
            "VIN_RECOVERED_01", "VERIFIED", "HATCHBACK",
            "2026-09-18T10:25:00Z", "30d", 65000.0,
            88.0, VehicleHealthBand.B_GOOD, SeverityLevel.NONE,
            0.90, 0.10, "SUFFICIENT",
            Arrays.asList(
                new SubsystemHealthAssessment("Engine", 88.0, SeverityLevel.NONE, 0.90, Arrays.asList("evt-51", "rep-01"), true)
            ),
            "v2.1", "v1.0", "REL-20260918-001"
        );
    }

    public static VehicleHealthAssessment getHistoricalComparison1() {
        return new VehicleHealthAssessment(
            "VIN_HIST_01", "VERIFIED", "SEDAN_FWD",
            "2026-08-18T10:00:00Z", "30d", 50000.0,
            85.0, VehicleHealthBand.B_GOOD, SeverityLevel.NONE,
            0.90, 0.10, "SUFFICIENT",
            Arrays.asList(
                new SubsystemHealthAssessment("Engine", 85.0, SeverityLevel.NONE, 0.90, Arrays.asList("evt-61"), true)
            ),
            "v2.1", "v1.0", "REL-20260918-001"
        );
    }

    public static VehicleHealthAssessment getHistoricalComparison2() {
        return new VehicleHealthAssessment(
            "VIN_HIST_01", "VERIFIED", "SEDAN_FWD",
            "2026-09-18T10:00:00Z", "30d", 52000.0,
            70.0, VehicleHealthBand.C_FAIR, SeverityLevel.MEDIUM,
            0.92, 0.08, "SUFFICIENT",
            Arrays.asList(
                new SubsystemHealthAssessment("Engine", 60.0, SeverityLevel.MEDIUM, 0.90, Arrays.asList("evt-61", "evt-62"), true)
            ),
            "v2.1", "v1.0", "REL-20260918-001"
        );
    }
}
