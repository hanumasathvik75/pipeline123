"""
diag_portal.py
Demo-ready PySide6 GUI + Lightweight ML decision pipeline for DTC -> root cause analysis.

IMPORTANT:
- DO NOT include datasets in this file.
- Where marked below add:
    * `obd2_dtc_diagnostic_database` (nested dict of DTCs -> sensors -> min/max/units/points_weight)
    * `vehicle_problems` (dict of root cause categories -> list)
    * `dtc_root_cause_repairs` (dict mapping dtc -> list of root cause dicts with repair_steps)
    * Optionally: historical sensor dataframe for anomaly model training

See the logic.pdf pre-requirements & working logic which this implementation follows.
(Reference for logic and pre-requirements from your uploaded file). 
"""

import sys
import time
import math
import json
from functools import partial
from collections import Counter, defaultdict, OrderedDict

# UI imports
from PySide6.QtWidgets import (
    QApplication, QWidget, QMainWindow, QVBoxLayout, QHBoxLayout, QLabel, QPushButton,
    QTextEdit, QListWidget, QListWidgetItem, QTableWidget, QTableWidgetItem, QFrame,
    QLineEdit, QMessageBox, QSplitter, QSizePolicy, QComboBox, QFileDialog
)
from PySide6.QtCore import Qt, QTimer
from PySide6.QtGui import QFont

# plotting
import matplotlib
matplotlib.use("QtAgg")
from matplotlib.backends.backend_qtagg import FigureCanvasQTAgg as FigureCanvas
from matplotlib.figure import Figure

# numerical
import numpy as np

# Try sklearn; otherwise fallback to simple heuristics
try:
    from sklearn.tree import DecisionTreeClassifier
    from sklearn.ensemble import IsolationForest
    from sklearn.svm import OneClassSVM
    SKLEARN_AVAILABLE = True
except Exception:
    SKLEARN_AVAILABLE = False

# ---------------------------
# PLACEHOLDERS: ADD YOUR DICTIONARIES HERE (DO NOT INCLUDE DATASETS)
# ---------------------------
# Add your obd2_dtc_diagnostic_database dict here (copy from logic.pdf / your dataset).
# Example small placeholder (you will replace with full dict from logic.pdf):


obd2_dtc_diagnostic_database = {
# FUEL SYSTEM AND AIR/FUEL MIXTURE CODES
"P0001": {
"description": "Fuel Volume Regulator Control Circuit/Open",
"points_weight": 7,
"sensors": {
"fuel_rail_pressure": {"min_reading": 0, "max_reading": 50, "unit": "psi",
"expected_condition": "below_normal"},
"fuel_pump_duty_cycle": {"min_reading": 0, "max_reading": 30, "unit": "%",
"expected_condition": "low"},
"engine_rpm": {"min_reading": 0, "max_reading": 2000, "unit": "rpm",
"expected_condition": "unstable"},
"maf_sensor": {"min_reading": 2.0, "max_reading": 8.0, "unit": "g/s",
"expected_condition": "normal"}
}
},
"P0087": {
"description": "Fuel Rail/System Pressure - Too Low",
"points_weight": 8,
"sensors": {
"fuel_rail_pressure": {"min_reading": 0, "max_reading": 35, "unit": "psi",
"expected_condition": "critically_low"},
"fuel_pump_current": {"min_reading": 2.0, "max_reading": 6.0, "unit": "A",
"expected_condition": "low"},
"fuel_injector_pulse_width": {"min_reading": 5.0, "max_reading": 15.0, "unit":
"ms", "expected_condition": "extended"},"o2_sensor_bank1": {"min_reading": 0.1, "max_reading": 0.3, "unit": "V",
"expected_condition": "lean"},
"short_term_fuel_trim": {"min_reading": 15, "max_reading": 25, "unit": "%",
"expected_condition": "high_positive"}
}
},
"P0088": {
"description": "Fuel Rail/System Pressure - Too High",
"points_weight": 7,
"sensors": {
"fuel_rail_pressure": {"min_reading": 65, "max_reading": 120, "unit": "psi",
"expected_condition": "high"},
"fuel_pressure_regulator_duty": {"min_reading": 80, "max_reading": 100, "unit":
"%", "expected_condition": "maximum"},
"o2_sensor_bank1": {"min_reading": 0.7, "max_reading": 0.9, "unit": "V",
"expected_condition": "rich"},
"short_term_fuel_trim": {"min_reading": -25, "max_reading": -15, "unit": "%",
"expected_condition": "high_negative"}
}
},
# MASS AIR FLOW AND INTAKE SYSTEM
"P0100": {
"description": "Mass or Volume Air Flow Circuit Malfunction",
"points_weight": 6,
"sensors": {
"maf_sensor": {"min_reading": 0.0, "max_reading": 1.0, "unit": "g/s",
"expected_condition": "erratic_or_stuck"},"map_sensor": {"min_reading": 10, "max_reading": 105, "unit": "kPa",
"expected_condition": "normal_backup"},
"iat_sensor": {"min_reading": -10, "max_reading": 60, "unit": "°C",
"expected_condition": "normal"},
"throttle_position": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "variable"},
"engine_load": {"min_reading": 10, "max_reading": 90, "unit": "%",
"expected_condition": "inconsistent"}
}
},
"P0101": {
"description": "Mass or Volume Air Flow Circuit Range/Performance Problem",
"points_weight": 5,
"sensors": {
"maf_sensor": {"min_reading": 1.5, "max_reading": 25.0, "unit": "g/s",
"expected_condition": "out_of_range"},
"map_sensor": {"min_reading": 20, "max_reading": 100, "unit": "kPa",
"expected_condition": "correlates_poorly"},
"engine_rpm": {"min_reading": 500, "max_reading": 6000, "unit": "rpm",
"expected_condition": "normal"},
"calculated_load": {"min_reading": 15, "max_reading": 85, "unit": "%",
"expected_condition": "mismatch_with_maf"}
}
},
# MANIFOLD PRESSURE AND THROTTLE SYSTEM
"P0105": {
"description": """Manifold Absolute Pressure/Barometric Pressure Circuit
Malfunction""",
"points_weight": 6,"sensors": {
"map_sensor": {"min_reading": 0, "max_reading": 10, "unit": "kPa",
"expected_condition": "stuck_low_or_high"},
"barometric_pressure": {"min_reading": 95, "max_reading": 105, "unit": "kPa",
"expected_condition": "reference_normal"},
"throttle_position": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "variable"},
"engine_rpm": {"min_reading": 600, "max_reading": 4000, "unit": "rpm",
"expected_condition": "normal"}
}
},
# TEMPERATURE SENSORS
"P0110": {
"description": "Intake Air Temperature Circuit Malfunction",
"points_weight": 4,
"sensors": {
"iat_sensor": {"min_reading": -40, "max_reading": 150, "unit": "°C",
"expected_condition": "out_of_range_or_stuck"},
"ect_sensor": {"min_reading": 80, "max_reading": 105, "unit": "°C",
"expected_condition": "normal_reference"},
"ambient_temp": {"min_reading": 10, "max_reading": 45, "unit": "°C",
"expected_condition": "normal_reference"},
"fuel_trim_adaptation": {"min_reading": -10, "max_reading": 10, "unit": "%",
"expected_condition": "compensating"}
}
},
"P0115": {
"description": "Engine Coolant Temperature Circuit Malfunction","points_weight": 7,
"sensors": {
"ect_sensor": {"min_reading": -40, "max_reading": 150, "unit": "°C",
"expected_condition": "stuck_or_erratic"},
"iat_sensor": {"min_reading": 15, "max_reading": 50, "unit": "°C",
"expected_condition": "reference_normal"},
"thermostat_position": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "unknown_state"},
"cooling_fan_duty": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "erratic_operation"}
}
},
# THROTTLE POSITION SENSOR
"P0120": {
"description": "Throttle/Pedal Position Sensor Circuit Malfunction",
"points_weight": 8,
"sensors": {
"throttle_position_1": {"min_reading": 0.0, "max_reading": 5.0, "unit": "V",
"expected_condition": "stuck_or_erratic"},
"throttle_position_2": {"min_reading": 0.0, "max_reading": 5.0, "unit": "V",
"expected_condition": "correlation_error"},
"accelerator_pedal_pos": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "normal_input"},
"idle_air_control": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "compensating"}
}
},
# OXYGEN SENSOR CODES
"P0130": {
"description": "O2 Sensor Circuit Malfunction (Bank 1 Sensor 1)",
"points_weight": 6,
"sensors": {
"o2_sensor_b1s1": {"min_reading": 0.0, "max_reading": 1.2, "unit": "V",
"expected_condition": "stuck_or_no_activity"},
"short_term_fuel_trim_b1": {"min_reading": -25, "max_reading": 25, "unit": "%",
"expected_condition": "erratic"},
"long_term_fuel_trim_b1": {"min_reading": -25, "max_reading": 25, "unit": "%",
"expected_condition": "hunting"},
"catalyst_efficiency": {"min_reading": 50, "max_reading": 100, "unit": "%",
"expected_condition": "reduced"}
}
},

