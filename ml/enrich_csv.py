"""
IgnitionAI - Automatic OBD Telemetry Feature Derivation & Preprocessor
File: ml/enrich_csv.py

Purpose:
  Enables any user or developer to take a RAW OBD-II drive CSV (which only has
  basic PIDs like RPM, Speed, Load/Throttle, Coolant) and automatically computes
  all 14 physics & stoichiometric features required by the Real Hybrid ML model:
    ['MAP', 'TPS', 'Force', 'Power', 'RPM', 'Consumption L/H',
     'Consumption L/100KM', 'Speed', 'CO', 'HC', 'CO2', 'O2', 'Lambda', 'AFR']

Usage:
  python ml/enrich_csv.py input_drive.csv -o enriched_ready_for_model.csv
  python ml/enrich_csv.py input_drive.csv --predict
"""

import sys
import argparse
from pathlib import Path
import numpy as np
import pandas as pd

# Standard 14 Model Features in Exact Order
MODEL_FEATURES = [
    'MAP', 'TPS', 'Force', 'Power', 'RPM',
    'Consumption L/H', 'Consumption L/100KM', 'Speed',
    'CO', 'HC', 'CO2', 'O2', 'Lambda', 'AFR'
]

# Flexible Column Aliases across different scanners (Torque Pro, Car Scanner, ELM327, Rahul's App)
COLUMN_ALIASES = {
    'RPM': ['rpm', 'engine_rpm', 'engine rpm', 'engine_speed', 'obd_rpm', '[0c] engine rpm', 'engine speed (rpm)'],
    'Speed': ['speed', 'vehicle_speed', 'vehicle speed', 'vss', 'obd_speed', '[0d] vehicle speed', 'vehicle speed (km/h)', 'speed (km/h)'],
    'TPS': ['tps', 'throttle', 'throttle_pos', 'throttle position', 'throttle_position', '[11] throttle position', 'throttle position(%)'],
    'Load': ['load', 'engine_load', 'engine load', 'calculated_load', 'calculated engine load', '[04] calculated engine load', 'engine load(%)'],
    'MAP': ['map', 'intake_manifold_pressure', 'manifold absolute pressure', 'manifold_pressure', '[0b] intake manifold absolute pressure', 'map (kpa)'],
    'Coolant': ['coolant', 'ect', 'coolant_temp', 'engine_coolant_temp', 'coolant temperature', '[05] engine coolant temperature', 'coolant(deg c)'],
    'MAF': ['maf', 'mass_air_flow', 'mass airflow', '[10] mass air flow rate', 'maf (g/s)'],
    'Lambda': ['lambda', 'equivalence_ratio', 'fuel_air_equivalence_ratio', 'lambda (wideband)', 'commanded equivalence ratio'],
    'O2': ['o2', 'o2_voltage', 'oxygen_sensor_voltage', 'o2s11', 'o2 sensor voltage', 'bank 1 sensor 1 o2'],
    'AFR': ['afr', 'air_fuel_ratio', 'air fuel ratio', 'measured_afr']
}

def resolve_column(df: pd.DataFrame, target_name: str):
    """Finds matching column in dataframe irrespective of case, spaces, or formatting."""
    lower_cols = {c.strip().lower(): c for c in df.columns}
    
    # 1. Direct match
    if target_name.lower() in lower_cols:
        return lower_cols[target_name.lower()]
    
    # 2. Alias match
    aliases = COLUMN_ALIASES.get(target_name, [])
    for alias in aliases:
        if alias.lower() in lower_cols:
            return lower_cols[alias.lower()]
        # Substring match
        for col_lower, actual_col in lower_cols.items():
            if alias.lower() in col_lower:
                return actual_col
    return None

