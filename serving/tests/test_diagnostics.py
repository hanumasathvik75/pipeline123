from pathlib import Path

from serving.diagnostics.pipeline import analyze_file
from serving.diagnostics.synthetic import generate_synthetic_telemetry


DATASET = Path(r"C:\Users\SHASHANK\Downloads\Ignitionaidata\dataset.csv")


def test_opel_dataset_runs_real_virtual_sensors() -> None:
    if not DATASET.exists():
        return
    result = analyze_file(str(DATASET))
    sensors = {sensor["name"]: sensor for sensor in result["virtual_sensors"]}
    assert result["source"]["rows"] > 1000
    assert all(sensor["status"] in {"normal", "flagged", "insufficient_data"} for sensor in sensors.values())
    assert all((sensor["status"] == "flagged") == sensor["flagged"] for sensor in sensors.values() if sensor["status"] != "insufficient_data")
    assert result["fusion"]["vehicle_health_index"] is not None
    assert len(result["repair_plan"]) == len(result["fusion"]["fault_candidates"])
    assert all(plan["steps"] for plan in result["repair_plan"])


def test_synthetic_generator_marks_provenance_and_separates_scenarios(tmp_path: Path) -> None:
    healthy = analyze_file(str(generate_synthetic_telemetry(tmp_path, "healthy", 120, 5, 7)))
    faulty = analyze_file(str(generate_synthetic_telemetry(tmp_path, "combined", 120, 5, 7)))
    assert healthy["source"]["synthetic"] is True
    assert faulty["source"]["synthetic"] is True
    assert not healthy["fusion"]["fault_candidates"]
    assert faulty["fusion"]["fault_candidates"]
    assert faulty["fusion"]["vehicle_health_index"] < healthy["fusion"]["vehicle_health_index"]
