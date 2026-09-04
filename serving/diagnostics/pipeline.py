"""Orchestrates real telemetry analysis; optional explanation is intentionally separate."""
from __future__ import annotations

from typing import Any

from .fusion import fuse
from .graph_loader import enrich_candidates_from_graph
from .ingestion import load_and_normalize, load_config, select_continuous_session
from .repair_plan import build_repair_plan
from .virtual_sensors import charging_stress, combustion_irregularity, fuel_system_health, load_response_lag, thermal_stress


def analyze_file(source_path: str, source_segment: str | None = None) -> dict[str, Any]:
    config = load_config(); telemetry = load_and_normalize(source_path); frame, selected_segment = select_continuous_session(telemetry.frame, source_segment)
    results = [combustion_irregularity(frame, config["combustion"]), fuel_system_health(frame, config["fuel"]), thermal_stress(frame, config["thermal"]), load_response_lag(frame, config["load_response"]), charging_stress(frame, config["charging"])]
    preview_columns = [name for name in ("rpm", "coolant_c", "voltage_v", "ltft_b1_pct") if name in frame]
    stride = max(1, len(frame) // 160)
    preview = frame.iloc[::stride][["timestamp", *preview_columns]].copy()
    preview["timestamp"] = preview["timestamp"].astype(str)
    fusion = fuse(results, config)
    fusion["fault_candidates"] = enrich_candidates_from_graph(fusion["fault_candidates"])
    return {"source": {"path": telemetry.source_path, "rows": len(frame), "selected_segment": selected_segment, "source_columns": telemetry.source_columns, "canonical_pids": [c for c in frame.columns if c not in {"timestamp", "source_segment"}], "sample_interval_seconds": telemetry.sample_interval_seconds, "synthetic": "synthetic_scenario" in telemetry.source_columns}, "raw_signal_preview": preview.to_dict(orient="records"), "virtual_sensors": [result.as_dict() for result in results], "fusion": fusion, "repair_plan": build_repair_plan(fusion["fault_candidates"])}

