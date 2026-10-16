"""
End-to-End Multi-Label ML Pipeline for IgnitionAI
1. 30-Second Window Feature Extraction (Physics & Statistical Residuals)
2. Drive-Level Group Splitting (Train / Val / Test by capture_id - Zero Data Leakage)
3. Cross-Validation (GroupKFold)
4. Multi-Algorithm Benchmarking (Logistic Regression, Random Forest, Extra Trees, LightGBM, XGBoost)
5. Hyperparameter Tuning on Top Models
6. Complete Metric Evaluation (Precision, Recall, F1, Subset Accuracy, Hamming Loss)
7. Model Persistence & Comprehensive Report
"""
import os
import json
import joblib
import numpy as np
import pandas as pd
from scipy.stats import kurtosis
from sklearn.model_selection import GroupShuffleSplit, GroupKFold
from sklearn.multioutput import MultiOutputClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, ExtraTreesClassifier
from sklearn.metrics import (
    precision_score, recall_score, f1_score,
    accuracy_score, hamming_loss, classification_report, multilabel_confusion_matrix
)
from xgboost import XGBClassifier
from lightgbm import LGBMClassifier

DATA_PATH = r"D:\IgnitionAI\serving\data\ml_synthetic\master_synthetic_75_drives.csv"
MODEL_DIR = r"D:\IgnitionAI\serving\diagnostics\models"
WINDOW_FEATURES_PATH = r"D:\IgnitionAI\serving\data\ml_synthetic\ml_window_features.csv"

TARGET_COLUMNS = [
    'fault_lean_trim',
    'fault_rich_trim',
    'fault_combustion_hunting',
    'fault_alternator_sag',
    'fault_thermal_delay'
]