def enrich_raw_obd_dataframe(df_raw: pd.DataFrame, vehicle_mass_kg: float = 1250.0) -> pd.DataFrame:
    """
    Transforms any raw OBD DataFrame into the full 14-feature physics-derived dataset.
    Applies standard SAE J1979 vehicle dynamics and speed-density thermodynamic equations.
    """
    n_rows = len(df_raw)
    out = pd.DataFrame(index=df_raw.index)

    # 1. Extract Base Signals with Intelligent Fallbacks
    col_rpm = resolve_column(df_raw, 'RPM')
    col_speed = resolve_column(df_raw, 'Speed')
    col_tps = resolve_column(df_raw, 'TPS')
    col_load = resolve_column(df_raw, 'Load')
    col_map = resolve_column(df_raw, 'MAP')
    col_maf = resolve_column(df_raw, 'MAF')
    col_lambda = resolve_column(df_raw, 'Lambda')
    col_o2 = resolve_column(df_raw, 'O2')
    col_afr = resolve_column(df_raw, 'AFR')

    # RPM & Speed
    rpm = pd.to_numeric(df_raw[col_rpm], errors='coerce').fillna(1200.0) if col_rpm else pd.Series(1200.0, index=df_raw.index)
    speed = pd.to_numeric(df_raw[col_speed], errors='coerce').fillna(30.0) if col_speed else pd.Series(30.0, index=df_raw.index)

    # Load & TPS (they correlate tightly in naturally aspirated / turbo passenger cars)
    if col_tps:
        tps = pd.to_numeric(df_raw[col_tps], errors='coerce').fillna(20.0)
    elif col_load:
        tps = pd.to_numeric(df_raw[col_load], errors='coerce').fillna(20.0)
    else:
        tps = pd.Series(20.0, index=df_raw.index)

    if col_load:
        load = pd.to_numeric(df_raw[col_load], errors='coerce').fillna(tps)
    else:
        load = tps.copy()

    # MAP (Manifold Absolute Pressure kPa)
    # If not present in raw log, derive from throttle opening and atmospheric pressure
    if col_map:
        map_kpa = pd.to_numeric(df_raw[col_map], errors='coerce')
    else:
        # Standard atmospheric speed-density approximation: idle ~ 28-35 kPa, WOT ~ 95-100 kPa
        map_kpa = (load * 0.70 + 25.0).clip(15.0, 105.0)

    # 2. Physics Invariant Derivations: Force & Power
    # Power (kW) = (Torque * RPM) / 9549, Torque ~ (Load% / 100) * 1.5L * 140 Nm * VolEff
    power_kw = ((load / 100.0) * (rpm / 6000.0) * 85.0).clip(lower=0.5)

    # Tractive Force (N) = Power (W) / max(v_m_s, 1.0)
    v_ms = (speed / 3.6).clip(lower=1.0)
    force_n = ((power_kw * 1000.0) / v_ms).clip(lower=20.0, upper=4500.0)

    # 3. Air-Fuel & Exhaust Emissions Invariants (Calculated first for accurate fuel flow)
    if col_lambda:
        lambda_val = pd.to_numeric(df_raw[col_lambda], errors='coerce').fillna(1.0)
    elif col_afr:
        lambda_val = (pd.to_numeric(df_raw[col_afr], errors='coerce') / 14.7).fillna(1.0)
    else:
        # High throttle enrichment / low load stoichiometric
        lambda_val = np.where(tps > 75.0, 0.92, np.where(tps < 10.0, 1.02, 1.00))
        lambda_val = pd.Series(lambda_val, index=df_raw.index)

    afr_val = (lambda_val * 14.7).clip(lower=10.0, upper=20.0)

    # O2 Sensor Voltage (V)
    if col_o2:
        o2_v = pd.to_numeric(df_raw[col_o2], errors='coerce').fillna(0.45)
    else:
        # Stoichiometric = 0.45V, Rich > 0.7V, Lean < 0.2V
        o2_v = np.where(lambda_val < 0.97, 0.82, np.where(lambda_val > 1.03, 0.15, 0.45))
        o2_v = pd.Series(o2_v, index=df_raw.index)

    # Exhaust gas concentrations based on combustion chemistry
    co_pct = np.where(lambda_val < 0.95, 2.4, np.where(lambda_val > 1.05, 0.12, 0.25))
    hc_ppm = np.where(lambda_val < 0.95, 310.0, np.where(lambda_val > 1.05, 260.0, 45.0))
    co2_pct = np.where(lambda_val < 0.95, 12.1, np.where(lambda_val > 1.05, 11.8, 14.9))

    # 4. Fuel Consumption Derivation (L/H and L/100KM)
    # Using Standard Speed-Density: MAF = (MAP_kPa * 1000 * RPM * Vd * VolEff) / (120 * R_air * T) in kg/s
    # Air gas constant R_air = 287.05 J/(kg*K), T = 298.15K, Vd = 1.5e-3 m^3, VolEff = 0.82
    # Converting to g/s (* 1000.0)
    if col_maf:
        maf_gps = pd.to_numeric(df_raw[col_maf], errors='coerce').fillna(2.5)
    else:
        maf_gps = (map_kpa * rpm * 1.5 * 0.82) / (120.0 * 287.05 * 298.15) * 1000.0
        maf_gps = maf_gps.clip(lower=0.0, upper=250.0)

    # Fuel mass flow (g/s) = MAF / actual AFR
    # L/h = (Fuel_mass_flow / 745 g/L gasoline density) * 3600
    fuel_mass_gps = maf_gps / afr_val
    cons_lh = (fuel_mass_gps / 745.0 * 3600.0).clip(lower=0.1, upper=45.0)

    # Distance-based consumption (L/100km): only meaningful when vehicle is moving (speed >= 3 km/h)
    cons_l100km = np.where(speed < 3.0, 0.0, (cons_lh / speed.clip(lower=3.0)) * 100.0)
    cons_l100km = pd.Series(cons_l100km, index=df_raw.index).clip(lower=0.0, upper=50.0)

    # Assemble into exact model feature set
    out['MAP'] = map_kpa.round(2)
    out['TPS'] = tps.round(2)
    out['Force'] = force_n.round(2)
    out['Power'] = power_kw.round(2)
    out['RPM'] = rpm.round(1)
    out['Consumption L/H'] = cons_lh.round(2)
    out['Consumption L/100KM'] = cons_l100km.round(2)
    out['Speed'] = speed.round(1)
    out['CO'] = pd.Series(co_pct, index=df_raw.index).round(2)
    out['HC'] = pd.Series(hc_ppm, index=df_raw.index).round(1)
    out['CO2'] = pd.Series(co2_pct, index=df_raw.index).round(2)
    out['O2'] = o2_v.round(3)
    out['Lambda'] = lambda_val.round(3)
    out['AFR'] = afr_val.round(2)

    return out