"P0171": {
"description": "System Too Lean (Bank 1)",
"points_weight": 7,
"sensors": {
"o2_sensor_b1s1": {"min_reading": 0.1, "max_reading": 0.35, "unit": "V",
"expected_condition": "consistently_lean"},
"short_term_fuel_trim_b1": {"min_reading": 15, "max_reading": 25, "unit": "%",
"expected_condition": "high_positive"},
"long_term_fuel_trim_b1": {"min_reading": 10, "max_reading": 25, "unit": "%",
"expected_condition": "high_positive"},
"maf_sensor": {"min_reading": 2.0, "max_reading": 20.0, "unit": "g/s",
"expected_condition": "potentially_high"},
"fuel_rail_pressure": {"min_reading": 30, "max_reading": 45, "unit": "psi",
"expected_condition": "potentially_low"}
}},
"P0172": {
"description": "System Too Rich (Bank 1)",
"points_weight": 7,
"sensors": {
"o2_sensor_b1s1": {"min_reading": 0.65, "max_reading": 0.9, "unit": "V",
"expected_condition": "consistently_rich"},
"short_term_fuel_trim_b1": {"min_reading": -25, "max_reading": -15, "unit": "%",
"expected_condition": "high_negative"},
"long_term_fuel_trim_b1": {"min_reading": -25, "max_reading": -10, "unit": "%",
"expected_condition": "high_negative"},
"fuel_rail_pressure": {"min_reading": 65, "max_reading": 85, "unit": "psi",
"expected_condition": "potentially_high"},
"maf_sensor": {"min_reading": 1.0, "max_reading": 15.0, "unit": "g/s",
"expected_condition": "potentially_low"}
}
},
"P0300": {
"description": "Random/Multiple Cylinder Misfire Detected",
"points_weight": 9,
"sensors": {
"crankshaft_position": {"min_reading": 0, "max_reading": 360, "unit": "degrees",
"expected_condition": "irregular_pattern"},
"camshaft_position": {"min_reading": 0, "max_reading": 720, "unit": "degrees",
"expected_condition": "correlation_issues"},
"engine_rpm": {"min_reading": 500, "max_reading": 4000, "unit": "rpm",
"expected_condition": "unstable"},"ignition_timing": {"min_reading": -10, "max_reading": 35, "unit": "degrees",
"expected_condition": "retarded"},
"fuel_injector_pulse": {"min_reading": 2.0, "max_reading": 12.0, "unit": "ms",
"expected_condition": "variable"}
}
},
"P0301": {
"description": "Cylinder 1 Misfire Detected",
"points_weight": 8,
"sensors": {
"cylinder_1_pressure": {"min_reading": 8, "max_reading": 15, "unit": "bar",
"expected_condition": "low_compression"},
"injector_1_current": {"min_reading": 0.5, "max_reading": 4.0, "unit": "A",
"expected_condition": "abnormal"},
"ignition_coil_1_primary": {"min_reading": 8, "max_reading": 14, "unit": "V",
"expected_condition": "poor_spark"},
"crankshaft_acceleration": {"min_reading": -500, "max_reading": 500, "unit":
"rpm/s", "expected_condition": "cylinder_1_deficit"}
}
},
# IGNITION SYSTEM CODES - NO START CRITICAL
"P0335": {
"description": "Crankshaft Position Sensor A Circuit Malfunction",
"points_weight": 10,
"sensors": {
"crankshaft_position": {"min_reading": 0.0, "max_reading": 1.0, "unit": "V",
"expected_condition": "no_signal_or_erratic"},"engine_rpm": {"min_reading": 0, "max_reading": 100, "unit": "rpm",
"expected_condition": "no_reading_or_intermittent"},
"ignition_timing": {"min_reading": 0, "max_reading": 0, "unit": "degrees",
"expected_condition": "unknown_or_default"},
"fuel_injection_timing": {"min_reading": 0, "max_reading": 0, "unit": "ms",
"expected_condition": "disabled_or_backup"}
}
},
"P0340": {
"description": "Camshaft Position Sensor Circuit Malfunction",
"points_weight": 9,
"sensors": {
"camshaft_position": {"min_reading": 0.0, "max_reading": 1.0, "unit": "V",
"expected_condition": "no_signal"},
"crankshaft_position": {"min_reading": 0, "max_reading": 360, "unit": "degrees",
"expected_condition": "normal_reference"},
"valve_timing": {"min_reading": 0, "max_reading": 0, "unit": "degrees",
"expected_condition": "unknown"},
"fuel_injection_sync": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "lost_synchronization"}
}
},
"P0350": {
"description": "Ignition Coil Primary/Secondary Circuit Malfunction",
"points_weight": 8,
"sensors": {"ignition_coil_primary_voltage": {"min_reading": 0, "max_reading": 8, "unit": "V",
"expected_condition": "low_or_absent"},
"ignition_coil_secondary_voltage": {"min_reading": 0, "max_reading": 15000,
"unit": "V", "expected_condition": "insufficient"},
"spark_plug_gap_voltage": {"min_reading": 5000, "max_reading": 25000, "unit":
"V", "expected_condition": "weak_spark"},
"dwell_time": {"min_reading": 1.0, "max_reading": 8.0, "unit": "ms",
"expected_condition": "incorrect"}
}
},
"P0351": {
"description": "Ignition Coil A Primary/Secondary Circuit",
"points_weight": 7,
"sensors": {
"ignition_coil_a_primary": {"min_reading": 0, "max_reading": 8, "unit": "V",
"expected_condition": "circuit_fault"},
"ignition_coil_a_current": {"min_reading": 0, "max_reading": 3, "unit": "A",
"expected_condition": "no_current_or_overcurrent"},
"cylinder_1_combustion": {"min_reading": 0, "max_reading": 80, "unit": "%",
"expected_condition": "poor_combustion"},
"knock_sensor_cyl1": {"min_reading": 0.5, "max_reading": 4.5, "unit": "V",
"expected_condition": "no_knock_detection"}
}
},
# FUEL INJECTOR CODES
"P0200": {
"description": "Injector Circuit Malfunction",
"points_weight": 8,"sensors": {
"fuel_injector_voltage": {"min_reading": 8, "max_reading": 16, "unit": "V",
"expected_condition": "supply_voltage_issue"},
"injector_pulse_width": {"min_reading": 0, "max_reading": 2, "unit": "ms",
"expected_condition": "no_pulse_or_short"},
"fuel_rail_pressure": {"min_reading": 40, "max_reading": 60, "unit": "psi",
"expected_condition": "normal_but_no_delivery"},
"engine_load": {"min_reading": 10, "max_reading": 90, "unit": "%",
"expected_condition": "high_but_no_fuel"}
}
},
"P0201": {
"description": "Injector Circuit Malfunction - Cylinder 1",
"points_weight": 7,
"sensors": {
"injector_1_resistance": {"min_reading": 10, "max_reading": 16, "unit": "Ohm",
"expected_condition": "out_of_spec"},
"injector_1_current": {"min_reading": 0, "max_reading": 1, "unit": "A",
"expected_condition": "no_current"},
"cylinder_1_contribution": {"min_reading": 0, "max_reading": 15, "unit": "%",
"expected_condition": "poor_contribution"},
"o2_sensor_response": {"min_reading": 0.1, "max_reading": 0.4, "unit": "V",
"expected_condition": "lean_indication"}
}
},
# FUEL PUMP CODES - NO START CRITICAL
"P0230": {
"description": "Fuel Pump Primary Circuit Malfunction","points_weight": 10,
"sensors": {
"fuel_pump_voltage": {"min_reading": 0, "max_reading": 8, "unit": "V",
"expected_condition": "low_or_no_voltage"},
"fuel_pump_current": {"min_reading": 0, "max_reading": 2, "unit": "A",
"expected_condition": "no_current"},
"fuel_rail_pressure": {"min_reading": 0, "max_reading": 20, "unit": "psi",
"expected_condition": "critically_low"},
"fuel_pump_relay_status": {"min_reading": 0, "max_reading": 1, "unit": "boolean",
"expected_condition": "relay_fault"}
}
},
# ADDITIONAL CRITICAL NO-START CODES
"P0016": {
"description": """Crankshaft Position - Camshaft Position Correlation Bank 1 Sensor
A""",
"points_weight": 9,
"sensors": {
"crankshaft_position": {"min_reading": 0, "max_reading": 360, "unit": "degrees",
"expected_condition": "normal_signal"},
"camshaft_position": {"min_reading": 0, "max_reading": 720, "unit": "degrees",
"expected_condition": "timing_offset"},
"valve_timing_error": {"min_reading": 5, "max_reading": 30, "unit": "degrees",
"expected_condition": "correlation_error"},
"vvt_actuator_position": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "stuck_or_slow"}
}
},
# KNOCK SENSOR
"P0325": {
"description": "Knock Sensor 1 Circuit Malfunction",
"points_weight": 5,
"sensors": {
"knock_sensor_1": {"min_reading": 0.0, "max_reading": 1.0, "unit": "V",
"expected_condition": "no_signal"},
"ignition_timing_retard": {"min_reading": 0, "max_reading": 5, "unit": "degrees",
"expected_condition": "no_retard_capability"},
"engine_knock_intensity": {"min_reading": 0, "max_reading": 10, "unit": "units",
"expected_condition": "undetected_knock"},
"combustion_pressure": {"min_reading": 20, "max_reading": 80, "unit": "bar",
"expected_condition": "potentially_damaging"}
}
},
# VEHICLE SPEED SENSOR
"P0500": {
"description": "Vehicle Speed Sensor Malfunction",
"points_weight": 4,
"sensors": {
"vehicle_speed": {"min_reading": 0, "max_reading": 5, "unit": "km/h",
"expected_condition": "no_signal_while_moving"},
"transmission_output_speed": {"min_reading": 0, "max_reading": 100, "unit":
"rpm", "expected_condition": "normal_reference"},
"wheel_speed_sensors": {"min_reading": 0, "max_reading": 2000, "unit": "rpm",
"expected_condition": "normal_backup"},
"speedometer_accuracy": {"min_reading": 0, "max_reading": 50, "unit": "%",
"expected_condition": "inaccurate"}
}
},# IDLE AIR CONTROL
"P0505": {
"description": "Idle Control System Malfunction",
"points_weight": 6,
"sensors": {
"idle_air_control_position": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "stuck_or_erratic"},
"engine_rpm_idle": {"min_reading": 400, "max_reading": 1200, "unit": "rpm",
"expected_condition": "unstable_idle"},
"throttle_position_idle": {"min_reading": 0, "max_reading": 5, "unit": "%",
"expected_condition": "closed_position"},
"manifold_vacuum_idle": {"min_reading": 45, "max_reading": 65, "unit": "kPa",
"expected_condition": "fluctuating"}
}
},
# EVAPORATIVE EMISSION SYSTEM
"P0440": {
"description": "Evaporative Emission Control System Malfunction",
"points_weight": 3,
"sensors": {
"fuel_tank_pressure": {"min_reading": -10, "max_reading": 10, "unit": "kPa",
"expected_condition": "abnormal_pressure"},
"purge_valve_duty_cycle": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "stuck_closed"},
"vent_valve_position": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "malfunction"},
"fuel_level_sensor": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "normal_reference"}
}},
"P0441": {
"description": "Evaporative Emission Control System Incorrect Purge Flow",
"points_weight": 4,
"sensors": {
"purge_valve_position": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "incorrect_flow"},
"manifold_vacuum": {"min_reading": 30, "max_reading": 70, "unit": "kPa",
"expected_condition": "insufficient_vacuum"},
"fuel_vapor_pressure": {"min_reading": -5, "max_reading": 15, "unit": "kPa",
"expected_condition": "abnormal"},
"engine_load_during_purge": {"min_reading": 20, "max_reading": 80, "unit": "%",
"expected_condition": "load_affected"}
}
},
# EGR SYSTEM CODES
"P0400": {
"description": "Exhaust Gas Recirculation Flow Malfunction",
"points_weight": 5,
"sensors": {
"egr_valve_position": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "stuck_or_blocked"},
"intake_manifold_temp": {"min_reading": 25, "max_reading": 80, "unit": "°C",
"expected_condition": "no_temperature_rise"},
"map_sensor_egr_test": {"min_reading": 20, "max_reading": 95, "unit": "kPa",
"expected_condition": "no_pressure_change"},
"nox_sensor": {"min_reading": 0, "max_reading": 2000, "unit": "ppm",
"expected_condition": "high_emissions"}}
},
"P0401": {
"description": "Exhaust Gas Recirculation Flow Insufficient Detected",
"points_weight": 5,
"sensors": {
"egr_flow_rate": {"min_reading": 0, "max_reading": 10, "unit": "g/s",
"expected_condition": "insufficient_flow"},
"differential_pressure_egr": {"min_reading": 0, "max_reading": 5, "unit": "kPa",
"expected_condition": "low_differential"},
"exhaust_back_pressure": {"min_reading": 1, "max_reading": 25, "unit": "kPa",
"expected_condition": "high_back_pressure"},
"intake_temp_post_egr": {"min_reading": 30, "max_reading": 60, "unit": "°C",
"expected_condition": "insufficient_heating"}
}
},
# CATALYST EFFICIENCY
"P0420": {
"description": "Catalyst System Efficiency Below Threshold (Bank 1)",
"points_weight": 6,
"sensors": {
"o2_sensor_b1s1": {"min_reading": 0.1, "max_reading": 0.9, "unit": "V",
"expected_condition": "normal_switching"},
"o2_sensor_b1s2": {"min_reading": 0.4, "max_reading": 0.6, "unit": "V",
"expected_condition": "similar_to_upstream"},
"catalyst_temperature": {"min_reading": 300, "max_reading": 800, "unit": "°C",
"expected_condition": "operating_temp"},"catalyst_efficiency": {"min_reading": 10, "max_reading": 60, "unit": "%",
"expected_condition": "below_threshold"}
}
},
# SECONDARY AIR INJECTION
"P0410": {
"description": "Secondary Air Injection System Malfunction",
"points_weight": 4,
"sensors": {
"air_pump_current": {"min_reading": 0, "max_reading": 3, "unit": "A",
"expected_condition": "no_current_or_overcurrent"},
"air_injection_valve": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "stuck_closed"},
"o2_sensor_during_air_test": {"min_reading": 0.0, "max_reading": 0.2, "unit": "V",
"expected_condition": "no_lean_shift"},
"exhaust_temp_upstream": {"min_reading": 200, "max_reading": 600, "unit": "°C",
"expected_condition": "no_cooling_effect"}
}
},
# ADDITIONAL MISFIRE CODES
"P0302": {
"description": "Cylinder 2 Misfire Detected",
"points_weight": 8,
"sensors": {
"cylinder_2_pressure": {"min_reading": 8, "max_reading": 15, "unit": "bar",
"expected_condition": "low_compression"},
"injector_2_current": {"min_reading": 0.5, "max_reading": 4.0, "unit": "A",
"expected_condition": "abnormal"},"ignition_coil_2_primary": {"min_reading": 8, "max_reading": 14, "unit": "V",
"expected_condition": "poor_spark"},
"crankshaft_acceleration_cyl2": {"min_reading": -500, "max_reading": 500, "unit":
"rpm/s", "expected_condition": "cylinder_2_deficit"}
}
},
"P0303": {
"description": "Cylinder 3 Misfire Detected",
"points_weight": 8,
"sensors": {
"cylinder_3_pressure": {"min_reading": 8, "max_reading": 15, "unit": "bar",
"expected_condition": "low_compression"},
"injector_3_current": {"min_reading": 0.5, "max_reading": 4.0, "unit": "A",
"expected_condition": "abnormal"},
"ignition_coil_3_primary": {"min_reading": 8, "max_reading": 14, "unit": "V",
"expected_condition": "poor_spark"},
"crankshaft_acceleration_cyl3": {"min_reading": -500, "max_reading": 500, "unit":
"rpm/s", "expected_condition": "cylinder_3_deficit"}
}
},
"P0304": {
"description": "Cylinder 4 Misfire Detected",
"points_weight": 8,
"sensors": {
"cylinder_4_pressure": {"min_reading": 8, "max_reading": 15, "unit": "bar",
"expected_condition": "low_compression"},
"injector_4_current": {"min_reading": 0.5, "max_reading": 4.0, "unit": "A",
"expected_condition": "abnormal"},"ignition_coil_4_primary": {"min_reading": 8, "max_reading": 14, "unit": "V",
"expected_condition": "poor_spark"},
"crankshaft_acceleration_cyl4": {"min_reading": -500, "max_reading": 500, "unit":
"rpm/s", "expected_condition": "cylinder_4_deficit"}
}
},
# MORE IGNITION COIL CODES
"P0352": {
"description": "Ignition Coil B Primary/Secondary Circuit",
"points_weight": 7,
"sensors": {
"ignition_coil_b_primary": {"min_reading": 0, "max_reading": 8, "unit": "V",
"expected_condition": "circuit_fault"},
"ignition_coil_b_current": {"min_reading": 0, "max_reading": 3, "unit": "A",
"expected_condition": "no_current_or_overcurrent"},
"cylinder_2_combustion": {"min_reading": 0, "max_reading": 80, "unit": "%",
"expected_condition": "poor_combustion"},
"knock_sensor_cyl2": {"min_reading": 0.5, "max_reading": 4.5, "unit": "V",
"expected_condition": "no_knock_detection"}
}
},
"P0353": {
"description": "Ignition Coil C Primary/Secondary Circuit",
"points_weight": 7,
"sensors": {
"ignition_coil_c_primary": {"min_reading": 0, "max_reading": 8, "unit": "V",
"expected_condition": "circuit_fault"},"ignition_coil_c_current": {"min_reading": 0, "max_reading": 3, "unit": "A",
"expected_condition": "no_current_or_overcurrent"},
"cylinder_3_combustion": {"min_reading": 0, "max_reading": 80, "unit": "%",
"expected_condition": "poor_combustion"},
"knock_sensor_cyl3": {"min_reading": 0.5, "max_reading": 4.5, "unit": "V",
"expected_condition": "no_knock_detection"}
}
},
"P0202": {
"description": "Injector Circuit Malfunction - Cylinder 2",
"points_weight": 7,
"sensors": {
"injector_2_resistance": {"min_reading": 10, "max_reading": 16, "unit": "Ohm",
"expected_condition": "out_of_spec"},
"injector_2_current": {"min_reading": 0, "max_reading": 1, "unit": "A",
"expected_condition": "no_current"},
"cylinder_2_contribution": {"min_reading": 0, "max_reading": 15, "unit": "%",
"expected_condition": "poor_contribution"},
"fuel_trim_cyl2": {"min_reading": -25, "max_reading": 25, "unit": "%",
"expected_condition": "compensating"}
}
},
"P0203": {
"description": "Injector Circuit Malfunction - Cylinder 3",
"points_weight": 7,
"sensors": {"injector_3_resistance": {"min_reading": 10, "max_reading": 16, "unit": "Ohm",
"expected_condition": "out_of_spec"},
"injector_3_current": {"min_reading": 0, "max_reading": 1, "unit": "A",
"expected_condition": "no_current"},
"cylinder_3_contribution": {"min_reading": 0, "max_reading": 15, "unit": "%",
"expected_condition": "poor_contribution"},
"fuel_trim_cyl3": {"min_reading": -25, "max_reading": 25, "unit": "%",
"expected_condition": "compensating"}
}
},
# ADDITIONAL POSITION SENSOR CODES
"P0336": {
"description": "Crankshaft Position Sensor A Circuit Range/Performance",
"points_weight": 9,
"sensors": {
"crankshaft_position_signal": {"min_reading": 0.5, "max_reading": 4.5, "unit": "V",
"expected_condition": "weak_or_noisy_signal"},
"crankshaft_signal_frequency": {"min_reading": 10, "max_reading": 200, "unit":
"Hz", "expected_condition": "irregular_frequency"},
"engine_sync_status": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "poor_synchronization"},
"timing_accuracy": {"min_reading": 60, "max_reading": 95, "unit": "%",
"expected_condition": "reduced_accuracy"}
}
},
"P0341": {
"description": "Camshaft Position Sensor Circuit Range/Performance",
"points_weight": 8,"sensors": {
"camshaft_position_signal": {"min_reading": 0.5, "max_reading": 4.5, "unit": "V",
"expected_condition": "intermittent_or_weak"},
"cam_to_crank_correlation": {"min_reading": 50, "max_reading": 90, "unit": "%",
"expected_condition": "poor_correlation"},
"valve_timing_accuracy": {"min_reading": 70, "max_reading": 95, "unit": "%",
"expected_condition": "timing_drift"},
"sequential_injection_sync": {"min_reading": 0, "max_reading": 80, "unit": "%",
"expected_condition": "lost_sequence"}
}
},
# ADDITIONAL TEMPERATURE CODES
"P0111": {
"description": "Intake Air Temperature Circuit Range/Performance",
"points_weight": 4,
"sensors": {
"iat_sensor_voltage": {"min_reading": 0.2, "max_reading": 4.8, "unit": "V",
"expected_condition": "out_of_expected_range"},
"iat_vs_ect_correlation": {"min_reading": -20, "max_reading": 50, "unit": "°C",
"expected_condition": "poor_correlation"},
"ambient_temp_reference": {"min_reading": 5, "max_reading": 50, "unit": "°C",
"expected_condition": "normal_reference"},
"fuel_density_correction": {"min_reading": 90, "max_reading": 110, "unit": "%",
"expected_condition": "incorrect_correction"}
}
},
"P0116": {
"description": "Engine Coolant Temperature Circuit Range/Performance","points_weight": 6,
"sensors": {
"ect_sensor_voltage": {"min_reading": 0.2, "max_reading": 4.8, "unit": "V",
"expected_condition": "stuck_voltage"},
"ect_warm_up_rate": {"min_reading": 0.5, "max_reading": 3.0, "unit": "°C/min",
"expected_condition": "abnormal_warm_up"},
"thermostat_opening_temp": {"min_reading": 82, "max_reading": 95, "unit": "°C",
"expected_condition": "delayed_or_no_opening"},
"cooling_system_efficiency": {"min_reading": 60, "max_reading": 95, "unit": "%",
"expected_condition": "reduced_efficiency"}
}
},
# TRANSMISSION RELATED CODES (affects driveability)
"P0700": {
"description": "Transmission Control System Malfunction",
"points_weight": 7,
"sensors": {
"transmission_fluid_temp": {"min_reading": 60, "max_reading": 120, "unit": "°C",
"expected_condition": "overheating_or_sensor_fault"},
"torque_converter_clutch": {"min_reading": 0, "max_reading": 100, "unit": "%",
"expected_condition": "slipping_or_stuck"},
"transmission_input_speed": {"min_reading": 0, "max_reading": 5000, "unit":
"rpm", "expected_condition": "correlation_issues"},
"transmission_output_speed": {"min_reading": 0, "max_reading": 3000, "unit":
"rpm", "expected_condition": "speed_sensor_fault"}
}
},
# ADDITIONAL OXYGEN SENSOR CODES
"P0131": {
"description": "O2 Sensor Circuit Low Voltage (Bank 1 Sensor 1)",
"points_weight": 6,
"sensors": {
"o2_sensor_b1s1": {"min_reading": 0.0, "max_reading": 0.3, "unit": "V",
"expected_condition": "stuck_lean"},
"exhaust_temp_b1": {"min_reading": 200, "max_reading": 800, "unit": "°C",
"expected_condition": "normal_operating_temp"},
"o2_heater_current_b1s1": {"min_reading": 0, "max_reading": 2, "unit": "A",
"expected_condition": "heater_circuit_fault"},
"fuel_trim_response_b1": {"min_reading": 15, "max_reading": 25, "unit": "%",
"expected_condition": "excessive_positive_trim"}
}
},
"P0132": {
"description": "O2 Sensor Circuit High Voltage (Bank 1 Sensor 1)",
"points_weight": 6,
"sensors": {
"o2_sensor_b1s1": {"min_reading": 0.7, "max_reading": 1.2, "unit": "V",
"expected_condition": "stuck_rich"},
"exhaust_lambda_b1": {"min_reading": 0.7, "max_reading": 0.9, "unit": "lambda",
"expected_condition": "consistently_rich"},
"carbon_deposits_indicator": {"min_reading": 60, "max_reading": 100, "unit": "%",
"expected_condition": "heavy_deposits"},
"fuel_trim_response_b1": {"min_reading": -25, "max_reading": -15, "unit": "%",
"expected_condition": "excessive_negative_trim"}
}
}}
vehicle_problems = {
"Fuel System Problems": [
"Fuel pump failure",
"Fuel injector failure",
"Fuel pressure regulator failure",
"Fuel filter blockage"
],
"Ignition System Problems": [
"Ignition coil failure",
"Spark plug failure",
"Crankshaft position sensor failure",
"Camshaft position sensor failure"
],
"Air Intake System Problems": [
"Mass airflow sensor failure",
"Manifold pressure sensor failure",
"Throttle position sensor failure",
"Air filter restriction"
],
"Engine Mechanical Problems": [
"Low compression (cylinder)",
"Valve timing issues",
"Intake manifold leak"
],
"Electrical System Problems": ["Wiring harness damage",
"ECU power supply issues",
"Alternator charging problems"
],
"Emissions System Problems": [
"Catalytic converter failure",
"EGR valve failure",
"EVAP system leak"
],
"Cooling System Problems": [
"Thermostat failure",
"Coolant temperature sensor failure"
]
}
# DTC × Root Cause combinations with repair steps
dtc_root_cause_repairs = {
"P0001": [
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel pressure regulator failure",
"repair_steps": [
"Verify fuel rail pressure vs spec during KOEO/idle/load; confirm regulator control and reference vacuum source",
"Inspect return line for restriction; replace regulator if control is non-responsive/stuck",
"Re-test trims and O2 response after replacement; clear codes and road test"
]
},{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel pump failure",
"repair_steps": [
"Check pump command, relay, fuse, and ground; scope pump current for health",
"Measure rail pressure and volume; replace pump if out-of-spec and confirm filter integrity",
"Replace filter/strainer together; verify pressure under load and trims normalize"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Perform harness wiggle test on regulator/pump circuits; inspect connectors for corrosion/back-out",
"Repair opens/shorts, replace terminals as needed; verify voltage drop under load",
"Clear codes and validate with freeze frame replication"
]
}
],
"P0087": [
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel filter blockage",
"repair_steps": ["Measure rail pressure vs spec at idle and WOT; if low, replace clogged fuel filter first",
"Inspect tank for contamination; flush lines if debris is present",
"Re-check pressure and trims; clear codes and road test"
]
},
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel pump failure",
"repair_steps": [
"Verify pump voltage, ground, and relay control; scope current for worn commutator patterns",
"Replace pump module if pressure/volume remain low after filter service",
"Re-verify under load; confirm LTFT/STSFT recover"
]
},
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel pressure regulator failure",
"repair_steps": [
"Command regulator (if electronic) and check response; for vacuum- referenced, verify diaphragm integrity",
"Replace regulator if stuck/bleeding; inspect return lines for restrictions/leaks",
"Re-test rail pressure stability during transients"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage","repair_steps": [
"Pin-check fuel rail pressure sensor and pump circuits; repair opens/shorts/corrosion",
"Confirm sensor plausibility vs mechanical gauge",
"Clear codes; confirm stable pressure and trims"
]
}
],
"P0088": [
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel pressure regulator failure",
"repair_steps": [
"Check regulator command and return flow; inspect for stuck-closed behavior",
"Replace regulator and verify return line is free of restriction",
"Re-verify trims trend negative-to-normal and O2 rich bias resolves"
]
},
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel filter blockage (return-restricted systems)",
"repair_steps": [
"Inspect for incorrect filter or collapsed internal media increasing pressure",
"Replace with OE-spec filter; confirm pressure normalizes",
"Clear codes and drive cycle to readiness"
]
},{
"root_cause_category": "Electrical System Problems",
"root_cause": "ECU power supply issues",
"repair_steps": [
"Check reference voltage and grounds to fuel pressure sensor and regulator driver",
"Repair/clean grounds and power feeds; re-test sensor plausibility",
"Validate with scan tool live data vs mechanical gauge"
]
}
],
"P0100": [
{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Mass airflow sensor failure",
"repair_steps": [
"Inspect intake tract for leaks upstream/downstream; clean MAF with proper cleaner",
"Check MAF g/s vs RPM heuristic; replace MAF if signal is erratic/stuck",
"Relearn idle/airflow; verify trims and drivability"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Pin-test MAF power/ground/signal; repair corroded pins and broken wires",
"Backprobe with scope for dropout/noise; correct routing away from EMI","Clear codes; confirm stable signal"
]
},
{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Air filter restriction",
"repair_steps": [
"Inspect/replace clogged filter; remove debris and check housing seals",
"Verify MAF readings normalize under load",
"Re-check trims and smoke-test intake for leaks"
]
}
],
"P0101": [
{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Mass airflow sensor failure",
"repair_steps": [
"Clean or replace contaminated MAF; inspect oiled filters causing residue",
"Compare MAF to MAP/VE; if out-of-model, replace MAF",
"Reset KAM/adaptations and road test"
]
},
{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Intake manifold leak",
"repair_steps": ["Smoke-test for unmetered air (hoses, PCV, gaskets); repair leaks",
"Reconfirm MAF-MAP correlation and load calc",
"Verify trims recover to normal range"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Inspect MAF/MAP harness for opens/shorts; repair terminals",
"Scope signals for noise; reroute/secure",
"Clear codes; verify performance"
]
}
],
"P0105": [
{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Manifold pressure sensor failure",
"repair_steps": [
"Compare MAP to BARO KOEO; if implausible, replace MAP",
"Verify vacuum source tubing and port cleanliness",
"Recheck MAP tracking vs throttle events"
]
},
{
"root_cause_category": "Air Intake System Problems","root_cause": "Intake manifold leak",
"repair_steps": [
"Smoke-test manifold and vacuum hoses; fix leaks",
"Verify MAP stability at idle and during snap throttle",
"Re-evaluate driveability and trims"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Pin-check 5V ref, ground, and signal; repair wiring/connectors",
"Scope signal for dropouts; confirm PCM reads plausibly",
"Clear and re-test"
]
}
],
"P0110": [
{
"root_cause_category": "Cooling System Problems",
"root_cause": "Coolant temperature sensor failure",
"repair_steps": [
"Compare ECT and IAT warmup correlation; test ECT/IAT resistance vs temp",
"Replace failed sensor; ensure connectors are clean and sealed",
"Verify trims no longer compensating"
]
},{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Manifold pressure sensor failure",
"repair_steps": [
"Verify intake temp plausibility vs ambient and engine bay heat soak",
"Repair IAT wiring or replace sensor if stuck/out-of-range",
"Re-validate fuel corrections"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "ECU power supply issues",
"repair_steps": [
"Check 5V ref integrity to temp sensors; repair shared ref/grounds",
"Confirm stable readings after repair",
"Clear codes and run warmup"
]
}
],
"P0111": [
{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Mass airflow sensor failure",
"repair_steps": [
"Validate IAT voltage range and responsiveness during airflow changes",
"Replace MAF/IAT assembly if integrated and IAT channel is faulty",
"Relearn and verify correlation to ECT/ambient"]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Inspect IAT wiring for shorts to ground/5V; repair",
"Confirm correct temp curve reading",
"Clear and verify"
]
}
],
"P0115": [
{
"root_cause_category": "Cooling System Problems",
"root_cause": "Coolant temperature sensor failure",
"repair_steps": [
"Ohm-check ECT vs temperature; replace if erratic/stuck",
"Inspect connector/terminal tension and corrosion; repair",
"Verify fan control and warmup behavior normalize"
]
},
{
"root_cause_category": "Cooling System Problems",
"root_cause": "Thermostat failure",
"repair_steps": [
"Assess warm-up rate; replace thermostat if stuck open/closed","Bleed cooling system; recheck warmup curve",
"Confirm ECT stability and code clearance"
]
}
],
"P0116": [
{
"root_cause_category": "Cooling System Problems",
"root_cause": "Thermostat failure",
"repair_steps": [
"Check warm-up slope; replace thermostat if delayed/no opening",
"Bleed system and verify heater/core circulation",
"Re-test ECT signal for expected dynamics"
]
},
{
"root_cause_category": "Cooling System Problems",
"root_cause": "Coolant temperature sensor failure",
"repair_steps": [
"Replace ECT if voltage stuck or noisy; repair connector",
"Confirm correlation with IAT/ambient after cold soak",
"Clear and drive cycle"
]
}
],
"P0120": [{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Throttle position sensor failure",
"repair_steps": [
"Check dual-track TPS correlation; clean throttle body and plate",
"Replace TPS or throttle body assembly if correlation fails",
"Perform throttle relearn and validate idle stability"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Inspect TPS harness for intermittent opens/shorts; repair terminals",
"Scope for dropout under pedal sweeps",
"Clear codes; confirm smooth response"
]
}
],
"P0130": [
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Check O2 B1S1 heater/signal circuits for opens/shorts; repair connectors",
"Verify heater current draw and sensor switching activity",
"Clear codes; confirm closed-loop operation"]
},
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel injector failure",
"repair_steps": [
"Balance test injectors if O2 activity is absent or biased; service/replace",
"Confirm O2 response recovers in closed loop",
"Recheck trims"
]
}
],
"P0131": [
{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Intake manifold leak",
"repair_steps": [
"Smoke-test for vacuum leaks causing lean O2 low voltage",
"Repair hoses/gaskets/PCV, then confirm O2 voltage normalizes",
"Verify positive fuel trims return to nominal"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Inspect O2 heater/signal wiring for high resistance; repair","Validate sensor switching and heater function",
"Clear and test drive"
]
}
],
"P0132": [
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel pressure regulator failure",
"repair_steps": [
"Test for high rail pressure driving rich O2; replace faulty regulator",
"Confirm trims move negative-to-normal post-repair",
"Drive cycle for monitor readiness"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Check for short-to-power on O2 signal; repair harness",
"Verify normal switching around stoich",
"Clear DTC and validate"
]
}
],
"P0171": [{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Mass airflow sensor failure",
"repair_steps": [
"Clean MAF and inspect intake tract; verify MAF g/s vs RPM",
"Replace MAF if out-of-range or noisy; reset adaptations",
"Confirm trims and O2 lean bias resolve"
]
},
{
"root_cause_category": "Engine Mechanical Problems",
"root_cause": "Intake manifold leak",
"repair_steps": [
"Smoke-test intake/PCV; repair leaks at hoses and gaskets",
"Verify fuel trims return to normal at idle and cruise",
"Re-run drive cycle"
]
},
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel pump failure",
"repair_steps": [
"Measure rail pressure; replace weak pump after verifying power/ground",
"Replace filter; confirm trims normalize under load",
"Clear codes and test"
]
},
{"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel filter blockage",
"repair_steps": [
"Replace clogged filter; flush lines if contaminated",
"Re-test pressure and trims; verify O2 activity",
"Drive cycle to confirm"
]
},
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel injector failure",
"repair_steps": [
"Perform injector balance/flow test; clean or replace",
"Verify STFT/LTFT stabilize and misfires resolve",
"Clear and road test"
]
}
],
"P0172": [
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel pressure regulator failure",
"repair_steps": [
"Check for high rail pressure; replace regulator if stuck or bleeding vacuum fuel",
"Verify negative trims return toward zero",
"Re-test O2 rich bias resolution"
]},
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel injector failure",
"repair_steps": [
"Identify leaking/stuck injector via balance/leak-down; replace",
"Change oil if fuel-contaminated; verify trims recover",
"Drive cycle monitors"
]
},
{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Mass airflow sensor failure",
"repair_steps": [
"Inspect MAF for under-reporting airflow; replace if faulty",
"Verify AFR returns to stoichiometric under closed loop",
"Clear DTC and validate"
]
}
],
"P0200": [
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel injector failure",
"repair_steps": [
"Check injector supply voltage, driver control, and coil resistance",
"Repair circuit faults; replace failed injector(s)","Verify cylinder contribution balances"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Locate opens/shorts in injector harness; repair and secure",
"Confirm driver operation with scope/NOID light",
"Clear codes; verify smooth operation"
]
}
],
"P0201": [
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel injector failure",
"repair_steps": [
"Measure injector 1 resistance/current; perform balance test",
"Clean or replace injector; code injector if required",
"Verify cylinder contribution and O2 response"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": ["Repair injector 1 connector/pins and leads",
"Confirm driver pulses with scope",
"Clear and retest"
]
}
],
"P0202": [
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel injector failure",
"repair_steps": [
"Test injector 2 coil/current; clean/replace as needed",
"Verify contribution and trims after repair",
"Clear and road test"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Repair injector 2 wiring/connector",
"Confirm driver control integrity",
"Validate with live data"
]
}
],"P0203": [
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel injector failure",
"repair_steps": [
"Diagnose injector 3 resistance and flow; replace if faulty",
"Ensure balanced fueling post-repair",
"Clear and confirm"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Repair injector 3 circuit faults",
"Verify driver pulses and cylinder contribution",
"Road test"
]
}
],
"P0230": [
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel pump failure",
"repair_steps": [
"Test pump relay/fuse/control; verify voltage at pump",
"Replace pump module if inoperative; renew filter/strainer","Confirm pressure and delivery meet spec"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Repair opens/shorts in primary pump circuit and grounds",
"Voltage drop test under load; correct poor grounds",
"Clear codes; verify start/run"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "ECU power supply issues",
"repair_steps": [
"Check PCM power/ground and pump driver outputs",
"Repair power feeds and relays as needed",
"Validate stable pump control"
]
}
],
"P0300": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Ignition coil failure",
"repair_steps": ["Identify misfiring cylinders via counters; swap-test coils",
"Replace weak coils and associated plugs",
"Verify misfire counters reset"
]
},
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Spark plug failure",
"repair_steps": [
"Inspect plugs for wear/fouling/incorrect gap; replace",
"Check dwell and secondary voltage",
"Confirm smooth idle/load"
]
},
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel injector failure",
"repair_steps": [
"Balance test injectors; clean/replace outliers",
"Confirm trims and misfires resolve",
"Drive cycle verification"
]
}
],
"P0301": [
{
"root_cause_category": "Ignition System Problems","root_cause": "Ignition coil failure",
"repair_steps": [
"Swap coil 1 with another cylinder to see if misfire follows",
"Replace coil and plug if confirmed",
"Verify crank acceleration smooths"
]
},
{
"root_cause_category": "Engine Mechanical Problems",
"root_cause": "Low compression (cylinder)",
"repair_steps": [
"Perform compression/leak-down on cylinder 1",
"Repair valves/rings/head gasket as indicated",
"Re-verify misfire elimination"
]
},
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel injector failure",
"repair_steps": [
"Test injector 1 current and flow; replace if faulty",
"Confirm contribution improves",
"Clear code and test"
]
}
],
"P0302": [{
"root_cause_category": "Ignition System Problems",
"root_cause": "Ignition coil failure",
"repair_steps": [
"Swap-test coil 2; replace if fault follows",
"Install new plug and verify firing",
"Re-check misfire counter"
]
},
{
"root_cause_category": "Engine Mechanical Problems",
"root_cause": "Low compression (cylinder)",
"repair_steps": [
"Compression/leakdown cylinder 2; repair mechanical issues",
"Verify smoothness post-repair",
"Road test"
]
}
],
"P0303": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Ignition coil failure",
"repair_steps": [
"Swap-test coil 3; replace if defective",
"Renew spark plug; verify ignition waveform",
"Confirm misfire resolved"]
}
],
"P0304": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Ignition coil failure",
"repair_steps": [
"Swap-test coil 4 and replace as needed",
"Replace spark plug 4 and inspect boot",
"Re-test for misfires"
]
}
],
"P0325": [
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Inspect knock sensor harness for chafe and poor shielding",
"Repair/replace connector and wiring; torque sensor to spec",
"Verify timing retard responds under knock test"
]
},
{
"root_cause_category": "Ignition System Problems","root_cause": "Spark plug failure",
"repair_steps": [
"Replace incorrect heat range/fouled plugs causing false knock",
"Verify knock counts normalize",
"Road test under load"
]
}
],
"P0335": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Crankshaft position sensor failure",
"repair_steps": [
"Scope CKP signal; check sensor gap/reluctor integrity",
"Replace CKP sensor if no/erratic signal; relearn if required",
"Verify start/run and sync"
]
},
{
"root_cause_category": "Engine Mechanical Problems",
"root_cause": "Valve timing issues",
"repair_steps": [
"Inspect timing chain/belt stretch and alignment; repair",
"Confirm CKP/CMP sync restores",
"Clear codes and validate"
]
},{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Repair CKP circuit opens/shorts and corroded terminals",
"Verify clean waveform and RPM",
"Test drive for stability"
]
}
],
"P0336": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Crankshaft position sensor failure",
"repair_steps": [
"Check frequency stability and signal amplitude",
"Replace sensor if noisy/weak",
"Confirm timing accuracy improves"
]
},
{
"root_cause_category": "Engine Mechanical Problems",
"root_cause": "Valve timing issues",
"repair_steps": [
"Inspect reluctor wheel damage/misalignment and chain/belt",
"Repair/replace components; re-time engine",
"Verify sync and performance"]
}
],
"P0340": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Camshaft position sensor failure",
"repair_steps": [
"Test CMP sensor output; replace if no/intermittent signal",
"Perform relearn if applicable",
"Confirm sync and drivability"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Repair CMP wiring opens/shorts and poor grounds",
"Scope for clean correlation with CKP",
"Clear DTCs; verify operation"
]
},
{
"root_cause_category": "Engine Mechanical Problems",
"root_cause": "Valve timing issues",
"repair_steps": [
"Inspect chain/belt timing and VVT actuator function","Correct timing and replace damaged components",
"Re-verify injection/ignition sync"
]
}
],
"P0341": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Camshaft position sensor failure",
"repair_steps": [
"Check CMP output quality and mounting; replace if weak/noisy",
"Relearn cam/crank if required",
"Verify correlation within spec"
]
},
{
"root_cause_category": "Engine Mechanical Problems",
"root_cause": "Valve timing issues",
"repair_steps": [
"Inspect chain stretch, tensioner, and phaser/VVT accuracy",
"Repair timing and verify correlation",
"Clear and road test"
]
}
],
"P0400": [{
"root_cause_category": "Emissions System Problems",
"root_cause": "EGR valve failure",
"repair_steps": [
"Command EGR and observe MAP/idle effect; clean/replace stuck valve",
"Clean carbon from passages; verify flow change in test",
"Confirm NOx proxy readings trend down"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Repair EGR control/feedback circuits; confirm actuation",
"Re-run EGR flow test",
"Clear codes"
]
}
],
"P0401": [
{
"root_cause_category": "Emissions System Problems",
"root_cause": "EGR valve failure",
"repair_steps": [
"Inspect for insufficient EGR flow; clean or replace valve",
"Decarbonize EGR ports and DPFE/differential pressure lines",
"Verify flow increases on command"]
},
{
"root_cause_category": "Engine Mechanical Problems",
"root_cause": "Intake manifold leak",
"repair_steps": [
"Repair vacuum leaks that reduce EGR effectiveness",
"Confirm expected MAP change during EGR actuation",
"Road test for stability"
]
}
],
"P0410": [
{
"root_cause_category": "Emissions System Problems",
"root_cause": "Catalytic converter failure",
"repair_steps": [
"Check for exhaust backpressure affecting secondary air flow",
"Repair underlying misfires/rich/lean before considering converter",
"Replace converter if substrate blocked and upstream issues resolved"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Inspect air pump fuse/relay/circuits; repair wiring/connectors","Command pump and check valve operation; replace failed components",
"Verify O2 lean response during air injection test"
]
}
],
"P0420": [
{
"root_cause_category": "Emissions System Problems",
"root_cause": "Catalytic converter failure",
"repair_steps": [
"Resolve upstream misfire/fuel/air faults first",
"Check for exhaust leaks; replace failing converter if efficiency remains low",
"Verify downstream O2 behavior differs from upstream"
]
},
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Spark plug failure",
"repair_steps": [
"Replace worn/fouled plugs and any weak coils contributing to misfire",
"Confirm catalyst temperatures/efficiency improve",
"Clear and drive cycle to readiness"
]
},
{
"root_cause_category": "Fuel System Problems",
"root_cause": "Fuel injector failure","repair_steps": [
"Address rich/lean injector faults damaging converter",
"Verify fuel control near stoich",
"Retest catalyst efficiency"
]
}
],
"P0440": [
{
"root_cause_category": "Emissions System Problems",
"root_cause": "EVAP system leak",
"repair_steps": [
"Inspect/replace gas cap; smoke-test EVAP for hose/canister leaks",
"Test purge and vent valves; replace faulty components",
"Verify FTP pressure integrity test passes"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Repair purge/vent/FTP sensor wiring faults",
"Re-run EVAP monitor",
"Confirm no recurrence"
]
}
],"P0441": [
{
"root_cause_category": "Emissions System Problems",
"root_cause": "EVAP system leak",
"repair_steps": [
"Function-test purge valve for stuck/flow error; replace if leaking",
"Check hoses and canister for restriction/leak",
"Verify manifold vacuum influence on purge is correct"
]
}
],
"P0500": [
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Inspect VSS/output speed/wheel speed wiring and connectors",
"Repair harness and confirm speed plausibility across sources",
"Clear codes and road test"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "ECU power supply issues",
"repair_steps": [
"Check sensor power/ground references and module supplies","Repair grounds/fuses/relays; confirm stable readings",
"Re-validate cluster and ABS correlation"
]
}
],
"P0505": [
{
"root_cause_category": "Air Intake System Problems",
"root_cause": "Throttle position sensor failure",
"repair_steps": [
"Clean throttle body; verify TPS/IAC commanded responses",
"Replace TPS/throttle body if needed and perform idle relearn",
"Confirm stable idle RPM"
]
},
{
"root_cause_category": "Engine Mechanical Problems",
"root_cause": "Intake manifold leak",
"repair_steps": [
"Smoke-test for vacuum leaks affecting idle control",
"Repair leaks; verify idle normalization",
"Clear and validate"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage","repair_steps": [
"Repair IAC/TPS wiring/connectors",
"Confirm control authority and RPM stabilization",
"Road test with A/C and load"
]
}
],
"P0700": [
{
"root_cause_category": "Electrical System Problems",
"root_cause": "ECU power supply issues",
"repair_steps": [
"Scan TCM for subcodes; verify power/ground/communication to TCM",
"Repair module supplies and CAN wiring as needed",
"Clear DTCs in PCM and TCM; confirm no return"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Inspect transmission input/output speed sensor wiring",
"Repair harness faults; confirm correlation of speeds",
"Road test for proper shifting"
]
}
],"P0016": [
{
"root_cause_category": "Engine Mechanical Problems",
"root_cause": "Valve timing issues",
"repair_steps": [
"Verify cam/crank correlation; inspect chain/belt, tensioner, and VVT actuator",
"Correct mechanical timing and replace worn parts",
"Confirm sync and drivability restore"
]
},
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Camshaft position sensor failure",
"repair_steps": [
"Scope CMP signal; replace sensor if intermittent",
"Relearn correlation if required",
"Validate stable sync"
]
},
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Crankshaft position sensor failure",
"repair_steps": [
"Check CKP waveform/reluctor; replace if erratic",
"Verify correlation normalizes post-repair",
"Clear and drive cycle"
]}
],
"P0350": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Ignition coil failure",
"repair_steps": [
"Check primary/secondary patterns; replace failing coils",
"Verify dwell control and coil supply voltage",
"Re-test misfire counters"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Repair coil primary wiring faults and grounds",
"Confirm coil current ramp is correct",
"Clear DTC and validate"
]
}
],
"P0351": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Ignition coil failure","repair_steps": [
"Test Coil A primary/secondary; replace coil and plug as needed",
"Confirm cylinder 1 combustion improves",
"Verify knock sensor now detects normal events"
]
},
{
"root_cause_category": "Electrical System Problems",
"root_cause": "Wiring harness damage",
"repair_steps": [
"Repair Coil A circuit wiring/connectors",
"Validate primary voltage/current",
"Road test"
]
}
],
"P0352": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Ignition coil failure",
"repair_steps": [
"Test Coil B; replace coil/plug if faulty",
"Confirm cylinder 2 combustion returns",
"Clear and retest"
]
}
],"P0353": [
{
"root_cause_category": "Ignition System Problems",
"root_cause": "Ignition coil failure",
"repair_steps": [
"Test Coil C; replace as needed",
"Verify cylinder 3 combustion normal",
"Validate no knock anomalies"
]
}
]
}
def sensor_in_expected_range(sensor_name, reading, dtc_spec):
    """Check plausibility with smooth scoring instead of binary in/out."""
    s = dtc_spec.get("sensors", {}).get(sensor_name)
    if not s:
        return None
    mn, mx = s.get("min_reading"), s.get("max_reading")
    if mn is None or mx is None:
        return 0.5
    if mn <= reading <= mx:
        return 1.0
    # Smooth penalty if outside range
    dist = min(abs(reading - mn), abs(reading - mx))
    rng = max(mx - mn, 1e-6)
    return max(0.0, 1.0 - dist / rng)
    print(max(0.0, 1.0 - dist / rng))
