"""
IgnitionAI Hybrid Diagnostic Engine (Option B: Hybrid ML + Physics Bounds)
Combines Machine Learning (XGBoost Multi-Output) with First-Principles Physics Invariants.

Architecture:
  Stage 1: Multi-Output Machine Learning Model (Learns complex multivariate non-linear patterns)
  Stage 2: Physics-Informed Safety Guards (Enforces strict mechanical/electrical first principles)
  Stage 3: Decision Fusion (Resolves ML shortcuts and assigns validated confidence)
  Stage 4: Subsystem Health Scoring (0-100) & Vehicle Health Index (VHI)
  Stage 5: Actionable Candidate Root Causes & Workshop Physical Tests
"""
import os
import joblib
import numpy as np
from dataclasses import dataclass, asdict
from typing import Any

_CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(_CURRENT_DIR, "models", "best_multilabel_xgb_model.joblib")

FEATURE_COLS = [
    "rpm_mean", "rpm_std", "rpm_kurtosis",
    "speed_mean", "speed_std", "idle_fraction",
    "throttle_mean", "engine_load_mean",
    "map_mean", "map_std", "maf_mean", "maf_map_ratio", "maf_physics_ratio",
    "trim_mean", "trim_std", "stoich_closed_loop_residual",
    "coolant_mean", "coolant_warmup_rate", "intake_mean", "coolant_intake_delta", "thermal_deficit_score",
    "voltage_mean", "voltage_ripple_std", "voltage_physics_margin",
    "rpm_det_std", "rpm_r3", "rpm_r7", "hunting_surge_metric"
]

TARGET_CONFIGS = [
    {
        "id": "fault_lean_trim",
        "name": "Lean Fuel-Trim Deviation (Observable Pattern)",
        "subsystem": "Fuel System",
        "root_cause": "Candidate physical factors: Air intake vacuum leak, unmetered air ingress, or dirty MAF sensor.",
        "workshop_test": "Perform smoke test on intake manifold; inspect fuel rail pressure under load."
    },
    {
        "id": "fault_rich_trim",
        "name": "Rich Fuel-Trim Deviation (Observable Pattern)",
        "subsystem": "Fuel System",
        "root_cause": "Candidate physical factors: Fuel injector leakage, excessive fuel rail pressure, or restricted intake filter.",
        "workshop_test": "Test injector leak-down rate and verify fuel pressure regulator operation."
    },
    {
        "id": "fault_combustion_hunting",
        "name": "Combustion Instability / Surge (Observable Pattern)",
        "subsystem": "Combustion",
        "root_cause": "Candidate physical factors: Idle air control valve sticking, carbon buildup on throttle plate, or cyclic torque disturbance.",
        "workshop_test": "Clean electronic throttle body; check spark plug gaps and ignition coil secondary resistance."
    },
    {
        "id": "fault_alternator_sag",
        "name": "Electrical Charging Voltage Sag (Observable Pattern)",
        "subsystem": "Electrical",
        "root_cause": "Candidate physical factors: Alternator diode/regulator wear, slipping serpentine belt, or high auxiliary electrical load.",
        "workshop_test": "Measure alternator output voltage under 30A electrical load; inspect belt tensioner."
    },
    {
        "id": "fault_thermal_delay",
        "name": "Thermal Warmup Delay (Observable Pattern)",
        "subsystem": "Cooling",
        "root_cause": "Candidate physical factors: Engine thermostat stuck open or premature radiator cooling fan activation.",
        "workshop_test": "Perform thermal imaging of upper radiator hose during cold start; test thermostat opening temp."
    }
]


@dataclass
class DiagnosticCandidate:
    target_id: str
    display_name: str
    subsystem: str
    raw_ml_prob: float
    physics_guard_active: bool
    confidence: float
    detection_source: str  # 'Hybrid Consensus', 'Physics Safety Override', or 'ML Pattern Match'
    evidence_reason: str
    root_cause: str
    workshop_test: str


@dataclass
class HybridDiagnosticResult:
    vehicle_health_index: float
    health_grade: str
    subsystem_scores: dict[str, float]
    fault_flags: dict[str, int]
    candidates: list[dict[str, Any]]
    detection_summary: dict[str, str]

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


