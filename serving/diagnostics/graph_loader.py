"""Load knowledge graph YAML files and bridge them into the diagnostic pipeline."""
from __future__ import annotations

from pathlib import Path
from typing import Any

import yaml


_SUBSYSTEMS_DIR = Path(__file__).parents[2] / "knowledge_graph" / "subsystems"
_graph_cache: dict[str, Any] | None = None


def load_all_subsystems() -> list[dict[str, Any]]:
    """Load and parse all YAML subsystem files from the knowledge graph."""
    global _graph_cache
    if _graph_cache is not None:
        return list(_graph_cache.values())

    _graph_cache = {}
    if not _SUBSYSTEMS_DIR.exists():
        return []

    for path in sorted(_SUBSYSTEMS_DIR.glob("*.yaml")):
        try:
            data = yaml.safe_load(path.read_text(encoding="utf-8"))
            if data and "subsystem_name" in data:
                _graph_cache[data["subsystem_name"]] = data
        except Exception:
            continue
    return list(_graph_cache.values())


def get_dtc_to_fault_modes() -> dict[str, list[dict[str, Any]]]:
    """Build a reverse index: DTC code → list of fault modes that reference it.

    This lets the DTC engine query the knowledge graph for additional context
    beyond the hardcoded repair procedures.
    """
    subsystems = load_all_subsystems()
    index: dict[str, list[dict[str, Any]]] = {}
    for subsystem in subsystems:
        for component in subsystem.get("components", []):
            for fault_mode in component.get("fault_modes", []):
                entry = {
                    "subsystem": subsystem["subsystem_name"],
                    "component": component["name"],
                    "fault_mode": fault_mode["name"],
                    "description": fault_mode.get("description", ""),
                    "prior_probability": fault_mode.get("prior_probability", 0.0),
                    "typical_symptoms": fault_mode.get("typical_symptoms", []),
                    "typical_pids": fault_mode.get("typical_pids", []),
                }
                for dtc in fault_mode.get("typical_dtcs", []):
                    index.setdefault(dtc, []).append(entry)
    return index


def enrich_candidates_from_graph(
    fault_candidates: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """Add knowledge graph context to fault candidates when available.

    Looks up DTCs referenced in candidate evidence and attaches subsystem,
    component, and symptom information from the YAML graph.
    """
    dtc_index = get_dtc_to_fault_modes()
    if not dtc_index:
        return fault_candidates

    for candidate in fault_candidates:
        # Check if any contributing DTC indices match known graph entries
        contributing = candidate.get("contributing_indices", [])
        graph_context = []
        for dtc in contributing:
            if dtc in dtc_index:
                graph_context.extend(dtc_index[dtc])
        if graph_context:
            candidate["knowledge_graph"] = graph_context
    return fault_candidates
