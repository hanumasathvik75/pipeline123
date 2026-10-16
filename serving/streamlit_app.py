"""
IgnitionAI - Interactive Multi-Label Diagnostics & VHI Streamlit App
Option B: Hybrid Machine Learning + First-Principles Physics Bounds Architecture
"""
import os
import sys
import glob
import json
import joblib
import numpy as np
import pandas as pd
import streamlit as st
from datetime import datetime, timedelta
from scipy.stats import kurtosis

# Ensure serving root is on sys.path
SERVING_DIR = os.path.dirname(os.path.abspath(__file__))
if SERVING_DIR not in sys.path:
    sys.path.append(SERVING_DIR)

from diagnostics.hybrid_engine import get_hybrid_engine, FEATURE_COLS, TARGET_CONFIGS

FLEET_DIR = os.path.join(SERVING_DIR, "data", "ml_synthetic")

# =====================================================================
# HELPER FUNCTIONS
# =====================================================================
def synthesize_live_telemetry_window(
    rpm_center: float,
    speed_kph: float,
    throttle_pct: float,
    load_pct: float,
    total_trim: float,
    o2_v: float,
    coolant_c: float,
    intake_c: float,
    voltage_v: float,
    map_kpa: float,
    maf_gps: float,
    hunting_active: bool,
    n_seconds: int = 30
) -> pd.DataFrame:
    """
    Generates a continuous, physically correlated 30-second OBD-II telemetry window.
    Applies first-principles dynamic formulas for combustion jitter, closed-loop O2 switching,
    Speed-Density airflow coupling, thermal dissipation, and alternator stator loading.
    Zero hardcoded summary statistics.
    """
    rng = np.random.default_rng(42)
    t = np.arange(n_seconds, dtype=float)
    
    # 1. Rotational Mechanics & Combustion Dynamics
    rpm_series = np.zeros(n_seconds)
    for i in range(n_seconds):
        combustion_jitter = rng.normal(0, 3.2)
        if hunting_active:
            # 0.15 Hz limit-cycle combustion hunting surge (period ~6.7s)
            hunting_surge = 125.0 * np.sin(2 * np.pi * 0.15 * t[i])
        else:
            hunting_surge = 0.0
        rpm_series[i] = np.clip(rpm_center + hunting_surge + combustion_jitter, 600.0, 5500.0)
        
    # 2. Vehicle Speed & Throttle
    speed_series = np.clip(speed_kph + rng.normal(0, 0.4 if speed_kph > 5 else 0.0, n_seconds), 0.0, 160.0)
    throttle_series = np.clip(throttle_pct + rng.normal(0, 0.25, n_seconds), 0.0, 100.0)
    
    # 3. Manifold Absolute Pressure (MAP) & Mass Air Flow (MAF)
    map_series = np.clip(map_kpa + 0.35 * np.sin(2 * np.pi * 0.35 * t) + rng.normal(0, 0.2, n_seconds), 20.0, 110.0)
    
    # Volumetric speed-density airflow coupling
    t_in_k = intake_c + 273.15
    sd_flow_index = (rpm_series * map_series) / t_in_k
    sd_base = (rpm_center * map_kpa) / t_in_k
    maf_series = np.clip(maf_gps * (sd_flow_index / max(0.1, sd_base)) + rng.normal(0, 0.04, n_seconds), 1.2, 85.0)
    
    # 4. Engine Load %
    load_series = np.clip(load_pct * (map_series / max(25.0, map_kpa)) + rng.normal(0, 0.4, n_seconds), 5.0, 100.0)
    
    # 5. Closed-Loop Fuel Trim Limit-Cycle & O2 Sensor Oscillations
    stft_series = np.zeros(n_seconds)
    ltft_series = np.zeros(n_seconds)
    o2_series = np.zeros(n_seconds)
    
    if coolant_c >= 45.0:
        for i in range(n_seconds):
            o2_osc = 0.18 * np.sin(2 * np.pi * 0.18 * t[i])
            o2_series[i] = np.clip(o2_v + o2_osc + rng.normal(0, 0.015), 0.05, 0.95)
            stft_series[i] = -1.6 * np.sin(2 * np.pi * 0.18 * t[i]) + rng.normal(0, 0.2)
            ltft_series[i] = total_trim - stft_series[i]
    else:
        stft_series[:] = 0.0
        ltft_series[:] = 0.0
        o2_series[:] = np.clip(o2_v + rng.normal(0, 0.01), 0.05, 0.95)
        
    total_trim_series = stft_series + ltft_series
    
    # 6. Thermal Dynamics (Newton's Cooling & Combustion Heat Generation)
    heat_in = 0.18 * (load_pct / 30.0)
    ram_air = 1.0 + 0.02 * speed_kph
    heat_out = 0.002 * ram_air * max(0.0, coolant_c - intake_c)
    net_heat_rate = (heat_in - heat_out)
    
    coolant_series = np.zeros(n_seconds)
    intake_series = np.zeros(n_seconds)
    for i in range(n_seconds):
        coolant_series[i] = coolant_c + (net_heat_rate * (t[i] / 60.0)) + rng.normal(0, 0.04)
        intake_series[i] = intake_c + (0.015 * t[i]) + rng.normal(0, 0.03)
        
    # 7. Electrical System (Alternator Output & Stator RPM Dynamics)
    voltage_series = np.zeros(n_seconds)
    for i in range(n_seconds):
        if voltage_v < 13.0:
            stator_droop = max(0.0, (1350.0 - rpm_series[i]) / 800.0) * 0.75
        else:
            stator_droop = max(0.0, (850.0 - rpm_series[i]) / 500.0) * 0.15
        diode_ripple = 0.025 * np.sin(2 * np.pi * 3.5 * t[i])
        voltage_series[i] = np.clip(voltage_v - stator_droop + diode_ripple + rng.normal(0, 0.015), 10.5, 14.8)
        
    df_window = pd.DataFrame({
        "timestamp": [datetime(2026, 10, 16, 10, 0, 0) + timedelta(seconds=int(s)) for s in t],
        "rpm": np.round(rpm_series, 1),
        "speed_kph": np.round(speed_series, 1),
        "throttle_pct": np.round(throttle_series, 1),
        "engine_load_pct": np.round(load_series, 1),
        "map_kpa": np.round(map_series, 1),
        "maf_gps": np.round(maf_series, 2),
        "stft_b1_pct": np.round(stft_series, 2),
        "ltft_b1_pct": np.round(ltft_series, 2),
        "total_trim": np.round(total_trim_series, 2),
        "coolant_c": np.round(coolant_series, 1),
        "intake_air_c": np.round(intake_series, 1),
        "ecu_voltage_v": np.round(voltage_series, 2),
        "o2_b1s1_v": np.round(o2_series, 3)
    })
    return df_window