# =====================================================================
# STEP 1: 30-SECOND WINDOW FEATURE EXTRACTION
# =====================================================================
def extract_window_features(df: pd.DataFrame, window_size: int = 30, stride: int = 10) -> pd.DataFrame:
    """
    Slices each 600s drive into 30-second sliding windows with 10s stride.
    Calculates 32 features combining statistical aggregations with First-Principles
    Physics Invariants (Nyquist hunting oscillations, stoichiometric closed-loop residuals,
    Speed-Density airflow indices, Newton thermal deficits, and alternator droop margins).
    """
    print(f"Extracting 30-second Dual-Nature (Physics + ML) window features (stride={stride}s)...")
    records = []
    
    # Calculate derived total trim
    df = df.copy()
    df['total_trim'] = df['stft_b1_pct'] + df['ltft_b1_pct']
    
    # Process each drive independently
    for capture_id, drive_df in df.groupby('capture_id'):
        drive_df = drive_df.sort_values('timestamp').reset_index(drop=True)
        vehicle_id = drive_df['vehicle_id'].iloc[0]
        n_rows = len(drive_df)
        
        for start_idx in range(0, n_rows - window_size + 1, stride):
            end_idx = start_idx + window_size
            win = drive_df.iloc[start_idx:end_idx]
            
            # --- Physical & Statistical Features ---
            # 1. Combustion / RPM dynamics (Dual Nature: Statistics + Nyquist Autocorrelation)
            rpm_vals = win['rpm'].to_numpy()
            rpm_mean = float(np.mean(rpm_vals))
            rpm_std = float(np.std(rpm_vals))
            
            # Local linear detrending (extracts high-frequency mechanical hunting oscillation)
            t_win = np.arange(window_size)
            detrended_rpm = rpm_vals - np.polyval(np.polyfit(t_win, rpm_vals, 1), t_win)
            det_std = float(np.std(detrended_rpm))
            c0 = det_std**2 + 1e-6
            r3 = float(np.mean(detrended_rpm[:-3] * detrended_rpm[3:]) / c0)
            r7 = float(np.mean(detrended_rpm[:-7] * detrended_rpm[7:]) / c0)
            # Hunting metric: 0.15 Hz sinusoidal oscillation has negative r3 and positive r7
            hunting_surge_metric = float((r7 - r3) * det_std)
            rpm_kurt = float(kurtosis(detrended_rpm)) if det_std > 1.0 else 0.0
            
            # 2. Driving context
            speed_mean = float(win['speed_kph'].mean())
            speed_std = float(win['speed_kph'].std())
            idle_fraction = float((win['speed_kph'] < 2).mean())
            throttle_mean = float(win['throttle_pct'].mean())
            engine_load_mean = float(win['engine_load_pct'].mean())
            
            # 3. Air-path / MAP / MAF & Speed-Density Physics Residual
            map_mean = float(win['map_kpa'].mean())
            map_std = float(win['map_kpa'].std())
            maf_mean = float(win['maf_gps'].mean())
            maf_map_ratio = float(maf_mean / max(1.0, map_mean))
            intake_mean = float(win['intake_air_c'].mean())
            t_intake_k = intake_mean + 273.15
            speed_density_index = (rpm_mean * map_mean) / t_intake_k
            maf_physics_ratio = float(maf_mean / max(0.1, speed_density_index))
            
            # 4. Fuel Trim dynamics & Stoichiometric Physics Residual
            trim_mean = float(win['total_trim'].mean())
            trim_std = float(win['total_trim'].std())
            trim_max = float(win['total_trim'].max())
            trim_min = float(win['total_trim'].min())
            trim_exceed_fraction = float(((win['total_trim'] >= 12.0) | (win['total_trim'] <= -10.0)).mean())
            
            # 5. Thermal dynamics & Newton's Cooling Deficit
            coolant_mean = float(win['coolant_c'].mean())
            coolant_warmup_rate = float((win['coolant_c'].iloc[-1] - win['coolant_c'].iloc[0]) / (window_size / 60.0))
            coolant_intake_delta = float(coolant_mean - intake_mean)
            
            # Physics-Informed Residuals:
            closed_loop_flag = 1.0 if coolant_mean >= 45.0 else 0.0
            stoich_closed_loop_residual = float(closed_loop_flag * trim_mean)
            thermal_deficit_score = float(max(0.0, 65.0 - coolant_mean) * min(1.0, start_idx / 180.0))
            
            # 6. Electrical / Charging dynamics & Ohm's Law Alternator Margin
            v_mean = float(win['ecu_voltage_v'].mean())
            v_min = float(win['ecu_voltage_v'].min())
            v_diff_ripple = float(np.std(np.diff(win['ecu_voltage_v'].to_numpy())))
            voltage_physics_margin = float(v_min - 12.45)
            
            # --- Window Multi-Label Target Definition ---
            targets = {}
            for target_col in TARGET_COLUMNS:
                active_fraction = float(win[target_col].mean())
                targets[target_col] = 1 if active_fraction >= 0.30 else 0
                
            row = {
                "capture_id": capture_id,
                "vehicle_id": vehicle_id,
                "window_start_sec": start_idx,
                "window_end_sec": end_idx,
                # Base Features
                "rpm_mean": rpm_mean,
                "rpm_std": rpm_std,
                "rpm_kurtosis": rpm_kurt,
                "speed_mean": speed_mean,
                "speed_std": speed_std,
                "idle_fraction": idle_fraction,
                "throttle_mean": throttle_mean,
                "engine_load_mean": engine_load_mean,
                "map_mean": map_mean,
                "map_std": map_std,
                "maf_mean": maf_mean,
                "maf_map_ratio": maf_map_ratio,
                "trim_mean": trim_mean,
                "trim_std": trim_std,
                "trim_max": trim_max,
                "trim_min": trim_min,
                "trim_exceed_fraction": trim_exceed_fraction,
                "coolant_mean": coolant_mean,
                "coolant_warmup_rate": coolant_warmup_rate,
                "intake_mean": intake_mean,
                "coolant_intake_delta": coolant_intake_delta,
                "voltage_mean": v_mean,
                "voltage_min": v_min,
                "voltage_ripple_std": v_diff_ripple,
                # Physics-Informed Dual-Nature Features
                "rpm_det_std": det_std,
                "rpm_r3": r3,
                "rpm_r7": r7,
                "hunting_surge_metric": hunting_surge_metric,
                "maf_physics_ratio": maf_physics_ratio,
                "stoich_closed_loop_residual": stoich_closed_loop_residual,
                "thermal_deficit_score": thermal_deficit_score,
                "voltage_physics_margin": voltage_physics_margin,
                # Labels
                **targets
            }
            records.append(row)
            
    features_df = pd.DataFrame(records)
    features_df.to_csv(WINDOW_FEATURES_PATH, index=False)
    print(f"Extracted {len(features_df)} window feature records across {df['capture_id'].nunique()} drives.")
    print(f"Saved window features to: {WINDOW_FEATURES_PATH}")
    return features_df


