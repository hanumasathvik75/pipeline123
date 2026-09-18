package com.ignitionai.obd;

/** Legacy aliases are accepted at boundaries; canonical IDs match obd-input.v1. */
public final class SignalIds {
    public static String canonical(String id) {
        switch (id) {
            case "vehicle_speed_kph": return "vehicle_speed";
            case "engine_coolant_temperature": return "coolant_temperature";
            case "short_term_fuel_trim_bank1": return "short_term_fuel_trim_b1";
            case "long_term_fuel_trim_bank1": return "long_term_fuel_trim_b1";
            default: return id;
        }
    }
}
