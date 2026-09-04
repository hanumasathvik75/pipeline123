"""
IgnitionAI — AI Diagnostic & Repair Narrative Engine
Powered by NVIDIA NIM LLM.
"""

from openai import OpenAI
from app.config import NVIDIA_API_KEY, NVIDIA_MODEL_NAME


def _get_client():
    """Return a configured OpenAI client for Nvidia NIM API."""
    return OpenAI(base_url="https://integrate.api.nvidia.com/v1", api_key=NVIDIA_API_KEY)


def generate_plain_english_diagnosis(
    dtcs: list[str],
    final_root_cause: str,
    sensor_values: dict,
    vehicle_info: str = "Patient Vehicle",
) -> str:
    """
    Generate a comprehensive, structured, mechanic-friendly diagnosis summary.
    """
    sensor_lines = [
        f"  - {k.replace('_', ' ').title()}: {v}"
        for k, v in list(sensor_values.items())[:12]
    ]
    sensor_summary = "\n".join(sensor_lines) if sensor_lines else "  - No abnormal live sensor deviations detected."
    dtc_list = ", ".join(dtcs) if dtcs else "None detected (Sensor Drift / Heuristic Match)"

    prompt = f"""You are IgnitionAI, the premier expert automotive diagnostic AI specialist for workshops.
A full diagnostic scan has concluded. Generate a comprehensive, crystal-clear, structured technical diagnostic report.

Vehicle Context: {vehicle_info}
Active Fault Codes (DTCs): {dtc_list}
Primary Fault Candidate / Root Cause: {final_root_cause}

Live Sensor Snapshot:
{sensor_summary}

Structure your response with the following sections and clean markdown formatting:

### 1. Executive Summary
Provide a plain-English explanation of what has failed in the vehicle and how it affects driveability and engine performance.

### 2. Root-Cause Analysis & Telemetry Correlation
Explain the technical failure mechanism. Correlate the specific DTCs and sensor values (fuel trims, temperatures, pressures, or timing) to prove why this root cause was diagnosed.

### 3. Severity & Safety Risk
- **Urgency Level:** (Critical / High / Medium / Low)
- **Drivability:** Can the vehicle be driven safely, or does it risk secondary damage (e.g., catalytic converter meltdown, engine stall, limp mode)?

### 4. Technician Verification Checklist
List 3-4 specific electrical or mechanical multimeter/scope checks to verify the faulty component before replacement.
"""

    try:
        client = _get_client()
        completion = client.chat.completions.create(
            model=NVIDIA_MODEL_NAME,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.2,
            max_tokens=1024,
            stream=False
        )
        return completion.choices[0].message.content.strip()
    except Exception as e:
        return (
            f"### Executive Summary\n"
            f"The vehicle diagnostic engine has identified **{final_root_cause}** as the primary root cause based on active fault codes ({dtc_list}).\n\n"
            f"### Sensor & Fault Context\n"
            f"- **Fault Codes:** {dtc_list}\n"
            f"- **Primary Component:** {final_root_cause}\n"
            f"- **AI Status:** Remote AI service encountered a temporary error ({e}). Rule-based analysis active.\n\n"
            f"### Recommended Technician Action\n"
            f"Perform standard visual and electrical inspection on {final_root_cause} circuits."
        )


def generate_repair_narrative(
    root_cause: str,
    dtcs: list[str],
    repair_steps: list[str],
) -> str:
    """
    Generate a detailed, step-by-step professional workshop repair guide.
    """
    steps_text = "\n".join(f"{i+1}. {s}" for i, s in enumerate(repair_steps)) if repair_steps else "1. Perform guided visual and component electrical bench test."
    dtc_list = ", ".join(dtcs) if dtcs else "None"

    prompt = f"""You are IgnitionAI, a master workshop technical advisor.
Generate an actionable, deeply detailed automotive repair and replacement guide for a certified technician.

Target Root Cause: {root_cause}
Associated Fault Codes: {dtc_list}
Baseline Steps Identified:
{steps_text}

Format your guide cleanly with these exact sections:

### 🛠️ Required Tools & Replacement Parts
- List essential workshop tools (sockets, multimeter, torque wrench, scan tool, etc.)
- List replacement components and consumables (gaskets, fluids, OEM part recommendations)

### 📋 Step-by-Step Workshop Repair Procedure
Provide a detailed, numbered sequence of technical actions (Disassembly, Component Replacement, Reassembly, ECU Adaptation/Reset).

### ⚠️ Critical Safety & Technical Warnings
List electrical, thermal, or high-pressure safety precautions the mechanic must observe.

### ⏱️ Estimates & Difficulty
- **Estimated Labor Time:** (e.g. 1.5 - 2.5 hours)
- **Difficulty Rating:** (Beginner / Intermediate / Advanced / Master Technician)
- **Post-Repair Calibration:** Specify if ECU reset, adaptation, or road cycle is required.
"""

    try:
        client = _get_client()
        completion = client.chat.completions.create(
            model=NVIDIA_MODEL_NAME,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.2,
            max_tokens=1200,
            stream=False
        )
        return completion.choices[0].message.content.strip()
    except Exception as e:
        fallback_steps = "\n".join(f"{i+1}. {s}" for i, s in enumerate(repair_steps)) if repair_steps else "1. Inspect component wiring and replace failed unit."
        return (
            f"### 🛠️ Repair Procedure for {root_cause}\n\n"
            f"{fallback_steps}\n\n"
            f"### ⚠️ Note\n"
            f"Remote AI narrative currently unavailable ({e}). Follow standard OEM service manual specs for torque and clearances."
        )


def generate_pdf_summary(
    dtcs: list[str],
    final_root_cause: str,
    sensor_values: dict,
    repair_steps: list[str],
    vehicle_info: str = "Patient Vehicle",
) -> str:
    """
    Generate a customer-facing summary for the diagnostic report.
    """
    first_step = repair_steps[0] if repair_steps else f"Inspect and service {final_root_cause}"
    prompt = f"""You are IgnitionAI. Write an executive summary paragraph for a vehicle owner's official diagnostic report.

Vehicle: {vehicle_info}
Identified Root Cause: {final_root_cause}
Fault Codes: {', '.join(dtcs) if dtcs else 'System Telemetry Irregularity'}
Primary Action: {first_step}

Write 2-3 clear, professional paragraphs explaining the issue in polite, non-technical customer terms, why immediate repair is advised, and how the repair restores vehicle reliability.
"""

    try:
        client = _get_client()
        completion = client.chat.completions.create(
            model=NVIDIA_MODEL_NAME,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.2,
            max_tokens=400,
            stream=False
        )
        return completion.choices[0].message.content.strip()
    except Exception:
        return (
            f"IgnitionAI diagnostic scan has detected a malfunction in your vehicle's {final_root_cause}. "
            f"This fault may impact engine efficiency and fuel economy. "
            f"We recommend authorizing your technician to perform the suggested inspection and repairs promptly."
        )
