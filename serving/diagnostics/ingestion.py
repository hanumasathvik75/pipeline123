"""Source-aware OBD telemetry normalization with no fabricated PIDs."""
from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import pandas as pd


@dataclass
class NormalizedTelemetry:
    frame: pd.DataFrame
    source_path: str
    source_columns: list[str]
    sample_interval_seconds: float | None


def load_config() -> dict[str, Any]:
    path = Path(__file__).parent / "config" / "defaults.json"
    return json.loads(path.read_text(encoding="utf-8"))


def load_and_normalize(source_path: str | Path) -> NormalizedTelemetry:
    """Read CSV/JSON telemetry and map known source columns to canonical names."""
    path = Path(source_path)
    if path.suffix.lower() == ".csv":
        raw = pd.read_csv(path, low_memory=False, na_values=["NA", "", "null"])
    elif path.suffix.lower() in {".json", ".jsonl"}:
        raw = pd.read_json(path, lines=path.suffix.lower() == ".jsonl")
    else:
        raise ValueError("Only CSV, JSON, and JSONL telemetry files are supported.")
    if raw.empty:
        raise ValueError("The telemetry file contains no rows.")

    config = load_config()
    normalized = pd.DataFrame(index=raw.index)
    source_columns = raw.columns.tolist()
    for canonical, aliases in config["pid_aliases"].items():
        actual = next((name for name in aliases if name in raw.columns), None)
        if actual is not None:
            normalized[canonical] = raw[actual]

    if "timestamp" not in normalized:
        normalized["timestamp"] = pd.RangeIndex(len(raw), name="timestamp").astype(float)
    else:
        parsed = pd.to_datetime(normalized["timestamp"], errors="coerce")
        if parsed.notna().sum() >= max(2, len(raw) // 2):
            normalized["timestamp"] = parsed
        else:
            normalized["timestamp"] = pd.RangeIndex(len(raw), name="timestamp").astype(float)

    for column in normalized.columns:
        if column not in {"timestamp", "source_segment"}:
            normalized[column] = pd.to_numeric(normalized[column], errors="coerce")
    normalized = normalized.dropna(how="all", subset=[c for c in normalized if c != "timestamp"])
    normalized = normalized.sort_values("timestamp").reset_index(drop=True)
    interval: float | None = None
    if pd.api.types.is_datetime64_any_dtype(normalized["timestamp"]):
        delta = normalized["timestamp"].diff().dt.total_seconds().dropna()
        if not delta.empty:
            interval = float(delta.median())
    return NormalizedTelemetry(normalized, str(path), source_columns, interval)


def select_continuous_session(frame: pd.DataFrame, requested_segment: str | None = None) -> tuple[pd.DataFrame, str | None]:
    """Choose one continuous trip; statistics must not bridge independent drives."""
    if "source_segment" not in frame:
        return frame, None
    labels = frame["source_segment"].astype("string")
    if requested_segment is not None:
        selected = frame[labels == requested_segment].copy()
        if selected.empty:
            raise ValueError(f"No telemetry rows match source segment '{requested_segment}'.")
        return selected.reset_index(drop=True), requested_segment
    valid = labels.dropna()
    if valid.empty:
        return frame, None
    chosen = str(valid.value_counts().index[0])
    return frame[labels == chosen].copy().reset_index(drop=True), chosen