def extract_features_from_window(win_df: pd.DataFrame) -> dict:
    """Calculates 28 physical, statistical, and residual features from a 30s window dataframe."""
    df = win_df.copy()
    if 'total_trim' not in df:
        stft = df['stft_b1_pct'] if 'stft_b1_pct' in df else df.get('SHORT_FUEL_TRIM_1', 0.0)
        ltft = df['ltft_b1_pct'] if 'ltft_b1_pct' in df else df.get('LONG_FUEL_TRIM_1', 0.0)
        df['total_trim'] = stft + ltft
        
    rpm_vals = df['rpm'].to_numpy() if 'rpm' in df else df['RPM'].to_numpy()
    rpm_mean = float(np.mean(rpm_vals))
    rpm_std = float(np.std(rpm_vals))
    
    # Dual Nature: local linear detrending & lag correlations
    t_win = np.arange(len(rpm_vals))
    detrended = rpm_vals - np.polyval(np.polyfit(t_win, rpm_vals, 1), t_win) if len(rpm_vals) > 3 else rpm_vals - np.mean(rpm_vals)
    det_std = float(np.std(detrended))
    c0 = det_std**2 + 1e-6
    r3 = float(np.mean(detrended[:-3] * detrended[3:]) / c0) if len(detrended) > 6 else 0.0
    r7 = float(np.mean(detrended[:-7] * detrended[7:]) / c0) if len(detrended) > 10 else 0.0
    hunting_surge_metric = float((r7 - r3) * det_std)
    rpm_kurt = float(kurtosis(detrended)) if det_std > 1.0 else 0.0
    
    speed = df['speed_kph'] if 'speed_kph' in df else df.get('SPEED', 0.0)
    throttle = df['throttle_pct'] if 'throttle_pct' in df else df.get('THROTTLE_POS', 0.0)
    load = df['engine_load_pct'] if 'engine_load_pct' in df else df.get('ENGINE_LOAD', 20.0)
    map_kpa = df['map_kpa'] if 'map_kpa' in df else df.get('INTAKE_PRESSURE', 35.0)
    maf = df['maf_gps'] if 'maf_gps' in df else df.get('MAF', 3.0)
    coolant = df['coolant_c'] if 'coolant_c' in df else df.get('COOLANT_TEMP', 85.0)
    intake = df['intake_air_c'] if 'intake_air_c' in df else df.get('INTAKE_TEMP', 25.0)
    voltage = df['ecu_voltage_v'] if 'ecu_voltage_v' in df else df.get('ELM_VOLTAGE', 14.1)
    
    map_m = float(map_kpa.mean())
    maf_m = float(maf.mean())
    intake_m = float(intake.mean())
    coolant_m = float(coolant.mean())
    v_min_val = float(voltage.min())
    
    # Physics residuals
    t_intake_k = intake_m + 273.15
    speed_density_index = (rpm_mean * map_m) / t_intake_k
    maf_physics_ratio = float(maf_m / max(0.1, speed_density_index))
    
    closed_loop_flag = 1.0 if coolant_m >= 45.0 else 0.0
    trim_mean_val = float(df['total_trim'].mean())
    stoich_closed_loop_residual = float(closed_loop_flag * trim_mean_val)
    thermal_deficit_score = float(max(0.0, 65.0 - coolant_m))
    voltage_physics_margin = float(v_min_val - 12.45)
    
    coolant_rate = float((coolant.iloc[-1] - coolant.iloc[0]) / max(0.2, len(df) / 60.0))
    v_diff = np.diff(voltage.to_numpy()) if len(voltage) > 1 else np.array([0.0])
    
    return {
        "rpm_mean": rpm_mean,
        "rpm_std": rpm_std,
        "rpm_kurtosis": rpm_kurt,
        "speed_mean": float(speed.mean()),
        "speed_std": float(speed.std()),
        "idle_fraction": float((speed < 2.0).mean()),
        "throttle_mean": float(throttle.mean()),
        "engine_load_mean": float(load.mean()),
        "map_mean": map_m,
        "map_std": float(map_kpa.std()),
        "maf_mean": maf_m,
        "maf_map_ratio": float(maf_m / max(1.0, map_m)),
        "trim_mean": trim_mean_val,
        "trim_std": float(df['total_trim'].std()),
        "trim_max": float(df['total_trim'].max()),
        "trim_min": float(df['total_trim'].min()),
        "trim_exceed_fraction": float(((df['total_trim'] >= 12.0) | (df['total_trim'] <= -10.0)).mean()),
        "coolant_mean": coolant_m,
        "coolant_warmup_rate": coolant_rate,
        "intake_mean": intake_m,
        "coolant_intake_delta": float(coolant_m - intake_m),
        "voltage_mean": float(voltage.mean()),
        "voltage_min": v_min_val,
        "voltage_ripple_std": float(np.std(v_diff)),
        "o2_b1s1_v": float(df['o2_b1s1_v'].mean()) if 'o2_b1s1_v' in df else 0.45,
        # Dual-Nature Physics Features
        "rpm_det_std": det_std,
        "rpm_r3": r3,
        "rpm_r7": r7,
        "hunting_surge_metric": hunting_surge_metric,
        "maf_physics_ratio": maf_physics_ratio,
        "stoich_closed_loop_residual": stoich_closed_loop_residual,
        "thermal_deficit_score": thermal_deficit_score,
        "voltage_physics_margin": voltage_physics_margin
    }


def estimate_maf_reference_range(features: dict[str, float]) -> tuple[float, float] | None:
    """Estimate a broad speed-density MAF range for a generic 1.0-3.0 L engine."""
    rpm = features["rpm_mean"]
    map_kpa = features["map_mean"]
    intake_c = features["intake_mean"]
    if not np.isfinite([rpm, map_kpa, intake_c]).all() or rpm <= 0 or map_kpa <= 0 or intake_c <= -273.15:
        return None

    air_gas_constant = 287.05
    intake_kelvin = intake_c + 273.15
    base_flow = (rpm / 120.0) * (map_kpa * 1000.0) / (air_gas_constant * intake_kelvin) * 1000.0
    min_flow = base_flow * 0.001 * 0.45
    max_flow = base_flow * 0.003 * 0.95
    return min_flow, max_flow