def score_dtcs_from_dtc_sensor_match(dtcs, sensor_values, dtc_db):
    """
    Part One: Score DTCs by matching sensor plausibility & points_weight.
    Uses smooth plausibility scoring.
    """
    results = []
    for dtc in dtcs:
        spec = dtc_db.get(dtc)
        if not spec:
            continue
        sw = spec.get("points_weight", 1)
        sensors = spec.get("sensors", {})
        plaus_sum = 0.0
        count = 0
        details = {}
        for sname in sensors:
            if sname in sensor_values:
                val = sensor_values[sname]
                plaus = sensor_in_expected_range(sname, val, spec)
                if plaus is not None:
                    plaus_sum += plaus
                    count += 1
                    details[sname] = {
                        "value": val,
                        "plaus": plaus,
                        "exp_min": sensors[sname]["min_reading"],
                        "exp_max": sensors[sname]["max_reading"],
                    }
        avg_plaus = (plaus_sum / count) if count > 0 else 0.5
        final_score = avg_plaus * sw
        results.append({
            "dtc": dtc,
            "score": final_score,
            "avg_plaus": avg_plaus,
            "points_weight": sw,
            "details": details,
        })
    results.sort(key=lambda x: x["score"], reverse=True)
    return results


def find_sensor_anomalies(sensor_values, historical_matrix=None):
    """
    Detect anomalous sensors. Adds a minimum floor to avoid zeroing out everything.
    """
    anomalies = {}
    if SKLEARN_AVAILABLE and historical_matrix is not None and historical_matrix.shape[0] > 10:
        iso = IsolationForest(contamination=0.05, n_estimators=50, random_state=42)
        iso.fit(historical_matrix)
        latest = historical_matrix[-1].reshape(1, -1)
        scores = iso.decision_function(latest)[0]
        inv = -scores
        for idx, name in enumerate(sensor_values.keys()):
            anomalies[name] = float(inv[idx]) if idx < len(inv) else 0.0
    else:
        vals = np.array(list(sensor_values.values()), dtype=float)
        med = np.median(vals)
        mad = np.median(np.abs(vals - med)) + 1e-6
        for k, v in sensor_values.items():
            anomalies[k] = float(abs(v - med) / mad)
    # Normalize + floor
    maxv = max(anomalies.values()) if anomalies else 1.0
    for k in anomalies:
        anomalies[k] = max(0.05, anomalies[k] / maxv)
    return anomalies

