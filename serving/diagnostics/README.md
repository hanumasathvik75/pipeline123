# IgnitionAI telemetry diagnostic engine

This is the durable core of the v0.1 diagnostic engine. `ingestion.py` maps
real CSV/JSON telemetry into a canonical PID schema; `virtual_sensors.py`
derives sensor indices from signal mathematics; `fusion.py` produces the VHI
and ranked evidence-backed candidates; `pipeline.py` orchestrates them.

All tunable PID aliases, thresholds, window sizes, and VHI weights are in
`config/defaults.json`. A missing PID yields `insufficient_data`; it never
creates a made-up score. The API persists virtual-sensor output and structured
diagnoses when a diagnostic session is supplied.

The language model is not an engine dependency. It may only receive a completed
structured diagnosis for a mechanic-readable explanation. Demo fault injection,
if required later, must live outside this package under a separately labeled
`demo/` module.
