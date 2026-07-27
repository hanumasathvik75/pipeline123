"""
IgnitionAI — OBD-II DTC Database
Extracted from demo.py. Contains all DTC specs, root cause mappings, and vehicle problems.
"""

# ─────────────────────────────────────────────────────────────────────────────
# OBD-II DTC Diagnostic Database
# Keys: DTC code → description, points_weight, sensors dict
# ─────────────────────────────────────────────────────────────────────────────
obd2_dtc_diagnostic_database = {
    "P0001": {
        "description": "Fuel Volume Regulator Control Circuit/Open",
        "points_weight": 7,
        "sensors": {
            "fuel_rail_pressure": {"min_reading": 0, "max_reading": 50, "unit": "psi", "expected_condition": "below_normal"},
            "fuel_pump_duty_cycle": {"min_reading": 0, "max_reading": 30, "unit": "%", "expected_condition": "low"},
            "engine_rpm": {"min_reading": 0, "max_reading": 2000, "unit": "rpm", "expected_condition": "unstable"},
            "maf_sensor": {"min_reading": 2.0, "max_reading": 8.0, "unit": "g/s", "expected_condition": "normal"},
        },
    },
    "P0087": {
        "description": "Fuel Rail/System Pressure - Too Low",
        "points_weight": 8,
        "sensors": {
            "fuel_rail_pressure": {"min_reading": 0, "max_reading": 35, "unit": "psi", "expected_condition": "critically_low"},
            "fuel_pump_current": {"min_reading": 2.0, "max_reading": 6.0, "unit": "A", "expected_condition": "low"},
            "fuel_injector_pulse_width": {"min_reading": 5.0, "max_reading": 15.0, "unit": "ms", "expected_condition": "extended"},
            "o2_sensor_bank1": {"min_reading": 0.1, "max_reading": 0.3, "unit": "V", "expected_condition": "lean"},
            "short_term_fuel_trim": {"min_reading": 15, "max_reading": 25, "unit": "%", "expected_condition": "high_positive"},
        },
    },
    "P0088": {
        "description": "Fuel Rail/System Pressure - Too High",
        "points_weight": 7,
        "sensors": {
            "fuel_rail_pressure": {"min_reading": 65, "max_reading": 120, "unit": "psi", "expected_condition": "high"},
            "fuel_pressure_regulator_duty": {"min_reading": 80, "max_reading": 100, "unit": "%", "expected_condition": "maximum"},
            "o2_sensor_bank1": {"min_reading": 0.7, "max_reading": 0.9, "unit": "V", "expected_condition": "rich"},
            "short_term_fuel_trim": {"min_reading": -25, "max_reading": -15, "unit": "%", "expected_condition": "high_negative"},
        },
    },
    "P0100": {
        "description": "Mass or Volume Air Flow Circuit Malfunction",
        "points_weight": 6,
        "sensors": {
            "maf_sensor": {"min_reading": 0.0, "max_reading": 1.0, "unit": "g/s", "expected_condition": "erratic_or_stuck"},
            "map_sensor": {"min_reading": 10, "max_reading": 105, "unit": "kPa", "expected_condition": "normal_backup"},
            "iat_sensor": {"min_reading": -10, "max_reading": 60, "unit": "°C", "expected_condition": "normal"},
            "throttle_position": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "variable"},
            "engine_load": {"min_reading": 10, "max_reading": 90, "unit": "%", "expected_condition": "inconsistent"},
        },
    },
    "P0101": {
        "description": "Mass or Volume Air Flow Circuit Range/Performance Problem",
        "points_weight": 5,
        "sensors": {
            "maf_sensor": {"min_reading": 1.5, "max_reading": 25.0, "unit": "g/s", "expected_condition": "out_of_range"},
            "map_sensor": {"min_reading": 20, "max_reading": 100, "unit": "kPa", "expected_condition": "correlates_poorly"},
            "engine_rpm": {"min_reading": 500, "max_reading": 6000, "unit": "rpm", "expected_condition": "normal"},
            "calculated_load": {"min_reading": 15, "max_reading": 85, "unit": "%", "expected_condition": "mismatch_with_maf"},
        },
    },
    "P0105": {
        "description": "Manifold Absolute Pressure/Barometric Pressure Circuit Malfunction",
        "points_weight": 6,
        "sensors": {
            "map_sensor": {"min_reading": 0, "max_reading": 10, "unit": "kPa", "expected_condition": "stuck_low_or_high"},
            "barometric_pressure": {"min_reading": 95, "max_reading": 105, "unit": "kPa", "expected_condition": "reference_normal"},
            "throttle_position": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "variable"},
            "engine_rpm": {"min_reading": 600, "max_reading": 4000, "unit": "rpm", "expected_condition": "normal"},
        },
    },
    "P0110": {
        "description": "Intake Air Temperature Circuit Malfunction",
        "points_weight": 4,
        "sensors": {
            "iat_sensor": {"min_reading": -40, "max_reading": 150, "unit": "°C", "expected_condition": "out_of_range_or_stuck"},
            "ect_sensor": {"min_reading": 80, "max_reading": 105, "unit": "°C", "expected_condition": "normal_reference"},
            "ambient_temp": {"min_reading": 10, "max_reading": 45, "unit": "°C", "expected_condition": "normal_reference"},
            "fuel_trim_adaptation": {"min_reading": -10, "max_reading": 10, "unit": "%", "expected_condition": "compensating"},
        },
    },
    "P0115": {
        "description": "Engine Coolant Temperature Circuit Malfunction",
        "points_weight": 7,
        "sensors": {
            "ect_sensor": {"min_reading": -40, "max_reading": 150, "unit": "°C", "expected_condition": "stuck_or_erratic"},
            "iat_sensor": {"min_reading": 15, "max_reading": 50, "unit": "°C", "expected_condition": "reference_normal"},
            "thermostat_position": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "unknown_state"},
            "cooling_fan_duty": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "erratic_operation"},
        },
    },
    "P0116": {
        "description": "Engine Coolant Temperature Circuit Range/Performance",
        "points_weight": 6,
        "sensors": {
            "ect_sensor_voltage": {"min_reading": 0.2, "max_reading": 4.8, "unit": "V", "expected_condition": "stuck_voltage"},
            "ect_warm_up_rate": {"min_reading": 0.5, "max_reading": 3.0, "unit": "°C/min", "expected_condition": "abnormal_warm_up"},
            "thermostat_opening_temp": {"min_reading": 82, "max_reading": 95, "unit": "°C", "expected_condition": "delayed_or_no_opening"},
            "cooling_system_efficiency": {"min_reading": 60, "max_reading": 95, "unit": "%", "expected_condition": "reduced_efficiency"},
        },
    },
    "P0120": {
        "description": "Throttle/Pedal Position Sensor Circuit Malfunction",
        "points_weight": 8,
        "sensors": {
            "throttle_position_1": {"min_reading": 0.0, "max_reading": 5.0, "unit": "V", "expected_condition": "stuck_or_erratic"},
            "throttle_position_2": {"min_reading": 0.0, "max_reading": 5.0, "unit": "V", "expected_condition": "correlation_error"},
            "accelerator_pedal_pos": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "normal_input"},
            "idle_air_control": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "compensating"},
        },
    },
    "P0130": {
        "description": "O2 Sensor Circuit Malfunction (Bank 1 Sensor 1)",
        "points_weight": 6,
        "sensors": {
            "o2_sensor_b1s1": {"min_reading": 0.0, "max_reading": 1.2, "unit": "V", "expected_condition": "stuck_or_no_activity"},
            "short_term_fuel_trim_b1": {"min_reading": -25, "max_reading": 25, "unit": "%", "expected_condition": "erratic"},
            "long_term_fuel_trim_b1": {"min_reading": -25, "max_reading": 25, "unit": "%", "expected_condition": "hunting"},
            "catalyst_efficiency": {"min_reading": 50, "max_reading": 100, "unit": "%", "expected_condition": "reduced"},
        },
    },
    "P0131": {
        "description": "O2 Sensor Circuit Low Voltage (Bank 1 Sensor 1)",
        "points_weight": 6,
        "sensors": {
            "o2_sensor_b1s1": {"min_reading": 0.0, "max_reading": 0.3, "unit": "V", "expected_condition": "stuck_lean"},
            "exhaust_temp_b1": {"min_reading": 200, "max_reading": 800, "unit": "°C", "expected_condition": "normal_operating_temp"},
            "o2_heater_current_b1s1": {"min_reading": 0, "max_reading": 2, "unit": "A", "expected_condition": "heater_circuit_fault"},
            "fuel_trim_response_b1": {"min_reading": 15, "max_reading": 25, "unit": "%", "expected_condition": "excessive_positive_trim"},
        },
    },
    "P0132": {
        "description": "O2 Sensor Circuit High Voltage (Bank 1 Sensor 1)",
        "points_weight": 6,
        "sensors": {
            "o2_sensor_b1s1": {"min_reading": 0.7, "max_reading": 1.2, "unit": "V", "expected_condition": "stuck_rich"},
            "exhaust_lambda_b1": {"min_reading": 0.7, "max_reading": 0.9, "unit": "lambda", "expected_condition": "consistently_rich"},
            "carbon_deposits_indicator": {"min_reading": 60, "max_reading": 100, "unit": "%", "expected_condition": "heavy_deposits"},
            "fuel_trim_response_b1": {"min_reading": -25, "max_reading": -15, "unit": "%", "expected_condition": "excessive_negative_trim"},
        },
    },
    "P0171": {
        "description": "System Too Lean (Bank 1)",
        "points_weight": 7,
        "sensors": {
            "o2_sensor_b1s1": {"min_reading": 0.1, "max_reading": 0.35, "unit": "V", "expected_condition": "consistently_lean"},
            "short_term_fuel_trim_b1": {"min_reading": 15, "max_reading": 25, "unit": "%", "expected_condition": "high_positive"},
            "long_term_fuel_trim_b1": {"min_reading": 10, "max_reading": 25, "unit": "%", "expected_condition": "high_positive"},
            "maf_sensor": {"min_reading": 2.0, "max_reading": 20.0, "unit": "g/s", "expected_condition": "potentially_high"},
            "fuel_rail_pressure": {"min_reading": 30, "max_reading": 45, "unit": "psi", "expected_condition": "potentially_low"},
        },
    },
    "P0172": {
        "description": "System Too Rich (Bank 1)",
        "points_weight": 7,
        "sensors": {
            "o2_sensor_b1s1": {"min_reading": 0.65, "max_reading": 0.9, "unit": "V", "expected_condition": "consistently_rich"},
            "short_term_fuel_trim_b1": {"min_reading": -25, "max_reading": -15, "unit": "%", "expected_condition": "high_negative"},
            "long_term_fuel_trim_b1": {"min_reading": -25, "max_reading": -10, "unit": "%", "expected_condition": "high_negative"},
            "fuel_rail_pressure": {"min_reading": 65, "max_reading": 85, "unit": "psi", "expected_condition": "potentially_high"},
            "maf_sensor": {"min_reading": 1.0, "max_reading": 15.0, "unit": "g/s", "expected_condition": "potentially_low"},
        },
    },
    "P0200": {
        "description": "Injector Circuit Malfunction",
        "points_weight": 8,
        "sensors": {
            "fuel_injector_voltage": {"min_reading": 8, "max_reading": 16, "unit": "V", "expected_condition": "supply_voltage_issue"},
            "injector_pulse_width": {"min_reading": 0, "max_reading": 2, "unit": "ms", "expected_condition": "no_pulse_or_short"},
            "fuel_rail_pressure": {"min_reading": 40, "max_reading": 60, "unit": "psi", "expected_condition": "normal_but_no_delivery"},
            "engine_load": {"min_reading": 10, "max_reading": 90, "unit": "%", "expected_condition": "high_but_no_fuel"},
        },
    },
    "P0201": {
        "description": "Injector Circuit Malfunction - Cylinder 1",
        "points_weight": 7,
        "sensors": {
            "injector_1_resistance": {"min_reading": 10, "max_reading": 16, "unit": "Ohm", "expected_condition": "out_of_spec"},
            "injector_1_current": {"min_reading": 0, "max_reading": 1, "unit": "A", "expected_condition": "no_current"},
            "cylinder_1_contribution": {"min_reading": 0, "max_reading": 15, "unit": "%", "expected_condition": "poor_contribution"},
            "o2_sensor_response": {"min_reading": 0.1, "max_reading": 0.4, "unit": "V", "expected_condition": "lean_indication"},
        },
    },
    "P0202": {
        "description": "Injector Circuit Malfunction - Cylinder 2",
        "points_weight": 7,
        "sensors": {
            "injector_2_resistance": {"min_reading": 10, "max_reading": 16, "unit": "Ohm", "expected_condition": "out_of_spec"},
            "injector_2_current": {"min_reading": 0, "max_reading": 1, "unit": "A", "expected_condition": "no_current"},
            "cylinder_2_contribution": {"min_reading": 0, "max_reading": 15, "unit": "%", "expected_condition": "poor_contribution"},
            "fuel_trim_cyl2": {"min_reading": -25, "max_reading": 25, "unit": "%", "expected_condition": "compensating"},
        },
    },
    "P0203": {
        "description": "Injector Circuit Malfunction - Cylinder 3",
        "points_weight": 7,
        "sensors": {
            "injector_3_resistance": {"min_reading": 10, "max_reading": 16, "unit": "Ohm", "expected_condition": "out_of_spec"},
            "injector_3_current": {"min_reading": 0, "max_reading": 1, "unit": "A", "expected_condition": "no_current"},
            "cylinder_3_contribution": {"min_reading": 0, "max_reading": 15, "unit": "%", "expected_condition": "poor_contribution"},
            "fuel_trim_cyl3": {"min_reading": -25, "max_reading": 25, "unit": "%", "expected_condition": "compensating"},
        },
    },
    "P0230": {
        "description": "Fuel Pump Primary Circuit Malfunction",
        "points_weight": 10,
        "sensors": {
            "fuel_pump_voltage": {"min_reading": 0, "max_reading": 8, "unit": "V", "expected_condition": "low_or_no_voltage"},
            "fuel_pump_current": {"min_reading": 0, "max_reading": 2, "unit": "A", "expected_condition": "no_current"},
            "fuel_rail_pressure": {"min_reading": 0, "max_reading": 20, "unit": "psi", "expected_condition": "critically_low"},
            "fuel_pump_relay_status": {"min_reading": 0, "max_reading": 1, "unit": "boolean", "expected_condition": "relay_fault"},
        },
    },
    "P0016": {
        "description": "Crankshaft Position - Camshaft Position Correlation Bank 1 Sensor A",
        "points_weight": 9,
        "sensors": {
            "crankshaft_position": {"min_reading": 0, "max_reading": 360, "unit": "degrees", "expected_condition": "normal_signal"},
            "camshaft_position": {"min_reading": 0, "max_reading": 720, "unit": "degrees", "expected_condition": "timing_offset"},
            "valve_timing_error": {"min_reading": 5, "max_reading": 30, "unit": "degrees", "expected_condition": "correlation_error"},
            "vvt_actuator_position": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "stuck_or_slow"},
        },
    },
    "P0300": {
        "description": "Random/Multiple Cylinder Misfire Detected",
        "points_weight": 9,
        "sensors": {
            "crankshaft_position": {"min_reading": 0, "max_reading": 360, "unit": "degrees", "expected_condition": "irregular_pattern"},
            "camshaft_position": {"min_reading": 0, "max_reading": 720, "unit": "degrees", "expected_condition": "correlation_issues"},
            "engine_rpm": {"min_reading": 500, "max_reading": 4000, "unit": "rpm", "expected_condition": "unstable"},
            "ignition_timing": {"min_reading": -10, "max_reading": 35, "unit": "degrees", "expected_condition": "retarded"},
            "fuel_injector_pulse": {"min_reading": 2.0, "max_reading": 12.0, "unit": "ms", "expected_condition": "variable"},
        },
    },
    "P0301": {
        "description": "Cylinder 1 Misfire Detected",
        "points_weight": 8,
        "sensors": {
            "cylinder_1_pressure": {"min_reading": 8, "max_reading": 15, "unit": "bar", "expected_condition": "low_compression"},
            "injector_1_current": {"min_reading": 0.5, "max_reading": 4.0, "unit": "A", "expected_condition": "abnormal"},
            "ignition_coil_1_primary": {"min_reading": 8, "max_reading": 14, "unit": "V", "expected_condition": "poor_spark"},
            "crankshaft_acceleration": {"min_reading": -500, "max_reading": 500, "unit": "rpm/s", "expected_condition": "cylinder_1_deficit"},
        },
    },
    "P0302": {
        "description": "Cylinder 2 Misfire Detected",
        "points_weight": 8,
        "sensors": {
            "cylinder_2_pressure": {"min_reading": 8, "max_reading": 15, "unit": "bar", "expected_condition": "low_compression"},
            "injector_2_current": {"min_reading": 0.5, "max_reading": 4.0, "unit": "A", "expected_condition": "abnormal"},
            "ignition_coil_2_primary": {"min_reading": 8, "max_reading": 14, "unit": "V", "expected_condition": "poor_spark"},
            "crankshaft_acceleration_cyl2": {"min_reading": -500, "max_reading": 500, "unit": "rpm/s", "expected_condition": "cylinder_2_deficit"},
        },
    },
    "P0303": {
        "description": "Cylinder 3 Misfire Detected",
        "points_weight": 8,
        "sensors": {
            "cylinder_3_pressure": {"min_reading": 8, "max_reading": 15, "unit": "bar", "expected_condition": "low_compression"},
            "injector_3_current": {"min_reading": 0.5, "max_reading": 4.0, "unit": "A", "expected_condition": "abnormal"},
            "ignition_coil_3_primary": {"min_reading": 8, "max_reading": 14, "unit": "V", "expected_condition": "poor_spark"},
            "crankshaft_acceleration_cyl3": {"min_reading": -500, "max_reading": 500, "unit": "rpm/s", "expected_condition": "cylinder_3_deficit"},
        },
    },
    "P0304": {
        "description": "Cylinder 4 Misfire Detected",
        "points_weight": 8,
        "sensors": {
            "cylinder_4_pressure": {"min_reading": 8, "max_reading": 15, "unit": "bar", "expected_condition": "low_compression"},
            "injector_4_current": {"min_reading": 0.5, "max_reading": 4.0, "unit": "A", "expected_condition": "abnormal"},
            "ignition_coil_4_primary": {"min_reading": 8, "max_reading": 14, "unit": "V", "expected_condition": "poor_spark"},
            "crankshaft_acceleration_cyl4": {"min_reading": -500, "max_reading": 500, "unit": "rpm/s", "expected_condition": "cylinder_4_deficit"},
        },
    },
    "P0325": {
        "description": "Knock Sensor 1 Circuit Malfunction",
        "points_weight": 5,
        "sensors": {
            "knock_sensor_1": {"min_reading": 0.0, "max_reading": 1.0, "unit": "V", "expected_condition": "no_signal"},
            "ignition_timing_retard": {"min_reading": 0, "max_reading": 5, "unit": "degrees", "expected_condition": "no_retard_capability"},
            "engine_knock_intensity": {"min_reading": 0, "max_reading": 10, "unit": "units", "expected_condition": "undetected_knock"},
            "combustion_pressure": {"min_reading": 20, "max_reading": 80, "unit": "bar", "expected_condition": "potentially_damaging"},
        },
    },
    "P0335": {
        "description": "Crankshaft Position Sensor A Circuit Malfunction",
        "points_weight": 10,
        "sensors": {
            "crankshaft_position": {"min_reading": 0.0, "max_reading": 1.0, "unit": "V", "expected_condition": "no_signal_or_erratic"},
            "engine_rpm": {"min_reading": 0, "max_reading": 100, "unit": "rpm", "expected_condition": "no_reading_or_intermittent"},
            "ignition_timing": {"min_reading": 0, "max_reading": 0, "unit": "degrees", "expected_condition": "unknown_or_default"},
            "fuel_injection_timing": {"min_reading": 0, "max_reading": 0, "unit": "ms", "expected_condition": "disabled_or_backup"},
        },
    },
    "P0336": {
        "description": "Crankshaft Position Sensor A Circuit Range/Performance",
        "points_weight": 9,
        "sensors": {
            "crankshaft_position_signal": {"min_reading": 0.5, "max_reading": 4.5, "unit": "V", "expected_condition": "weak_or_noisy_signal"},
            "crankshaft_signal_frequency": {"min_reading": 10, "max_reading": 200, "unit": "Hz", "expected_condition": "irregular_frequency"},
            "engine_sync_status": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "poor_synchronization"},
            "timing_accuracy": {"min_reading": 60, "max_reading": 95, "unit": "%", "expected_condition": "reduced_accuracy"},
        },
    },
    "P0340": {
        "description": "Camshaft Position Sensor Circuit Malfunction",
        "points_weight": 9,
        "sensors": {
            "camshaft_position": {"min_reading": 0.0, "max_reading": 1.0, "unit": "V", "expected_condition": "no_signal"},
            "crankshaft_position": {"min_reading": 0, "max_reading": 360, "unit": "degrees", "expected_condition": "normal_reference"},
            "valve_timing": {"min_reading": 0, "max_reading": 0, "unit": "degrees", "expected_condition": "unknown"},
            "fuel_injection_sync": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "lost_synchronization"},
        },
    },
    "P0341": {
        "description": "Camshaft Position Sensor Circuit Range/Performance",
        "points_weight": 8,
        "sensors": {
            "camshaft_position_signal": {"min_reading": 0.5, "max_reading": 4.5, "unit": "V", "expected_condition": "intermittent_or_weak"},
            "cam_to_crank_correlation": {"min_reading": 50, "max_reading": 90, "unit": "%", "expected_condition": "poor_correlation"},
            "valve_timing_accuracy": {"min_reading": 70, "max_reading": 95, "unit": "%", "expected_condition": "timing_drift"},
            "sequential_injection_sync": {"min_reading": 0, "max_reading": 80, "unit": "%", "expected_condition": "lost_sequence"},
        },
    },
    "P0350": {
        "description": "Ignition Coil Primary/Secondary Circuit Malfunction",
        "points_weight": 8,
        "sensors": {
            "ignition_coil_primary_voltage": {"min_reading": 0, "max_reading": 8, "unit": "V", "expected_condition": "low_or_absent"},
            "ignition_coil_secondary_voltage": {"min_reading": 0, "max_reading": 15000, "unit": "V", "expected_condition": "insufficient"},
            "spark_plug_gap_voltage": {"min_reading": 5000, "max_reading": 25000, "unit": "V", "expected_condition": "weak_spark"},
            "dwell_time": {"min_reading": 1.0, "max_reading": 8.0, "unit": "ms", "expected_condition": "incorrect"},
        },
    },
    "P0351": {
        "description": "Ignition Coil A Primary/Secondary Circuit",
        "points_weight": 7,
        "sensors": {
            "ignition_coil_a_primary": {"min_reading": 0, "max_reading": 8, "unit": "V", "expected_condition": "circuit_fault"},
            "ignition_coil_a_current": {"min_reading": 0, "max_reading": 3, "unit": "A", "expected_condition": "no_current_or_overcurrent"},
            "cylinder_1_combustion": {"min_reading": 0, "max_reading": 80, "unit": "%", "expected_condition": "poor_combustion"},
            "knock_sensor_cyl1": {"min_reading": 0.5, "max_reading": 4.5, "unit": "V", "expected_condition": "no_knock_detection"},
        },
    },
    "P0352": {
        "description": "Ignition Coil B Primary/Secondary Circuit",
        "points_weight": 7,
        "sensors": {
            "ignition_coil_b_primary": {"min_reading": 0, "max_reading": 8, "unit": "V", "expected_condition": "circuit_fault"},
            "ignition_coil_b_current": {"min_reading": 0, "max_reading": 3, "unit": "A", "expected_condition": "no_current_or_overcurrent"},
            "cylinder_2_combustion": {"min_reading": 0, "max_reading": 80, "unit": "%", "expected_condition": "poor_combustion"},
            "knock_sensor_cyl2": {"min_reading": 0.5, "max_reading": 4.5, "unit": "V", "expected_condition": "no_knock_detection"},
        },
    },
    "P0353": {
        "description": "Ignition Coil C Primary/Secondary Circuit",
        "points_weight": 7,
        "sensors": {
            "ignition_coil_c_primary": {"min_reading": 0, "max_reading": 8, "unit": "V", "expected_condition": "circuit_fault"},
            "ignition_coil_c_current": {"min_reading": 0, "max_reading": 3, "unit": "A", "expected_condition": "no_current_or_overcurrent"},
            "cylinder_3_combustion": {"min_reading": 0, "max_reading": 80, "unit": "%", "expected_condition": "poor_combustion"},
            "knock_sensor_cyl3": {"min_reading": 0.5, "max_reading": 4.5, "unit": "V", "expected_condition": "no_knock_detection"},
        },
    },
    "P0400": {
        "description": "Exhaust Gas Recirculation Flow Malfunction",
        "points_weight": 5,
        "sensors": {
            "egr_valve_position": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "stuck_or_blocked"},
            "intake_manifold_temp": {"min_reading": 25, "max_reading": 80, "unit": "°C", "expected_condition": "no_temperature_rise"},
            "map_sensor_egr_test": {"min_reading": 20, "max_reading": 95, "unit": "kPa", "expected_condition": "no_pressure_change"},
            "nox_sensor": {"min_reading": 0, "max_reading": 2000, "unit": "ppm", "expected_condition": "high_emissions"},
        },
    },
    "P0401": {
        "description": "Exhaust Gas Recirculation Flow Insufficient Detected",
        "points_weight": 5,
        "sensors": {
            "egr_flow_rate": {"min_reading": 0, "max_reading": 10, "unit": "g/s", "expected_condition": "insufficient_flow"},
            "differential_pressure_egr": {"min_reading": 0, "max_reading": 5, "unit": "kPa", "expected_condition": "low_differential"},
            "exhaust_back_pressure": {"min_reading": 1, "max_reading": 25, "unit": "kPa", "expected_condition": "high_back_pressure"},
            "intake_temp_post_egr": {"min_reading": 30, "max_reading": 60, "unit": "°C", "expected_condition": "insufficient_heating"},
        },
    },
    "P0410": {
        "description": "Secondary Air Injection System Malfunction",
        "points_weight": 4,
        "sensors": {
            "air_pump_current": {"min_reading": 0, "max_reading": 3, "unit": "A", "expected_condition": "no_current_or_overcurrent"},
            "air_injection_valve": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "stuck_closed"},
            "o2_sensor_during_air_test": {"min_reading": 0.0, "max_reading": 0.2, "unit": "V", "expected_condition": "no_lean_shift"},
            "exhaust_temp_upstream": {"min_reading": 200, "max_reading": 600, "unit": "°C", "expected_condition": "no_cooling_effect"},
        },
    },
    "P0420": {
        "description": "Catalyst System Efficiency Below Threshold (Bank 1)",
        "points_weight": 6,
        "sensors": {
            "o2_sensor_b1s1": {"min_reading": 0.1, "max_reading": 0.9, "unit": "V", "expected_condition": "normal_switching"},
            "o2_sensor_b1s2": {"min_reading": 0.4, "max_reading": 0.6, "unit": "V", "expected_condition": "similar_to_upstream"},
            "catalyst_temperature": {"min_reading": 300, "max_reading": 800, "unit": "°C", "expected_condition": "operating_temp"},
            "catalyst_efficiency": {"min_reading": 10, "max_reading": 60, "unit": "%", "expected_condition": "below_threshold"},
        },
    },
    "P0440": {
        "description": "Evaporative Emission Control System Malfunction",
        "points_weight": 3,
        "sensors": {
            "fuel_tank_pressure": {"min_reading": -10, "max_reading": 10, "unit": "kPa", "expected_condition": "abnormal_pressure"},
            "purge_valve_duty_cycle": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "stuck_closed"},
            "vent_valve_position": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "malfunction"},
            "fuel_level_sensor": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "normal_reference"},
        },
    },
    "P0441": {
        "description": "Evaporative Emission Control System Incorrect Purge Flow",
        "points_weight": 4,
        "sensors": {
            "purge_valve_position": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "incorrect_flow"},
            "manifold_vacuum": {"min_reading": 30, "max_reading": 70, "unit": "kPa", "expected_condition": "insufficient_vacuum"},
            "fuel_vapor_pressure": {"min_reading": -5, "max_reading": 15, "unit": "kPa", "expected_condition": "abnormal"},
            "engine_load_during_purge": {"min_reading": 20, "max_reading": 80, "unit": "%", "expected_condition": "load_affected"},
        },
    },
    "P0500": {
        "description": "Vehicle Speed Sensor Malfunction",
        "points_weight": 4,
        "sensors": {
            "vehicle_speed": {"min_reading": 0, "max_reading": 5, "unit": "km/h", "expected_condition": "no_signal_while_moving"},
            "transmission_output_speed": {"min_reading": 0, "max_reading": 100, "unit": "rpm", "expected_condition": "normal_reference"},
            "wheel_speed_sensors": {"min_reading": 0, "max_reading": 2000, "unit": "rpm", "expected_condition": "normal_backup"},
            "speedometer_accuracy": {"min_reading": 0, "max_reading": 50, "unit": "%", "expected_condition": "inaccurate"},
        },
    },
    "P0505": {
        "description": "Idle Control System Malfunction",
        "points_weight": 6,
        "sensors": {
            "idle_air_control_position": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "stuck_or_erratic"},
            "engine_rpm_idle": {"min_reading": 400, "max_reading": 1200, "unit": "rpm", "expected_condition": "unstable_idle"},
            "throttle_position_idle": {"min_reading": 0, "max_reading": 5, "unit": "%", "expected_condition": "closed_position"},
            "manifold_vacuum_idle": {"min_reading": 45, "max_reading": 65, "unit": "kPa", "expected_condition": "fluctuating"},
        },
    },
    "P0700": {
        "description": "Transmission Control System Malfunction",
        "points_weight": 7,
        "sensors": {
            "transmission_fluid_temp": {"min_reading": 60, "max_reading": 120, "unit": "°C", "expected_condition": "overheating_or_sensor_fault"},
            "torque_converter_clutch": {"min_reading": 0, "max_reading": 100, "unit": "%", "expected_condition": "slipping_or_stuck"},
            "transmission_input_speed": {"min_reading": 0, "max_reading": 5000, "unit": "rpm", "expected_condition": "correlation_issues"},
            "transmission_output_speed": {"min_reading": 0, "max_reading": 3000, "unit": "rpm", "expected_condition": "speed_sensor_fault"},
        },
    },
}

