from pydantic import BaseModel
from typing import List, Optional, Dict, Any

# -- Telemetry & Sessions --

class TelemetryLogBase(BaseModel):
    timestamp: str
    sensor_data: Dict[str, Any]
    dtcs: List[str]

class TelemetryLogCreate(TelemetryLogBase):
    pass

class TelemetryLog(TelemetryLogBase):
    id: int
    session_id: int
    class Config:
        from_attributes = True

class DiagnosticSessionBase(BaseModel):
    timestamp: str
    status: str

class DiagnosticSessionCreate(DiagnosticSessionBase):
    pass

class DiagnosticSession(DiagnosticSessionBase):
    id: int
    vehicle_id: int
    telemetry_logs: List[TelemetryLog] = []
    class Config:
        from_attributes = True


class AnalyzeFileRequest(BaseModel):
    source_path: str
    source_segment: Optional[str] = None
    session_id: Optional[int] = None


class VirtualSensorOutput(BaseModel):
    name: str
    status: str
    score: Optional[float] = None
    health_score: Optional[float] = None
    flagged: bool
    evidence: Dict[str, Any]
    candidate: Optional[str] = None


class AnalysisResult(BaseModel):
    source: Dict[str, Any]
    raw_signal_preview: List[Dict[str, Any]]
    virtual_sensors: List[VirtualSensorOutput]
    fusion: Dict[str, Any]


class ExplanationRequest(BaseModel):
    vehicle_health_index: Optional[float] = None
    fault_candidates: List[Dict[str, Any]] = []


class ExplanationResponse(BaseModel):
    report: str

# -- Graph Logic Schema --

class RepairStepBase(BaseModel):
    step_number: int
    instruction: str

class RepairStepCreate(RepairStepBase):
    pass

class RepairStep(RepairStepBase):
    id: int
    root_cause_id: int
    class Config:
        from_attributes = True

class RootCauseBase(BaseModel):
    description: str
    probability: float

class RootCauseCreate(RootCauseBase):
    pass

class RootCause(RootCauseBase):
    id: int
    decision_node_id: int
    repair_steps: List[RepairStep] = []
    class Config:
        from_attributes = True

class DecisionNodeBase(BaseModel):
    question: str
    condition: str

class DecisionNodeCreate(DecisionNodeBase):
    pass

class DecisionNode(DecisionNodeBase):
    id: int
    symptom_id: int
    root_causes: List[RootCause] = []
    class Config:
        from_attributes = True

class SymptomBase(BaseModel):
    phrasing: str
    category: str

class SymptomCreate(SymptomBase):
    pass

class Symptom(SymptomBase):
    id: int
    scenario_id: int
    decision_nodes: List[DecisionNode] = []
    class Config:
        from_attributes = True

class FaultScenarioBase(BaseModel):
    name: str
    description: str

class FaultScenarioCreate(FaultScenarioBase):
    pass

class FaultScenario(FaultScenarioBase):
    id: int
    symptoms: List[Symptom] = []
    class Config:
        from_attributes = True
