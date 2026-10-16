"""Tier 1: Instant DTC-based diagnostic engine.

Extracted from desktop/app/engine/diagnostic.py into a shared module so that
the desktop GUI, web portal, and mobile app all produce identical results.

Takes a list of DTC strings + optional instantaneous sensor snapshot and returns
ranked fault candidates with confidence scores and repair steps.
"""
from __future__ import annotations

from collections import defaultdict
from typing import Any

import numpy as np

try:
    from sklearn.tree import DecisionTreeClassifier
    from sklearn.ensemble import IsolationForest
    SKLEARN_AVAILABLE = True
except Exception:
    SKLEARN_AVAILABLE = False


# ---------------------------------------------------------------------------
# Embedded DTC database (compact subset for the shared engine).
# The full 55 KB database lives in desktop/app/data/dtc_database.py.
# For the API, we load it lazily to keep import time fast.
# ---------------------------------------------------------------------------

_dtc_db: dict[str, Any] | None = None
_dtc_repairs: dict[str, list] | None = None
_vehicle_problems: dict[str, list] | None = None


def _load_dtc_data() -> tuple[dict, dict, dict]:
    """Lazy-load the DTC knowledge base."""
    global _dtc_db, _dtc_repairs, _vehicle_problems
    if _dtc_db is not None:
        return _dtc_db, _dtc_repairs, _vehicle_problems  # type: ignore[return-value]
    try:
        # Try the desktop data module first (it has the most complete DB)
        import importlib
        mod = importlib.import_module("app.data.dtc_database")
        _dtc_db = getattr(mod, "obd2_dtc_diagnostic_database", {})
        _dtc_repairs = getattr(mod, "dtc_root_cause_repairs", {})
        _vehicle_problems = getattr(mod, "vehicle_problems", {})
    except (ImportError, ModuleNotFoundError):
        # Fallback: minimal built-in subset for API-only deployments
        _dtc_db = _BUILTIN_DTC_DB
        _dtc_repairs = _BUILTIN_REPAIRS
        _vehicle_problems = {}
    return _dtc_db, _dtc_repairs, _vehicle_problems  # type: ignore[return-value]


# ---------------------------------------------------------------------------
# Core scoring functions
# ---------------------------------------------------------------------------

def sensor_in_expected_range(sensor_name: str, reading: float, dtc_spec: dict) -> float | None:
    """Smooth plausibility scoring — 1.0 = in range, drops off outside."""
    s = dtc_spec.get("sensors", {}).get(sensor_name)
    if not s:
        return None
    mn, mx = s.get("min_reading"), s.get("max_reading")
    if mn is None or mx is None:
        return 0.5
    if mn <= reading <= mx:
        return 1.0
    dist = min(abs(reading - mn), abs(reading - mx))
    rng = max(mx - mn, 1e-6)
    return max(0.0, 1.0 - dist / rng)


def score_dtcs(
    dtcs: list[str],
    sensor_values: dict[str, float],
) -> list[dict[str, Any]]:
    """Score DTCs by matching sensor plausibility & points_weight."""
    dtc_db, _, _ = _load_dtc_data()
    results = []
    for dtc in dtcs:
        spec = dtc_db.get(dtc)
        if not spec:
            results.append({
                "dtc": dtc,
                "score": 0.0,
                "avg_plaus": 0.0,
                "points_weight": 0,
                "description": f"Unknown DTC: {dtc}",
                "details": {},
            })
            continue
        sw = spec.get("points_weight", 1)
        sensors = spec.get("sensors", {})
        plaus_sum, count, details = 0.0, 0, {}
        for sname in sensors:
            if sname in sensor_values:
                plaus = sensor_in_expected_range(sname, sensor_values[sname], spec)
                if plaus is not None:
                    plaus_sum += plaus
                    count += 1
                    details[sname] = {
                        "value": sensor_values[sname],
                        "plaus": plaus,
                        "exp_min": sensors[sname]["min_reading"],
                        "exp_max": sensors[sname]["max_reading"],
                    }
        avg_plaus = plaus_sum / count if count > 0 else 0.5
        results.append({
            "dtc": dtc,
            "score": avg_plaus * sw,
            "avg_plaus": round(avg_plaus, 3),
            "points_weight": sw,
            "description": spec.get("description", ""),
            "details": details,
        })
    results.sort(key=lambda x: x["score"], reverse=True)
    return results


