"""Deterministic, evidence-gated workshop procedures for supported candidates."""
from __future__ import annotations

from typing import Any


PROCEDURES: dict[str, list[dict[str, str]]] = {
    "misfire_like_combustion_irregularity": [
        {"stage": "Verify", "action": "Reproduce and localize the irregularity", "method": "Warm the engine, record idle and 1,500-2,500 rpm data, then check OEM misfire counters and stored/pending DTCs by cylinder.", "pass": "No counter rises repeatedly under the same operating condition."},
        {"stage": "Ignition", "action": "Test the ignition path", "method": "Inspect plugs for gap, deposits and cracking; compare coil primary/secondary behavior or swap a suspect coil between cylinders when OEM procedure permits.", "pass": "The fault does not follow a swapped coil and spark waveform/dwell meet service data."},
        {"stage": "Fuel", "action": "Check injector contribution", "method": "Run cylinder balance or injector cut-out testing; compare injector current ramp and rail-pressure stability before replacing parts.", "pass": "Cylinder contribution is balanced and pressure remains within OEM tolerance."},
        {"stage": "Mechanical", "action": "Exclude compression and valve faults", "method": "If ignition and fuel pass, perform relative compression followed by leak-down on the affected cylinder(s).", "pass": "Cylinder variation and leakage are within manufacturer limits."},
        {"stage": "Validate", "action": "Confirm the repair", "method": "Clear learned fault state only after repair, repeat the original drive condition, and compare misfire counters plus IgnitionAI combustion health.", "pass": "No recurring DTC/counter rise and the virtual-sensor flag clears on a comparable log."},
    ],
    "air_path_or_throttle_response_lag": [
        {"stage": "Verify", "action": "Confirm commanded-versus-actual lag", "method": "Log accelerator request, throttle command/position, MAP/MAF and RPM during repeatable tip-in events; check throttle and boost-related DTCs.", "pass": "Actual response tracks command within OEM response tolerance."},
        {"stage": "Air path", "action": "Inspect for restriction or leakage", "method": "Inspect the air filter and ducting; smoke-test intake/charge piping and verify clamps, hoses and intercooler joints where fitted.", "pass": "No smoke leak, collapse or restriction is present."},
        {"stage": "Actuator", "action": "Test throttle/boost actuation", "method": "Use bidirectional scan-tool actuation where supported and compare commanded position with feedback; inspect connector power, ground and signal integrity.", "pass": "Feedback follows command smoothly with no dropout or binding."},
        {"stage": "Sensor", "action": "Validate load sensors", "method": "Key-on/engine-off MAP should agree with barometric pressure; inspect MAF plausibility against engine displacement and operating point using OEM data.", "pass": "MAP/MAF values are plausible and stable across the test range."},
        {"stage": "Validate", "action": "Repeat the transient test", "method": "Re-run the same throttle events and compare estimated response lag against the original recording.", "pass": "Lag is at or below the configured/OEM limit with no returning code."},
    ],
    "cooling_system_warmup_or_stability_anomaly": [
        {"stage": "Verify", "action": "Validate coolant temperature measurement", "method": "After a cold soak, compare ECT with intake-air temperature and ambient; cross-check warm readings with a calibrated surface/infrared measurement at the specified location.", "pass": "Cold values agree closely and warm readings track without jumps."},
        {"stage": "Basic checks", "action": "Inspect coolant circuit condition", "method": "When cold, verify level and mixture; pressure-test for external leakage and inspect cap, hoses and trapped-air symptoms.", "pass": "Correct level/concentration, stable pressure and no trapped air or leakage."},
        {"stage": "Control", "action": "Check thermostat and fan operation", "method": "Observe hose-temperature transition during warm-up and command fans with a scan tool where supported; verify relay/current behavior.", "pass": "Thermostat transition and fan enable points match service specifications."},
        {"stage": "Flow", "action": "Evaluate circulation", "method": "If instability remains, inspect radiator temperature distribution and pump flow/drive condition using OEM diagnostic procedure.", "pass": "Uniform heat rejection and adequate circulation are confirmed."},
        {"stage": "Validate", "action": "Repeat cold-start warm-up", "method": "Capture a full cold-to-operating-temperature log and compare warm-up slope and steady-state variation.", "pass": "Warm-up rate and steady-state variation return inside configured limits."},
    ],
    "air_intake_vacuum_leak_or_maf_drift": [
        {"stage": "Verify", "action": "Characterize fuel-trim direction", "method": "Compare STFT/LTFT at hot idle and near 2,500 rpm; review front oxygen/air-fuel sensor response and related DTCs.", "pass": "Combined trim remains within OEM limits at both operating points."},
        {"stage": "Leak test", "action": "Test unmetered-air paths", "method": "Smoke-test intake manifold, PCV, brake-booster and purge plumbing; isolate the purge valve if its flow is suspect.", "pass": "No smoke escape and commanded-closed purge flow is absent."},
        {"stage": "MAF", "action": "Validate air-mass measurement", "method": "Inspect contamination and wiring, then compare MAF output with OEM expected values; clean only with approved MAF cleaner if specified.", "pass": "MAF signal is stable and plausible across idle and loaded operation."},
        {"stage": "Validate", "action": "Confirm trim correction", "method": "After an evidence-confirmed repair, reset adaptations only if OEM procedure calls for it and repeat the same steady-state test.", "pass": "Trim and oxygen-sensor behavior normalize without returning codes."},
    ],
    "injector_or_fuel_pressure_overfueling": [
        {"stage": "Verify", "action": "Confirm rich operation", "method": "Compare both banks, oxygen/air-fuel response, fuel pressure and purge command; inspect oil for fuel dilution before prolonged running.", "pass": "Mixture feedback and pressure remain within OEM limits."},
        {"stage": "Pressure", "action": "Test pressure regulation", "method": "Measure commanded and actual rail pressure plus residual pressure decay using the specified safety procedure.", "pass": "Running and residual pressure satisfy service data."},
        {"stage": "Injector", "action": "Check leakage and contribution", "method": "Perform injector balance/leak-down and current-ramp tests; inspect only cylinders indicated by the evidence.", "pass": "Pressure drop and electrical waveform are balanced across injectors."},
        {"stage": "Validate", "action": "Confirm mixture correction", "method": "Repair only the failed circuit/component, then repeat hot-idle and cruise trim tests and check for fuel dilution.", "pass": "Fuel trims stabilize and no rich-condition code returns."},
    ],
    "charging_system_voltage_stress": [
        {"stage": "Safety", "action": "Inspect battery and high-current connections", "method": "Check terminal security, corrosion and cable condition with ignition off; follow EV/high-voltage isolation rules where applicable.", "pass": "Connections are clean, secure and undamaged."},
        {"stage": "Battery", "action": "Test battery state and capacity", "method": "Measure open-circuit voltage, conductance/capacity and cranking voltage using the battery maker's procedure.", "pass": "State of charge and measured capacity meet specification."},
        {"stage": "Charging", "action": "Measure regulated output and ripple", "method": "Test at idle and electrical load; voltage-drop test positive and ground paths and measure AC ripple at the battery.", "pass": "Voltage, cable drop and ripple remain within OEM limits."},
        {"stage": "Validate", "action": "Repeat loaded charging test", "method": "After correcting the proven failure, repeat the same load profile and compare the IgnitionAI charging index.", "pass": "Stable voltage with no stress flag or charging DTC."},
    ],
}


def build_repair_plan(fault_candidates: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Return only procedures corresponding to evidence-backed candidates."""
    plans: list[dict[str, Any]] = []
    for rank, fault in enumerate(fault_candidates, start=1):
        candidate = fault.get("candidate")
        steps = PROCEDURES.get(candidate)
        if not steps:
            continue
        plans.append({
            "rank": rank,
            "candidate": candidate,
            "confidence": fault.get("confidence"),
            "evidence": fault.get("evidence", {}),
            "steps": steps,
        })
    return plans
