"""Physical and statistical virtual sensors. No LLM calls belong here."""
from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any

import numpy as np
import pandas as pd


@dataclass
class SensorResult:
    name: str
    status: str
    score: float | None
    health_score: float | None
    flagged: bool
    evidence: dict[str, Any]
    candidate: str | None = None

    def as_dict(self) -> dict[str, Any]:
        return asdict(self)


def _interval(frame: pd.DataFrame, fallback: float = 1.0) -> float:
    if pd.api.types.is_datetime64_any_dtype(frame["timestamp"]):
        d = frame.timestamp.diff().dt.total_seconds().median()
        return float(d) if pd.notna(d) and d > 0 else fallback
    return fallback


def _missing(name: str, required: list[str]) -> SensorResult:
    return SensorResult(name, "insufficient_data", None, None, False, {"missing_pids": required})


def combustion_irregularity(frame: pd.DataFrame, cfg: dict[str, Any]) -> SensorResult:
    if "rpm" not in frame or frame.rpm.notna().sum() < cfg["min_samples"]:
        return _missing("combustion", ["rpm"])
    rpm = frame.rpm.interpolate(limit_direction="both")
    n = max(5, round(cfg["window_seconds"] / _interval(frame)))
    detrended = rpm - rpm.rolling(n, center=True, min_periods=2).median()
    spread = detrended.rolling(n, min_periods=max(3, n // 2)).std()
    kurtosis = detrended.rolling(n, min_periods=max(4, n // 2)).kurt()
    spread_limit = spread.mean() + cfg["threshold_k"] * spread.std(ddof=0)
    kurt_limit = kurtosis.mean() + cfg["threshold_k"] * kurtosis.std(ddof=0)
    flags = (spread > spread_limit) | (kurtosis > kurt_limit)
    flag_fraction = float(flags.mean())
    spread_ratio = spread / max(spread_limit, 1e-6)
    robust_severity = float(np.nanpercentile(spread_ratio, 95)) if spread_ratio.notna().any() else 0.0
    score = float(np.clip(100 * (0.7 * flag_fraction / 0.08 + 0.3 * max(0, robust_severity - 1)), 0, 100))
    indices = np.flatnonzero(flags.to_numpy())[:8].tolist()
    # A single transient is not a diagnosis. Require a persistent anomaly
    # population so long recordings do not fail on isolated numerical tails.
    flagged = int(flags.sum()) >= max(3, round(len(frame) * 0.025))
    return SensorResult("combustion", "flagged" if flagged else "normal", score, round(100 - score, 2), flagged, {
        "window_samples": n, "flagged_windows": int(flags.sum()), "flagged_fraction": round(flag_fraction, 4), "rpm_residual_std_threshold": round(float(spread_limit), 3),
        "flag_timestamps": [str(frame.timestamp.iloc[i]) for i in indices]
    }, "misfire_like_combustion_irregularity" if flagged else None)


def fuel_system_health(frame: pd.DataFrame, cfg: dict[str, Any]) -> SensorResult:
    trim_cols = [c for c in ("ltft_b1_pct", "stft_b1_pct") if c in frame]
    if not trim_cols:
        return _missing("fuel", ["ltft_b1_pct or stft_b1_pct"])
    trim = frame[trim_cols].mean(axis=1, skipna=True)
    steady = pd.Series(True, index=frame.index)
    if "rpm" in frame:
        steady &= frame.rpm.rolling(8, min_periods=3).std().fillna(np.inf) < cfg["steady_rpm_std"]
    if "engine_load_pct" in frame:
        steady &= frame.engine_load_pct.rolling(8, min_periods=3).std().fillna(np.inf) < cfg["steady_load_std"]
    usable = trim[steady & trim.notna()]
    if usable.empty:
        return SensorResult("fuel", "insufficient_data", None, None, False, {"reason": "no steady-state trim samples"})
    mean_trim = float(usable.mean())
    exceed = usable.abs() > cfg["ltft_limit_pct"]
    score = float(np.clip(100 - max(0, usable.abs().mean() - 2) * 7 - exceed.mean() * 35, 0, 100))
    direction = "lean" if mean_trim > cfg["ltft_limit_pct"] else "rich" if mean_trim < -cfg["ltft_limit_pct"] else None
    candidate = {"lean": "air_intake_vacuum_leak_or_maf_drift", "rich": "injector_or_fuel_pressure_overfueling"}.get(direction)
    return SensorResult("fuel", "flagged" if direction is not None else "normal", round(100 - score, 2), round(score, 2), direction is not None, {
        "steady_samples": int(usable.size), "mean_trim_pct": round(mean_trim, 3), "threshold_pct": cfg["ltft_limit_pct"],
        "exceedance_fraction": round(float(exceed.mean()), 3), "o2_available": "o2_b1s1_v" in frame
    }, candidate)


def thermal_stress(frame: pd.DataFrame, cfg: dict[str, Any]) -> SensorResult:
    if "coolant_c" not in frame or frame.coolant_c.notna().sum() < cfg["min_samples"]:
        return _missing("thermal", ["coolant_c"])
    coolant = frame.coolant_c.dropna()
    seconds = max(len(coolant) * _interval(frame), 1.0)
    rate = float((coolant.iloc[-1] - coolant.iloc[0]) / seconds * 60)
    warmed = coolant[coolant >= cfg["operating_temp_c"]]
    steady_std = float(warmed.std(ddof=0)) if len(warmed) >= 5 else None
    flagged = rate < cfg["min_warmup_rate_c_per_min"] or rate > cfg["max_warmup_rate_c_per_min"] or (steady_std is not None and steady_std > cfg["max_steady_std_c"])
    penalty = max(0, cfg["min_warmup_rate_c_per_min"] - rate) * 15 + max(0, rate - cfg["max_warmup_rate_c_per_min"]) * 8
    if steady_std is not None: penalty += max(0, steady_std - cfg["max_steady_std_c"]) * 8
    health = float(np.clip(100 - penalty, 0, 100))
    return SensorResult("thermal", "flagged" if flagged else "normal", round(100 - health, 2), round(health, 2), flagged, {
        "warmup_rate_c_per_min": round(rate, 3), "steady_state_std_c": round(steady_std, 3) if steady_std is not None else None,
        "operating_temperature_reached": not warmed.empty
    }, "cooling_system_warmup_or_stability_anomaly" if flagged else None)


def load_response_lag(frame: pd.DataFrame, cfg: dict[str, Any]) -> SensorResult:
    response = "rpm" if "rpm" in frame else "map_kpa" if "map_kpa" in frame else None
    if "throttle_pct" not in frame or response is None or len(frame) < cfg["min_samples"]:
        return _missing("load_response", ["throttle_pct", "rpm or map_kpa"])
    a = frame.throttle_pct.interpolate(limit_direction="both").to_numpy(); b = frame[response].interpolate(limit_direction="both").to_numpy()
    a = a - a.mean(); b = b - b.mean(); interval = _interval(frame)
    # Only physically plausible positive lags are searched. This stays linear in
    # the telemetry length, unlike a full O(n²) correlation on long trip logs.
    max_shift = max(1, round(cfg["max_search_seconds"] / interval))
    correlations = [float(np.dot(a[:len(a) - shift], b[shift:])) for shift in range(max_shift + 1)]
    lag_samples = int(np.argmax(correlations)); lag_seconds = lag_samples * interval
    health = float(np.clip(100 - max(0, lag_seconds - cfg["max_lag_seconds"]) * 35, 0, 100))
    flagged = lag_seconds > cfg["max_lag_seconds"]
    return SensorResult("load_response", "flagged" if flagged else "normal", round(100 - health, 2), round(health, 2), flagged, {
        "response_signal": response, "estimated_lag_seconds": round(lag_seconds, 3), "max_lag_seconds": cfg["max_lag_seconds"]
    }, "air_path_or_throttle_response_lag" if flagged else None)


def charging_stress(frame: pd.DataFrame, cfg: dict[str, Any]) -> SensorResult:
    if "voltage_v" not in frame or frame.voltage_v.notna().sum() < cfg["min_samples"]:
        return _missing("charging", ["voltage_v"])
    voltage = frame.voltage_v.dropna(); ripple = float(voltage.diff().abs().median()); mean = float(voltage.mean())
    penalty = max(0, cfg["low_voltage_v"] - mean) * 40 + max(0, mean - cfg["high_voltage_v"]) * 25 + max(0, ripple - cfg["max_ripple_v"]) * 60
    health = float(np.clip(100 - penalty, 0, 100)); flagged = health < 75
    return SensorResult("charging", "flagged" if flagged else "normal", round(100 - health, 2), round(health, 2), flagged, {
        "mean_voltage_v": round(mean, 3), "median_step_ripple_v": round(ripple, 4), "acceptable_range_v": [cfg["low_voltage_v"], cfg["high_voltage_v"]]
    }, "charging_system_voltage_stress" if flagged else None)