def describe_air_path_context(features: dict[str, float]) -> str:
    """Describe trim context without treating MAP/MAF as a confirmed fault diagnosis."""
    trim = features["trim_mean"]
    map_kpa = features["map_mean"]
    engine_load = features["engine_load_mean"]

    if trim >= 12.0 and map_kpa <= 40.0:
        return (
            "Positive fuel trim with low MAP (high-vacuum operating context). "
            "An intake air leak is one candidate to investigate; verify fuel trims at "
            "idle and raised RPM and perform appropriate workshop tests."
        )
    if trim >= 12.0 and map_kpa >= 80.0 and engine_load >= 65.0:
        return (
            "Positive fuel trim with high MAP and high calculated load. "
            "If the ECU is in closed loop, investigate fuel delivery and air-metering "
            "under load; confirm with fuel-pressure and vehicle-specific tests."
        )
    if trim >= 12.0:
        return (
            "Positive fuel trim is present, but this window does not provide enough "
            "operating context to distinguish an air leak from fuel-delivery or "
            "air-metering causes."
        )
    return (
        "No strong positive-trim pattern in this window. MAP and MAF are shown as "
        "air-path context signals, not standalone fault diagnoses."
    )


# =====================================================================
# STREAMLIT UI SETUP
# =====================================================================
# STREAMLIT UI SETUP & MODERN AUTOMOTIVE THEME
# =====================================================================
st.set_page_config(page_title="IgnitionAI Hybrid Diagnostics", page_icon="⚡", layout="wide")

st.markdown("""
<style>
    /* Automotive Dark UI Theme */
    .stApp { background-color: #0d1117; color: #e6edf3; }
    
    /* Metric Cards */
    .metric-card { 
        background: linear-gradient(145deg, #161b22, #1c2128); 
        padding: 20px; 
        border-radius: 12px; 
        border-left: 6px solid #238636; 
        margin-bottom: 12px; 
        box-shadow: 0 4px 12px rgba(0,0,0,0.4); 
    }
    .metric-title { font-size: 12px; color: #8b949e; text-transform: uppercase; font-weight: 700; letter-spacing: 0.8px; margin-bottom: 4px; }
    .metric-value { font-size: 32px; font-weight: 800; color: #ffffff; line-height: 1.1; }
    
    /* Control Deck Container */
    .control-deck {
        background-color: #161b22;
        border: 1px solid #30363d;
        border-radius: 12px;
        padding: 18px 24px;
        margin-bottom: 20px;
        box-shadow: 0 6px 16px rgba(0,0,0,0.3);
    }
    .control-card-title {
        font-size: 15px;
        font-weight: 700;
        color: #58a6ff;
        border-bottom: 1px solid #21262d;
        padding-bottom: 6px;
        margin-bottom: 12px;
    }
    
    /* Authority Badges */
    .badge-consensus { background-color: #1b5e20; color: #a5d6a7; padding: 4px 12px; border-radius: 20px; font-weight: 700; font-size: 12px; display: inline-block; border: 1px solid #2e7d32; }
    .badge-override { background-color: #0d47a1; color: #90caf9; padding: 4px 12px; border-radius: 20px; font-weight: 700; font-size: 12px; display: inline-block; border: 1px solid #1565c0; }
    .badge-ml { background-color: #4a148c; color: #ce93d8; padding: 4px 12px; border-radius: 20px; font-weight: 700; font-size: 12px; display: inline-block; border: 1px solid #7b1fa2; }
    .badge-nominal { background-color: #21262d; color: #7ee787; padding: 4px 12px; border-radius: 20px; font-weight: 600; font-size: 12px; display: inline-block; border: 1px solid #30363d; }
    
    .status-pill-green { background-color: rgba(35, 134, 54, 0.2); color: #3fb950; border: 1px solid #238636; padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; display: inline-block; }
    .status-pill-yellow { background-color: rgba(210, 153, 34, 0.2); color: #d29922; border: 1px solid #9e6a03; padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; display: inline-block; }
</style>
""", unsafe_allow_html=True)

st.title("⚡ IgnitionAI — Hybrid ML + Physics Bounds Telemetry Workstation")
st.caption("Stage 1: Multi-Output XGBoost (28 Features) • Stage 2: First-Principles Physics Invariants • Stage 3: Decision Fusion & VHI Scoring")

try:
    engine = get_hybrid_engine()
except Exception as e:
    st.error(f"Failed to initialize Hybrid Diagnostic Engine: {e}")
    st.stop()


# =====================================================================
# SIDEBAR NAVIGATION (CLEAN & MINIMAL — NO SLIDERS IN SIDEBAR)
# =====================================================================
st.sidebar.markdown("### ⚡ IgnitionAI Console")
st.sidebar.caption("Automotive Telemetry Diagnostics")

input_mode = st.sidebar.radio(
    "Select Operating Mode:",
    ["🎛️ Live Virtual Sensor Deck", "🚗 Fleet Inspection Drives", "📤 Upload Telemetry CSV"]
)

st.sidebar.markdown("---")
st.sidebar.markdown("#### ⚙️ System Calibration")
st.sidebar.markdown("""
<div style="font-size: 12px; color: #8b949e; line-height: 1.6;">
    • <b>Model:</b> Multi-Output XGBoost<br>
    • <b>Features:</b> 28 Continuous Channels<br>
    • <b>Physics Guards:</b> 5 Active Invariants<br>
    • <b>Sampling Window:</b> 30-Sec Sliding<br>
    • <b>Portability:</b> Generic OBD-II PIDs
</div>
""", unsafe_allow_html=True)

active_df = None
selected_info = ""
default_window_start = 120

if input_mode == "🚗 Fleet Inspection Drives":
    st.sidebar.markdown("---")
    st.sidebar.subheader("Select Fleet Recording:")
    files = sorted(glob.glob(os.path.join(FLEET_DIR, "drive_*.csv")))
    if not files:
        st.sidebar.error("No fleet files found in: " + FLEET_DIR)
    else:
        file_options = {os.path.basename(f): f for f in files}
        chosen_file = st.sidebar.selectbox("Choose Drive Session:", list(file_options.keys()))
        active_df = pd.read_csv(file_options[chosen_file])
        selected_info = f"Loaded `{chosen_file}` ({len(active_df)} seconds)"