# =====================================================================
# STEP 2: DRIVE-LEVEL GROUP TRAIN / VAL / TEST SPLIT
# =====================================================================
def split_by_drive(df: pd.DataFrame, test_ratio: float = 0.17, val_ratio: float = 0.16, seed: int = 42):
    """
    Strictly splits dataset by complete capture_id/drive.
    Guarantees ZERO drive-level leakage between train, validation, and test.
    """
    gss_test = GroupShuffleSplit(n_splits=1, test_size=test_ratio, random_state=seed)
    train_val_idx, test_idx = next(gss_test.split(df, groups=df['capture_id']))
    
    train_val_df = df.iloc[train_val_idx].reset_index(drop=True)
    test_df = df.iloc[test_idx].reset_index(drop=True)
    
    # Split train_val into train and validation
    val_rel_ratio = val_ratio / (1.0 - test_ratio)
    gss_val = GroupShuffleSplit(n_splits=1, test_size=val_rel_ratio, random_state=seed)
    train_idx, val_idx = next(gss_val.split(train_val_df, groups=train_val_df['capture_id']))
    
    train_df = train_val_df.iloc[train_idx].reset_index(drop=True)
    val_df = train_val_df.iloc[val_idx].reset_index(drop=True)
    
    print("\n--- Drive-Level Split (Zero Leakage) ---")
    print(f"Train Set:      {train_df['capture_id'].nunique()} drives | {len(train_df)} windows")
    print(f"Validation Set: {val_df['capture_id'].nunique()} drives | {len(val_df)} windows")
    print(f"Test Set:       {test_df['capture_id'].nunique()} drives | {len(test_df)} windows")
    return train_df, val_df, test_df


# =====================================================================
# STEP 3: BENCHMARK MULTI-OUTPUT ALGORITHMS & CROSS-VALIDATION
# =====================================================================
def evaluate_predictions(y_true: np.ndarray, y_pred: np.ndarray, target_names: list[str]) -> dict:
    """Calculates comprehensive multi-label evaluation metrics."""
    res = {
        "subset_accuracy": float(accuracy_score(y_true, y_pred)),
        "hamming_loss": float(hamming_loss(y_true, y_pred)),
        "macro_f1": float(f1_score(y_true, y_pred, average="macro", zero_division=0)),
        "micro_f1": float(f1_score(y_true, y_pred, average="micro", zero_division=0)),
        "weighted_f1": float(f1_score(y_true, y_pred, average="weighted", zero_division=0)),
        "per_fault": {}
    }
    
    for idx, name in enumerate(target_names):
        p = float(precision_score(y_true[:, idx], y_pred[:, idx], zero_division=0))
        r = float(recall_score(y_true[:, idx], y_pred[:, idx], zero_division=0))
        f = float(f1_score(y_true[:, idx], y_pred[:, idx], zero_division=0))
        res["per_fault"][name] = {"precision": round(p, 4), "recall": round(r, 4), "f1": round(f, 4)}
        
    return res


def benchmark_models(train_df, val_df, feature_cols):
    """Benchmarks 5 diverse ML algorithms on train and validation sets."""
    X_train = train_df[feature_cols].to_numpy()
    Y_train = train_df[TARGET_COLUMNS].to_numpy()
    
    X_val = val_df[feature_cols].to_numpy()
    Y_val = val_df[TARGET_COLUMNS].to_numpy()
    
    models = {
        "Logistic Regression (Baseline)": MultiOutputClassifier(LogisticRegression(max_iter=1000, random_state=42)),
        "Random Forest": MultiOutputClassifier(RandomForestClassifier(n_estimators=100, max_depth=8, random_state=42)),
        "Extra Trees": MultiOutputClassifier(ExtraTreesClassifier(n_estimators=100, max_depth=8, random_state=42)),
        "LightGBM": MultiOutputClassifier(LGBMClassifier(n_estimators=100, max_depth=5, learning_rate=0.08, colsample_bytree=0.65, random_state=42, verbose=-1)),
        "XGBoost": MultiOutputClassifier(XGBClassifier(n_estimators=100, max_depth=5, learning_rate=0.08, colsample_bytree=0.65, subsample=0.85, random_state=42, eval_metric="logloss"))
    }
    
    print("\n" + "=" * 75)
    print("BENCHMARKING MULTI-LABEL ALGORITHMS (Validation Set Evaluation)")
    print("=" * 75)
    
    results = {}
    for name, model in models.items():
        model.fit(X_train, Y_train)
        preds = model.predict(X_val)
        eval_metrics = evaluate_predictions(Y_val, preds, TARGET_COLUMNS)
        results[name] = {"model": model, "metrics": eval_metrics}
        
        print(f"\nModel: {name}")
        print(f"  Subset Accuracy: {eval_metrics['subset_accuracy']*100:.2f}% | Hamming Loss: {eval_metrics['hamming_loss']:.4f}")
        print(f"  Macro F1:        {eval_metrics['macro_f1']*100:.2f}% | Micro F1:      {eval_metrics['micro_f1']*100:.2f}%")
        for fault, scores in eval_metrics['per_fault'].items():
            print(f"    - {fault:25s}: P={scores['precision']*100:5.1f}% | R={scores['recall']*100:5.1f}% | F1={scores['f1']*100:5.1f}%")
            
    return results


