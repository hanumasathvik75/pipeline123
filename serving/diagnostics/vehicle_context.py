"""Indian Automotive Market Vehicle Context & Knowledge Base.

Provides OEM specifications, BS6 Phase 2 / RDE emissions context, E20 ethanol blend
diagnostics, and Indian model-specific failure modes for enriching AI prompts.
"""
from __future__ import annotations

from typing import Any, Dict, List, Optional

INDIAN_OEM_DATABASE: Dict[str, Dict[str, Any]] = {
    "maruti_suzuki": {
        "name": "Maruti Suzuki India Limited",
        "popular_models": ["Swift", "Baleno", "Dzire", "Brezza", "Ertiga", "Grand Vitara", "Fronx"],
        "engines": {
            "1.2L K12N DualJet": {
                "fuel": "Petrol / CNG",
                "common_issues": ["EGR valve carbon accumulation", "Fuel injector tip fouling on E20 blend", "O2 sensor slow response"],
                "fuel_trim_tolerance_pct": 12.0,
            },
            "1.5L K15C Smart Hybrid": {
                "fuel": "Petrol",
                "common_issues": ["ISG (Integrated Starter Generator) belt slippage", "Lithium-ion auxiliary battery voltage drift", "Purge valve sticking open"],
                "fuel_trim_tolerance_pct": 10.0,
            },
        },
    },
    "tata_motors": {
        "name": "Tata Motors Limited",
        "popular_models": ["Nexon", "Punch", "Harrier", "Safari", "Altroz", "Tiago"],
        "engines": {
            "1.2L Revotron Turbo": {
                "fuel": "Petrol / iCNG",
                "common_issues": ["Turbocharger wastegate actuator lag", "Intercooler hose coupling air leak", "MAP sensor oil contamination"],
                "fuel_trim_tolerance_pct": 15.0,
            },
            "2.0L Kryotec Diesel": {
                "fuel": "Diesel (BS6)",
                "common_issues": ["DPF soot load differential pressure sensor drift", "DEF/AdBlue injector crystallization", "EGR cooler flow restriction"],
                "fuel_trim_tolerance_pct": 10.0,
            },
        },
    },
    "hyundai_india": {
        "name": "Hyundai Motor India",
        "popular_models": ["Creta", "Venue", "i20", "Verna", "Exter", "Alcazar"],
        "engines": {
            "1.5L U2 CRDi Diesel": {
                "fuel": "Diesel (BS6)",
                "common_issues": ["VGT turbo solenoid vacuum line leak", "High pressure fuel rail sensor connector corrosion", "DPF frequent regeneration flag"],
                "fuel_trim_tolerance_pct": 10.0,
            },
            "1.0L Turbo GDI": {
                "fuel": "Petrol",
                "common_issues": ["Intake valve carbon buildup", "High-pressure fuel pump (HPFP) noise/pressure drop", "Spark plug gap erosion"],
                "fuel_trim_tolerance_pct": 12.0,
            },
        },
    },
    "mahindra": {
        "name": "Mahindra & Mahindra",
        "popular_models": ["Scorpio-N", "XUV700", "Thar", "Bolero Neo", "XUV3XO"],
        "engines": {
            "2.2L mHawk Diesel": {
                "fuel": "Diesel (BS6 Phase 2)",
                "common_issues": ["SCR dosing pump pressure failure", "EGR valve sticking due to soot", "Turbocharger boost pressure sensor deviation"],
                "fuel_trim_tolerance_pct": 10.0,
            },
            "2.0L mStallion TGDi": {
                "fuel": "Petrol",
                "common_issues": ["Fuel rail pressure sensor calibration drift", "Ignition coil pack thermal degradation"],
                "fuel_trim_tolerance_pct": 12.0,
            },
        },
    },
}

INDIAN_REGULATORY_CONTEXT: Dict[str, str] = {
    "emission_norm": "BS6 Phase 2 (RDE - Real Driving Emissions compliant)",
    "fuel_standard": "E20 Petrol (20% Ethanol blend) / BS6 Ultra-Low Sulfur Diesel",
    "common_environmental_factors": "High ambient temperature (35°C–45°C), high dust/humidity, frequent stop-and-go city traffic causing elevated thermal stress and rapid DPF/EGR soot accumulation.",
}


def get_vehicle_context(make: Optional[str] = None, model: Optional[str] = None, year: Optional[int] = None) -> Dict[str, Any]:
    """Retrieve Indian market context for a given vehicle make/model/year.

    Used by explanation.py to inject OEM specific failure patterns into LLM prompts.
    """
    if not make:
        return {
            "market": "India",
            "regulatory": INDIAN_REGULATORY_CONTEXT,
            "oem_notes": "Generic Indian market workshop vehicle context applied.",
        }

    make_key = make.lower().replace(" ", "_").replace("-", "_")
    oem_data = INDIAN_OEM_DATABASE.get(make_key)

    if not oem_data:
        for key, data in INDIAN_OEM_DATABASE.items():
            if make_key in key or key in make_key:
                oem_data = data
                break

    if not oem_data:
        return {
            "market": "India",
            "make": make,
            "model": model,
            "year": year,
            "regulatory": INDIAN_REGULATORY_CONTEXT,
            "oem_notes": f"Indian regulatory context active for {make}.",
        }

    matched_engine = None
    if model:
        for engine_name, engine_info in oem_data.get("engines", {}).items():
            matched_engine = {"name": engine_name, **engine_info}
            break

    return {
        "market": "India",
        "oem_name": oem_data["name"],
        "model": model,
        "year": year,
        "engine_context": matched_engine,
        "regulatory": INDIAN_REGULATORY_CONTEXT,
    }
