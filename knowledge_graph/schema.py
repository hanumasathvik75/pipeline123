from typing import List, Dict, Optional
from pydantic import BaseModel, Field, field_validator

class PIDCondition(BaseModel):
    pid_name: str
    condition: str  # e.g., "< 160", "> 230", "oscillating"
    unit: str
    description: str

class Symptom(BaseModel):
    phrasing: str
    category: str  # e.g., "drivability", "hvac", "visual"

class FaultMode(BaseModel):
    name: str
    description: str
    typical_dtcs: List[str]
    typical_pids: List[PIDCondition]
    typical_symptoms: List[Symptom]
    prior_probability: float = Field(ge=0.0, le=1.0)
    
    @field_validator('typical_dtcs')
    def validate_dtcs(cls, v: List[str]) -> List[str]:
        for dtc in v:
            if not (dtc.startswith('P') and len(dtc) == 5):
                raise ValueError(f"DTC {dtc} must follow SAE J2012 format (e.g., P0128)")
        return v

class Component(BaseModel):
    name: str
    fault_modes: List[FaultMode]

class SubsystemGraph(BaseModel):
    subsystem_name: str
    components: List[Component]
