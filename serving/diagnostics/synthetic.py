"""Reproducible physics-inspired OBD telemetry generation for validation."""
from __future__ import annotations

from datetime import datetime
from pathlib import Path

import numpy as np
import pandas as pd


SCENARIOS = {
    "healthy": "Healthy reference",
    "combustion": "Combustion irregularity",
    "air_path": "Air-path response lag",
    "cooling": "Cooling warm-up anomaly",
    "charging": "Charging-system stress",
    "combined": "Combined multi-system faults",
}


def generate_synthetic_telemetry(
    output_dir: str | Path,
    scenario: str,
    duration_seconds: int = 600,
    sample_rate_hz: float = 5.0,
    seed: int = 42,
) -> Path:
    """Generate a trace using first-order powertrain and thermal dynamics.

    Synthetic files are explicitly marked and are intended for validation and
    presentation only; they are never represented as measured vehicle data.
    """
    if scenario not in SCENARIOS:
        raise ValueError(f"Unsupported synthetic scenario: {scenario}")
    if duration_seconds < 60 or not 1 <= sample_rate_hz <= 20:
        raise ValueError("Duration must be at least 60 seconds and rate 1-20 Hz.")

    rng = np.random.default_rng(seed)
    dt = 1.0 / sample_rate_hz
    samples = int(duration_seconds * sample_rate_hz)
    t = np.arange(samples, dtype=float) * dt

    # Smooth drive-cycle demand with city-speed transients.
    throttle = np.clip(18 + 13 * np.sin(2 * np.pi * t / 47) + 8 * np.sin(2 * np.pi * t / 13), 3, 72)
    throttle += np.where((t % 95 > 20) & (t % 95 < 28), 18, 0)
    throttle = np.clip(throttle, 3, 88)
    speed = np.zeros(samples)
    rpm = np.zeros(samples)
    speed[0], rpm[0] = 0.0, 780.0
    response_tau = 3.5 if scenario in {"air_path", "combined"} else 0.22
    for index in range(1, samples):
        speed_target = max(0.0, (throttle[index] - 7) * 1.25)
        speed[index] = speed[index - 1] + dt * (speed_target - speed[index - 1]) / 4.5
        rpm_target = 760 + throttle[index] * 34 + speed[index] * 1.2
        rpm[index] = rpm[index - 1] + dt * (rpm_target - rpm[index - 1]) / response_tau

    rpm += rng.normal(0, 3.0, samples)
    if scenario in {"combustion", "combined"}:
        firing_disturbance = 95 * np.sin(2 * np.pi * 3.2 * t)
        burst = ((t.astype(int) % 19) < 3).astype(float)
        rpm += firing_disturbance * burst + rng.normal(0, 28, samples) * burst

    ambient = 24.0
    coolant = np.zeros(samples)
    coolant[0] = ambient
    thermal_tau = 10000.0 if scenario in {"cooling", "combined"} else 220.0
    for index in range(1, samples):
        heat_target = 91.0 + 0.025 * throttle[index]
        coolant[index] = coolant[index - 1] + dt * (heat_target - coolant[index - 1]) / thermal_tau
    if scenario in {"cooling", "combined"}:
        coolant += 0.35 * np.sin(2 * np.pi * t / 31)
    else:
        coolant += rng.normal(0, 0.08, samples)

    engine_load = np.clip(throttle * 0.82 + speed * 0.11 + rng.normal(0, 0.7, samples), 2, 100)
    maf = np.clip(rpm * engine_load / 11500 + rng.normal(0, 0.12, samples), 1.2, None)
    map_kpa = np.clip(25 + throttle * 0.78 + rng.normal(0, 0.4, samples), 20, 100)
    trim_bias = 14.5 if scenario == "combined" else 0.8
    ltft = trim_bias + rng.normal(0, 0.45, samples)
    stft = 1.7 * np.sin(2 * np.pi * t / 8) + rng.normal(0, 0.6, samples)
    o2 = np.clip(0.45 + 0.35 * np.sin(2 * np.pi * 1.1 * t), 0.05, 0.9)
    voltage = 11.65 + rng.normal(0, 0.08, samples) if scenario in {"charging", "combined"} else 14.25 + rng.normal(0, 0.025, samples)

    frame = pd.DataFrame({
        "timestamp": pd.Timestamp("2026-01-01T09:00:00Z") + pd.to_timedelta(t, unit="s"),
        "RPM": np.round(rpm, 2),
        "SPEED": np.round(speed, 2),
        "THROTTLE_POS": np.round(throttle, 2),
        "ENGINE_LOAD": np.round(engine_load, 2),
        "COOLANT_TEMP": np.round(coolant, 3),
        "INTAKE_TEMP": np.round(ambient + 2 + 0.015 * t, 3),
        "MAF": np.round(maf, 3),
        "INTAKE_PRESSURE": np.round(map_kpa, 3),
        "SHORT_FUEL_TRIM_1": np.round(stft, 3),
        "LONG_FUEL_TRIM_1": np.round(ltft, 3),
        "O2_B1S1": np.round(o2, 4),
        "ELM_VOLTAGE": np.round(voltage, 4),
        "synthetic_scenario": scenario,
        "synthetic_seed": seed,
    })
    target_dir = Path(output_dir)
    target_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    path = target_dir / f"ignitionai_synthetic_{scenario}_{stamp}.csv"
    frame.to_csv(path, index=False)
    return path
