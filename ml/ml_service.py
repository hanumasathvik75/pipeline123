"""
IgnitionAI - ML Diagnostic FastAPI Service
Exposes Real Hybrid ML Model (XGBoost + Physics Invariants)
Trained on 55,999 Real EngineFaultDB samples.

Port: 8000
Endpoints:
  GET  /health
  POST /api/diagnostics/enginefault-predict
  POST /api/diagnostics/evaluate-session
"""

import os
import io
import json
import logging
from pathlib import Path
from typing import Dict, Any, List, Optional

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# Setup logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("IgnitionAI-MLService")

app = FastAPI(
    title="IgnitionAI Real Hybrid Diagnostic ML Service",
    version="2.0.0",
    description="Physical Invariants + XGBoost 4-Class Diagnostic Microservice"
)

# Enable CORS for Android / Spring Boot access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Model paths
BASE_DIR = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIR / "models" / "best_hybrid_enginefault_model.joblib"

CLASS_LABELS = {
    0: "No Fault (Normal)",
    1: "Rich Mixture",
    2: "Lean Mixture",
    3: "Low Voltage"
}

FEATURE_COLUMNS = [
    'MAP', 'TPS', 'Force', 'Power', 'RPM',
    'Consumption L/H', 'Consumption L/100KM', 'Speed',
    'CO', 'HC', 'CO2', 'O2', 'Lambda', 'AFR'
]

# Global model instance
model = None

@app.on_event("startup")
def load_diagnostic_model():
    global model
    if not MODEL_PATH.exists():
        logger.error(f"Model file not found at {MODEL_PATH}")
        raise FileNotFoundError(f"Missing hybrid model: {MODEL_PATH}")
    logger.info(f"Loading Hybrid Model from {MODEL_PATH}...")
    model = joblib.load(MODEL_PATH)
    logger.info("Hybrid EngineFault XGBoost model loaded successfully.")

# --------------------------------------------------------------------------------------------------
# Request / Response Schemas
# --------------------------------------------------------------------------------------------------

class EngineTelemetryInput(BaseModel):
    MAP: Optional[float] = Field(None, description="Manifold Absolute Pressure (kPa)")
    TPS: Optional[float] = Field(None, description="Throttle Position (%)")
    RPM: Optional[float] = Field(None, description="Engine Speed (rpm)")
    Speed: Optional[float] = Field(None, description="Vehicle Speed (km/h)")
    Coolant: Optional[float] = Field(None, description="Engine Coolant Temp (°C)")
    Load: Optional[float] = Field(None, description="Calculated Engine Load (%)")
    
    # Derived / Direct Emissions & Physics parameters
    Force: Optional[float] = Field(None, description="Tractive Force (N)")
    Power: Optional[float] = Field(None, description="Engine Power (kW)")
    Consumption_LH: Optional[float] = Field(None, alias="Consumption L/H", description="Fuel Rate (L/h)")
    Consumption_L100KM: Optional[float] = Field(None, alias="Consumption L/100KM", description="L/100km")
    CO: Optional[float] = Field(None, description="Carbon Monoxide (%)")
    HC: Optional[float] = Field(None, description="Hydrocarbons (ppm)")
    CO2: Optional[float] = Field(None, description="Carbon Dioxide (%)")
    O2: Optional[float] = Field(None, description="Oxygen Sensor Voltage (V)")
    Lambda: Optional[float] = Field(None, description="Air-Fuel Equivalence Ratio")
    AFR: Optional[float] = Field(None, description="Air Fuel Ratio")

    class Config:
        populate_by_name = True

class SinglePredictionResponse(BaseModel):
    fault_code: int
    fault_name: str
    confidence: float
    probabilities: Dict[str, float]
    detection_source: str
    recommended_action: str
    features_used: Dict[str, float]

class SessionEvaluationRequest(BaseModel):
    session_id: Optional[str] = "session-unknown"
    vin: Optional[str] = None
    records: Optional[List[Dict[str, Any]]] = None
    session_records: Optional[List[Dict[str, Any]]] = None
    raw_jsonl: Optional[str] = None
    file_path: Optional[str] = None

    class Config:
        populate_by_name = True

class SessionEvaluationResponse(BaseModel):
    session_id: str
    vhi_score: int
    health_status: str
    dominant_fault: str
    confidence: float
    frames_evaluated: int
    clean_frames: int
    anomaly_frames: int
    fault_breakdown: Dict[str, Dict[str, Any]]
    telemetry_summary: Dict[str, Any]
    workshop_inspection_plan: List[str]
    status: str