elif input_mode == "📤 Upload Telemetry CSV":
    st.sidebar.markdown("---")
    st.sidebar.subheader("Telemetry File Upload:")
    uploaded = st.sidebar.file_uploader("Upload OBD-II CSV (ELM327 Format)", type=["csv"])
    if uploaded is not None:
        active_df = pd.read_csv(uploaded)
        selected_info = f"Uploaded `{uploaded.name}` ({len(active_df)} rows)"
    else:
        st.info("Upload a vehicle telemetry CSV in the sidebar to begin analysis.")


# =====================================================================
# MAIN PAGE: LIVE VIRTUAL SENSOR DECK (FULL-WIDTH 3-COLUMN CARDS)
# =====================================================================
PRESET_MAP = {
    "— Custom Manual Adjustment —": None,
    "🚗 Nominal Healthy Drive (Swift/i20 - VHI 100)": {
        "rpm": 750, "speed": 0, "throttle": 4.0, "load": 18.0,
        "trim": 0.5, "o2": 0.45, "coolant": 88, "intake": 26,
        "voltage": 14.10, "map": 30.0, "maf": 2.2, "hunting": False
    },
    "💨 Intake Vacuum Leak / Lean Trim (+18%)": {
        "rpm": 750, "speed": 0, "throttle": 4.0, "load": 18.0,
        "trim": 18.0, "o2": 0.18, "coolant": 86, "intake": 25,
        "voltage": 14.10, "map": 32.0, "maf": 2.1, "hunting": False
    },
    "⛽ Rich Injector Leak / Overfueling (-16%)": {
        "rpm": 750, "speed": 0, "throttle": 4.0, "load": 18.0,
        "trim": -16.0, "o2": 0.85, "coolant": 86, "intake": 25,
        "voltage": 14.10, "map": 30.0, "maf": 2.2, "hunting": False
    },
    "⚡ Alternator Charging Voltage Sag (11.9V)": {
        "rpm": 740, "speed": 0, "throttle": 4.0, "load": 22.0,
        "trim": 0.5, "o2": 0.45, "coolant": 88, "intake": 26,
        "voltage": 11.90, "map": 30.0, "maf": 2.2, "hunting": False
    },
    "🔄 Idle Combustion Instability / IACV Hunting": {
        "rpm": 750, "speed": 0, "throttle": 4.0, "load": 18.0,
        "trim": 0.5, "o2": 0.45, "coolant": 88, "intake": 26,
        "voltage": 14.10, "map": 30.0, "maf": 2.2, "hunting": True
    },
    "🌡️ Stuck Thermostat / Warmup Delay (Highway 52°C)": {
        "rpm": 2200, "speed": 80, "throttle": 22.0, "load": 45.0,
        "trim": 1.0, "o2": 0.45, "coolant": 52, "intake": 22,
        "voltage": 14.10, "map": 45.0, "maf": 8.5, "hunting": False
    },
    "🌪️ Compound Dual Fault (Lean Leak + Alternator Sag)": {
        "rpm": 740, "speed": 0, "throttle": 4.0, "load": 20.0,
        "trim": 18.0, "o2": 0.18, "coolant": 86, "intake": 25,
        "voltage": 11.90, "map": 32.0, "maf": 2.1, "hunting": False
    },
    "🚨 Compound Triple Fault (Lean + Hunting + Sag)": {
        "rpm": 750, "speed": 0, "throttle": 4.0, "load": 20.0,
        "trim": 17.5, "o2": 0.18, "coolant": 86, "intake": 25,
        "voltage": 11.85, "map": 32.0, "maf": 2.1, "hunting": True
    }
}

window_df = None

if input_mode == "🎛️ Live Virtual Sensor Deck":
    with st.container():
        deck_header_col1, deck_header_col2 = st.columns([1.2, 1.0])
        with deck_header_col1:
            st.subheader("🎛️ Virtual Vehicle Telemetry Control Deck")
            st.caption("Adjust physical sensors across 3 domain banks below. Real-time 30s continuous waveforms are synthesized without hardcoded values.")
        with deck_header_col2:
            preset_choice = st.selectbox("🎯 1-Click Fault Scenario Presets:", list(PRESET_MAP.keys()), index=0)
            
        # Check if preset changed to populate session state
        if preset_choice in PRESET_MAP and PRESET_MAP[preset_choice] is not None:
            if st.session_state.get("active_preset") != preset_choice:
                p_cfg = PRESET_MAP[preset_choice]
                st.session_state["slider_rpm"] = p_cfg["rpm"]
                st.session_state["slider_speed"] = p_cfg["speed"]
                st.session_state["slider_throttle"] = p_cfg["throttle"]
                st.session_state["slider_load"] = p_cfg["load"]
                st.session_state["slider_trim"] = p_cfg["trim"]
                st.session_state["slider_o2"] = p_cfg["o2"]
                st.session_state["slider_coolant"] = p_cfg["coolant"]
                st.session_state["slider_intake"] = p_cfg["intake"]
                st.session_state["slider_voltage"] = p_cfg["voltage"]
                st.session_state["slider_map"] = p_cfg["map"]
                st.session_state["slider_maf"] = p_cfg["maf"]
                st.session_state["slider_hunting"] = p_cfg["hunting"]
                st.session_state["active_preset"] = preset_choice

        # Initialize defaults if not present
        if "slider_rpm" not in st.session_state:
            st.session_state["slider_rpm"] = 750
            st.session_state["slider_speed"] = 0
            st.session_state["slider_throttle"] = 4.0
            st.session_state["slider_load"] = 18.0
            st.session_state["slider_trim"] = 0.5
            st.session_state["slider_o2"] = 0.45
            st.session_state["slider_coolant"] = 88
            st.session_state["slider_intake"] = 26
            st.session_state["slider_voltage"] = 14.10
            st.session_state["slider_map"] = 30.0
            st.session_state["slider_maf"] = 2.2
            st.session_state["slider_hunting"] = False

        # 3 Spacious Full-Width Domain Columns on the Main Page
        deck_col1, deck_col2, deck_col3 = st.columns(3)
        
        with deck_col1:
            st.markdown('<div class="control-card-title">🏎️ Powertrain & Driving Dynamics</div>', unsafe_allow_html=True)
            s_rpm = st.slider("Engine Speed (RPM)", 650, 4500, key="slider_rpm", step=50)
            s_speed = st.slider("Vehicle Speed (km/h)", 0, 140, key="slider_speed", step=5)
            s_throttle = st.slider("Throttle Position (throttle_pct %)", 0.0, 100.0, key="slider_throttle", step=1.0)
            s_load = st.slider("Calculated Engine Load (%)", 5.0, 100.0, key="slider_load", step=1.0)
            s_hunting = st.checkbox("Simulate Periodic RPM Hunting / Surge (0.15 Hz)", key="slider_hunting")

        with deck_col2:
            st.markdown('<div class="control-card-title">💨 Air Intake & Fuel Metering</div>', unsafe_allow_html=True)
            s_trim = st.slider("Total Fuel Trim (STFT + LTFT %)", -30.0, 35.0, key="slider_trim", step=0.5)
            s_o2 = st.slider("O2 Sensor Voltage (o2_b1s1_v Volts)", 0.05, 0.95, key="slider_o2", step=0.02)
            s_map = st.slider("Manifold Absolute Pressure MAP (kPa)", 25.0, 101.0, key="slider_map", step=1.0)
            s_maf = st.slider("Mass Air Flow MAF (g/s)", 1.5, 30.0, key="slider_maf", step=0.2)

        with deck_col3:
            st.markdown('<div class="control-card-title">⚡ Electrical & Thermal Balance</div>', unsafe_allow_html=True)
            s_voltage = st.slider("ECU / System Voltage (Volts)", 11.0, 14.8, key="slider_voltage", step=0.05)
            s_coolant = st.slider("Coolant Temp (coolant_c °C)", 20, 110, key="slider_coolant", step=1)
            s_intake = st.slider("Intake Air Temp (intake_air_c °C)", -5, 65, key="slider_intake", step=1)
            
            # Closed-Loop Status Indicator Pill
            if s_coolant >= 45:
                st.markdown('<div style="margin-top: 14px;"><span class="status-pill-green">🟢 Closed-Loop Active (O2 Feedback Enabled)</span></div>', unsafe_allow_html=True)
            else:
                st.markdown('<div style="margin-top: 14px;"><span class="status-pill-yellow">🟡 Cold Open-Loop (Warmup Enrichment Mode)</span></div>', unsafe_allow_html=True)

    # Synthesize live 30s continuous physical waveform
    window_df = synthesize_live_telemetry_window(
        rpm_center=float(s_rpm),
        speed_kph=float(s_speed),
        throttle_pct=float(s_throttle),
        load_pct=float(s_load),
        total_trim=float(s_trim),
        o2_v=float(s_o2),
        coolant_c=float(s_coolant),
        intake_c=float(s_intake),
        voltage_v=float(s_voltage),
        map_kpa=float(s_map),
        maf_gps=float(s_maf),
        hunting_active=s_hunting
    )
    features = extract_features_from_window(window_df)
    result = engine.evaluate(features)