def rank_dtcs_by_sensor_anomaly_correlation(dtcs, sensor_anomalies, dtc_db, sensor_values):
    """
    Reverse correlate DTCs: reward those covering most anomalous sensors.
    """
    dtc_scores = {}
    for dtc in dtcs:
        spec = dtc_db.get(dtc, {})
        sensors = spec.get("sensors", {})
        score = 0.0
        for sname in sensors:
            if sname in sensor_anomalies:
                score += sensor_anomalies[sname] * spec.get("points_weight", 1)
        dtc_scores[dtc] = score
    ranked = sorted(
        [{"dtc": d, "score": s} for d, s in dtc_scores.items()],
        key=lambda x: x["score"], reverse=True
    )
    return ranked


def rank_root_causes_from_dtcs(dtcs, dtc_db, dtc_root_repairs):
    """
    For each dtc produce a ranked list of root causes from dtc_root_cause_repairs
    (This implements Part One Step 2).
    We'll rank by a simple heuristic: appearance order + dtc points_weight.
    """
    root_scores = defaultdict(float)
    for dtc in dtcs:
        spec = dtc_db.get(dtc, {})
        sw = spec.get("points_weight", 1)
        repairs = dtc_root_repairs.get(dtc, [])
        for position, entry in enumerate(repairs):
            # higher position (earlier) gets more points
            root = entry.get("root_cause")
            # score = points_weight * (1 + (len-reverse_position)/len)
            position_score = sw * (1.0 + (len(repairs)-position)/ (len(repairs)+1))
            root_scores[root] += position_score
    ranked = sorted([{"root":k,"score":v} for k,v in root_scores.items()], key=lambda x: x["score"], reverse=True)
    return ranked