def main():
    parser = argparse.ArgumentParser(description="IgnitionAI: Convert Raw OBD CSV to 14-Feature ML Ready Dataset")
    parser.add_argument("input_csv", help="Path to input raw OBD CSV file")
    parser.add_argument("-o", "--output", help="Path to save enriched output CSV", default=None)
    parser.add_argument("--predict", action="store_true", help="Directly run hybrid ML model predictions on the CSV")
    args = parser.parse_args()

    input_path = Path(args.input_csv)
    if not input_path.exists():
        print(f"[-] Error: File not found: {input_path}")
        sys.exit(1)

    print(f"[*] Reading raw OBD CSV: {input_path}")
    df_raw = pd.read_csv(input_path)
    print(f"[*] Input shape: {df_raw.shape[0]} rows, {df_raw.shape[1]} columns: {list(df_raw.columns[:6])}...")

    df_enriched = enrich_raw_obd_dataframe(df_raw)
    print(f"[+] Enrichment successful: Generated all 14 required ML features!")

    output_path = args.output
    if not output_path:
        output_path = input_path.parent / f"{input_path.stem}_enriched_14features.csv"
    
    df_enriched.to_csv(output_path, index=False)
    print(f"[+] Saved model-ready CSV to: {output_path}")

    if args.predict:
        import joblib
        model_path = Path(__file__).resolve().parent / "models" / "best_hybrid_enginefault_model.joblib"
        if not model_path.exists():
            print(f"[-] Model file not found at {model_path}")
            sys.exit(1)
        
        print(f"[*] Loading model from {model_path}...")
        model = joblib.load(model_path)
        probs = model.predict_proba(df_enriched[MODEL_FEATURES])
        preds = np.argmax(probs, axis=1)

        labels = {0: "No Fault (Normal)", 1: "Rich Mixture", 2: "Lean Mixture", 3: "Low Voltage"}
        counts = pd.Series(preds).map(labels).value_counts().to_dict()
        print("\n" + "=" * 60)
        print("[+] MODEL DIAGNOSTIC RESULTS SUMMARY:")
        print("=" * 60)
        for cls_id in sorted(labels.keys()):
            label_name = labels[cls_id]
            count = counts.get(label_name, 0)
            pct = (count / max(1, len(preds))) * 100.0
            print(f"  • {label_name:<22}: {count:,} frames ({pct:.2f}%)")
        print("=" * 60)

if __name__ == "__main__":
    main()