class HybridDiagnosticEngine:
    def __init__(self, model_path: str = MODEL_PATH):
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"Trained ML model not found at: {model_path}")
        self.model = joblib.load(model_path)
        print("HybridDiagnosticEngine initialized successfully with model:", type(self.model))

    def evaluate(self, features: dict[str, float], window_seconds: float = 30.0) -> HybridDiagnosticResult:
        """
        Executes Option B: Hybrid ML + Physics Bounds Evaluation.
        """
        # --- STAGE 0: ENSURE DUAL-NATURE PHYSICS RESIDUALS ARE PRESENT ---
        f = dict(features)
        
        # 1. Combustion hunting oscillation metric
        if "rpm_det_std" not in f:
            f["rpm_det_std"] = f.get("rpm_std", 10.0) * 0.8
        if "rpm_r3" not in f:
            f["rpm_r3"] = 0.0
        if "rpm_r7" not in f:
            f["rpm_r7"] = 0.0
        if "hunting_surge_metric" not in f:
            # If detrended correlation not pre-computed, use kurtosis-weighted dispersion
            f["hunting_surge_metric"] = (f["rpm_r7"] - f["rpm_r3"]) * f["rpm_det_std"]
            
        # 2. Air-path physics Volumetric Efficiency ratio
        if "maf_physics_ratio" not in f:
            rpm_m = f.get("rpm_mean", 750.0)
            map_m = f.get("map_mean", 35.0)
            intake_k = f.get("intake_mean", 25.0) + 273.15
            sd_index = (rpm_m * map_m) / intake_k
            f["maf_physics_ratio"] = f.get("maf_mean", 2.5) / max(0.1, sd_index)
            
        # 3. Stoichiometric closed-loop residual
        if "stoich_closed_loop_residual" not in f:
            coolant_m = f.get("coolant_mean", 85.0)
            cl_flag = 1.0 if coolant_m >= 45.0 else 0.0
            f["stoich_closed_loop_residual"] = cl_flag * f.get("trim_mean", 0.0)
            
        # 4. Thermal deficit score (Newton's cooling invariant)
        if "thermal_deficit_score" not in f:
            coolant_m = f.get("coolant_mean", 85.0)
            f["thermal_deficit_score"] = float(max(0.0, 65.0 - coolant_m))
            
        # 5. Electrical voltage margin
        # Provide sensible physical defaults for any missing mobile telemetry features
        defaults = {
            "rpm_mean": 750.0, "rpm_std": 6.0, "rpm_kurtosis": 0.0,
            "speed_mean": 0.0, "speed_std": 0.0,
            "throttle_mean": 4.0, "engine_load_mean": 18.0,
            "map_mean": 30.0, "map_std": 0.5, "maf_mean": 2.2, "maf_map_ratio": 0.073,
            "trim_mean": 0.0, "trim_std": 0.5,
            "coolant_mean": 88.0, "coolant_warmup_rate": 0.8, "intake_mean": 25.0,
            "coolant_intake_delta": 63.0,
            "voltage_mean": 14.1, "voltage_ripple_std": 0.02
        }
        for k, v in defaults.items():
            if k not in f:
                f[k] = v
        if "idle_fraction" not in f:
            f["idle_fraction"] = 1.0 if f.get("speed_mean", 0.0) < 5.0 else 0.0

        # --- STAGE 1: MACHINE LEARNING INFERENCE ---
        feat_vector = np.array([[float(f.get(c, 0.0)) for c in FEATURE_COLS]])
        ml_preds = self.model.predict(feat_vector)[0]
        
        # Estimate ML probabilities from individual estimators
        ml_probs = []
        for idx, estimator in enumerate(self.model.estimators_):
            if hasattr(estimator, "predict_proba"):
                prob = estimator.predict_proba(feat_vector)[0][1]
            else:
                prob = float(ml_preds[idx])
            ml_probs.append(float(prob))
            
        # --- STAGE 2: FIRST-PRINCIPLES PHYSICS SAFETY GUARDS ---
        # Physical Invariants that MUST be respected regardless of ML shortcuts:
        v_mean = features.get("voltage_mean", 14.1)
        v_min = features.get("voltage_min", 14.0)
        rpm_mean = features.get("rpm_mean", 750.0)
        trim_mean = features.get("trim_mean", 0.0)
        coolant_mean = features.get("coolant_mean", 85.0)
        coolant_rate = features.get("coolant_warmup_rate", 1.0)
        rpm_std = features.get("rpm_std", 5.0)
        
        physics_guards = {
            # 1. Electrical Invariant (Ohm's Law): Running voltage < 12.45V is ALWAYS a charging fault, regardless of speed
            "fault_alternator_sag": (v_mean < 12.45 or v_min < 12.20) and rpm_mean > 500.0,
            
            # 2. Stoichiometric Lean Invariant: Closed-loop trim >= 13.0% is physically lean
            "fault_lean_trim": trim_mean >= 13.0 and coolant_mean >= 45.0,
            
            # 3. Stoichiometric Rich Invariant: Closed-loop trim <= -10.5% is physically rich
            "fault_rich_trim": trim_mean <= -10.5 and coolant_mean >= 45.0,
            
            # 4. Combustion Stability Invariant: Macro RPM standard deviation > 85 RPM with idle speed indicates surge
            "fault_combustion_hunting": rpm_std > 85.0 and features.get("rpm_kurtosis", 0.0) > 1.5,
            
            # 5. Thermodynamic Warmup Invariant: Coolant < 60°C after prolonged running
            "fault_thermal_delay": coolant_mean < 62.0 and coolant_rate < 0.25 and features.get("intake_mean", 25.0) > 15.0
        }
        
        # --- STAGE 3: HYBRID DECISION FUSION ---
        final_flags = {}
        candidates = []
        sources = {}
        
        for idx, cfg in enumerate(TARGET_CONFIGS):
            target_id = cfg["id"]
            ml_active = bool(ml_preds[idx] == 1)
            guard_active = bool(physics_guards.get(target_id, False))
            prob = ml_probs[idx]
            
            # Cold open-loop suppression (prevents false trim alarms before O2 sensor is hot)
            if target_id in {"fault_lean_trim", "fault_rich_trim"} and coolant_mean < 45.0:
                final_active = False
                source = "Suppressed (Engine in Cold Open-Loop)"
                conf = 0.0
                evidence = "Suppressed: Narrow-band O2 sensor not at operating temperature (<45°C coolant)."
            elif guard_active and ml_active:
                final_active = True
                source = "Hybrid Consensus (ML + Physics Confirmed)"
                conf = round(prob, 2)
                evidence = f"Both XGBoost model (prob={prob*100:.1f}%) and physical heuristic boundary violated."
            elif guard_active and not ml_active:
                # Physics Guard catches ML shortcut (e.g. vehicle speed bug!)
                final_active = True
                source = "Physics Safety Override (Enforced)"
                conf = round(prob, 2)
                evidence = f"Enforced by Physics Guard: Physical invariant violated (e.g., voltage {v_mean:.2f}V < 12.45V threshold) despite low ML confidence."
            elif ml_active and not guard_active:
                # ML detects complex multivariate pattern before hard boundary
                final_active = True
                source = "Machine Learning (Pattern Match)"
                conf = round(prob, 2)
                evidence = f"Detected by XGBoost Decision Trees (prob={prob*100:.1f}%); physical boundary not yet reached."
            else:
                final_active = False
                source = "Nominal (No Fault Pattern)"
                conf = round(prob, 2)
                evidence = "Nominal telemetry within standard operational bounds."
                
            final_flags[target_id] = 1 if final_active else 0
            sources[target_id] = source
            
            if final_active:
                candidates.append(DiagnosticCandidate(
                    target_id=target_id,
                    display_name=cfg["name"],
                    subsystem=cfg["subsystem"],
                    raw_ml_prob=round(prob, 3),
                    physics_guard_active=guard_active,
                    confidence=round(conf, 2),
                    detection_source=source,
                    evidence_reason=evidence,
                    root_cause=cfg["root_cause"],
                    workshop_test=cfg["workshop_test"]
                ))

        # --- STAGE 4: CONTINUOUS MULTI-FACTOR SUBSYSTEM HEALTH & VHI ---
        # Extracts all continuous operating channels
        rpm_val = f.get('rpm_mean', 750.0)
        speed_val = f.get('speed_mean', 0.0)
        trim_val = f.get('trim_mean', 0.5)
        coolant_val = f.get('coolant_mean', 88.0)
        intake_val = f.get('intake_mean', 26.0)
        v_val = f.get('voltage_mean', 14.1)
        o2_val = f.get('o2_b1s1_v', 0.45)
        map_val = f.get('map_mean', 30.0)
        maf_val = f.get('maf_mean', 2.2)
        hunt_metric_val = f.get('hunting_surge_metric', 0.0)
        rpm_std_val = f.get('rpm_std', 5.0)

        # 1. FUEL SYSTEM HEALTH (Continuous degradation across trim, O2, MAF speed-density)
        h_fuel = 100.0
        abs_trim = max(0.0, abs(trim_val) - 1.0)  # Grace margin for nominal trim oscillation
        if abs_trim <= 4.0:
            h_fuel -= abs_trim * 0.8
        elif abs_trim <= 10.0:
            h_fuel -= 3.2 + (abs_trim - 4.0) * 1.8
        else:
            h_fuel -= 14.0 + (abs_trim - 10.0) * 2.2
            
        # O2 sensor stoichiometry bias (only in closed loop)
        if coolant_val >= 45.0:
            o2_dev = max(0.0, abs(o2_val - 0.45) - 0.03)
            h_fuel -= min(15.0, (o2_dev / 0.45) * 25.0)
            
        # Volumetric Efficiency Speed-Density airflow discrepancy
        t_in_k = intake_val + 273.15
        sd_index = (rpm_val * map_val) / t_in_k
        ideal_maf = 0.029 * sd_index
        maf_dev = max(0.0, (abs(maf_val - ideal_maf) / max(0.5, ideal_maf)) - 0.05)
        h_fuel -= min(10.0, maf_dev * 15.0)
        
        # ML predicted fault probabilities
        p_lean = ml_probs[0] if len(ml_probs) > 0 else 0.0
        p_rich = ml_probs[1] if len(ml_probs) > 1 else 0.0
        h_fuel -= (p_lean * 15.0 + p_rich * 15.0)
        if final_flags.get("fault_lean_trim") or final_flags.get("fault_rich_trim"):
            h_fuel -= 12.0
        fuel_health = float(np.clip(h_fuel, 10.0, 100.0))

        # 2. ELECTRICAL SYSTEM HEALTH (Continuous charging curve & diode ripple)
        h_elec = 100.0
        if v_val >= 14.0:
            h_elec -= max(0.0, (14.05 - v_val) * 2.0)
        elif v_val >= 13.5:
            h_elec -= (14.0 - v_val) * 8.0
        elif v_val >= 12.8:
            h_elec -= 4.0 + (13.5 - v_val) * 22.0
        elif v_val >= 12.45:
            h_elec -= 19.4 + (12.8 - v_val) * 45.0
        else:
            h_elec -= 35.15 + (12.45 - v_val) * 60.0
            
        p_sag = ml_probs[3] if len(ml_probs) > 3 else 0.0
        h_elec -= p_sag * 16.0
        if final_flags.get("fault_alternator_sag"):
            h_elec -= 12.0
        charging_health = float(np.clip(h_elec, 10.0, 100.0))

        # 3. THERMAL SYSTEM HEALTH (Continuous Newton cooling curve & ram-air speed penalty)
        h_therm = 100.0
        if 84.0 <= coolant_val <= 94.0:
            h_therm -= max(0.0, abs(coolant_val - 88.0) - 2.0) * 0.3
        elif 72.0 <= coolant_val < 84.0:
            h_therm -= 1.2 + (84.0 - coolant_val) * 1.2
        elif coolant_val < 72.0:
            h_therm -= 15.6 + (72.0 - coolant_val) * 1.6
            if speed_val > 30.0:
                h_therm -= min(15.0, (speed_val / 40.0) * 4.5)
        else:
            h_therm -= (coolant_val - 94.0) * 3.0
            
        p_therm = ml_probs[4] if len(ml_probs) > 4 else 0.0
        h_therm -= p_therm * 16.0
        if final_flags.get("fault_thermal_delay"):
            h_therm -= 12.0
        thermal_health = float(np.clip(h_therm, 10.0, 100.0))

        # 4. ENGINE & COMBUSTION HEALTH (Continuous RPM envelope, throttle-load mechanical match, and surge)
        throttle_val = f.get('throttle_mean', 4.0)
        load_val = f.get('engine_load_mean', 18.0)
        h_comb = 100.0
        
        # RPM Operating Envelope (Lugging/stall risk vs high-rev mechanical stress)
        if rpm_val < 700.0:
            h_comb -= (700.0 - rpm_val) * 0.45  # Stalling / lugging threshold
        elif rpm_val > 3200.0:
            h_comb -= min(18.0, (rpm_val - 3200.0) * 0.015)  # High RPM stress
            if speed_val < 10.0:
                h_comb -= min(12.0, (rpm_val - 3200.0) * 0.01) # Stationary over-revving penalty
                
        # Throttle vs Engine Load Consistency (Mechanical binding drag vs choked power loss)
        expected_load = 14.0 + (throttle_val * 0.75)
        load_mismatch = abs(load_val - expected_load)
        if load_mismatch > 15.0:
            # Detects high drag at closed throttle (converter/accessory drag) or low power at WOT (choked intake)
            h_comb -= min(25.0, (load_mismatch - 15.0) * 0.55)
            
        # Dynamic Air Breathing / Volumetric Consistency
        if maf_dev > 0.12:
            h_comb -= min(12.0, (maf_dev - 0.12) * 18.0)
            
        # Rotational Torque Stability & Cylinder Misfire Jitter
        rpm_jitter = max(0.0, rpm_std_val - 6.0)
        h_comb -= min(15.0, rpm_jitter * 0.35)
        
        # Periodic Surge / Autocorrelation Hunting Metric
        if hunt_metric_val > 5.0:
            h_comb -= min(35.0, (hunt_metric_val / 40.0) * 35.0)
            
        p_hunt = ml_probs[2] if len(ml_probs) > 2 else 0.0
        h_comb -= p_hunt * 16.0
        if final_flags.get("fault_combustion_hunting"):
            h_comb -= 14.0
        combustion_health = float(np.clip(h_comb, 10.0, 100.0))

        # COMPOSITE VHI WITH CROSS-SYSTEM COMPOUND PENALTY
        vhi_raw = (0.30 * fuel_health) + (0.25 * combustion_health) + (0.25 * charging_health) + (0.20 * thermal_health)
        degraded_count = sum(1 for s in [fuel_health, combustion_health, charging_health, thermal_health] if s < 85.0)
        if degraded_count > 1:
            vhi_raw -= (degraded_count - 1) * 3.5

        vhi = round(float(np.clip(vhi_raw, 10.0, 100.0)), 1)
        grade = "A+ (Flawless)" if vhi >= 95.0 else "A (Excellent)" if vhi >= 85.0 else "B (Satisfactory)" if vhi >= 70.0 else "C (Needs Attention)"

        return HybridDiagnosticResult(
            vehicle_health_index=vhi,
            health_grade=grade,
            subsystem_scores={
                "fuel_system": round(fuel_health, 1),
                "combustion": round(combustion_health, 1),
                "electrical": round(charging_health, 1),
                "thermal": round(thermal_health, 1)
            },
            fault_flags=final_flags,
            candidates=[asdict(c) for c in candidates],
            detection_summary=sources
        )


# Singleton Instance
_engine_instance = None

def get_hybrid_engine() -> HybridDiagnosticEngine:
    global _engine_instance
    if _engine_instance is None:
        _engine_instance = HybridDiagnosticEngine()
    return _engine_instance