def borda_count_fuse(rank_listA, rank_listB):
    """
    Simple Borda count rank aggregation:
    rank_listA and rank_listB are lists of items in order (best->worst) or dicts with 'dtc' keys.
    Return merged ranking (list of item->borda_score).
    """
    # extract keys
    def extract_keys(r):
        if not r: return []
        first = r[0]
        if isinstance(first, dict):
            keyname = 'dtc' if 'dtc' in first else ('root' if 'root' in first else None)
            return [it.get(keyname) for it in r]
        else:
            return list(r)
    A = extract_keys(rank_listA)
    B = extract_keys(rank_listB)
    all_items = list(dict.fromkeys(A + B))
    borda = {}
    for idx, it in enumerate(A):
        borda[it] = borda.get(it, 0) + (len(A) - idx)
    for idx, it in enumerate(B):
        borda[it] = borda.get(it, 0) + (len(B) - idx)
    merged = sorted([{"item":k,"borda":v} for k,v in borda.items()], key=lambda x: x["borda"], reverse=True)
    return merged

def decide_between_answers(answerA, answerB, sensor_values, dtc_db):
    """
    Final Decide* between Answer A and Answer B:
    - If top choices match -> accept
    - Else use a DecisionTree (or simple heuristic) using features:
        feature vector: [topA_score, topB_score, avg_anomaly_of_topA_sensors, avg_anomaly_of_topB_sensors]
    Returns final chosen root cause and explanation trace.
    """
    topA = answerA["root_ranking"][0] if answerA["root_ranking"] else None
    topB = answerB["root_ranking"][0] if answerB["root_ranking"] else None
    explanation = []
    if topA and topB and topA["root"] == topB["root"]:
        explanation.append("Top root cause matches between A and B; accepting consensus.")
        return {"final_root": topA["root"], "method": "consensus", "explanation": explanation}
    # else create small feature vector and run DecisionTreeClassifier or fallback heuristic
    feat = []
    topA_score = answerA["dtc_rankings"][0]["score"] if answerA["dtc_rankings"] else 0.0
    topB_score = answerB["dtc_rankings"][0]["score"] if answerB["dtc_rankings"] else 0.0
    feat = np.array([[topA_score, topB_score]])
    # If sklearn available, build a tiny decision tree using synthetic labels (we don't have ground truth here)
    # Instead: we train a tiny tree on a few synthetic rules so we can use it as deterministic Decide*.
    if SKLEARN_AVAILABLE:
        X = np.array([
            [0.9, 0.2],
            [0.2, 0.9],
            [0.8, 0.8],
            [0.1, 0.1]
        ])
        y = np.array([0, 1, 0, 1])  # 0 -> prefer A, 1 -> prefer B (toy)
        clf = DecisionTreeClassifier(max_depth=3, random_state=42)
        clf.fit(X, y)
        pred = clf.predict(feat)[0]
        method = "DecisionTree"
        explanation.append(f"DecisionTree predicted {'B' if pred==1 else 'A'} using top scores ({topA_score:.2f},{topB_score:.2f}).")
        chosen = topB["root"] if pred==1 and topB else (topA["root"] if topA else None)
        return {"final_root": chosen, "method": method, "explanation": explanation}
    else:
        # heuristic: choose which top has higher relative score
        if topB_score > topA_score * 1.2:
            explanation.append("Heuristic chose B because topB_score > 1.2 * topA_score")
            chosen = topB["root"] if topB else (topA["root"] if topA else None)
            return {"final_root": chosen, "method": "heuristic", "explanation": explanation}
        else:
            explanation.append("Heuristic chose A by default (no strong evidence for B).")
            chosen = topA["root"] if topA else (topB["root"] if topB else None)
            return {"final_root": chosen, "method": "heuristic", "explanation": explanation}

