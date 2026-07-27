"""
IgnitionAI — Virtual OBD Simulator
Built-in simulator that generates realistic OBD-II scan data for demo/dev use.
"""

import random
import time
from typing import Dict, List, Tuple


# ─── Predefined Simulation Scenarios ──────────────────────────────────────────
SIMULATION_SCENARIOS = {
    "🔥 Random Misfire Engine": {
        "dtcs": ["P0300", "P0301"],
        "description": "Multiple cylinder misfires with poor ignition",
        "sensors": {
            "engine_rpm": (650, 820),
            "crankshaft_position": (5, 25),
            "camshaft_position": (690, 720),
            "ignition_timing": (-15, -5),
            "fuel_injector_pulse": (3.5, 9.0),
            "maf_sensor": (2.0, 6.0),
            "o2_sensor_b1s1": (0.1, 0.85),
            "short_term_fuel_trim_b1": (-8.0, 8.0),
        },
    },
    "⛽ Fuel Starvation (Lean)": {
        "dtcs": ["P0087", "P0171"],
        "description": "Low fuel pressure causing lean condition",
        "sensors": {
            "fuel_rail_pressure": (10, 28),
            "fuel_pump_current": (2.2, 3.5),
            "fuel_injector_pulse_width": (8.0, 14.0),
            "o2_sensor_b1s1": (0.1, 0.3),
            "short_term_fuel_trim_b1": (16.0, 23.0),
            "long_term_fuel_trim_b1": (12.0, 22.0),
            "maf_sensor": (3.5, 18.0),
        },
    },
    "🌡️ Engine Overheating": {
        "dtcs": ["P0115", "P0116"],
        "description": "Cooling system fault / stuck thermostat",
        "sensors": {
            "ect_sensor": (115, 135),
            "iat_sensor": (35, 55),
            "thermostat_position": (0, 30),
            "cooling_fan_duty": (90, 100),
            "engine_rpm": (780, 950),
            "maf_sensor": (4.0, 12.0),
        },
    },
    "💡 Catalytic Converter Fail": {
        "dtcs": ["P0420"],
        "description": "Catalyst efficiency below threshold Bank 1",
        "sensors": {
            "o2_sensor_b1s1": (0.1, 0.9),
            "o2_sensor_b1s2": (0.41, 0.59),
            "catalyst_temperature": (310, 780),
            "catalyst_efficiency": (15, 55),
        },
    },
    "🔌 Crankshaft Sensor Fault": {
        "dtcs": ["P0335", "P0336"],
        "description": "CKP signal lost — potential no-start condition",
        "sensors": {
            "crankshaft_position": (0.0, 0.5),
            "engine_rpm": (0, 80),
            "ignition_timing": (0, 2),
            "fuel_injection_timing": (0, 0),
            "engine_load": (0, 15),
        },
    },
    "🚗 Throttle Body Malfunction": {
        "dtcs": ["P0120", "P0505"],
        "description": "TPS circuit fault causing idle instability",
        "sensors": {
            "throttle_position_1": (0.1, 2.8),
            "throttle_position_2": (2.5, 4.9),
            "accelerator_pedal_pos": (0, 100),
            "idle_air_control": (60, 100),
            "idle_air_control_position": (70, 100),
            "engine_rpm_idle": (420, 1180),
            "manifold_vacuum_idle": (35, 68),
        },
    },
}


def simulate_scan(scenario_name: str) -> Tuple[List[str], Dict[str, float], str]:
    """
    Generate a simulated OBD scan for a given scenario.
    Returns: (dtcs, sensor_values, description)
    """
    scenario = SIMULATION_SCENARIOS.get(scenario_name)
    if not scenario:
        return [], {}, "Unknown scenario"

    dtcs = scenario["dtcs"].copy()
    description = scenario["description"]
    sensor_values: Dict[str, float] = {}

    for sensor_name, (low, high) in scenario["sensors"].items():
        # Add realistic noise
        base = random.uniform(low, high)
        noise = random.gauss(0, (high - low) * 0.03)
        value = round(base + noise, 3)
        sensor_values[sensor_name] = value

    return dtcs, sensor_values, description


def generate_live_pid_stream(scenario_name: str, num_frames: int = 10):
    """
    Generator that yields multiple sensor frames simulating live OBD PID streaming.
    Each frame is a dict of sensor_name -> value.
    """
    scenario = SIMULATION_SCENARIOS.get(scenario_name)
    if not scenario:
        return

    for _ in range(num_frames):
        frame = {}
        for sensor_name, (low, high) in scenario["sensors"].items():
            base = random.uniform(low, high)
            noise = random.gauss(0, (high - low) * 0.04)
            frame[sensor_name] = round(base + noise, 3)
        yield frame
        time.sleep(0.05)  # Simulate 50ms polling interval


def get_scenario_names() -> List[str]:
    return list(SIMULATION_SCENARIOS.keys())
