from sqlalchemy import Column, Integer, String, Float, ForeignKey, JSON
from sqlalchemy.orm import relationship
from .database import Base

class Vehicle(Base):
    __tablename__ = "vehicles"
    id = Column(Integer, primary_key=True, index=True)
    vin = Column(String, unique=True, index=True)
    make = Column(String)
    model = Column(String)
    year = Column(Integer)
    
    sessions = relationship("DiagnosticSession", back_populates="vehicle")

class DiagnosticSession(Base):
    __tablename__ = "diagnostic_sessions"
    id = Column(Integer, primary_key=True, index=True)
    vehicle_id = Column(Integer, ForeignKey("vehicles.id"))
    timestamp = Column(String) # Storing as string for simplicity, or use DateTime
    status = Column(String) # e.g., "in_progress", "completed"
    
    vehicle = relationship("Vehicle", back_populates="sessions")
    telemetry_logs = relationship("TelemetryLog", back_populates="session")

class TelemetryLog(Base):
    __tablename__ = "telemetry_logs"
    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(Integer, ForeignKey("diagnostic_sessions.id"))
    timestamp = Column(String)
    sensor_data = Column(JSON) # JSON dump of PID values
    dtcs = Column(JSON) # List of DTC strings
    
    session = relationship("DiagnosticSession", back_populates="telemetry_logs")


class VirtualSensorOutput(Base):
    """Persisted derived quantity; raw telemetry remains in TelemetryLog."""
    __tablename__ = "virtual_sensor_outputs"
    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(Integer, ForeignKey("diagnostic_sessions.id"), index=True)
    sensor_name = Column(String, index=True, nullable=False)
    status = Column(String, nullable=False)
    score = Column(Float, nullable=True)
    health_score = Column(Float, nullable=True)
    flagged = Column(String, nullable=False)
    evidence = Column(JSON, nullable=False)


class FaultDiagnosis(Base):
    """Evidence-backed candidate diagnosis produced before any LLM explanation."""
    __tablename__ = "fault_diagnoses"
    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(Integer, ForeignKey("diagnostic_sessions.id"), index=True)
    vehicle_health_index = Column(Float, nullable=True)
    candidates = Column(JSON, nullable=False)
    explanation = Column(String, nullable=True)

# -- Graph Logic Schema for Ground Truth (Anish's Focus) --

class FaultScenario(Base):
    __tablename__ = "fault_scenarios"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, index=True)
    description = Column(String)
    
    symptoms = relationship("Symptom", back_populates="scenario")

class Symptom(Base):
    __tablename__ = "symptoms"
    id = Column(Integer, primary_key=True, index=True)
    scenario_id = Column(Integer, ForeignKey("fault_scenarios.id"))
    phrasing = Column(String)
    category = Column(String) # e.g., "drivability", "visual"
    
    scenario = relationship("FaultScenario", back_populates="symptoms")
    decision_nodes = relationship("DecisionNode", back_populates="symptom")

class DecisionNode(Base):
    __tablename__ = "decision_nodes"
    id = Column(Integer, primary_key=True, index=True)
    symptom_id = Column(Integer, ForeignKey("symptoms.id"))
    question = Column(String) # e.g., "Is fuel pressure < 20 psi?"
    condition = Column(String) # e.g., "fuel_pressure < 20"
    
    symptom = relationship("Symptom", back_populates="decision_nodes")
    root_causes = relationship("RootCause", back_populates="decision_node")

class RootCause(Base):
    __tablename__ = "root_causes"
    id = Column(Integer, primary_key=True, index=True)
    decision_node_id = Column(Integer, ForeignKey("decision_nodes.id"))
    description = Column(String) # e.g., "Faulty fuel pump"
    probability = Column(Float)
    
    decision_node = relationship("DecisionNode", back_populates="root_causes")
    repair_steps = relationship("RepairStep", back_populates="root_cause")

class RepairStep(Base):
    __tablename__ = "repair_steps"
    id = Column(Integer, primary_key=True, index=True)
    root_cause_id = Column(Integer, ForeignKey("root_causes.id"))
    step_number = Column(Integer)
    instruction = Column(String) # e.g., "Replace fuel pump relay"
    
    root_cause = relationship("RootCause", back_populates="repair_steps")


class MechanicFeedback(Base):
    """Mechanic's verdict on a diagnostic candidate — closes the learning loop."""
    __tablename__ = "mechanic_feedback"
    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(Integer, ForeignKey("diagnostic_sessions.id"), index=True)
    candidate = Column(String, nullable=False)
    verdict = Column(String, nullable=False)  # "confirmed", "rejected", "alternative"
    actual_cause = Column(String, nullable=True)
    notes = Column(String, nullable=True)
    created_at = Column(String, nullable=False)