# --------------------------------------------------------------------------------------------------
# Physics Feature Engineering Helper
# --------------------------------------------------------------------------------------------------

def synthesize_missing_features(row: Dict[str, Any]) -> Dict[str, float]:
    """
    Fills in missing SAE J1979 physical derivatives using deterministic automotive formulas
    so any standard OBD stream can be evaluated by the 14-feature Real Hybrid model.
    Supports both lowercase and uppercase keys seamlessly.
    """
    # Normalize input dictionary keys to uppercase for uniform access
    norm = {str(k).upper(): v for k, v in row.items() if v is not None}

    rpm = float(norm.get('RPM') if norm.get('RPM') is not None else 1500.0)
    speed = float(norm.get('SPEED') if norm.get('SPEED') is not None else 40.0)
    tps = float(norm.get('TPS') if norm.get('TPS') is not None else (norm.get('LOAD') if norm.get('LOAD') is not None else 25.0))
    load = float(norm.get('LOAD') if norm.get('LOAD') is not None else tps)
    
    # MAP (kPa) estimation from load if not measured directly
    map_val = float(norm.get('MAP') if norm.get('MAP') is not None else (load * 0.85 + 20.0))
    
    # Vehicle parameters (Standard 1.4L - 1.6L C-segment passenger car, m=1250 kg)
    # Power (kW) approx = Torque * RPM / 9549, Torque ~ Load * Displacement * 0.8
    power = float(norm.get('POWER') if norm.get('POWER') is not None else max(1.0, (load / 100.0) * (rpm / 6000.0) * 85.0))
    
    # Tractive Force (N) = Power (W) / max(v_ms, 0.5)
    v_ms = max(speed / 3.6, 1.0)
    force = float(norm.get('FORCE') if norm.get('FORCE') is not None else min(4500.0, (power * 1000.0) / v_ms))
    
    # Air-Fuel Ratio & Lambda
    lambda_val = float(norm.get('LAMBDA') if norm.get('LAMBDA') is not None else 1.0)
    afr = float(norm.get('AFR') if norm.get('AFR') is not None else (lambda_val * 14.7))
    o2 = float(norm.get('O2') if norm.get('O2') is not None else (0.45 if abs(lambda_val - 1.0) < 0.03 else (0.85 if lambda_val < 0.98 else 0.15)))

    # Fuel Consumption L/h: Standard Speed-Density Air Mass Calculation
    # MAF = (MAP_kPa * 1000 * RPM * Vd * VolEff) / (120 * R_air * T) in kg/s, converted to g/s (* 1000)
    # Air gas constant R_air = 287.05 J/(kg*K), T = 298.15K, Vd = 1.5e-3 m^3, VolEff = 0.82
    if norm.get('MAF') is not None:
        maf_gps = float(norm.get('MAF'))
    else:
        maf_gps = (map_val * rpm * 1.5 * 0.82) / (120.0 * 287.05 * 298.15) * 1000.0
    maf_gps = max(0.0, min(250.0, maf_gps))

    # Fuel mass flow (g/s) = MAF / actual AFR
    # L/h = (Fuel_mass_flow / 745 g/L gasoline density) * 3600
    fuel_mass_gps = maf_gps / max(10.0, afr)
    cons_lh = float(norm.get('CONSUMPTION L/H') or norm.get('CONSUMPTION_LH') or max(0.1, min(45.0, (fuel_mass_gps / 745.0) * 3600.0)))
    
    # Distance-based consumption (L/100km): 0.0 below 3 km/h
    if speed < 3.0:
        cons_l100km = 0.0
    else:
        cons_l100km = float(norm.get('CONSUMPTION L/100KM') or norm.get('CONSUMPTION_L100KM') or min(50.0, (cons_lh / speed) * 100.0))
    
    # Exhaust Emissions Invariants
    if lambda_val < 0.96:
        co = float(norm.get('CO') if norm.get('CO') is not None else 2.2) # Rich: elevated CO
        hc = float(norm.get('HC') if norm.get('HC') is not None else 320.0)
        co2 = float(norm.get('CO2') if norm.get('CO2') is not None else 12.2)
    elif lambda_val > 1.04:
        co = float(norm.get('CO') if norm.get('CO') is not None else 0.15)
        hc = float(norm.get('HC') if norm.get('HC') is not None else 280.0) # Lean misfire: unburned HC
        co2 = float(norm.get('CO2') if norm.get('CO2') is not None else 11.5)
    else:
        co = float(norm.get('CO') if norm.get('CO') is not None else 0.25)
        hc = float(norm.get('HC') if norm.get('HC') is not None else 45.0)
        co2 = float(norm.get('CO2') if norm.get('CO2') is not None else 14.8)
        
    return {
        'MAP': round(map_val, 2),
        'TPS': round(tps, 2),
        'Force': round(force, 2),
        'Power': round(power, 2),
        'RPM': round(rpm, 1),
        'Consumption L/H': round(cons_lh, 2),
        'Consumption L/100KM': round(cons_l100km, 2),
        'Speed': round(speed, 1),
        'CO': round(co, 2),
        'HC': round(hc, 1),
        'CO2': round(co2, 2),
        'O2': round(o2, 2),
        'Lambda': round(lambda_val, 2),
        'AFR': round(afr, 2)
    }

