"""
IgnitionAI — Diagnostic Engine
Full rule-based ML pipeline from demo.py, refactored as a clean module.
"""

import numpy as np
from collections import defaultdict
from typing import Dict, List, Tuple, Optional

try:
    from sklearn.tree import DecisionTreeClassifier
    from sklearn.ensemble import IsolationForest
    SKLEARN_AVAILABLE = True
except Exception:
    SKLEARN_AVAILABLE = False

# Import the data layer
from app.data.dtc_database import (
    obd2_dtc_diagnostic_database,
    dtc_root_cause_repairs,
    vehicle_problems,
)


# ─── Core Scoring Functions ────────────────────────────────────────────────────

def sensor_in_expected_range(sensor_name: str, reading: float, dtc_spec: dict) -> Optional[float]:
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


def score_dtcs_from_dtc_sensor_match(
    dtcs: List[str],
    sensor_values: Dict[str, float],
    dtc_db: dict,
) -> List[dict]:
    """
    Part One: Score DTCs by matching sensor plausibility & points_weight.
    Returns sorted list of {dtc, score, avg_plaus, points_weight, details}.
    """
    results = []
    for dtc in dtcs:
        spec = dtc_db.get(dtc)
        if not spec:
            continue
        sw = spec.get("points_weight", 1)
        sensors = spec.get("sensors", {})
        plaus_sum = 0.0
        count = 0
        details = {}
        for sname in sensors:
            if sname in sensor_values:
                val = sensor_values[sname]
                plaus = sensor_in_expected_range(sname, val, spec)
                if plaus is not None:
                    plaus_sum += plaus
                    count += 1
                    details[sname] = {
                        "value": val,
                        "plaus": plaus,
                        "exp_min": sensors[sname]["min_reading"],
                        "exp_max": sensors[sname]["max_reading"],
                    }
        avg_plaus = (plaus_sum / count) if count > 0 else 0.5
        final_score = avg_plaus * sw
        results.append({
            "dtc": dtc,
            "score": final_score,
            "avg_plaus": avg_plaus,
            "points_weight": sw,
            "description": spec.get("description", ""),
            "details": details,
        })
    results.sort(key=lambda x: x["score"], reverse=True)
    return results


def find_sensor_anomalies(
    sensor_values: Dict[str, float],
    historical_matrix: Optional[np.ndarray] = None,
) -> Dict[str, float]:
    """Detect anomalous sensors using IsolationForest or z-score fallback."""
    anomalies: Dict[str, float] = {}
    if (
        SKLEARN_AVAILABLE
        and historical_matrix is not None
        and historical_matrix.shape[0] > 10
    ):
        iso = IsolationForest(contamination=0.05, n_estimators=50, random_state=42)
        iso.fit(historical_matrix)
        latest = historical_matrix[-1].reshape(1, -1)
        scores = iso.decision_function(latest)[0]
        inv = -scores
        for idx, name in enumerate(sensor_values.keys()):
            anomalies[name] = float(inv[idx]) if idx < len(inv) else 0.0
    else:
        vals = np.array(list(sensor_values.values()), dtype=float)
        med = np.median(vals)
        mad = np.median(np.abs(vals - med)) + 1e-6
        for k, v in sensor_values.items():
            anomalies[k] = float(abs(v - med) / mad)

    maxv = max(anomalies.values()) if anomalies else 1.0
    for k in anomalies:
        anomalies[k] = max(0.05, anomalies[k] / maxv)
    return anomalies


def rank_dtcs_by_sensor_anomaly_correlation(
    dtcs: List[str],
    sensor_anomalies: Dict[str, float],
    dtc_db: dict,
    sensor_values: Dict[str, float],
) -> List[dict]:
    """Part Two: Reverse correlate DTCs — reward those covering most anomalous sensors."""
    dtc_scores: Dict[str, float] = {}
    for dtc in dtcs:
        spec = dtc_db.get(dtc, {})
        sensors = spec.get("sensors", {})
        score = 0.0
        for sname in sensors:
            if sname in sensor_anomalies:
                score += sensor_anomalies[sname] * spec.get("points_weight", 1)
        dtc_scores[dtc] = score

    return sorted(
        [{"dtc": d, "score": s} for d, s in dtc_scores.items()],
        key=lambda x: x["score"],
        reverse=True,
    )


def rank_root_causes_from_dtcs(
    dtcs: List[str],
    dtc_db: dict,
    dtc_root_repairs: dict,
) -> List[dict]:
    """Rank root causes aggregated from all input DTCs."""
    root_scores: Dict[str, float] = defaultdict(float)
    for dtc in dtcs:
        spec = dtc_db.get(dtc, {})
        sw = spec.get("points_weight", 1)
        repairs = dtc_root_repairs.get(dtc, [])
        for position, entry in enumerate(repairs):
            root = entry.get("root_cause")
            if root:
                position_score = sw * (1.0 + (len(repairs) - position) / (len(repairs) + 1))
                root_scores[root] += position_score
    return sorted(
        [{"root": k, "score": v} for k, v in root_scores.items()],
        key=lambda x: x["score"],
        reverse=True,
    )