# ---------------------------
# GUI: PySide6 Application
# ---------------------------
class MatPlotCanvas(FigureCanvas):
    def __init__(self, parent=None, width=4, height=3, dpi=100):
        fig = Figure(figsize=(width, height), dpi=dpi, tight_layout=True)
        self.ax = fig.add_subplot(111)
        super().__init__(fig)
        self.setParent(parent)

    def plot_bar(self, names, values, title=""):
        self.ax.clear()
        self.ax.bar(range(len(values)), values)
        self.ax.set_xticks(range(len(values)))
        self.ax.set_xticklabels(names, rotation=45, ha="right")
        self.ax.set_title(title)
        self.draw()

class DiagnosticPortal(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("DiagPortal — Lightweight ML Diagnostic Demo")
        self.resize(1400, 800)  # Increased default size
        font = QFont("Segoe UI", 10)
        self.setFont(font)

        # Main splitter: left (inputs) / right (dashboard)
        main_split = QSplitter(Qt.Horizontal)
        self.setCentralWidget(main_split)

        # Left panel (inputs)
        left = QFrame()
        left_layout = QVBoxLayout()
        left.setLayout(left_layout)

        left_layout.addWidget(QLabel("<b>Inputs</b>"))
        self.dtc_input = QLineEdit()
        self.dtc_input.setPlaceholderText("Enter comma separated DTCs (e.g. P0300,P0230)")
        left_layout.addWidget(self.dtc_input)

        self.sensor_text = QTextEdit()
        self.sensor_text.setPlaceholderText('Enter sensor values as JSON-like lines, e.g.\nengine_rpm:750\nfuel_rail_pressure:5')
        left_layout.addWidget(self.sensor_text)

        load_btn = QPushButton("Load example data (small)")
        load_btn.clicked.connect(self.load_example)
        left_layout.addWidget(load_btn)

        load_json_btn = QPushButton("Load JSON File")
        load_json_btn.clicked.connect(self.load_json_file)
        left_layout.addWidget(load_json_btn)

        run_btn = QPushButton("Run Analysis")
        run_btn.setStyleSheet("font-weight:bold; font-size:14px; padding:8px;")
        run_btn.clicked.connect(self.run_analysis)
        left_layout.addWidget(run_btn)

        left_layout.addSpacing(10)
        left_layout.addWidget(QLabel("<b>Model / Options</b>"))
        self.iso_checkbox = QComboBox()
        self.iso_checkbox.addItems(["IsolationForest (if available)", "z-score fallback"])
        left_layout.addWidget(self.iso_checkbox)

        self.show_logs_btn = QPushButton("Clear Logs")
        self.show_logs_btn.clicked.connect(self.clear_logs)
        left_layout.addWidget(self.show_logs_btn)

        left_layout.addStretch()
        main_split.addWidget(left)

        # Right panel: dashboard
        right_container = QWidget()
        right_layout = QVBoxLayout()
        right_container.setLayout(right_layout)

        # Top: pipeline visual + logs
        top_h = QHBoxLayout()
        pipeline_box = QFrame()
        pipeline_box.setFrameShape(QFrame.StyledPanel)
        pipeline_layout = QVBoxLayout()
        pipeline_box.setLayout(pipeline_layout)
        pipeline_layout.addWidget(QLabel("<b>AI Pipeline Trace</b>"))
        self.pipeline_log = QTextEdit()
        self.pipeline_log.setReadOnly(True)
        self.pipeline_log.setMinimumHeight(160)
        pipeline_layout.addWidget(self.pipeline_log)
        top_h.addWidget(pipeline_box, 2)

        # Right small: final result
        result_box = QFrame()
        result_box.setFrameShape(QFrame.StyledPanel)
        result_layout = QVBoxLayout()
        result_box.setLayout(result_layout)
        result_layout.addWidget(QLabel("<b>Final Decision</b>"))
        self.final_label = QLabel("No run yet")
        self.final_label.setWordWrap(True)
        result_layout.addWidget(self.final_label)
        result_layout.addStretch()
        top_h.addWidget(result_box, 1)

        right_layout.addLayout(top_h)

        # Middle: charts and ranked lists
        mid_h = QHBoxLayout()

        left_mid = QFrame()
        left_mid.setFrameShape(QFrame.StyledPanel)
        lm_layout = QVBoxLayout()
        left_mid.setLayout(lm_layout)
        lm_layout.addWidget(QLabel("<b>Ranked DTCs (Answer A)</b>"))
        self.dtc_table = QTableWidget(0, 3)
        self.dtc_table.setHorizontalHeaderLabels(["DTC", "Score", "PointsWeight"])
        lm_layout.addWidget(self.dtc_table)

        lm_layout.addWidget(QLabel("<b>Ranked Root Causes (Answer A)</b>"))
        self.roots_list = QListWidget()
        lm_layout.addWidget(self.roots_list)

        mid_h.addWidget(left_mid, 1)

        center_mid = QFrame()
        cm_layout = QVBoxLayout()
        center_mid.setLayout(cm_layout)
        cm_layout.addWidget(QLabel("<b>Sensor Anomalies</b>"))
        self.canvas_anom = MatPlotCanvas(self, width=4, height=3)
        cm_layout.addWidget(self.canvas_anom)
        mid_h.addWidget(center_mid, 1)

        right_mid = QFrame()
        rm_layout = QVBoxLayout()
        right_mid.setLayout(rm_layout)
        rm_layout.addWidget(QLabel("<b>Reverse-ranked DTCs (Answer B)</b>"))
        self.rev_list = QListWidget()
        rm_layout.addWidget(self.rev_list)
        rm_layout.addWidget(QLabel("<b>Non-#1 Root Causes (for inspection)</b>"))
        self.non_top_list = QListWidget()
        rm_layout.addWidget(self.non_top_list)
        mid_h.addWidget(right_mid, 1)

        right_layout.addLayout(mid_h)

        # Bottom: repair steps & raw
        bottom_h = QHBoxLayout()
        repair_box = QFrame()
        rb_layout = QVBoxLayout()
        repair_box.setLayout(rb_layout)
        rb_layout.addWidget(QLabel("<b>Repair Steps for selected root cause</b>"))
        self.repair_text = QTextEdit()
        self.repair_text.setReadOnly(True)
        rb_layout.addWidget(self.repair_text)
        bottom_h.addWidget(repair_box, 2)

        raw_box = QFrame()
        raw_layout = QVBoxLayout()
        raw_box.setLayout(raw_layout)
        raw_layout.addWidget(QLabel("<b>Raw DTC / Sensor Input</b>"))
        self.raw_display = QTextEdit()
        self.raw_display.setReadOnly(True)
        raw_layout.addWidget(self.raw_display)
        bottom_h.addWidget(raw_box, 1)

        right_layout.addLayout(bottom_h)

        main_split.addWidget(right_container)
        main_split.setSizes([350, 850])

        # signals
        self.roots_list.itemClicked.connect(self.show_repair_for_root)
        self.non_top_list.itemClicked.connect(self.show_repair_for_root)

        # small state
        self.latest_run = {}

    def log(self, text):
        ts = time.strftime("%H:%M:%S")
        self.pipeline_log.append(f"[{ts}] {text}")

    def clear_logs(self):
        self.pipeline_log.clear()

    def load_example(self):
        # Pre-fill with small sample data. The user will replace this with real input.
        self.dtc_input.setText("P0300,P0230")
        s = "engine_rpm:750\nfuel_rail_pressure:5\ncrankshaft_position:10\nignition_timing:-5\nfuel_pump_voltage:0"
        self.sensor_text.setPlainText(s)
        self.log("Loaded example data (replace with your dataset).")

    def load_json_file(self):
        """Load DTCs and sensor values from a JSON file."""
        file_path, _ = QFileDialog.getOpenFileName(self, "Select JSON File", "", "JSON Files (*.json)")
        if not file_path:
            return  # User canceled the file dialog

        try:
            with open(file_path, "r") as f:
                data = json.load(f)
                dtcs = data.get("dtcs", [])
                sensors = data.get("sensors", {})

                if not isinstance(dtcs, list) or not isinstance(sensors, dict):
                    raise ValueError("Invalid JSON structure. Expected 'dtcs' as a list and 'sensors' as a dictionary.")

                self.dtc_input.setText(",".join(dtcs))
                self.sensor_text.setPlainText("\n".join(f"{k}:{v}" for k, v in sensors.items()))
                self.log(f"Loaded DTCs and sensor values from {file_path}.")
        except Exception as e:
            QMessageBox.critical(self, "Error", f"Failed to load JSON file: {e}")

    def parse_sensor_text(self, txt):
        out = {}
        for ln in txt.strip().splitlines():
            if not ln.strip(): continue
            if ":" in ln:
                k,v = ln.split(":",1)
                try:
                    out[k.strip()] = float(v.strip())
                except:
                    out[k.strip()] = v.strip()
        return out

    def run_analysis(self):
        # parse inputs
        dtcs_raw = [d.strip().upper() for d in self.dtc_input.text().split(",") if d.strip()]
        sensor_values = self.parse_sensor_text(self.sensor_text.toPlainText())
        self.raw_display.setPlainText(f"DTCs: {dtcs_raw}\nSensors: {sensor_values}")
        self.log("Starting analysis pipeline per logic specification.")

        # PART ONE: DTC -> sensor plausibility -> rank dtcs -> rank root causes per DTC -> produce Answer A
        dtc_rankings = score_dtcs_from_dtc_sensor_match(dtcs_raw, sensor_values, obd2_dtc_diagnostic_database)
        self.log(f"Part One: scored {len(dtc_rankings)} DTC(s) by sensor plausibility & points weight.")
        self.dtc_table.setRowCount(0)
        for row in dtc_rankings:
            r = self.dtc_table.rowCount()
            self.dtc_table.insertRow(r)
            self.dtc_table.setItem(r, 0, QTableWidgetItem(row["dtc"]))
            self.dtc_table.setItem(r, 1, QTableWidgetItem(f"{row['score']:.3f}"))
            self.dtc_table.setItem(r, 2, QTableWidgetItem(f"{row['points_weight']}"))
        # rank root causes aggregated from dtcs
        root_rankA = rank_root_causes_from_dtcs([d["dtc"] for d in dtc_rankings], obd2_dtc_diagnostic_database, dtc_root_cause_repairs)
        self.roots_list.clear()
        for r in root_rankA:
            item = QListWidgetItem(f"{r['root']}  (score {r['score']:.2f})")
            item.setData(Qt.UserRole, r)
            self.roots_list.addItem(item)
        answerA = {"dtc_rankings": dtc_rankings, "root_ranking": root_rankA}
        self.log("Part One complete -> Answer A prepared.")
        self.log(f"Answer A DTC rankings: {dtc_rankings}")
        self.log(f"Answer A Root causes: {root_rankA}")

        # PART TWO: sensor anomalies & correlations -> reverse-rank DTCs -> rank root causes -> Answer B
        # For anomalies we would prefer historical data matrix; for demo we pass None (fallback zscore)
        hist = None  # In real deployments, pass historical sensor matrix (2D numpy array)
        sensor_anoms = find_sensor_anomalies(sensor_values, hist)
        self.log(f"Part Two: detected anomalies in sensors: {', '.join([f'{k}:{v:.2f}' for k,v in sensor_anoms.items()])}")
        # plot sensor anomaly bar chart
        names = list(sensor_anoms.keys())
        vals = [sensor_anoms[n] for n in names]
        self.canvas_anom.plot_bar(names[:5], vals[:5], title="Normalized Anomaly Score (higher = more anomalous)")

        rev_dtcs = rank_dtcs_by_sensor_anomaly_correlation(dtcs_raw, sensor_anoms, obd2_dtc_diagnostic_database, sensor_values)
        self.rev_list.clear()
        for r in rev_dtcs:
            item = QListWidgetItem(f"{r['dtc']}  (rev-score {r['score']:.3f})")
            item.setData(Qt.UserRole, r)
            self.rev_list.addItem(item)
        # rank root causes likewise by mapping reverse dtc ranks -> root causes
        # here we convert rev_dtcs to simple list and feed to rank_root_causes_from_dtcs
        rev_dtcs_list = [d['dtc'] for d in rev_dtcs]
        root_rankB = rank_root_causes_from_dtcs(rev_dtcs_list, obd2_dtc_diagnostic_database, dtc_root_cause_repairs)
        # non-top root causes
        self.non_top_list.clear()
        for r in root_rankB[1:6]:  # list next few non-top causes
            item = QListWidgetItem(f"{r['root']}  (score {r['score']:.2f})")
            item.setData(Qt.UserRole, r)
            self.non_top_list.addItem(item)
        answerB = {"dtc_rankings": rev_dtcs, "root_ranking": root_rankB}
        self.log("Part Two complete -> Answer B prepared.")
        self.log(f"Answer B reverse DTC rankings: {rev_dtcs}")
        self.log(f"Answer B Root causes: {root_rankB}")


        # Decide between Answer A and Answer B
        final_decision = decide_between_answers(answerA, answerB, sensor_values, obd2_dtc_diagnostic_database)
        self.latest_run = {"A": answerA, "B": answerB, "final": final_decision, "sensor_anoms": sensor_anoms}
        self.final_label.setText(f"Final Root Cause: <b>{final_decision.get('final_root')}</b>\nMethod: {final_decision.get('method')}\nReason: {'; '.join(final_decision.get('explanation',[]))}")
        self.log(f"Final decision: {final_decision.get('final_root')} via {final_decision.get('method')}")
        # show repair steps for the final root
        final_root = final_decision.get("final_root")
        if final_root:
            # find repair steps by searching dtc_root_cause_repairs
            steps = []
            for dtc in dtcs_raw:
                for entry in dtc_root_cause_repairs.get(dtc, []):
                    if entry.get("root_cause") == final_root:
                        steps = entry.get("repair_steps", [])
                        break
            if steps:
                txt = f"Final root cause ({final_root}) repair steps:\n" + "\n".join([f"- {s}" for s in steps])
            else:
                txt = f"Final root cause ({final_root}) - no repair steps found in dtc_root_cause_repairs."
            self.repair_text.setPlainText(txt)
        else:
            self.repair_text.setPlainText("No final root cause found.")

    def show_repair_for_root(self, item):
        data = item.data(Qt.UserRole)
        root = data.get("root")
        # search dtc_root_cause_repairs for matching root
        repairs = []
        for dtc, entries in dtc_root_cause_repairs.items():
            for e in entries:
                if e.get("root_cause") == root:
                    repairs.extend(e.get("repair_steps", []))
        if repairs:
            txt = f"Root cause: {root}\n\nSuggested repair steps:\n" + "\n".join([f"- {r}" for r in repairs])
        else:
            txt = f"No explicit repair steps found for {root} in dtc_root_cause_repairs."
        self.repair_text.setPlainText(txt)

def main():
    app = QApplication(sys.argv)
    w = DiagnosticPortal()
    w.show()
    sys.exit(app.exec())

if __name__ == "__main__":
    main()