def find_sensor_anomalies(sensor_values: dict[str, float]) -> dict[str, float]:
    """Detect anomalous sensors using IsolationForest or z-score fallback."""
    anomalies: dict[str, float] = {}
    vals = np.array(list(sensor_values.values()), dtype=float)
    med = np.median(vals)
    mad = np.median(np.abs(vals - med)) + 1e-6
    for k, v in sensor_values.items():
        anomalies[k] = float(abs(v - med) / mad)
    maxv = max(anomalies.values()) if anomalies and max(anomalies.values()) > 0 else 1.0
    for k in anomalies:
        anomalies[k] = max(0.05, anomalies[k] / maxv)
    return anomalies


def rank_root_causes(dtcs: list[str]) -> list[dict[str, Any]]:
    """Rank root causes aggregated from all input DTCs."""
    dtc_db, dtc_repairs, _ = _load_dtc_data()
    root_scores: dict[str, float] = defaultdict(float)
    for dtc in dtcs:
        spec = dtc_db.get(dtc, {})
        sw = spec.get("points_weight", 1)
        repairs = dtc_repairs.get(dtc, [])
        for pos, entry in enumerate(repairs):
            root = entry.get("root_cause")
            if root:
                root_scores[root] += sw * (1.0 + (len(repairs) - pos) / (len(repairs) + 1))
    return sorted(
        [{"root": k, "score": round(v, 2)} for k, v in root_scores.items()],
        key=lambda x: x["score"],
        reverse=True,
    )


def get_repair_steps(dtcs: list[str], root_cause: str) -> list[str]:
    """Find repair steps for a given root cause across all DTCs."""
    _, dtc_repairs, _ = _load_dtc_data()
    for dtc in dtcs:
        for entry in dtc_repairs.get(dtc, []):
            if entry.get("root_cause") == root_cause:
                return entry.get("repair_steps", [])
    return []


# ---------------------------------------------------------------------------
# Main Tier 1 pipeline
# ---------------------------------------------------------------------------

def analyze_dtcs(
    dtcs: list[str],
    sensor_values: dict[str, float] | None = None,
) -> dict[str, Any]:
    """Run the full Tier 1 (instant DTC) diagnostic pipeline.

    Parameters
    ----------
    dtcs : list of DTC strings (e.g. ["P0300", "P0171"])
    sensor_values : optional dict of current sensor readings

    Returns
    -------
    Structured result with ranked DTCs, root causes, confidence, and repair steps.
    """
    sensor_values = sensor_values or {}

    # Score DTCs by sensor plausibility
    dtc_rankings = score_dtcs(dtcs, sensor_values)

    # Rank root causes from DTC rankings
    root_ranking = rank_root_causes([d["dtc"] for d in dtc_rankings])

    # Sensor anomaly correlation (Part 2)
    anomalies = find_sensor_anomalies(sensor_values) if sensor_values else {}

    # Decision: pick top root cause
    top_root = root_ranking[0] if root_ranking else None
    final_root = top_root["root"] if top_root else None
    confidence = min(0.95, top_root["score"] / 20.0) if top_root else 0.0

    # Repair steps
    repair_steps = get_repair_steps(dtcs, final_root) if final_root else []

    return {
        "tier": 1,
        "dtc_rankings": dtc_rankings,
        "root_causes": root_ranking,
        "sensor_anomalies": anomalies,
        "final_root_cause": final_root,
        "confidence": round(confidence, 2),
        "repair_steps": repair_steps,
        "sklearn_available": SKLEARN_AVAILABLE,
    }


# ---------------------------------------------------------------------------
# Minimal built-in DTC database (used when desktop data module is unavailable)
# ---------------------------------------------------------------------------