# ─────────────────────────────────────────────────────────────────────────────
# Vehicle Problem Categories
# ─────────────────────────────────────────────────────────────────────────────
vehicle_problems = {
    "Fuel System Problems": [
        "Fuel pump failure", "Fuel injector failure",
        "Fuel pressure regulator failure", "Fuel filter blockage",
    ],
    "Ignition System Problems": [
        "Ignition coil failure", "Spark plug failure",
        "Crankshaft position sensor failure", "Camshaft position sensor failure",
    ],
    "Air Intake System Problems": [
        "Mass airflow sensor failure", "Manifold pressure sensor failure",
        "Throttle position sensor failure", "Air filter restriction",
    ],
    "Engine Mechanical Problems": [
        "Low compression (cylinder)", "Valve timing issues", "Intake manifold leak",
    ],
    "Electrical System Problems": [
        "Wiring harness damage", "ECU power supply issues", "Alternator charging problems",
    ],
    "Emissions System Problems": [
        "Catalytic converter failure", "EGR valve failure", "EVAP system leak",
    ],
    "Cooling System Problems": [
        "Thermostat failure", "Coolant temperature sensor failure",
    ],
}

# ─────────────────────────────────────────────────────────────────────────────
# DTC → Root Cause → Repair Steps
# ─────────────────────────────────────────────────────────────────────────────
dtc_root_cause_repairs = {
    "P0001": [
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel pressure regulator failure",
            "repair_steps": [
                "Verify fuel rail pressure vs spec during KOEO/idle/load; confirm regulator control and reference vacuum source",
                "Inspect return line for restriction; replace regulator if control is non-responsive/stuck",
                "Re-test trims and O2 response after replacement; clear codes and road test",
            ],
        },
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel pump failure",
            "repair_steps": [
                "Check pump command, relay, fuse, and ground; scope pump current for health",
                "Measure rail pressure and volume; replace pump if out-of-spec and confirm filter integrity",
                "Replace filter/strainer together; verify pressure under load and trims normalize",
            ],
        },
        {
            "root_cause_category": "Electrical System Problems",
            "root_cause": "Wiring harness damage",
            "repair_steps": [
                "Perform harness wiggle test on regulator/pump circuits; inspect connectors for corrosion/back-out",
                "Repair opens/shorts, replace terminals as needed; verify voltage drop under load",
                "Clear codes and validate with freeze frame replication",
            ],
        },
    ],
    "P0087": [
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel filter blockage",
            "repair_steps": [
                "Measure rail pressure vs spec at idle and WOT; if low, replace clogged fuel filter first",
                "Inspect tank for contamination; flush lines if debris is present",
                "Re-check pressure and trims; clear codes and road test",
            ],
        },
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel pump failure",
            "repair_steps": [
                "Verify pump voltage, ground, and relay control; scope current for worn commutator patterns",
                "Replace pump module if pressure/volume remain low after filter service",
                "Re-verify under load; confirm LTFT/STFT recover",
            ],
        },
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel pressure regulator failure",
            "repair_steps": [
                "Command regulator (if electronic) and check response; for vacuum-referenced, verify diaphragm integrity",
                "Replace regulator if stuck/bleeding; inspect return lines for restrictions/leaks",
                "Re-test rail pressure stability during transients",
            ],
        },
    ],
    "P0088": [
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel pressure regulator failure",
            "repair_steps": [
                "Check regulator command and return flow; inspect for stuck-closed behavior",
                "Replace regulator and verify return line is free of restriction",
                "Re-verify trims trend negative-to-normal and O2 rich bias resolves",
            ],
        },
    ],
    "P0100": [
        {
            "root_cause_category": "Air Intake System Problems",
            "root_cause": "Mass airflow sensor failure",
            "repair_steps": [
                "Inspect intake tract for leaks upstream/downstream; clean MAF with proper cleaner",
                "Check MAF g/s vs RPM heuristic; replace MAF if signal is erratic/stuck",
                "Relearn idle/airflow; verify trims and drivability",
            ],
        },
        {
            "root_cause_category": "Electrical System Problems",
            "root_cause": "Wiring harness damage",
            "repair_steps": [
                "Pin-test MAF power/ground/signal; repair corroded pins and broken wires",
                "Backprobe with scope for dropout/noise; correct routing away from EMI",
                "Clear codes; confirm stable signal",
            ],
        },
    ],
    "P0101": [
        {
            "root_cause_category": "Air Intake System Problems",
            "root_cause": "Mass airflow sensor failure",
            "repair_steps": [
                "Clean or replace contaminated MAF; inspect oiled filters causing residue",
                "Compare MAF to MAP/VE; if out-of-model, replace MAF",
                "Reset KAM/adaptations and road test",
            ],
        },
        {
            "root_cause_category": "Engine Mechanical Problems",
            "root_cause": "Intake manifold leak",
            "repair_steps": [
                "Smoke-test for unmetered air (hoses, PCV, gaskets); repair leaks",
                "Reconfirm MAF-MAP correlation and load calc",
                "Verify trims recover to normal range",
            ],
        },
    ],
    "P0115": [
        {
            "root_cause_category": "Cooling System Problems",
            "root_cause": "Coolant temperature sensor failure",
            "repair_steps": [
                "Ohm-check ECT vs temperature; replace if erratic/stuck",
                "Inspect connector/terminal tension and corrosion; repair",
                "Verify fan control and warmup behavior normalize",
            ],
        },
        {
            "root_cause_category": "Cooling System Problems",
            "root_cause": "Thermostat failure",
            "repair_steps": [
                "Assess warm-up rate; replace thermostat if stuck open/closed",
                "Bleed cooling system; recheck warmup curve",
                "Confirm ECT stability and code clearance",
            ],
        },
    ],
    "P0116": [
        {
            "root_cause_category": "Cooling System Problems",
            "root_cause": "Thermostat failure",
            "repair_steps": [
                "Check warm-up slope; replace thermostat if delayed/no opening",
                "Bleed system and verify heater/core circulation",
                "Re-test ECT signal for expected dynamics",
            ],
        },
        {
            "root_cause_category": "Cooling System Problems",
            "root_cause": "Coolant temperature sensor failure",
            "repair_steps": [
                "Replace ECT if voltage stuck or noisy; repair connector",
                "Confirm correlation with IAT/ambient after cold soak",
                "Clear and drive cycle",
            ],
        },
    ],
    "P0120": [
        {
            "root_cause_category": "Air Intake System Problems",
            "root_cause": "Throttle position sensor failure",
            "repair_steps": [
                "Check dual-track TPS correlation; clean throttle body and plate",
                "Replace TPS or throttle body assembly if correlation fails",
                "Perform throttle relearn and validate idle stability",
            ],
        },
        {
            "root_cause_category": "Electrical System Problems",
            "root_cause": "Wiring harness damage",
            "repair_steps": [
                "Inspect TPS harness for intermittent opens/shorts; repair terminals",
                "Scope for dropout under pedal sweeps",
                "Clear codes; confirm smooth response",
            ],
        },
    ],
    "P0171": [
        {
            "root_cause_category": "Air Intake System Problems",
            "root_cause": "Mass airflow sensor failure",
            "repair_steps": [
                "Clean MAF and inspect intake tract; verify MAF g/s vs RPM",
                "Replace MAF if out-of-range or noisy; reset adaptations",
                "Confirm trims and O2 lean bias resolve",
            ],
        },
        {
            "root_cause_category": "Engine Mechanical Problems",
            "root_cause": "Intake manifold leak",
            "repair_steps": [
                "Smoke-test intake/PCV; repair leaks at hoses and gaskets",
                "Verify fuel trims return to normal at idle and cruise",
                "Re-run drive cycle",
            ],
        },
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel pump failure",
            "repair_steps": [
                "Measure rail pressure; replace weak pump after verifying power/ground",
                "Replace filter; confirm trims normalize under load",
                "Clear codes and test",
            ],
        },
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel filter blockage",
            "repair_steps": [
                "Replace clogged filter; flush lines if contaminated",
                "Re-test pressure and trims; verify O2 activity",
                "Drive cycle to confirm",
            ],
        },
    ],
    "P0172": [
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel pressure regulator failure",
            "repair_steps": [
                "Check for high rail pressure; replace regulator if stuck or bleeding vacuum fuel",
                "Verify negative trims return toward zero",
                "Re-test O2 rich bias resolution",
            ],
        },
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel injector failure",
            "repair_steps": [
                "Identify leaking/stuck injector via balance/leak-down; replace",
                "Change oil if fuel-contaminated; verify trims recover",
                "Drive cycle monitors",
            ],
        },
    ],
    "P0200": [
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel injector failure",
            "repair_steps": [
                "Check injector supply voltage, driver control, and coil resistance",
                "Repair circuit faults; replace failed injector(s)",
                "Verify cylinder contribution balances",
            ],
        },
        {
            "root_cause_category": "Electrical System Problems",
            "root_cause": "Wiring harness damage",
            "repair_steps": [
                "Locate opens/shorts in injector harness; repair and secure",
                "Confirm driver operation with scope/NOID light",
                "Clear codes; verify smooth operation",
            ],
        },
    ],
    "P0201": [
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel injector failure",
            "repair_steps": [
                "Measure injector 1 resistance/current; perform balance test",
                "Clean or replace injector; code injector if required",
                "Verify cylinder contribution and O2 response",
            ],
        },
    ],
    "P0230": [
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel pump failure",
            "repair_steps": [
                "Test pump relay/fuse/control; verify voltage at pump",
                "Replace pump module if inoperative; renew filter/strainer",
                "Confirm pressure and delivery meet spec",
            ],
        },
        {
            "root_cause_category": "Electrical System Problems",
            "root_cause": "Wiring harness damage",
            "repair_steps": [
                "Repair opens/shorts in primary pump circuit and grounds",
                "Voltage drop test under load; correct poor grounds",
                "Clear codes; verify start/run",
            ],
        },
    ],
    "P0300": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Ignition coil failure",
            "repair_steps": [
                "Identify misfiring cylinders via counters; swap-test coils",
                "Replace weak coils and associated plugs",
                "Verify misfire counters reset",
            ],
        },
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Spark plug failure",
            "repair_steps": [
                "Inspect plugs for wear/fouling/incorrect gap; replace",
                "Check dwell and secondary voltage",
                "Confirm smooth idle/load",
            ],
        },
        {
            "root_cause_category": "Fuel System Problems",
            "root_cause": "Fuel injector failure",
            "repair_steps": [
                "Balance test injectors; clean/replace outliers",
                "Confirm trims and misfires resolve",
                "Drive cycle verification",
            ],
        },
    ],
    "P0301": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Ignition coil failure",
            "repair_steps": [
                "Swap coil 1 with another cylinder to see if misfire follows",
                "Replace coil and plug if confirmed",
                "Verify crank acceleration smooths",
            ],
        },
        {
            "root_cause_category": "Engine Mechanical Problems",
            "root_cause": "Low compression (cylinder)",
            "repair_steps": [
                "Perform compression/leak-down on cylinder 1",
                "Repair valves/rings/head gasket as indicated",
                "Re-verify misfire elimination",
            ],
        },
    ],
    "P0302": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Ignition coil failure",
            "repair_steps": [
                "Swap-test coil 2; replace if fault follows",
                "Install new plug and verify firing",
                "Re-check misfire counter",
            ],
        },
    ],
    "P0303": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Ignition coil failure",
            "repair_steps": [
                "Swap-test coil 3; replace if defective",
                "Renew spark plug; verify ignition waveform",
                "Confirm misfire resolved",
            ],
        },
    ],
    "P0304": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Ignition coil failure",
            "repair_steps": [
                "Swap-test coil 4 and replace as needed",
                "Replace spark plug 4 and inspect boot",
                "Re-test for misfires",
            ],
        },
    ],
    "P0325": [
        {
            "root_cause_category": "Electrical System Problems",
            "root_cause": "Wiring harness damage",
            "repair_steps": [
                "Inspect knock sensor harness for chafe and poor shielding",
                "Repair/replace connector and wiring; torque sensor to spec",
                "Verify timing retard responds under knock test",
            ],
        },
    ],
    "P0335": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Crankshaft position sensor failure",
            "repair_steps": [
                "Scope CKP signal; check sensor gap/reluctor integrity",
                "Replace CKP sensor if no/erratic signal; relearn if required",
                "Verify start/run and sync",
            ],
        },
        {
            "root_cause_category": "Engine Mechanical Problems",
            "root_cause": "Valve timing issues",
            "repair_steps": [
                "Inspect timing chain/belt stretch and alignment; repair",
                "Confirm CKP/CMP sync restores",
                "Clear codes and validate",
            ],
        },
    ],
    "P0336": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Crankshaft position sensor failure",
            "repair_steps": [
                "Check frequency stability and signal amplitude",
                "Replace sensor if noisy/weak",
                "Confirm timing accuracy improves",
            ],
        },
    ],
    "P0340": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Camshaft position sensor failure",
            "repair_steps": [
                "Test CMP sensor output; replace if no/intermittent signal",
                "Perform relearn if applicable",
                "Confirm sync and drivability",
            ],
        },
    ],
    "P0341": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Camshaft position sensor failure",
            "repair_steps": [
                "Check CMP output quality and mounting; replace if weak/noisy",
                "Relearn cam/crank if required",
                "Verify correlation within spec",
            ],
        },
    ],
    "P0350": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Ignition coil failure",
            "repair_steps": [
                "Check primary/secondary patterns; replace failing coils",
                "Verify dwell control and coil supply voltage",
                "Re-test misfire counters",
            ],
        },
    ],
    "P0351": [
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Ignition coil failure",
            "repair_steps": [
                "Test Coil A primary/secondary; replace coil and plug as needed",
                "Confirm cylinder 1 combustion improves",
                "Verify knock sensor now detects normal events",
            ],
        },
    ],
    "P0400": [
        {
            "root_cause_category": "Emissions System Problems",
            "root_cause": "EGR valve failure",
            "repair_steps": [
                "Command EGR and observe MAP/idle effect; clean/replace stuck valve",
                "Clean carbon from passages; verify flow change in test",
                "Confirm NOx proxy readings trend down",
            ],
        },
    ],
    "P0420": [
        {
            "root_cause_category": "Emissions System Problems",
            "root_cause": "Catalytic converter failure",
            "repair_steps": [
                "Resolve upstream misfire/fuel/air faults first",
                "Check for exhaust leaks; replace failing converter if efficiency remains low",
                "Verify downstream O2 behavior differs from upstream",
            ],
        },
        {
            "root_cause_category": "Ignition System Problems",
            "root_cause": "Spark plug failure",
            "repair_steps": [
                "Replace worn/fouled plugs and any weak coils contributing to misfire",
                "Confirm catalyst temperatures/efficiency improve",
                "Clear and drive cycle to readiness",
            ],
        },
    ],
    "P0440": [
        {
            "root_cause_category": "Emissions System Problems",
            "root_cause": "EVAP system leak",
            "repair_steps": [
                "Inspect/replace gas cap; smoke-test EVAP for hose/canister leaks",
                "Test purge and vent valves; replace faulty components",
                "Verify FTP pressure integrity test passes",
            ],
        },
    ],
    "P0441": [
        {
            "root_cause_category": "Emissions System Problems",
            "root_cause": "EVAP system leak",
            "repair_steps": [
                "Function-test purge valve for stuck/flow error; replace if leaking",
                "Check hoses and canister for restriction/leak",
                "Verify manifold vacuum influence on purge is correct",
            ],
        },
    ],
    "P0505": [
        {
            "root_cause_category": "Air Intake System Problems",
            "root_cause": "Throttle position sensor failure",
            "repair_steps": [
                "Clean throttle body; verify TPS/IAC commanded responses",
                "Replace TPS/throttle body if needed and perform idle relearn",
                "Confirm stable idle RPM",
            ],
        },
        {
            "root_cause_category": "Engine Mechanical Problems",
            "root_cause": "Intake manifold leak",
            "repair_steps": [
                "Smoke-test for vacuum leaks affecting idle control",
                "Repair leaks; verify idle normalization",
                "Clear and validate",
            ],
        },
    ],
    "P0700": [
        {
            "root_cause_category": "Electrical System Problems",
            "root_cause": "ECU power supply issues",
            "repair_steps": [
                "Scan TCM for subcodes; verify power/ground/communication to TCM",
                "Repair module supplies and CAN wiring as needed",
                "Clear DTCs in PCM and TCM; confirm no return",
            ],
        },
    ],
    "P0016": [
        {
            "root_cause_category": "Engine Mechanical Problems",
            "root_cause": "Valve timing issues",
            "repair_steps": [
                "Verify cam/crank correlation; inspect chain/belt, tensioner, and VVT actuator",
                "Correct mechanical timing and replace worn parts",
                "Confirm sync and drivability restore",
            ],
        },
    ],
}