# =====================================================================
# STEP 4: HYPERPARAMETER TUNING & FINAL TEST EVALUATION
# =====================================================================
def tune_and_train_best(train_df, val_df, test_df, feature_cols):
    """
    Performs grid tuning on top candidate (XGBoost), trains final model,
    and conducts held-out test evaluation.
    """
    print("\n" + "=" * 75)
    print("HYPERPARAMETER TUNING ON TOP CANDIDATE (XGBoost)")
    print("=" * 75)
    
    # Combine Train + Val for final GroupKFold tuning
    train_val_df = pd.concat([train_df, val_df], ignore_index=True)
    X_train_val = train_val_df[feature_cols].to_numpy()
    Y_train_val = train_val_df[TARGET_COLUMNS].to_numpy()
    groups_train_val = train_val_df['capture_id'].to_numpy()
    
    X_test = test_df[feature_cols].to_numpy()
    Y_test = test_df[TARGET_COLUMNS].to_numpy()
    
    param_grid = [
        {"n_estimators": 120, "max_depth": 4, "learning_rate": 0.05, "subsample": 0.85, "colsample_bytree": 0.65},
        {"n_estimators": 150, "max_depth": 5, "learning_rate": 0.06, "subsample": 0.85, "colsample_bytree": 0.65},
        {"n_estimators": 180, "max_depth": 5, "learning_rate": 0.04, "subsample": 0.90, "colsample_bytree": 0.70},
    ]
    
    gkf = GroupKFold(n_splits=4)
    best_score = -1.0
    best_params = None
    
    for params in param_grid:
        cv_f1_scores = []
        for train_fold_idx, val_fold_idx in gkf.split(X_train_val, Y_train_val, groups=groups_train_val):
            clf = MultiOutputClassifier(XGBClassifier(**params, random_state=42, eval_metric="logloss"))
            clf.fit(X_train_val[train_fold_idx], Y_train_val[train_fold_idx])
            pred_val = clf.predict(X_train_val[val_fold_idx])
            cv_f1_scores.append(f1_score(Y_train_val[val_fold_idx], pred_val, average="macro", zero_division=0))
            
        mean_cv_f1 = float(np.mean(cv_f1_scores))
        print(f"Params: {params} -> 4-Fold CV Macro F1 = {mean_cv_f1*100:.2f}%")
        if mean_cv_f1 > best_score:
            best_score = mean_cv_f1
            best_params = params
            
    print(f"\nWinning Hyperparameters: {best_params} (CV Macro F1: {best_score*100:.2f}%)")
    
    # Train Best Model on full Train+Val
    best_model = MultiOutputClassifier(XGBClassifier(**best_params, random_state=42, eval_metric="logloss"))
    best_model.fit(X_train_val, Y_train_val)
    
    # =====================================================================
    # FINAL EVALUATION ON UNSEEN TEST SET (HOLDOUT DRIVES)
    # =====================================================================
    print("\n" + "=" * 75)
    print("FINAL EVALUATION ON COMPLETELY UNSEEN TEST DRIVES")
    print("=" * 75)
    
    test_preds = best_model.predict(X_test)
    final_metrics = evaluate_predictions(Y_test, test_preds, TARGET_COLUMNS)
    
    print(f"Test Set Evaluation ({test_df['capture_id'].nunique()} Unseen Drives, {len(test_df)} Windows):")
    print(f"  Subset Accuracy: {final_metrics['subset_accuracy']*100:.2f}% (Exact match across all 5 flags)")
    print(f"  Hamming Loss:    {final_metrics['hamming_loss']:.4f}")
    print(f"  Macro F1:        {final_metrics['macro_f1']*100:.2f}%")
    print(f"  Micro F1:        {final_metrics['micro_f1']*100:.2f}%")
    print(f"  Weighted F1:     {final_metrics['weighted_f1']*100:.2f}%\n")
    
    print("Detailed Per-Fault Performance on Test Drives:")
    for fault, scores in final_metrics['per_fault'].items():
        print(f"  - {fault:26s} | Precision: {scores['precision']*100:5.1f}% | Recall: {scores['recall']*100:5.1f}% | F1: {scores['f1']*100:5.1f}%")
        
    # Multilabel confusion matrices
    mcm = multilabel_confusion_matrix(Y_test, test_preds)
    print("\nConfusion Matrices per Fault (TN, FP / FN, TP):")
    for idx, fault in enumerate(TARGET_COLUMNS):
        tn, fp, fn, tp = mcm[idx].ravel()
        print(f"  - {fault:26s} -> TN: {tn:4d}, FP: {fp:3d} | FN: {fn:3d}, TP: {tp:3d}")
        
    # Save Model & Report
    os.makedirs(MODEL_DIR, exist_ok=True)
    model_save_path = os.path.join(MODEL_DIR, "best_multilabel_xgb_model.joblib")
    joblib.dump(best_model, model_save_path)
    
    # Calculate feature importances per target
    feature_importances = {}
    for idx, fault in enumerate(TARGET_COLUMNS):
        est = best_model.estimators_[idx]
        imps = est.feature_importances_
        sorted_indices = np.argsort(imps)[::-1]
        feature_importances[fault] = {
            feature_cols[i]: round(float(imps[i]), 4)
            for i in sorted_indices if imps[i] > 0.005
        }
    
    report_data = {
        "model_type": "MultiOutputClassifier(XGBoost)",
        "best_hyperparameters": best_params,
        "features_used": feature_cols,
        "target_columns": TARGET_COLUMNS,
        "cv_macro_f1": best_score,
        "test_metrics": final_metrics,
        "test_confusion_matrices": {fault: mcm[idx].tolist() for idx, fault in enumerate(TARGET_COLUMNS)},
        "feature_importances_per_target": feature_importances
    }
    
    report_path = os.path.join(MODEL_DIR, "training_evaluation_report.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report_data, f, indent=2)
        
    print(f"\nModel saved successfully to:  {model_save_path}")
    print(f"Report saved successfully to: {report_path}")
    return best_model, final_metrics


# =====================================================================
# MAIN RUNNER
# =====================================================================
def run_pipeline():
    # 1. Load data
    raw_df = pd.read_csv(DATA_PATH)
    
    # 2. Extract features
    features_df = extract_window_features(raw_df, window_size=30, stride=10)
    
    feature_cols = [
        "rpm_mean", "rpm_std", "rpm_kurtosis",
        "speed_mean", "speed_std", "idle_fraction",
        "throttle_mean", "engine_load_mean",
        "map_mean", "map_std", "maf_mean", "maf_map_ratio", "maf_physics_ratio",
        "trim_mean", "trim_std", "stoich_closed_loop_residual",
        "coolant_mean", "coolant_warmup_rate", "intake_mean", "coolant_intake_delta", "thermal_deficit_score",
        "voltage_mean", "voltage_ripple_std", "voltage_physics_margin",
        "rpm_det_std", "rpm_r3", "rpm_r7", "hunting_surge_metric"
    ]
    
    # 3. Group-based split
    train_df, val_df, test_df = split_by_drive(features_df, test_ratio=0.17, val_ratio=0.16, seed=42)
    
    # 4. Benchmark all algorithms
    benchmark_models(train_df, val_df, feature_cols)
    
    # 5. Tune best candidate and evaluate on test set
    tune_and_train_best(train_df, val_df, test_df, feature_cols)


if __name__ == "__main__":
    run_pipeline()