def run_hybrid_inference(features_dict: Dict[str, float]):
    """
    Executes the trained XGBoost model and applies Hybrid Physics Invariant Rules.
    """
    df_row = pd.DataFrame([features_dict])[FEATURE_COLUMNS]
    probs = model.predict_proba(df_row)[0]
    raw_pred = int(np.argmax(probs))
    max_p = float(probs[raw_pred])
    
    # Apply Physics Decision Fusion
    if raw_pred == 0:
        final_pred = 0
        conf = max_p
        source = "ML Baseline (Normal Factory Operating Envelope)"
        action = "Vehicle nominal. All combustion, fuel trims, and emissions within standard factory tolerance."
    elif raw_pred == 1:
        final_pred = 1
        conf = max_p
        if features_dict['CO'] > 1.8 or features_dict['Lambda'] < 0.95:
            source = "Hybrid Consensus (ML + Exhaust CO Invariant Confirmed)"
        else:
            source = "ML Pattern Match (Rich Fuel Invariant)"
        action = "Inspect fuel injectors for leakage; inspect fuel pressure regulator, MAF sensor fouling, and air intake restriction."
    else:
        # Tier 2: Anomaly Confirmed (Class 2: Lean Mixture vs Class 3: Low Voltage/Ignition)
        if probs[2] >= probs[3]:
            final_pred = 2
            conf = float(probs[2])
            source = "Hybrid Anomaly Match (Primary: Lean Mixture | Secondary: Ignition Coil)"
            action = "Check intake manifold vacuum leaks (DTC P0171); verify fuel pump pressure and fuel filter. Inspect ignition coil spark strength."
        else:
            final_pred = 3
            conf = float(probs[3])
            source = "Hybrid Anomaly Match (Primary: Low Voltage | Secondary: Lean Mixture)"
            action = "Measure ignition coil primary/secondary winding resistance; test battery cranking voltage and alternator charging output."

    prob_map = {CLASS_LABELS[i]: round(float(probs[i]), 4) for i in range(len(CLASS_LABELS))}
    return final_pred, conf, prob_map, source, action

# --------------------------------------------------------------------------------------------------
# Endpoints
# --------------------------------------------------------------------------------------------------

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "IgnitionAI Real Hybrid ML Diagnostics",
        "model_loaded": model is not None,
        "classes": list(CLASS_LABELS.values()),
        "features": FEATURE_COLUMNS
    }

@app.post("/api/diagnostics/enginefault-predict", response_model=SinglePredictionResponse)
def predict_single_frame(telemetry: EngineTelemetryInput):
    if model is None:
        raise HTTPException(status_code=503, detail="Model is not loaded")
    
    raw_dict = telemetry.dict(by_alias=True)
    features = synthesize_missing_features(raw_dict)
    
    pred_code, conf, prob_map, source, action = run_hybrid_inference(features)
    
    return SinglePredictionResponse(
        fault_code=pred_code,
        fault_name=CLASS_LABELS[pred_code],
        confidence=round(conf, 4),
        probabilities=prob_map,
        detection_source=source,
        recommended_action=action,
        features_used={k: round(v, 2) for k, v in features.items()}
    )

