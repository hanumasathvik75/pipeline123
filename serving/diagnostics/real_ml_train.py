"""
IgnitionAI: Real-World Engine Fault Classification Benchmark (real_ml_train.py)
Dataset: EngineFaultDB (55,999 labeled real spark-ignition engine rows)
Source: D:\EngineFaultDB_Final.csv

Algorithms Trained & Benchmarked:
  1. Multinomial Logistic Regression (StandardScaler + L2 Regularization)
  2. Decision Tree Classifier
  3. Random Forest Classifier (100 Trees)
  4. XGBoost Classifier (Extreme Gradient Boosting)
  5. Gradient Boosting Classifier (Sklearn Ensembling)
"""

import os
import time
import json
import joblib
import numpy as np
import pandas as pd

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from xgboost import XGBClassifier
from sklearn.metrics import (
    accuracy_score, precision_recall_fscore_support,
    classification_report, confusion_matrix
)

DATA_PATH = r"D:\EngineFaultDB_Final.csv"
OUTPUT_DIR = r"D:\IgnitionAI\serving\diagnostics\models"
BENCHMARK_JSON = r"D:\IgnitionAI\real_ml_benchmark_results.json"

CLASS_NAMES = {
    0: "No Fault (Normal)",
    1: "Rich Mixture",
    2: "Lean Mixture",
    3: "Low Voltage"
}

def run_benchmark():
    print("=" * 80)
    print("[*] IGNITIONAI: REAL-WORLD ML BENCHMARK ON ENGINEFAULTDB (55,999 SAMPLES)")
    print("=" * 80)
    
    if not os.path.exists(DATA_PATH):
        raise FileNotFoundError(f"Dataset not found at {DATA_PATH}")
        
    print(f"[*] Loading dataset from: {DATA_PATH} ...")
    df = pd.read_csv(DATA_PATH)
    print(f"[*] Dataset shape: {df.shape[0]:,} rows x {df.shape[1]} columns")
    print("[*] Class distribution:")
    for cls_idx, count in df['Fault'].value_counts().sort_index().items():
        pct = (count / len(df)) * 100
        print(f"    - Class {cls_idx} ({CLASS_NAMES[cls_idx]}): {count:,} samples ({pct:.1f}%)")

    # Features and Target
    feature_cols = [c for c in df.columns if c != 'Fault']
    X = df[feature_cols]
    y = df['Fault']
    
    # 80/20 Stratified Split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42, stratify=y
    )
    print(f"[*] Stratified split complete: Train = {len(X_train):,}, Test = {len(X_test):,}")
    print("-" * 80)

    # Models Dictionary
    models = {
        "Logistic Regression": Pipeline([
            ("scaler", StandardScaler()),
            ("clf", LogisticRegression(max_iter=1000, random_state=42, multi_class="multinomial"))
        ]),
        "Decision Tree": DecisionTreeClassifier(
            max_depth=12, min_samples_split=10, min_samples_leaf=5, random_state=42
        ),
        "Random Forest": RandomForestClassifier(
            n_estimators=100, max_depth=14, n_jobs=-1, random_state=42
        ),
        "XGBoost": XGBClassifier(
            n_estimators=120, max_depth=6, learning_rate=0.08,
            subsample=0.85, colsample_bytree=0.85,
            eval_metric="mlogloss", random_state=42, n_jobs=-1
        )
    }

    results = {}
    best_f1 = -1.0
    best_model_name = None
    best_model_obj = None

    print(f"{'Model Name':<22} | {'Accuracy':<9} | {'Macro Prec':<10} | {'Macro Rec':<10} | {'Macro F1':<9} | {'Train Time':<10}")
    print("-" * 80)

    for name, model in models.items():
        t0 = time.time()
        model.fit(X_train, y_train)
        t_train = time.time() - t0
        
        y_pred = model.predict(X_test)
        
        acc = accuracy_score(y_test, y_pred)
        prec_macro, rec_macro, f1_macro, _ = precision_recall_fscore_support(
            y_test, y_pred, average="macro", zero_division=0
        )
        prec_wt, rec_wt, f1_wt, _ = precision_recall_fscore_support(
            y_test, y_pred, average="weighted", zero_division=0
        )
        
        # Per-class metrics
        prec_per, rec_per, f1_per, sup_per = precision_recall_fscore_support(
            y_test, y_pred, average=None, zero_division=0
        )
        
        cm = confusion_matrix(y_test, y_pred).tolist()
        
        per_class_res = {}
        for idx in range(len(CLASS_NAMES)):
            per_class_res[CLASS_NAMES[idx]] = {
                "precision": round(float(prec_per[idx]) * 100, 2),
                "recall": round(float(rec_per[idx]) * 100, 2),
                "f1_score": round(float(f1_per[idx]) * 100, 2),
                "test_support": int(sup_per[idx])
            }

        results[name] = {
            "accuracy": round(float(acc) * 100, 2),
            "macro_precision": round(float(prec_macro) * 100, 2),
            "macro_recall": round(float(rec_macro) * 100, 2),
            "macro_f1": round(float(f1_macro) * 100, 2),
            "weighted_f1": round(float(f1_wt) * 100, 2),
            "train_time_sec": round(t_train, 2),
            "per_class": per_class_res,
            "confusion_matrix": cm
        }

        print(f"{name:<22} | {acc*100:>7.2f}% | {prec_macro*100:>8.2f}% | {rec_macro*100:>8.2f}% | {f1_macro*100:>7.2f}% | {t_train:>8.2f}s")

        if f1_macro > best_f1:
            best_f1 = f1_macro
            best_model_name = name
            best_model_obj = model

    print("=" * 80)
    print(f"[+] CHAMPION MODEL: {best_model_name} (Macro F1 = {best_f1*100:.2f}%)")
    print("=" * 80)

    # Detailed Breakdown for Champion Model
    print(f"\n[+] DETAILED PER-CLASS METRICS FOR {best_model_name.upper()}:")
    print(f"{'Class / Fault Condition':<28} | {'Precision':<10} | {'Recall':<10} | {'F1-Score':<10} | {'Test Support'}")
    print("-" * 75)
    for cls_name, m_dict in results[best_model_name]["per_class"].items():
        print(f"{cls_name:<28} | {m_dict['precision']:>8.2f}% | {m_dict['recall']:>8.2f}% | {m_dict['f1_score']:>8.2f}% | {m_dict['test_support']:>8}")

    # Feature Importance for Tree-Based Best Model
    if hasattr(best_model_obj, "feature_importances_"):
        fi = best_model_obj.feature_importances_
        sorted_fi = sorted(zip(feature_cols, fi), key=lambda x: x[1], reverse=True)
        print(f"\n[+] TOP FEATURE IMPORTANCES ({best_model_name}):")
        for f_name, imp in sorted_fi[:8]:
            print(f"  * {f_name:<20}: {imp*100:>5.2f}%")
        results[best_model_name]["feature_importances"] = {k: round(float(v)*100, 2) for k, v in sorted_fi}

    # Save Best Model to joblib
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    best_model_path = os.path.join(OUTPUT_DIR, "best_real_enginefault_model.joblib")
    joblib.dump(best_model_obj, best_model_path)
    print(f"\n[+] Champion model successfully saved to: {best_model_path}")

    # Save Benchmark Report JSON
    with open(BENCHMARK_JSON, "w") as f:
        json.dump(results, f, indent=2)
    print(f"[+] Full JSON benchmark results saved to: {BENCHMARK_JSON}")

    return results

if __name__ == "__main__":
    run_benchmark()