elif active_df is not None:
    st.info(f"📁 Active Drive Session: **{selected_info}**")
    max_sec = len(active_df)
    window_start = st.slider("Select 30-Second Analysis Window (Timeline Sec):", 0, max(0, max_sec - 30), min(default_window_start, max(0, max_sec - 30)), 10)
    window_df = active_df.iloc[window_start:window_start + 30].reset_index(drop=True)
    features = extract_features_from_window(window_df)
    result = engine.evaluate(features)

else:
    st.warning("Please upload a CSV or switch to '🎛️ Live Virtual Sensor Deck' in the sidebar.")
    st.stop()

maf_reference_range = estimate_maf_reference_range(features)


# =====================================================================
# DISPLAY DASHBOARD (5 INTERACTIVE TABS)
# =====================================================================
tab1, tab2, tab3, tab4, tab5 = st.tabs([
    "📊 Diagnostic Overview & VHI", 
    "📈 Telemetry Waveforms", 
    "🔍 Dual-Nature Features",
    "🧠 Hybrid Architecture & Invariants",
    "📑 Dealership Demo Report"
])

with tab1:
    vhi_val = result.vehicle_health_index
    subsystems = result.subsystem_scores
    
    vhi_color = "#4CAF50" if vhi_val >= 85 else "#FF9800" if vhi_val >= 70 else "#F44336"
    
    col_vhi, col_f, col_c, col_e, col_t = st.columns(5)
    
    with col_vhi:
        st.markdown(f"""
        <div class="metric-card" style="border-left-color: {vhi_color};">
            <div class="metric-title">Vehicle Health Index (VHI)</div>
            <div class="metric-value" style="color: {vhi_color};">{vhi_val} <span style="font-size:16px;">/ 100</span></div>
            <small style="color: #bbb;">Grade: <b>{result.health_grade}</b></small>
        </div>
        """, unsafe_allow_html=True)
        
    with col_f:
        f_val = subsystems["fuel_system"]
        f_color = "normal" if f_val >= 90 else "inverse"
        st.metric("Fuel System Score", f"{f_val} / 100", delta=f"{f_val - 100:.0f} pts" if f_val < 100 else "Nominal", delta_color=f_color)
    with col_c:
        c_val = subsystems["combustion"]
        c_color = "normal" if c_val >= 90 else "inverse"
        st.metric("Engine & Combustion", f"{c_val} / 100", delta=f"{c_val - 100:.0f} pts" if c_val < 100 else "Nominal", delta_color=c_color)
    with col_e:
        e_val = subsystems["electrical"]
        e_color = "normal" if e_val >= 90 else "inverse"
        st.metric("Electrical Score", f"{e_val} / 100", delta=f"{e_val - 100:.0f} pts" if e_val < 100 else "Nominal", delta_color=e_color)
    with col_t:
        t_val = subsystems["thermal"]
        t_color = "normal" if t_val >= 90 else "inverse"
        st.metric("Cooling Score", f"{t_val} / 100", delta=f"{t_val - 100:.0f} pts" if t_val < 100 else "Nominal", delta_color=t_color)
        
    st.markdown("---")
    st.subheader("📡 Live OBD-II Physical Sensor Channels")
    m_col1, m_col2, m_col3, m_col4, m_col5 = st.columns(5)
    with m_col1:
        st.metric("Throttle Angle (throttle_pct)", f"{features.get('throttle_mean', 0.0):.1f}%")
    with m_col2:
        st.metric("Engine Load (engine_load_pct)", f"{features.get('engine_load_mean', 0.0):.1f}%")
    with m_col3:
        st.metric("Coolant Temp (coolant_c)", f"{features.get('coolant_mean', 0.0):.1f} °C")
    with m_col4:
        st.metric("Intake Air Temp (intake_air_c)", f"{features.get('intake_mean', 0.0):.1f} °C")
    with m_col5:
        o2_disp = features.get('o2_b1s1_v', 0.45)
        st.metric("O2 Sensor Voltage (o2_b1s1_v)", f"{o2_disp:.2f} V")

    st.markdown("---")
    st.subheader("Air-Path Context (MAP / MAF and Fuel Trim)")
    context_col, plausibility_col = st.columns(2)
    with context_col:
        st.metric("MAF / MAP ratio", f"{features['maf_map_ratio']:.3f}")
        st.info(describe_air_path_context(features))
        st.caption(
            "This is an operating-context hint, not a root-cause classification. "
            "Fuel trims may not be meaningful outside closed-loop operation."
        )

    with plausibility_col:
        st.markdown("**Generic MAF plausibility check**")
        if maf_reference_range is None:
            st.info("Unable to estimate an airflow reference range from these inputs.")
        else:
            maf_min, maf_max = maf_reference_range
            maf_value = features["maf_mean"]
            if maf_min <= maf_value <= maf_max:
                st.success(
                    f"MAF {maf_value:.1f} g/s is within the broad estimated range "
                    f"{maf_min:.1f}-{maf_max:.1f} g/s for the current RPM, MAP, and intake temperature."
                )
            else:
                st.warning(
                    f"MAF {maf_value:.1f} g/s is outside the broad estimated range "
                    f"{maf_min:.1f}-{maf_max:.1f} g/s for the current RPM, MAP, and intake temperature. "
                    "Review the readings and vehicle specifications."
                )
        st.caption(
            "Generic speed-density estimate (1.0-3.0 L engine, 45-95% volumetric efficiency). "
            "This check does not identify a failed part or change the ML result or VHI."
        )
    
    # Active Candidates Section
    st.subheader("🛠️ Option B: Hybrid Multi-Label Diagnostic Output")
    
    if result.candidates:
        st.warning(f"⚠️ **{len(result.candidates)} Subsystem Anomalies Confirmed** via Hybrid Decision Fusion")
        
        for cand in result.candidates:
            source = cand["detection_source"]
            if "Consensus" in source:
                badge_html = f'<span class="badge-consensus">🟢 {source}</span>'
            elif "Override" in source:
                badge_html = f'<span class="badge-override">🔵 {source}</span>'
            else:
                badge_html = f'<span class="badge-ml">🟣 {source}</span>'
                
            with st.expander(f"⚠️ {cand['display_name']} ({cand['subsystem']}) — Confidence: {int(cand['confidence']*100)}%", expanded=True):
                c1, c2 = st.columns([1, 2])
                with c1:
                    st.markdown(f"**Authority:**<br>{badge_html}", unsafe_allow_html=True)
                    st.markdown(f"**XGBoost Model Probability:** `{int(cand.get('raw_ml_prob', cand['confidence'])*100)}%`")
                    st.markdown(f"**Physics Guard Status:** `{'⚠️ Boundary Violated' if cand.get('physics_guard_active', False) else '✅ Within Bounds'}`")
                    st.markdown(f"**Subsystem Affected:** `{cand['subsystem']}`")
                with c2:
                    st.markdown(f"**Telemetry Evidence:**\n> {cand['evidence_reason']}")
                    st.markdown(f"**Physical Hypothesis:**\n> {cand['root_cause']}")
                    st.markdown(f"**Prescribed Workshop Action:**\n> 🔧 *{cand['workshop_test']}*")
    else:
        st.success("✅ **Clean Subsystem Health Certificate:** All physical bounds and machine learning classifiers confirm nominal operation. No active faults detected in this window.")

    # All Target Summary Table
    st.markdown("#### 📋 5-Target Subsystem Decision Matrix")
    summary_rows = []
    for cfg in TARGET_CONFIGS:
        tid = cfg["id"]
        is_active = result.fault_flags.get(tid, 0) == 1
        summary_rows.append({
            "Target Fault Pattern": cfg["name"],
            "Subsystem": cfg["subsystem"],
            "Status": "⚠️ DETECTED" if is_active else "✅ NOMINAL",
            "Detection Authority": result.detection_summary.get(tid, "Normal")
        })
    st.dataframe(pd.DataFrame(summary_rows), use_container_width=True)

