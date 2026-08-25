"""Gemini last-mile explanation of already structured diagnostic evidence."""
from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from dotenv import load_dotenv

load_dotenv(Path(__file__).parents[1] / ".env")


def explain_structured_diagnosis(diagnosis: dict[str, Any]) -> str:
    """Return a mechanic report without transmitting raw telemetry to any LLM."""
    allowed = {"vehicle_health_index": diagnosis.get("vehicle_health_index"), "fault_candidates": diagnosis.get("fault_candidates", [])}
    prompt = """You are the final reporting layer for an automotive diagnostic system.
Write a concise mechanic-readable report using ONLY the supplied structured JSON.
Do not infer, add, or name any fault candidate absent from fault_candidates.
For every claim, state the evidence field that supports it. If candidates are
empty, state that no configured evidence threshold was crossed. Do not claim a
confirmed repair. The application provides a separate deterministic stepwise
repair workflow, so do not provide repair instructions. Use plain text, no
Markdown symbols. Use only three headings: Assessment, Evidence, Technician handoff.

STRUCTURED DIAGNOSIS:\n""" + json.dumps(allowed, ensure_ascii=False)
    provider = os.getenv("EXPLANATION_PROVIDER", "nvidia_nim")
    if provider == "nvidia_nim":
        key = os.getenv("NVIDIA_API_KEY")
        model = os.getenv("NVIDIA_MODEL", "nvidia/llama-3.3-nemotron-super-49b-v1")
        payload = json.dumps({"model": model, "temperature": 0.1, "max_tokens": 500, "messages": [{"role": "system", "content": "You are a constrained automotive diagnostic reporting layer. Follow the user's evidence-only instructions exactly."}, {"role": "user", "content": prompt}]}).encode("utf-8")
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
