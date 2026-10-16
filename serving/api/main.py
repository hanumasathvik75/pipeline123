"""IgnitionAI Diagnostic Engine API — FastAPI backend."""
from __future__ import annotations

import os
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import List

from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session

from serving.core import models, schemas
from serving.core.database import engine, get_db
from serving.diagnostics.pipeline import analyze_file
from serving.diagnostics.explanation import explain_structured_diagnosis
from serving.diagnostics.dtc_engine import analyze_dtcs
from serving.diagnostics.hybrid_engine import get_hybrid_engine
from serving.diagnostics.enginefault_service import predict_enginefault

# Create all database tables
models.Base.metadata.create_all(bind=engine)

app = FastAPI(title="IgnitionAI Diagnostic Engine API")
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://localhost:3000",
        "http://127.0.0.1:5173",
    ],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------

@app.get("/")
def read_root():
    return {"message": "Welcome to IgnitionAI Backend", "version": "0.2.0"}


# ---------------------------------------------------------------------------
# Scenarios (Graph Logic)
# ---------------------------------------------------------------------------

@app.get("/api/scenarios/", response_model=List[schemas.FaultScenario])
def read_scenarios(skip: int = 0, limit: int = 10, db: Session = Depends(get_db)):
    scenarios = db.query(models.FaultScenario).offset(skip).limit(limit).all()
    return scenarios


# ---------------------------------------------------------------------------
# Sessions — list & detail
# ---------------------------------------------------------------------------