with tab2:
    plot_df = active_df if active_df is not None else window_df
    if plot_df is not None:
        plot_title = "Drive Session Telemetry Plots (Full Drive)" if active_df is not None else "Simulated 30-Second Physical Telemetry Waveforms (From Live Sliders)"
        st.subheader(plot_title)
        c1, c2 = st.columns(2)
        with c1:
            st.line_chart(plot_df[['speed_kph', 'rpm']], height=250)
            st.caption("Vehicle Speed (km/h) & Engine RPM")
        with c2:
            trim_col = plot_df['stft_b1_pct'] + plot_df['ltft_b1_pct'] if 'stft_b1_pct' in plot_df else plot_df['total_trim'] if 'total_trim' in plot_df else None
            if trim_col is not None:
                st.line_chart(trim_col, height=250)
                st.caption("Combined Fuel Trim (STFT + LTFT %)")
        c3, c4 = st.columns(2)
        with c3:
            v_col = plot_df['ecu_voltage_v'] if 'ecu_voltage_v' in plot_df else plot_df.get('ELM_VOLTAGE')
            if v_col is not None:
                st.line_chart(v_col, height=250)
                st.caption("ECU / Alternator Voltage (V)")
        with c4:
            cool_col = plot_df['coolant_c'] if 'coolant_c' in plot_df else plot_df.get('COOLANT_TEMP')
            if cool_col is not None:
                st.line_chart(cool_col, height=250)
                st.caption("Coolant Temperature (°C)")
    else:
        st.info("Upload or select a file to view full driving graphs.")

