"""NVIDIA NIM last-mile explanation of structured diagnostic evidence for Indian workshops."""
from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from dotenv import load_dotenv

from .vehicle_context import get_vehicle_context

load_dotenv(Path(__file__).parents[1] / ".env")


def explain_structured_diagnosis(diagnosis: dict[str, Any], vehicle_info: dict[str, Any] | None = None) -> str:
    """Return a mechanic-focused diagnostic report using NVIDIA NIM LLM.

    Includes Indian market context (OEM failure modes, BS6 Phase 2, E20 fuel factors).
    """
    make = vehicle_info.get("make") if vehicle_info else None
    model = vehicle_info.get("model") if vehicle_info else None
    year = vehicle_info.get("year") if vehicle_info else None

    context = get_vehicle_context(make=make, model=model, year=year)

    allowed = {
        "vehicle_health_index": diagnosis.get("vehicle_health_index"),
        "fault_candidates": diagnosis.get("fault_candidates", []),
        "indian_market_context": context,
    }

    prompt = """You are IgnitionAI, the senior automotive diagnostic AI for Indian workshops.
Write a concise, professional mechanic report using ONLY the supplied structured JSON data.
Do not infer, add, or name any fault candidate absent from fault_candidates.
For every claim, state the exact evidence field supporting it.
Consider Indian driving conditions, BS6 Phase 2 emission norms, and E20 ethanol blend impacts if relevant to the evidence.

Format using plain text with these headings ONLY:
1. Assessment: Plain-English summary of root cause
2. Evidence: Data fields and sensor values confirming the diagnosis
3. Risk & Urgency: Safety, drivability impact, and urgency score
4. Technician Handoff: Key verification checks before replacing parts

STRUCTURED DIAGNOSIS:
""" + json.dumps(allowed, ensure_ascii=False, indent=2)

    provider = os.getenv("EXPLANATION_PROVIDER", "nvidia_nim")
    if provider == "nvidia_nim":
        key = os.getenv("NVIDIA_API_KEY")
        model = os.getenv("NVIDIA_MODEL", "meta/llama-3.2-11b-vision-instruct")
        payload = json.dumps({
            "model": model,
            "temperature": 0.1,
            "max_tokens": 600,
            "messages": [
                {
                    "role": "system",
                    "content": "You are a constrained automotive diagnostic reporting layer for Indian auto workshops. Follow instructions strictly.",
                },
                {"role": "user", "content": prompt},
            ],
        }).encode("utf-8")
        url = "https://integrate.api.nvidia.com/v1/chat/completions"
        headers = {"Content-Type": "application/json", "Authorization": f"Bearer {key}"}
    elif provider == "gemini":
        key = os.getenv("GEMINI_API_KEY")
        model = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
        payload = json.dumps({"contents": [{"parts": [{"text": prompt}]}]}).encode("utf-8")
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
        headers = {"Content-Type": "application/json", "x-goog-api-key": key}
    else:
        raise RuntimeError(f"Unsupported explanation provider: {provider}")

    if not key:
        raise RuntimeError(f"{provider} API key is not configured on the API server.")

    request = Request(url, data=payload, headers=headers, method="POST")
    try:
        with urlopen(request, timeout=30) as response:
            body = json.loads(response.read().decode("utf-8"))
    except HTTPError as exc:
        raise RuntimeError(f"Explanation request failed ({exc.code}).") from exc

    try:
        if provider == "nvidia_nim":
            return body["choices"][0]["message"]["content"]
        return body["candidates"][0]["content"]["parts"][0]["text"]
    except (KeyError, IndexError, TypeError) as exc:
        raise RuntimeError("The explanation provider returned no usable report.") from exc


def generate_explanation(diagnosis: dict[str, Any], vehicle_info: dict[str, Any] | None = None) -> str:
    """Alias for explain_structured_diagnosis used by the desktop app."""
    return explain_structured_diagnosis(diagnosis, vehicle_info)

