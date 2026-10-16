"""
IgnitionAI: Real EngineFault Diagnostic Service (XGBoost + Hybrid Physics)
Exposes inference service for the mobile REST API endpoint.
"""
from __future__ import annotations

import re
from pathlib import Path
from typing import Dict, Any, Tuple
import joblib
import numpy as np
import pandas as pd

# The 14 features in the exact training sequence expected by the XGBoost model
ORDERED_FEATURES = [
    'MAP', 'TPS', 'Force', 'Power', 'RPM',
    'Consumption L/H', 'Consumption L/100KM', 'Speed',
    'CO', 'HC', 'CO2', 'O2', 'Lambda', 'AFR'
]

# Physical baseline medians from Class 0 (Normal) for missing input defaults
BASELINE_DEFAULTS = {
    'MAP': 1.471,
    'TPS': 1.013,
    'Force': 99.61,
    'Power': 2.25,
    'RPM': 1871.5,
    'Consumption L/H': 3.889,
    'Consumption L/100KM': 9.218,
    'Speed': 40.35,
    'CO': 2.04,
    'HC': 196.94,
    'CO2': 13.38,
    'O2': 0.535,
    'Lambda': 0.957,
    'AFR': 14.075
}

CLASS_METADATA = {
    0: {
        "name": "No Fault (Normal Baseline)",
        "severity": "NOMINAL",
        "action": "Vehicle nominal. All combustion and emissions parameters within factory tolerance."
    },
    1: {
        "name": "Rich Mixture",
        "severity": "WARNING",
        "action": "Inspect fuel injectors for leakage; check fuel pressure regulator & air filter."
    },
    2: {
        "name": "Lean Mixture",
        "severity": "WARNING",
        "action": "Check intake vacuum lines (DTC P0171); verify fuel pump pressure and clean MAF/MAP sensor."
    },
    3: {
        "name": "Low Voltage (Electrical / Ignition)",
        "severity": "CRITICAL",
        "action": "Measure ignition coil primary/secondary resistance; test alternator charging voltage (>13.8V) and spark plug gap."
    }
}

def _normalize_key(key: str) -> str:
    """Normalize any string by stripping all non-alphanumeric chars and lowercasing."""
    return re.sub(r'[^a-zA-Z0-9]', '', str(key)).lower()

CANONICAL_ALIASES: Dict[str, str] = {
    _normalize_key('MAP'): 'MAP',
    _normalize_key('TPS'): 'TPS',
    _normalize_key('Force'): 'Force',
    _normalize_key('Power'): 'Power',
    _normalize_key('RPM'): 'RPM',
    _normalize_key('Consumption L/H'): 'Consumption L/H',
    _normalize_key('Consumption L/100KM'): 'Consumption L/100KM',
    _normalize_key('Speed'): 'Speed',
    _normalize_key('CO'): 'CO',
    _normalize_key('HC'): 'HC',
    _normalize_key('CO2'): 'CO2',
    _normalize_key('O2'): 'O2',
    _normalize_key('Lambda'): 'Lambda',
    _normalize_key('AFR'): 'AFR',
    # Common mobile telemetry aliases
    'mapkpa': 'MAP',
    'mapbar': 'MAP',
    'throttle': 'TPS',
    'throttleposition': 'TPS',
    'throttlepct': 'TPS',
    'torque': 'Force',
    'enginepower': 'Power',
    'enginerpm': 'RPM',
    'consumptionlh': 'Consumption L/H',
    'consumptionlperhour': 'Consumption L/H',
    'fuelflowlh': 'Consumption L/H',
    'consumptionl100km': 'Consumption L/100KM',
    'consumptionlper100km': 'Consumption L/100KM',
    'fueleconomy': 'Consumption L/100KM',
    'vehiclespeed': 'Speed',
    'speedkmh': 'Speed',
    'carbonmonoxide': 'CO',
    'hydrocarbons': 'HC',
    'carbondioxide': 'CO2',
    'oxygen': 'O2',
    'lambdaval': 'Lambda',
    'lambdavalue': 'Lambda',
    'airfuelratio': 'AFR',
    'airfuel': 'AFR',
}

_MODEL = None