with tab3:
    st.subheader("Extracted 30-Second Statistical & Physics Features (28 Vector Features)")
    feat_df = pd.DataFrame([features]).T.rename(columns={0: "Computed Value"})
    st.dataframe(feat_df, use_container_width=True)
    
    st.markdown("---")
    st.subheader("🔬 Multi-Sensor Machine Learning Feature Importance (Trained XGBoost)")
    st.caption("Demonstrates genuine non-linear multivariate dependency across multiple sensors (Zero Single-Feature Shortcuts):")
    
    rep_path = os.path.join(SERVING_DIR, "diagnostics", "models", "training_evaluation_report.json")
    if os.path.exists(rep_path):
        try:
            with open(rep_path, "r", encoding="utf-8") as rf:
                rep_data = json.load(rf)
            imp_dict = rep_data.get("feature_importances_per_target", {})
            if imp_dict:
                cols_imp = st.columns(len(imp_dict))
                for idx, (target_name, feats) in enumerate(imp_dict.items()):
                    with cols_imp[idx]:
                        short_name = target_name.replace("fault_", "").replace("_", " ").title()
                        st.markdown(f"**{short_name}**")
                        top_items = list(feats.items())[:6]
                        top_df = pd.DataFrame(top_items, columns=["Sensor Feature", "Importance"])
                        top_df["Importance"] = top_df["Importance"].apply(lambda v: f"{v*100:.1f}%")
                        st.dataframe(top_df, hide_index=True, use_container_width=True)
        except Exception as ex:
            st.caption(f"Feature importance display note: {ex}")

with tab4:
    st.subheader("🧠 Option B Architecture: Why Pure ML Isn't Enough & How Physics Bounds Protect Diagnostics")
    
    st.markdown(r"""
    ### The Fundamental Problem with Pure ML Telemetry
    In real-world OBD-II data and synthetic drive cycles, machine learning models (XGBoost, Random Forest, Neural Nets) 
    seek mathematical correlations. Sometimes, those correlations become **spurious shortcuts**:
    * **The Speed Bug Example:** In driving sessions, the vehicle often idles at a stop when the battery is heavily loaded. 
      A pure decision tree learns: `if speed == 0 and voltage < 12.0 -> Fault`. 
      If the user moves the speed slider from `0 km/h` to `5 km/h`, a pure ML model might drop the fault because it relied on `speed` rather than Ohm's law!
    
    ### How Option B Resolves This (5-Stage Hybrid Pipeline)
    ```
    ┌────────────────────────┐      ┌───────────────────────────────┐
    │ 30-Sec OBD-II Telemetry │ ───> │ 24 Extracted Physics Features │
    └────────────────────────┘      └───────────────────────────────┘
                                                    │
                 ┌──────────────────────────────────┴──────────────────────────────────┐
                 ▼                                                                     ▼
    ┌───────────────────────────┐                                         ┌──────────────────────────┐
    │  Stage 1: Multi-Output ML │                                         │ Stage 2: Physics Guards  │
    │  XGBoost Classifier       │                                         │ First-Principles Limits  │
    │  (Non-linear multivariate)│                                         │ (Ohm, Stoichiometry, etc)│
    └───────────────────────────┘                                         └──────────────────────────┘
                 │                                                                     │
                 └──────────────────────────────────┬──────────────────────────────────┘
                                                    ▼
                                    ┌───────────────────────────────┐
                                    │    Stage 3: Decision Fusion   │
                                    │  • Hybrid Consensus (ML+Phys) │
                                    │  • Safety Override (Enforced) │
                                    │  • Cold Suppression (O2 Loop) │
                                    └───────────────────────────────┘
                                                    │
                                                    ▼
                                    ┌───────────────────────────────┐
                                    │  Stage 4: Subsystem & VHI     │
                                    │  Fuel, Spark, Power, Coolant  │
                                    └───────────────────────────────┘
                                                    │
                                                    ▼
                                    ┌───────────────────────────────┐
                                    │  Stage 5: Actionable Workshop │
                                    │  Root Cause & Smoke/Bench Test│
                                    └───────────────────────────────┘
    ```
    
    ### The 5 Hard First-Principles Physics Invariants:
    1. **Ohm's Law & Alternator Rectification:** 
       If `running voltage < 12.45V` while `engine RPM > 500`, the battery is discharging into the ignition system. The alternator is physically failing, **irrespective of vehicle speed**.
    2. **Closed-Loop Stoichiometry (Lean):**
       If `coolant >= 45°C` and `total fuel trim (STFT + LTFT) >= +13.0%`, the ECU is compensating for unmetered air ingress (intake vacuum leak).
    3. **Closed-Loop Stoichiometry (Rich):**
       If `coolant >= 45°C` and `total fuel trim <= -10.5%`, the ECU is subtracting fuel due to excessive delivery or restricted airflow.
    4. **Cold Start Open-Loop Suppression:**
       While `coolant < 45°C`, narrow-band oxygen sensors are not at light-off temperature ($>300^\circ\text{C}$). Fuel trim alarms are physically suppressed to prevent false positives.
    5. **Thermodynamic Heat Balance:**
       If prolonged running occurs and `coolant < 62°C` while ambient air is warm, heat is being dumped into the radiator prematurely (thermostat stuck open).
    """)
    
    st.markdown("#### Decision Fusion Logic Table")
    fusion_table = [
        {"ML Classifier": "Triggered (1)", "Physics Guard": "Violated (1)", "Decision": "🟢 HYBRID CONSENSUS", "Confidence": "96% - 99%", "Behavior": "Both empirical data and physical laws confirm fault."},
        {"ML Classifier": "Not Triggered (0)", "Physics Guard": "Violated (1)", "Decision": "🔵 PHYSICS SAFETY OVERRIDE", "Confidence": "94%", "Behavior": "Physics catches ML shortcut (e.g. speed bug) and enforces ground truth."},
        {"ML Classifier": "Triggered (1)", "Physics Guard": "Nominal (0)", "Decision": "🟣 ML PATTERN MATCH", "Confidence": "ML Prob (70-90%)", "Behavior": "Early multivariate detection before hard physical boundary."},
        {"ML Classifier": "Not Triggered (0)", "Physics Guard": "Nominal (0)", "Decision": "✅ NORMAL", "Confidence": "100%", "Behavior": "Subsystem operating within normal engineering envelope."}
    ]
    st.dataframe(pd.DataFrame(fusion_table), use_container_width=True)

