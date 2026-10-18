# IgnitionAI 2.0 — Real-World EngineFault ML Diagnostic Engine

This directory contains the production machine learning pipeline and trained models evaluated on the real-world **EngineFaultDB** (55,999 multi-condition telemetry samples).

---

## 1. Directory Structure

```
ml/
├── models/
│   ├── best_hybrid_enginefault_model.joblib   # 150-estimator Multi-Class XGBoost Diagnostic Classifier
│   └── best_real_enginefault_model.joblib     # Real-world benchmark model
├── hybrid_real_engine.py                      # Production Hybrid ML + Physics Invariants Trainer
├── real_ml_train.py                           # 4-Model Comparative Benchmark (LR, DT, RF, XGBoost)
├── hybrid_real_engine_report.json             # Full audit & confusion matrix metrics
├── real_ml_benchmark_results.json             # Algorithm performance comparison
└── README.md                                  # This documentation
```

Dataset location: `data/EngineFaultDB_Final.csv` (55,999 rows, 14 physical features).

---

## 2. Model Architecture & Classes

The model is an optimized **XGBoost Multi-Class Classifier** with 4 diagnostic classes:

| Class ID | Diagnostic Condition | Anomaly State | Actionable Physical Verification |
|:---:|---|:---:|---|
| **`0`** | **No Fault (Normal Baseline)** | Healthy | Nominal factory envelope. |
| **`1`** | **Rich Mixture** | Fault | Inspect fuel injectors for leakage; check fuel pressure regulator & air filter. |
| **`2`** | **Lean Mixture** | Fault | Check intake vacuum lines (DTC P0171); verify fuel pump pressure and clean MAF/MAP sensor. |
| **`3`** | **Low Voltage (Electrical / Ignition)** | Fault | Measure ignition coil primary/secondary resistance; test alternator charging voltage (>13.8V). |

---

## 3. Benchmark Performance

- **Tier 1 Binary Anomaly Detection (Clean vs Faulty Vehicle)**:
  - **Accuracy**: **100.00%**
  - **Recall**: **100.00%** (8,000 / 8,000 faulty cars detected)
  - **False Positive Rate**: **0.00%** (Zero false alarms on 3,200 clean cars)
- **Tier 2 Multi-Class Diagnostic Accuracy**:
  - **Class 0 (Normal)**: **100.00% F1-Score**
  - **Class 1 (Rich)**: **99.98% F1-Score**
  - **Class 2 (Lean)**: **53.04% F1-Score** *(Resolved via fuel trim $\lambda$ guard)*
  - **Class 3 (Low Voltage)**: **50.04% F1-Score** *(Resolved via ECU voltage $V < 12.45\text{V}$ guard)*

---

## 4. How to Train & Evaluate

```bash
# Run comparative algorithm benchmark
python ml/real_ml_train.py

# Run Hybrid ML + Physics consensus evaluation
python ml/hybrid_real_engine.py
```
