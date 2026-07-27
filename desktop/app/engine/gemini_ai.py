"""
IgnitionAI — Gemini AI Integration
Uses gemini-1.5-flash-latest for fast, cost-effective AI explanations.
"""

from google import genai
from google.genai import types
from app.config import GEMINI_API_KEY, GEMINI_MODEL_NAME


def _get_client():
    """Return a configured Gemini client."""
    return genai.Client(api_key=GEMINI_API_KEY)


def generate_plain_english_diagnosis(
    dtcs: list[str],
    final_root_cause: str,
    sensor_values: dict,
    vehicle_info: str = "Unknown Vehicle",
) -> str:
    """
    Generate a plain-English mechanic-friendly diagnosis summary.
    """
    sensor_summary = "\n".join(
        f"  - {k.replace('_', ' ').title()}: {v}"
        for k, v in list(sensor_values.items())[:8]
    )
    dtc_list = ", ".join(dtcs) if dtcs else "None detected"

    prompt = f"""You are IgnitionAI, an expert automotive diagnostic AI assistant.
A vehicle scan has been completed. Provide a clear, professional, mechanic-friendly diagnosis.

Vehicle: {vehicle_info}
Fault Codes (DTCs): {dtc_list}
AI Identified Root Cause: {final_root_cause}

Key Sensor Readings:
{sensor_summary}

Write a concise diagnosis report (2-3 short paragraphs) that:
1. Explains what is wrong with the vehicle in plain English (avoid jargon)
2. Explains WHY the AI identified this root cause based on the sensor data
3. States the urgency level (Critical / High / Medium / Low) and if the car is safe to drive

Keep it professional but conversational. Max 150 words."""

    try:
        client = _get_client()
        response = client.models.generate_content(
            model=GEMINI_MODEL_NAME,
            contents=prompt,
        )
        return response.text.strip()
    except Exception as e:
        return f"[AI Unavailable] Could not generate diagnosis: {str(e)}\n\nManual Review Required: Root cause identified as '{final_root_cause}' based on DTC analysis."


def generate_repair_narrative(
    root_cause: str,
    dtcs: list[str],
    repair_steps: list[str],
) -> str:
    """
    Generate a detailed, step-by-step repair narrative for a mechanic.
    """
    steps_text = "\n".join(f"{i+1}. {s}" for i, s in enumerate(repair_steps))
    dtc_list = ", ".join(dtcs) if dtcs else "None"

    prompt = f"""You are IgnitionAI, a senior automotive repair advisor.
Based on the diagnostic results, write a professional repair guide.

Root Cause: {root_cause}
Related DTCs: {dtc_list}

Technical Repair Steps:
{steps_text}

Write a structured repair guide that:
1. Lists required tools and parts (estimate)
2. Provides a numbered step-by-step procedure in mechanic language
3. Includes safety warnings where relevant
4. Estimates time and difficulty (Beginner/Intermediate/Expert)

Format as a clean, professional guide. Max 200 words."""

    try:
        client = _get_client()
        response = client.models.generate_content(
            model=GEMINI_MODEL_NAME,
            contents=prompt,
        )
        return response.text.strip()
    except Exception as e:
        # Fallback to the raw steps
        fallback = "\n".join(f"• {s}" for s in repair_steps)
        return f"[AI Unavailable] Raw repair steps:\n\n{fallback}"


def generate_pdf_summary(
    dtcs: list[str],
    final_root_cause: str,
    sensor_values: dict,
    repair_steps: list[str],
    vehicle_info: str = "Unknown Vehicle",
) -> str:
    """
    Generate a short executive summary suitable for a customer-facing PDF report.
    """
    prompt = f"""You are IgnitionAI. Generate a professional customer-facing summary for a vehicle diagnostic report.

Vehicle: {vehicle_info}
Fault Codes: {', '.join(dtcs)}
Root Cause: {final_root_cause}
Recommended Action: {repair_steps[0] if repair_steps else 'See technician'}

Write 2 sentences maximum. Use formal, customer-friendly language. 
Start with "IgnitionAI has detected..." """

    try:
        client = _get_client()
        response = client.models.generate_content(
            model=GEMINI_MODEL_NAME,
            contents=prompt,
        )
        return response.text.strip()
    except Exception:
        return f"IgnitionAI has detected a fault in your vehicle's {final_root_cause.lower()}. Please consult a certified technician for immediate attention."
