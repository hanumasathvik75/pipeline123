"""Persistence boundary for virtual-sensor sessions and structured diagnoses."""
from __future__ import annotations

from datetime import datetime, timezone
from hashlib import sha1
from pathlib import Path
from typing import Any

from serving.core import models
from serving.core.database import SessionLocal, engine


def persist_analysis(result: dict[str, Any]) -> int:
    """Persist an analysis session; returns its database session identifier."""
    models.Base.metadata.create_all(bind=engine)
    source_path = result["source"]["path"]
    vin = f"IGNITIONAI-DATA-{sha1(source_path.encode()).hexdigest()[:12].upper()}"
    db = SessionLocal()
    try:
        vehicle = db.query(models.Vehicle).filter_by(vin=vin).first()
        if vehicle is None:
            vehicle = models.Vehicle(vin=vin, make="Telemetry", model=Path(source_path).stem, year=None)
            db.add(vehicle); db.flush()
        session = models.DiagnosticSession(vehicle_id=vehicle.id, timestamp=datetime.now(timezone.utc).isoformat(), status="completed")
        db.add(session); db.flush()
        for sensor in result["virtual_sensors"]:
            db.add(models.VirtualSensorOutput(session_id=session.id, sensor_name=sensor["name"], status=sensor["status"], score=sensor["score"], health_score=sensor["health_score"], flagged=str(sensor["flagged"]), evidence=sensor["evidence"]))
        db.add(models.FaultDiagnosis(session_id=session.id, vehicle_health_index=result["fusion"]["vehicle_health_index"], candidates=result["fusion"]["fault_candidates"]))
        db.commit()
        return int(session.id)
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()