@app.get("/api/sessions/")
def list_sessions(skip: int = 0, limit: int = 50, db: Session = Depends(get_db)):
    """List all diagnostic sessions with summary info."""
    sessions = (
        db.query(models.DiagnosticSession)
        .order_by(models.DiagnosticSession.id.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
    results = []
    for s in sessions:
        diagnosis = (
            db.query(models.FaultDiagnosis)
            .filter(models.FaultDiagnosis.session_id == s.id)
            .first()
        )
        vehicle = (
            db.query(models.Vehicle)
            .filter(models.Vehicle.id == s.vehicle_id)
            .first()
        )
        results.append({
            "id": s.id,
            "timestamp": s.timestamp,
            "status": s.status,
            "vehicle": {
                "vin": vehicle.vin if vehicle else None,
                "make": vehicle.make if vehicle else None,
                "model": vehicle.model if vehicle else None,
                "year": vehicle.year if vehicle else None,
            } if vehicle else None,
            "vehicle_health_index": diagnosis.vehicle_health_index if diagnosis else None,
            "fault_count": len(diagnosis.candidates) if diagnosis and diagnosis.candidates else 0,
        })
    return results


@app.get("/api/sessions/{session_id}")
def get_session(session_id: int, db: Session = Depends(get_db)):
    """Get full detail for a single diagnostic session."""
    session = (
        db.query(models.DiagnosticSession)
        .filter(models.DiagnosticSession.id == session_id)
        .first()
    )
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    diagnosis = (
        db.query(models.FaultDiagnosis)
        .filter(models.FaultDiagnosis.session_id == session_id)
        .first()
    )
    sensors = (
        db.query(models.VirtualSensorOutput)
        .filter(models.VirtualSensorOutput.session_id == session_id)
        .all()
    )
    vehicle = (
        db.query(models.Vehicle)
        .filter(models.Vehicle.id == session.vehicle_id)
        .first()
    )
    feedback_records = (
        db.query(models.MechanicFeedback)
        .filter(models.MechanicFeedback.session_id == session_id)
        .all()
    )

    return {
        "id": session.id,
        "timestamp": session.timestamp,
        "status": session.status,
        "vehicle": {
            "vin": vehicle.vin,
            "make": vehicle.make,
            "model": vehicle.model,
            "year": vehicle.year,
        } if vehicle else None,
        "virtual_sensors": [
            {
                "name": s.sensor_name,
                "status": s.status,
                "score": s.score,
                "health_score": s.health_score,
                "flagged": s.flagged,
                "evidence": s.evidence,
            }
            for s in sensors
        ],
        "diagnosis": {
            "vehicle_health_index": diagnosis.vehicle_health_index,
            "candidates": diagnosis.candidates,
            "explanation": diagnosis.explanation,
        } if diagnosis else None,
        "feedback": [
            {
                "id": f.id,
                "candidate": f.candidate,
                "verdict": f.verdict,
                "actual_cause": f.actual_cause,
                "notes": f.notes,
                "created_at": f.created_at,
            }
            for f in feedback_records
        ],
    }


# ---------------------------------------------------------------------------
# Telemetry ingestion
# ---------------------------------------------------------------------------

@app.post("/api/telemetry/ingest", response_model=schemas.TelemetryLog)
def ingest_telemetry(log: schemas.TelemetryLogCreate, session_id: int, db: Session = Depends(get_db)):
    db_session = db.query(models.DiagnosticSession).filter(models.DiagnosticSession.id == session_id).first()
    if not db_session:
        raise HTTPException(status_code=404, detail="Session not found")

    db_log = models.TelemetryLog(**log.dict(), session_id=session_id)
    db.add(db_log)
    db.commit()
    db.refresh(db_log)
    return db_log


# ---------------------------------------------------------------------------
# Diagnostics — file-based analysis (with path sanitization)
# ---------------------------------------------------------------------------

# Allowed upload directories. Extend as needed.
_ALLOWED_ROOTS = [
    Path(os.getenv("IGNITIONAI_DATA_DIR", Path.home() / "Downloads")).resolve(),
    Path(tempfile.gettempdir()).resolve(),
]


def _sanitize_path(raw: str) -> Path:
    """Resolve and verify a source path is within allowed roots."""
    resolved = Path(raw).resolve()
    if not any(resolved.is_relative_to(root) for root in _ALLOWED_ROOTS):
        raise HTTPException(
            status_code=403,
            detail="Access denied: source_path is outside allowed directories.",
        )
    if not resolved.exists():
        raise HTTPException(status_code=404, detail="File not found.")
    return resolved


@app.post("/api/diagnostics/analyze-file", response_model=schemas.AnalysisResult)
def analyze_telemetry_file(request: schemas.AnalyzeFileRequest, db: Session = Depends(get_db)):
    """Compute physical/statistical virtual sensors from a local telemetry file.

    The resulting candidate faults are fully structured; a language model is not
    called from this endpoint.
    """
    safe_path = _sanitize_path(request.source_path)
    try:
        result = analyze_file(str(safe_path), request.source_segment)
    except (OSError, ValueError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc

    if request.session_id is not None:
        session = db.query(models.DiagnosticSession).filter(models.DiagnosticSession.id == request.session_id).first()
        if not session:
            raise HTTPException(status_code=404, detail="Session not found")
        for sensor in result["virtual_sensors"]:
            db.add(models.VirtualSensorOutput(
                session_id=session.id,
                sensor_name=sensor["name"],
                status=sensor["status"],
                score=sensor["score"],
                health_score=sensor["health_score"],
                flagged=str(sensor["flagged"]),
                evidence=sensor["evidence"],
            ))
        db.add(models.FaultDiagnosis(
            session_id=session.id,
            vehicle_health_index=result["fusion"]["vehicle_health_index"],
            candidates=result["fusion"]["fault_candidates"],
        ))
        db.commit()
    return result


# ---------------------------------------------------------------------------
# Diagnostics — JSON telemetry analysis (no file path needed)
# ---------------------------------------------------------------------------

class AnalyzeTelemetryRequest(schemas.BaseModel):
    """Accept telemetry data directly as JSON — no file path required."""
    records: List[dict]  # List of {timestamp, rpm, coolant_c, ...} dicts
    source_segment: str | None = None


@app.post("/api/diagnostics/analyze", response_model=schemas.AnalysisResult)
def analyze_telemetry_json(request: AnalyzeTelemetryRequest):
    """Accepts telemetry as a JSON array, writes to a temp CSV, and runs the
    pipeline. This avoids the security risk of accepting arbitrary file paths."""
    import pandas as pd

    if not request.records:
        raise HTTPException(status_code=422, detail="No telemetry records provided.")

    frame = pd.DataFrame(request.records)
    with tempfile.NamedTemporaryFile(suffix=".csv", delete=False, mode="w") as f:
        frame.to_csv(f, index=False)
        tmp_path = f.name

    try:
        result = analyze_file(tmp_path, request.source_segment)
    except (OSError, ValueError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    finally:
        Path(tmp_path).unlink(missing_ok=True)

    return result


# ---------------------------------------------------------------------------
# Diagnostics — LLM explanation
# ---------------------------------------------------------------------------

@app.post("/api/diagnostics/explain", response_model=schemas.ExplanationResponse)
def explain_diagnosis(request: schemas.ExplanationRequest) -> dict[str, str]:
    """Gemini receives this structured fusion result only, never raw telemetry."""
    try:
        return {"report": explain_structured_diagnosis(request.model_dump())}
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


# ---------------------------------------------------------------------------
# Diagnostics — Tier 1 instant DTC quick-scan
# ---------------------------------------------------------------------------

class QuickScanRequest(schemas.BaseModel):
    """Instant DTC-based diagnosis — no telemetry file needed."""
    dtcs: List[str]
    sensor_snapshot: dict[str, float] | None = None
    vehicle: dict[str, str | int | None] | None = None  # {make, model, year, engine}


@app.post("/api/diagnostics/quick-scan")
def quick_scan(request: QuickScanRequest):
    """Tier 1: Instant DTC analysis from fault codes + optional sensor snapshot.

    This is the fast path for mechanics — scan codes with an ELM327,
    paste them here, get ranked root causes in under a second.
    """
    if not request.dtcs:
        raise HTTPException(status_code=422, detail="At least one DTC is required.")

    result = analyze_dtcs(request.dtcs, request.sensor_snapshot)

    # Attach vehicle context if provided
    if request.vehicle:
        result["vehicle"] = request.vehicle

    return result


# ---------------------------------------------------------------------------
# Option B: Hybrid ML + Physics Bounds Evaluation
# ---------------------------------------------------------------------------

@app.post("/api/diagnostics/hybrid-evaluate")
def evaluate_hybrid_telemetry(features: dict[str, float]):
    """Option B: Evaluates a 30-second OBD-II feature vector using Hybrid ML + Physics Bounds.

    Accepts 24 statistical & physics features extracted from a 30s driving window.
    Returns: VHI (0-100), health grade, subsystem scores, fault flags, and actionable candidates with detection source badges.
    """
    engine = get_hybrid_engine()
    result = engine.evaluate(features)
    return result.to_dict()


# ---------------------------------------------------------------------------
# Real EngineFault XGBoost Diagnostic Model (Mobile REST API Endpoint)
# ---------------------------------------------------------------------------

@app.post(
    "/api/diagnostics/enginefault-predict",
    response_model=schemas.EngineFaultPredictResponse,
    tags=["Diagnostics"]
)
def predict_enginefault_endpoint(payload: schemas.EngineFaultPredictRequest):
    """Real EngineFault XGBoost Inference Endpoint (Trained on 55,999 Real Telemetry Samples).

    Dedicated REST API for Mobile Application (Flutter/Android/iOS/Postman).
    Accepts 14 engine telemetry parameters with flexible key naming and physical default fallbacks.
    Returns:
      - Predicted fault ID (0: Normal, 1: Rich Mixture, 2: Lean Mixture, 3: Low Voltage)
      - Fault label & severity
      - Diagnostic confidence & full multi-class probability distribution
      - Hybrid physics confirmation source
      - Actionable workshop repair action
    """
    try:
        data = payload.model_dump()
        extra = getattr(payload, "__pydantic_extra__", {}) or {}
        data.update(extra)
        
        result = predict_enginefault(data)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"EngineFault diagnostic inference failed: {str(e)}")


# ---------------------------------------------------------------------------
# Mechanic feedback
# ---------------------------------------------------------------------------

@app.post("/api/feedback", response_model=schemas.MechanicFeedback)
def submit_feedback(feedback: schemas.MechanicFeedbackCreate, db: Session = Depends(get_db)):
    """Mechanic submits verdict on a diagnostic candidate (confirmed / rejected / alternative)."""
    session = db.query(models.DiagnosticSession).filter(models.DiagnosticSession.id == feedback.session_id).first()
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    valid_verdicts = {"confirmed", "rejected", "alternative"}
    if feedback.verdict not in valid_verdicts:
        raise HTTPException(status_code=422, detail=f"verdict must be one of: {valid_verdicts}")

    db_feedback = models.MechanicFeedback(
        session_id=feedback.session_id,
        candidate=feedback.candidate,
        verdict=feedback.verdict,
        actual_cause=feedback.actual_cause,
        notes=feedback.notes,
        created_at=datetime.now(timezone.utc).isoformat(),
    )
    db.add(db_feedback)
    db.commit()
    db.refresh(db_feedback)
    return db_feedback


@app.get("/api/feedback/")
def list_feedback(session_id: int | None = None, limit: int = 100, db: Session = Depends(get_db)):
    """List mechanic feedback, optionally filtered by session."""
    query = db.query(models.MechanicFeedback)
    if session_id is not None:
        query = query.filter(models.MechanicFeedback.session_id == session_id)
    return query.order_by(models.MechanicFeedback.id.desc()).limit(limit).all()
