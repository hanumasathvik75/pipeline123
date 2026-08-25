"""Fuse available virtual sensors into VHI and evidence-backed candidate faults."""
from __future__ import annotations

from typing import Any

from .virtual_sensors import SensorResult


def fuse(results: list[SensorResult], config: dict[str, Any]) -> dict[str, Any]:
    weights = config["vhi_weights"]; usable = [r for r in results if r.health_score is not None]
    total_weight = sum(weights.get(r.name, 0) for r in usable)
    vhi = sum(r.health_score * weights.get(r.name, 0) for r in usable) / total_weight if total_weight else None
    faults = []
    for result in results:
        if result.flagged and result.candidate:
            confidence = min(0.98, max(config["fusion"]["minimum_confidence"], (100 - result.health_score) / 100 * 0.7 + 0.35))
            faults.append({"candidate": result.candidate, "confidence": round(confidence, 2), "contributing_indices": [result.name], "evidence": result.evidence})
    faults.sort(key=lambda item: item["confidence"], reverse=True)
    return {"vehicle_health_index": round(float(vhi), 2) if vhi is not None else None, "available_indices": [r.name for r in usable], "fault_candidates": faults}