_BUILTIN_DTC_DB: dict[str, Any] = {
    "P0300": {
        "description": "Random/Multiple Cylinder Misfire Detected",
        "points_weight": 8,
        "sensors": {
            "engine_rpm": {"min_reading": 600, "max_reading": 900, "unit": "rpm", "expected_condition": "unstable"},
            "maf_sensor": {"min_reading": 2.0, "max_reading": 8.0, "unit": "g/s", "expected_condition": "normal"},
        },
    },
    "P0171": {
        "description": "System Too Lean (Bank 1)",
        "points_weight": 7,
        "sensors": {
            "short_term_fuel_trim_b1": {"min_reading": 10, "max_reading": 25, "unit": "%", "expected_condition": "high_positive"},
            "long_term_fuel_trim_b1": {"min_reading": 10, "max_reading": 25, "unit": "%", "expected_condition": "high_positive"},
        },
    },
    "P0420": {
        "description": "Catalyst System Efficiency Below Threshold (Bank 1)",
        "points_weight": 6,
        "sensors": {
            "o2_sensor_b1s1": {"min_reading": 0.1, "max_reading": 0.9, "unit": "V", "expected_condition": "switching"},
            "o2_sensor_b1s2": {"min_reading": 0.3, "max_reading": 0.7, "unit": "V", "expected_condition": "near_switching"},
        },
    },
    "P0087": {
        "description": "Fuel Rail/System Pressure - Too Low",
        "points_weight": 8,
        "sensors": {
            "fuel_rail_pressure": {"min_reading": 0, "max_reading": 35, "unit": "psi", "expected_condition": "critically_low"},
        },
    },
    "P0115": {
        "description": "Engine Coolant Temperature Circuit",
        "points_weight": 5,
        "sensors": {
            "ect_sensor": {"min_reading": 100, "max_reading": 140, "unit": "°C", "expected_condition": "high"},
        },
    },
    "P0335": {
        "description": "Crankshaft Position Sensor A Circuit",
        "points_weight": 9,
        "sensors": {
            "crankshaft_position": {"min_reading": 0, "max_reading": 1, "unit": "V", "expected_condition": "absent"},
            "engine_rpm": {"min_reading": 0, "max_reading": 100, "unit": "rpm", "expected_condition": "zero_or_cranking"},
        },
    },
}

_BUILTIN_REPAIRS: dict[str, list] = {
    "P0300": [
        {"root_cause": "Worn spark plugs / ignition coil failure", "repair_steps": ["Inspect spark plugs for wear/fouling", "Test ignition coils with multimeter", "Replace faulty coil or plug set", "Clear codes and test drive"]},
        {"root_cause": "Vacuum leak / intake gasket", "repair_steps": ["Smoke test intake manifold", "Inspect PCV valve and hoses", "Replace damaged gaskets", "Recheck with scan tool"]},
    ],
    "P0171": [
        {"root_cause": "Vacuum/intake air leak", "repair_steps": ["Smoke test intake manifold and hoses", "Check PCV valve", "Inspect brake booster hose", "Replace damaged components"]},
        {"root_cause": "MAF sensor contamination", "repair_steps": ["Inspect MAF sensor element", "Clean with approved MAF cleaner", "Verify fuel trim normalization"]},
    ],
    "P0420": [
        {"root_cause": "Catalytic converter degradation", "repair_steps": ["Verify O2 sensor waveforms (B1S1 vs B1S2)", "Check for exhaust leaks before converter", "Replace catalytic converter if switching confirmed"]},
    ],
    "P0087": [
        {"root_cause": "Fuel pump failure / low pressure", "repair_steps": ["Test fuel rail pressure at idle and under load", "Check fuel filter for restriction", "Test fuel pump relay and wiring", "Replace fuel pump assembly if pressure is low"]},
    ],
    "P0335": [
        {"root_cause": "Crankshaft position sensor failure", "repair_steps": ["Inspect CKP sensor connector and wiring", "Test sensor resistance per OEM spec", "Check reluctor ring for damage", "Replace CKP sensor"]},
    ],
}