with tab5:
    st.markdown("### 📑 Dealership Vehicle Inspection & Diagnostic Demo Report")
    st.caption("Official Pre-Purchase & Workshop Diagnostic Assessment Certificate")
    
    # Inspection Header Box
    rep_vhi = result.vehicle_health_index
    rep_grade = result.health_grade
    v_badge_color = "#2e7d32" if rep_vhi >= 85 else "#f57c00" if rep_vhi >= 70 else "#c62828"
    
    st.markdown(f"""
    <div style="background-color: #0e1117; padding: 20px; border-radius: 12px; border: 1px solid #30363d; margin-bottom: 20px;">
        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #21262d; padding-bottom: 12px; margin-bottom: 15px;">
            <div>
                <h2 style="margin: 0; color: #ffffff; font-size: 24px;">⚡ IgnitionAI Powertrain Certificate</h2>
                <span style="color: #8b949e; font-size: 13px;">Evidence-Based Automotive Diagnostic Platform • Dealership Triage Edition</span>
            </div>
            <div style="text-align: right;">
                <span style="background-color: {v_badge_color}; color: #ffffff; padding: 6px 14px; border-radius: 20px; font-weight: 700; font-size: 15px;">
                    {rep_grade} (VHI: {rep_vhi}/100)
                </span>
            </div>
        </div>
        <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; font-size: 13px; color: #c9d1d9;">
            <div><b>Inspection Source:</b><br><span style="color: #58a6ff;">{selected_info or 'Live Simulation Feed'}</span></div>
            <div><b>Protocol:</b><br>10-Min Indian Road Drive Test</div>
            <div><b>Engine Status:</b><br>{'Closed-Loop Warmed Up' if features.get('coolant_mean', 85) >= 45 else 'Cold Open-Loop'}</div>
            <div><b>Diagnostic Engine:</b><br>Dual-Nature XGBoost + Physics Guards</div>
        </div>
    </div>
    """, unsafe_allow_html=True)
    
    # 4 Subsystems Scorecard
    st.subheader("Subsystem Health Evaluation")
    s_col1, s_col2, s_col3, s_col4 = st.columns(4)
    with s_col1:
        f_s = result.subsystem_scores['fuel_system']
        st.metric("Fuel Delivery", f"{f_s}/100", delta="Nominal" if f_s == 100 else f"-{100-f_s} pts", delta_color="normal" if f_s >= 85 else "inverse")
    with s_col2:
        c_s = result.subsystem_scores['combustion']
        st.metric("Combustion Stability", f"{c_s}/100", delta="Nominal" if c_s == 100 else f"-{100-c_s} pts", delta_color="normal" if c_s >= 85 else "inverse")
    with s_col3:
        e_s = result.subsystem_scores['electrical']
        st.metric("Electrical / Charging", f"{e_s}/100", delta="Nominal" if e_s == 100 else f"-{100-e_s} pts", delta_color="normal" if e_s >= 85 else "inverse")
    with s_col4:
        t_s = result.subsystem_scores['thermal']
        st.metric("Engine Thermal Management", f"{t_s}/100", delta="Nominal" if t_s == 100 else f"-{100-t_s} pts", delta_color="normal" if t_s >= 85 else "inverse")
        
    st.markdown("---")
    
    # Diagnostic Findings Table
    st.subheader("Confirmed Diagnostic Findings")
    if result.candidates:
        for idx, cand in enumerate(result.candidates, 1):
            st.markdown(f"#### #{idx}. {cand['display_name']}")
            st.markdown(f"- **Subsystem:** `{cand['subsystem']}`")
            st.markdown(f"- **Detection Authority:** `{cand['detection_source']}` (Confidence: **{int(cand['confidence']*100)}%**)")
            st.markdown(f"- **Observed Telemetry Evidence:** {cand['evidence_reason']}")
            st.markdown(f"- **Physical Root Cause Hypothesis:** {cand['root_cause']}")
            st.markdown(f"- **Workshop Verification Procedure:** 🔧 *{cand['workshop_test']}*")
            st.markdown("")
    else:
        st.success("🎉 **No Mechanical or Electrical Faults Detected:** All telemetry parameters comply with physical and empirical health standards.")

    st.markdown("---")
    
    # Workshop Prescription Action Checklist
    st.subheader("Mechanic Action Plan & Prescribed Tests")
    if result.candidates:
        st.info("The following physical workshop tests are required prior to customer delivery or repair sign-off:")
        for idx, cand in enumerate(result.candidates, 1):
            st.checkbox(f"**Step {idx}:** {cand['workshop_test']} — *Targeting: {cand['display_name']}*", value=False)
    else:
        st.write("✅ Vehicle is qualified for acquisition or release. Standard fluid inspection recommended.")
        
    st.markdown("---")
    
    # Download Demo Report
    report_md = f"""# IgnitionAI Vehicle Inspection & Diagnostic Report
Generated: {pd.Timestamp.now().strftime('%Y-%m-%d %H:%M:%S')}
Session: {selected_info or 'Live Telemetry Window'}

## Vehicle Health Index: {rep_vhi} / 100 ({rep_grade})

### Subsystem Scores:
- Fuel Delivery: {result.subsystem_scores['fuel_system']}/100
- Combustion Stability: {result.subsystem_scores['combustion']}/100
- Electrical System: {result.subsystem_scores['electrical']}/100
- Engine Cooling: {result.subsystem_scores['thermal']}/100

### Key Telemetry Invariants:
- Engine Speed: {features.get('rpm_mean', 0):.1f} RPM (Detrended Std: {features.get('rpm_det_std', 0):.1f})
- Total Fuel Trim: {features.get('trim_mean', 0):.2f}% (Closed-Loop Residual: {features.get('stoich_closed_loop_residual', 0):.2f}%)
- System Voltage: {features.get('voltage_mean', 0):.2f}V (Min: {features.get('voltage_min', 0):.2f}V, Margin: {features.get('voltage_physics_margin', 0):.2f}V)
- Coolant Temp: {features.get('coolant_mean', 0):.1f}°C (Warmup Deficit: {features.get('thermal_deficit_score', 0):.1f})
- MAF Volumetric Ratio: {features.get('maf_physics_ratio', 0):.3f}

### Diagnostic Findings ({len(result.candidates)} Active):
"""
    if result.candidates:
        for idx, c in enumerate(result.candidates, 1):
            report_md += f"""
#### {idx}. {c['display_name']} ({c['subsystem']})
- Authority: {c['detection_source']} (Confidence: {int(c['confidence']*100)}%)
- Evidence: {c['evidence_reason']}
- Hypothesis: {c['root_cause']}
- Action Plan: {c['workshop_test']}
"""
    else:
        report_md += "\n- Clean Health Record: No active fault signatures detected.\n"

    st.download_button(
        label="📥 Download Official Inspection Report (.md)",
        data=report_md,
        file_name=f"IgnitionAI_Inspection_Report_{int(rep_vhi)}.md",
        mime="text/markdown"
    )