def get_model():
    """Singleton cached loader for the best trained Hybrid XGBoost model."""
    global _MODEL
    if _MODEL is None:
        primary_path = Path(__file__).resolve().parent / "models" / "best_hybrid_enginefault_model.joblib"
        fallback_path = Path(r"D:\IgnitionAI\serving\diagnostics\models\best_hybrid_enginefault_model.joblib")
        
        target = primary_path if primary_path.exists() else fallback_path
        if not target.exists():
            raise FileNotFoundError(f"EngineFault model file not found at {primary_path} or {fallback_path}")
        _MODEL = joblib.load(target)
    return _MODEL

def extract_features(raw_input: Dict[str, Any]) -> Dict[str, float]:
    """
    Extracts the 14 ordered features from any mobile request dictionary.
    Handles case variations, snake_case, spaces, slashes, and defaults missing values.
    """
    normalized_input: Dict[str, float] = {}
    for k, v in raw_input.items():
        if v is None:
            continue
        try:
            val_float = float(v)
            norm_k = _normalize_key(k)
            if norm_k in CANONICAL_ALIASES:
                canonical = CANONICAL_ALIASES[norm_k]
                normalized_input[canonical] = val_float
        except (ValueError, TypeError):
            continue

    # Fill in exact ordered features with baseline defaults if omitted
    final_features: Dict[str, float] = {}
    for feat in ORDERED_FEATURES:
        if feat in normalized_input:
            final_features[feat] = normalized_input[feat]
        else:
            final_features[feat] = BASELINE_DEFAULTS[feat]

    return final_features

def predict_enginefault(raw_input: Dict[str, Any]) -> Dict[str, Any]:
    """
    Executes XGBoost inference with Hybrid Physics Consensus for mobile calls.
    Returns:
      - predicted_fault_id (int: 0, 1, 2, 3)
      - predicted_fault_name (str)
      - severity (NOMINAL, WARNING, CRITICAL)
      - confidence (float)
      - class_probabilities (dict)
      - class_probabilities_named (dict)
      - detection_source (str)
      - workshop_action (str)
      - input_features (dict of 14 features)
    """
    model = get_model()
    features = extract_features(raw_input)
    df = pd.DataFrame([features])[ORDERED_FEATURES]

    probs = model.predict_proba(df)[0]
    raw_pred = int(model.predict(df)[0])

    p0, p1, p2, p3 = float(probs[0]), float(probs[1]), float(probs[2]), float(probs[3])

    # Tier 1 & 2 Hybrid Decision Fusion
    if raw_pred == 0:
        final_pred = 0
        conf = p0
        detection_source = "ML Baseline (Normal Factory Operating Envelope)"
    elif raw_pred == 1:
        final_pred = 1
        conf = p1
        if features['CO'] > 1.8 or features['Lambda'] < 0.95:
            detection_source = "Hybrid Consensus (ML + Exhaust CO Invariant Confirmed)"
        else:
            detection_source = "ML Pattern Match (Rich Fuel Invariant)"
    else:
        # Fault class 2 vs 3 disambiguation
        if p2 >= p3:
            final_pred = 2
            conf = p2
            detection_source = "Hybrid Anomaly Match (Primary: Lean Mixture | Secondary: Ignition Coil)"
        else:
            final_pred = 3
            conf = p3
            detection_source = "Hybrid Anomaly Match (Primary: Low Voltage | Secondary: Lean Mixture)"

    meta = CLASS_METADATA.get(final_pred, CLASS_METADATA[0])

    return {
        "status": "success",
        "predicted_fault_id": final_pred,
        "predicted_fault_name": meta["name"],
        "severity": meta["severity"],
        "confidence": round(float(conf), 4),
        "class_probabilities": {
            "0": round(p0, 4),
            "1": round(p1, 4),
            "2": round(p2, 4),
            "3": round(p3, 4)
        },
        "class_probabilities_named": {
            "Normal": round(p0, 4),
            "Rich Mixture": round(p1, 4),
            "Lean Mixture": round(p2, 4),
            "Low Voltage": round(p3, 4)
        },
        "detection_source": detection_source,
        "workshop_action": meta["action"],
        "input_features": features
    }