@app.post("/api/diagnostics/evaluate-session", response_model=SessionEvaluationResponse)
def evaluate_drive_session(req: SessionEvaluationRequest):
    if model is None:
        raise HTTPException(status_code=503, detail="Model is not loaded")

    session_id = req.session_id or "session-unknown"
    records = []

    # 1. Parse from direct records list or session_records
    if req.records:
        records = req.records
    elif req.session_records:
        records = req.session_records
    # 2. Parse from raw JSONL string
    elif req.raw_jsonl:
        for line in req.raw_jsonl.strip().split("\n"):
            line = line.strip()
            if line:
                try:
                    records.append(json.loads(line))
                except Exception:
                    pass
    # 3. Read from file path if specified
    elif req.file_path and os.path.exists(req.file_path):
        with open(req.file_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line:
                    try:
                        records.append(json.loads(line))
                    except Exception:
                        pass

    if not records:
        raise HTTPException(status_code=400, detail="No session records provided or found in payload.")

    # ----------------------------------------------------------------------------------------------
    # Frame Assembly (Supports both Rahul's OBD_OBSERVATION events & Direct Telemetry Frame dicts)
    # ----------------------------------------------------------------------------------------------
    frames_dict = {}
    signal_history = {"RPM": [], "Speed": [], "Coolant Temperature": [], "Calculated Engine Load": []}
    direct_frames = []

    for rec in records:
        if not isinstance(rec, dict):
            continue
        event_type = rec.get("event_type")
        if event_type == "OBD_OBSERVATION":
            status = rec.get("status")
            if status != "VALID":
                continue
                
            sig = rec.get("signal_name") or ""
            val = rec.get("value")
            if val is None:
                continue
                
            details = rec.get("details") or {}
            cycle = details.get("cycle_index") or rec.get("sequence", 0) // 4
            
            if cycle not in frames_dict:
                frames_dict[cycle] = {}
            
            # Normalize signal name
            sig_lower = sig.lower()
            if "rpm" in sig_lower:
                frames_dict[cycle]["RPM"] = float(val)
                signal_history["RPM"].append(float(val))
            elif "speed" in sig_lower:
                frames_dict[cycle]["Speed"] = float(val)
                signal_history["Speed"].append(float(val))
            elif "coolant" in sig_lower:
                frames_dict[cycle]["Coolant"] = float(val)
                signal_history["Coolant Temperature"].append(float(val))
            elif "load" in sig_lower:
                frames_dict[cycle]["Load"] = float(val)
                frames_dict[cycle]["TPS"] = float(val)
                signal_history["Calculated Engine Load"].append(float(val))
            elif "map" in sig_lower or "manifold" in sig_lower:
                frames_dict[cycle]["MAP"] = float(val)
            elif "lambda" in sig_lower:
                frames_dict[cycle]["Lambda"] = float(val)
        else:
            # Direct frame object (e.g. {"rpm": 2500, "speed": 60, "map": 85, "tps": 35} or uppercase)
            synth = synthesize_missing_features(rec)
            direct_frames.append(synth)
            rpm_val = float(synth.get("RPM", 0.0))
            spd_val = float(synth.get("Speed", 0.0))
            load_val = float(synth.get("TPS", 0.0))
            if rpm_val > 0:
                signal_history["RPM"].append(rpm_val)
            if spd_val >= 0:
                signal_history["Speed"].append(spd_val)
            if load_val > 0:
                signal_history["Calculated Engine Load"].append(load_val)

    evaluation_frames = list(direct_frames)

    # If structured observation cycles were grouped, add them
    for c_id, f_data in sorted(frames_dict.items()):
        if "RPM" in f_data or "Speed" in f_data:
            synth = synthesize_missing_features(f_data)
            evaluation_frames.append(synth)

    if not evaluation_frames:
        avg_rpm = float(np.mean(signal_history["RPM"])) if signal_history["RPM"] else 1200.0
        avg_speed = float(np.mean(signal_history["Speed"])) if signal_history["Speed"] else 30.0
        avg_load = float(np.mean(signal_history["Calculated Engine Load"])) if signal_history["Calculated Engine Load"] else 22.0
        synth = synthesize_missing_features({"RPM": avg_rpm, "Speed": avg_speed, "Load": avg_load})
        evaluation_frames.append(synth)

    # ----------------------------------------------------------------------------------------------
    # Evaluate All Frames Through Model
    # ----------------------------------------------------------------------------------------------
    df_all = pd.DataFrame(evaluation_frames)[FEATURE_COLUMNS]
    all_probs = model.predict_proba(df_all)
    raw_preds = np.argmax(all_probs, axis=1)

    class_counts = {0: 0, 1: 0, 2: 0, 3: 0}
    total_frames = len(df_all)

    for i in range(total_frames):
        rp = int(raw_preds[i])
        class_counts[rp] += 1

    clean_count = class_counts[0]
    rich_count = class_counts[1]
    lean_count = class_counts[2]
    volt_count = class_counts[3]
    anomaly_count = rich_count + lean_count + volt_count

    # Calculate VHI (Vehicle Health Index) on 0 - 100 scale
    # Clean drive gets 100. Anomalies proportionally deduct health based on severity.
    rich_pct = (rich_count / total_frames) * 100.0
    lean_pct = (lean_count / total_frames) * 100.0
    volt_pct = (volt_count / total_frames) * 100.0

    vhi_raw = 100.0 - (rich_pct * 0.60 + lean_pct * 0.70 + volt_pct * 0.85)
    vhi_score = int(np.clip(round(vhi_raw), 0, 100))

    if vhi_score >= 88:
        health_status = "HEALTHY"
    elif vhi_score >= 65:
        health_status = "ATTENTION_REQUIRED"
    else:
        health_status = "CRITICAL_FAULT"

    # Identify dominant fault
    if anomaly_count == 0 or (clean_count / total_frames) >= 0.85:
        dominant_fault = "No Fault (Normal)"
        dominant_conf = float(np.mean(all_probs[:, 0]))
    else:
        fault_order = sorted([(1, rich_count), (2, lean_count), (3, volt_count)], key=lambda x: x[1], reverse=True)
        top_f = fault_order[0][0]
        dominant_fault = CLASS_LABELS[top_f]
        dominant_conf = float(np.mean(all_probs[:, top_f]))

    # Workshop Inspection Steps
    inspection_plan = []
    if dominant_fault == "No Fault (Normal)":
        inspection_plan.append("Factory parameters nominal. Continue routine oil and filter maintenance.")
        inspection_plan.append("Clear temporary diagnostic flags if present.")
    elif dominant_fault == "Rich Mixture":
        inspection_plan.append("1. Inspect fuel injector spray patterns and check for leaking pintles.")
        inspection_plan.append("2. Verify fuel rail pressure with mechanical gauge against factory specification.")
        inspection_plan.append("3. Inspect engine air filter and check MAF hot-wire for particulate contamination.")
        inspection_plan.append("4. Check upstream O2 / Lambda sensor response rate.")
    elif dominant_fault == "Lean Mixture":
        inspection_plan.append("1. Perform smoke test on intake manifold and vacuum hoses (DTC P0171).")
        inspection_plan.append("2. Inspect fuel pump delivery volume and replace fuel filter.")
        inspection_plan.append("3. Check PCV valve diaphragm for unmetered air ingress.")
    elif dominant_fault == "Low Voltage":
        inspection_plan.append("1. Measure battery resting voltage (>= 12.6V) and starter cranking voltage (>= 10.2V).")
        inspection_plan.append("2. Test alternator charging voltage under full electrical load (target 13.8V - 14.4V).")
        inspection_plan.append("3. Measure primary and secondary resistance on ignition coil packs.")

    fault_breakdown = {}
    for code, label in CLASS_LABELS.items():
        cnt = class_counts[code]
        pct = round((cnt / total_frames) * 100.0, 2)
        fault_breakdown[label] = {
            "frames": cnt,
            "percentage": pct
        }

    telemetry_summary = {
        "avg_rpm": round(float(np.mean(signal_history["RPM"])), 1) if signal_history["RPM"] else 0.0,
        "max_rpm": round(float(np.max(signal_history["RPM"])), 1) if signal_history["RPM"] else 0.0,
        "avg_speed": round(float(np.mean(signal_history["Speed"])), 1) if signal_history["Speed"] else 0.0,
        "max_speed": round(float(np.max(signal_history["Speed"])), 1) if signal_history["Speed"] else 0.0,
        "avg_load": round(float(np.mean(signal_history["Calculated Engine Load"])), 1) if signal_history["Calculated Engine Load"] else 0.0,
        "valid_observations_recorded": sum(len(v) for v in signal_history.values())
    }

    logger.info(f"Session {session_id} evaluated: {total_frames} frames -> VHI={vhi_score}, Status={health_status}, Fault={dominant_fault}")

    return SessionEvaluationResponse(
        session_id=session_id,
        vhi_score=vhi_score,
        health_status=health_status,
        dominant_fault=dominant_fault,
        confidence=round(dominant_conf, 4),
        frames_evaluated=total_frames,
        clean_frames=clean_count,
        anomaly_frames=anomaly_count,
        fault_breakdown=fault_breakdown,
        telemetry_summary=telemetry_summary,
        workshop_inspection_plan=inspection_plan,
        status="SUCCESS"
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("ml_service:app", host="0.0.0.0", port=8000, reload=False)
