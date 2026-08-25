from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List

from serving.core import models, schemas
from serving.core.database import engine, get_db
from serving.diagnostics.pipeline import analyze_file
from serving.diagnostics.explanation import explain_structured_diagnosis

# Create all database tables
models.Base.metadata.create_all(bind=engine)

app = FastAPI(title="IgnitionAI Diagnostic Engine API")
app.add_middleware(CORSMiddleware, allow_origins=["http://localhost:5173"], allow_methods=["*"], allow_headers=["*"])

@app.get("/")
def read_root():
    return {"message": "Welcome to IgnitionAI Backend"}

@app.get("/api/scenarios/", response_model=List[schemas.FaultScenario])
def read_scenarios(skip: int = 0, limit: int = 10, db: Session = Depends(get_db)):
    scenarios = db.query(models.FaultScenario).offset(skip).limit(limit).all()
    return scenarios

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


@app.post("/api/diagnostics/analyze-file", response_model=schemas.AnalysisResult)
def analyze_telemetry_file(request: schemas.AnalyzeFileRequest, db: Session = Depends(get_db)):
    """Compute physical/statistical virtual sensors from a local telemetry file.

    The resulting candidate faults are fully structured; a language model is not
    called from this endpoint.
    """
    try:
        result = analyze_file(request.source_path, request.source_segment)
    except (OSError, ValueError) as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    if request.session_id is not None:
        session = db.query(models.DiagnosticSession).filter(models.DiagnosticSession.id == request.session_id).first()
        if not session:
            raise HTTPException(status_code=404, detail="Session not found")
        for sensor in result["virtual_sensors"]:
            db.add(models.VirtualSensorOutput(session_id=session.id, sensor_name=sensor["name"], status=sensor["status"], score=sensor["score"], health_score=sensor["health_score"], flagged=str(sensor["flagged"]), evidence=sensor["evidence"]))
        db.add(models.FaultDiagnosis(session_id=session.id, vehicle_health_index=result["fusion"]["vehicle_health_index"], candidates=result["fusion"]["fault_candidates"]))
        db.commit()
    return result


@app.post("/api/diagnostics/explain", response_model=schemas.ExplanationResponse)
def explain_diagnosis(request: schemas.ExplanationRequest) -> dict[str, str]:
    """Gemini receives this structured fusion result only, never raw telemetry."""
    try:
        return {"report": explain_structured_diagnosis(request.model_dump())}
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