def decide_between_answers(
    answerA: dict,
    answerB: dict,
    sensor_values: Dict[str, float],
    dtc_db: dict,
) -> dict:
    """Fuse Answer A and Answer B using consensus or DecisionTree."""
    topA = answerA["root_ranking"][0] if answerA["root_ranking"] else None
    topB = answerB["root_ranking"][0] if answerB["root_ranking"] else None
    explanation = []

    if topA and topB and topA["root"] == topB["root"]:
        explanation.append("Both pipelines agree — strong consensus found.")
        return {"final_root": topA["root"], "method": "consensus", "explanation": explanation, "confidence": 0.92}

    topA_score = answerA["dtc_rankings"][0]["score"] if answerA["dtc_rankings"] else 0.0
    topB_score = answerB["dtc_rankings"][0]["score"] if answerB["dtc_rankings"] else 0.0
    feat = np.array([[topA_score, topB_score]])

    if SKLEARN_AVAILABLE:
        X = np.array([[0.9, 0.2], [0.2, 0.9], [0.8, 0.8], [0.1, 0.1]])
        y = np.array([0, 1, 0, 1])
        clf = DecisionTreeClassifier(max_depth=3, random_state=42)
        clf.fit(X, y)
        pred = clf.predict(feat)[0]
        explanation.append(f"Decision tree chose {'B' if pred == 1 else 'A'} (scores A={topA_score:.2f}, B={topB_score:.2f})")
        chosen = topB["root"] if pred == 1 and topB else (topA["root"] if topA else None)
        confidence = topB_score / (topA_score + topB_score + 1e-6) if pred == 1 else topA_score / (topA_score + topB_score + 1e-6)
        return {"final_root": chosen, "method": "DecisionTree", "explanation": explanation, "confidence": round(confidence, 2)}
    else:
        if topB_score > topA_score * 1.2:
            explanation.append("Heuristic: anomaly correlation strongly favors B.")
            chosen = topB["root"] if topB else (topA["root"] if topA else None)
            confidence = 0.65
        else:
            explanation.append("Heuristic: sensor plausibility favors A.")
            chosen = topA["root"] if topA else (topB["root"] if topB else None)
            confidence = 0.70
        return {"final_root": chosen, "method": "heuristic", "explanation": explanation, "confidence": confidence}


def get_repair_steps_for_root(
    dtcs: List[str],
    root_cause: str,
    dtc_root_repairs: dict,
) -> List[str]:
    """Find repair steps for a given root cause across all DTCs."""
    for dtc in dtcs:
        for entry in dtc_root_repairs.get(dtc, []):
            if entry.get("root_cause") == root_cause:
                return entry.get("repair_steps", [])
    return []


# ─── Main Analysis Pipeline ────────────────────────────────────────────────────

def run_full_analysis(
    dtcs: List[str],
    sensor_values: Dict[str, float],
    historical_matrix: Optional[np.ndarray] = None,
) -> dict:
    """
    Run the complete two-part diagnostic pipeline.
    Returns a comprehensive result dict.
    """
    # Part One: DTC→sensor plausibility scoring
    dtc_rankings = score_dtcs_from_dtc_sensor_match(
        dtcs, sensor_values, obd2_dtc_diagnostic_database
    )
    root_rankA = rank_root_causes_from_dtcs(
        [d["dtc"] for d in dtc_rankings],
        obd2_dtc_diagnostic_database,
        dtc_root_cause_repairs,
    )
    answerA = {"dtc_rankings": dtc_rankings, "root_ranking": root_rankA}

    # Part Two: Sensor anomaly correlation
    sensor_anoms = find_sensor_anomalies(sensor_values, historical_matrix)
    rev_dtcs = rank_dtcs_by_sensor_anomaly_correlation(
        dtcs, sensor_anoms, obd2_dtc_diagnostic_database, sensor_values
    )
    root_rankB = rank_root_causes_from_dtcs(
        [d["dtc"] for d in rev_dtcs],
        obd2_dtc_diagnostic_database,
        dtc_root_cause_repairs,
    )
    answerB = {"dtc_rankings": rev_dtcs, "root_ranking": root_rankB}

    # Decision fusion
    final_decision = decide_between_answers(
        answerA, answerB, sensor_values, obd2_dtc_diagnostic_database
    )

    # Collect repair steps for final root cause
    final_root = final_decision.get("final_root", "")
    repair_steps = get_repair_steps_for_root(dtcs, final_root, dtc_root_cause_repairs)

    return {
        "dtc_rankings": dtc_rankings,
        "root_rankA": root_rankA,
        "rev_dtcs": rev_dtcs,
        "root_rankB": root_rankB,
        "sensor_anomalies": sensor_anoms,
        "final_decision": final_decision,
        "final_root": final_root,
        "repair_steps": repair_steps,
        "sklearn_available": SKLEARN_AVAILABLE,
    }
