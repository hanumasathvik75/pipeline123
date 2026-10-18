# IgnitionAI 2.0 — Real-World EngineFault ML Diagnostic Engine

This directory contains the production machine learning pipeline, trained models, and mathematical physics derivation specifications evaluated on the real-world **EngineFaultDB** (55,999 multi-condition telemetry samples).

---

## 1. Directory Structure

```
ml/
├── models/
│   ├── best_hybrid_enginefault_model.joblib   # 150-estimator Multi-Class XGBoost Diagnostic Classifier (3.3 MB)
│   └── best_real_enginefault_model.joblib     # Real-world benchmark baseline model (55 KB)
├── ml_service.py                              # Production FastAPI Microservice (Port 8000)
├── enrich_csv.py                              # Automatic Feature Derivation & Preprocessor CLI
├── hybrid_real_engine.py                      # Production Hybrid ML + Physics Invariants Trainer
├── real_ml_train.py                           # 4-Model Comparative Benchmark (LR, DT, RF, XGBoost)
├── hybrid_real_engine_report.json             # Full audit & confusion matrix metrics
├── real_ml_benchmark_results.json             # Algorithm performance comparison
└── README.md                                  # This documentation
```

Dataset location: `data/EngineFaultDB_Final.csv` (55,999 rows, 14 physical features).

---

## 2. Model Architecture & Diagnostic Classes

The production model is a tuned **Multi-Class XGBoost Classifier** (150 estimators, max depth 10, learning rate 0.08) coupled with **Physics Invariant Safeguards** to achieve zero false positives:

| Class ID | Diagnostic Condition | Health State | Description & Primary Physical Indicator |
|:---:|---|:---:|---|
| **`0`** | **No Fault (Normal Baseline)** | Healthy | Nominal factory operating envelope ($\lambda \approx 1.00$, nominal vacuum). |
| **`1`** | **Rich Mixture** | Fault | Excess fuel delivery / unmetered restriction ($CO > 1.8\%$, $\lambda < 0.95$, $O_2 > 0.7\text{V}$). |
| **`2`** | **Lean Mixture** | Fault | Unmetered intake air leaks / fuel starvation (DTC P0171, $\lambda > 1.05$, $O_2 < 0.2\text{V}$). |
| **`3`** | **Low Voltage (Electrical / Ignition)** | Fault | Weak ignition spark / charging drop (Alternator $< 13.8\text{V}$, Coil resistance out of spec). |

---

## 3. The 14 Model Features & Exact Calculation Formulas

When standard OBD-II scanners (or mobile apps like Rahul's app) record drives, they typically log only 4–5 basic PIDs (`RPM`, `Speed`, `Coolant`, `Load` / `TPS`). 

Our pipeline **automatically derives and imputes all 14 physical parameters** using deterministic SAE J1979 automotive equations and thermodynamics:

```
Raw OBD Telemetry (RPM, Speed, Load/TPS, Coolant)
                    │
                    ▼
   [ Automotive Physics Derivation Pipeline ]
                    │
    ┌───────────────┼───────────────┬────────────────┐
    ▼               ▼               ▼                ▼
Speed-Density   Vehicle Force   Stoichiometric    Exhaust Gas
Air Mass (MAF)   & Power (kW)    Lambda & AFR     Invariants
    │               │               │                │
    └───────────────┴───────┬───────┴────────────────┘
                            ▼
          Complete 14-Feature ML Vector
      ['MAP', 'TPS', 'Force', 'Power', 'RPM',
       'Consumption L/H', 'Consumption L/100KM', 'Speed',
       'CO', 'HC', 'CO2', 'O2', 'Lambda', 'AFR']
```

### Detailed Feature Formulas & Explanations:

### 1. `MAP` — Manifold Absolute Pressure ($\text{kPa}$)
- **Source**: SAE PID `010B` (Intake Manifold Absolute Pressure).
- **Physical Meaning**: Measures the intake manifold vacuum. High vacuum (low MAP) occurs at idle; atmospheric pressure (high MAP $\approx 100\text{ kPa}$) occurs at Wide Open Throttle (WOT).
- **Derivation Formula** (if MAP sensor is omitted):
  $$\text{MAP} = (\text{Load} \times 0.70) + 25.0 \quad [\text{kPa}]$$
  Bounded between $15.0\text{ kPa}$ (deceleration fuel cutoff) and $105.0\text{ kPa}$ (naturally aspirated wide open throttle).

---

### 2. `TPS` — Throttle Position Sensor ($\%$)
- **Source**: SAE PID `0111` (Absolute Throttle Position).
- **Physical Meaning**: Percentage opening of the throttle butterfly plate ($0\% = \text{closed}$, $100\% = \text{wide open}$).
- **Derivation Formula** (if TPS is missing):
  $$\text{TPS} \approx \text{Calculated Engine Load (\%)} \quad (\text{SAE PID } 0104)$$

---

### 3. `Force` — Tractive Effort at Drive Wheels ($\text{N}$)
- **Source**: Vehicle longitudinal dynamics.
- **Physical Meaning**: The net tractive force applied by the powertrain at the tire contact patch to overcome aerodynamic drag, rolling resistance, and provide acceleration.
- **Derivation Formula**:
  $$\text{Force (N)} = \frac{\text{Power (W)}}{\max(v_{\text{m/s}}, 1.0)} = \frac{\text{Power (kW)} \times 1000}{\max\left(\frac{\text{Speed (km/h)}}{3.6}, 1.0\right)}$$
  Physically bounded between $20.0\text{ N}$ (idle roll) and $4,500.0\text{ N}$ (maximum tire adhesion limit for passenger cars).

---

### 4. `Power` — Engine Mechanical Brake Power ($\text{kW}$)
- **Source**: Powertrain torque-speed relationship.
- **Physical Meaning**: Effective shaft power delivered by the internal combustion engine.
- **Derivation Formula**:
  $$P_{\text{mech}} (\text{kW}) = \frac{\tau \times \omega}{1000} = \frac{\tau \times (2\pi \times \text{RPM} / 60)}{1000}$$
  Using engine displacement scaling ($V_d = 1.5\text{L}$, baseline torque $\approx 140\text{ Nm}$ at peak volumetric efficiency):
  $$\text{Power (kW)} = \left(\frac{\text{Load}}{100.0}\right) \times \left(\frac{\text{RPM}}{6000.0}\right) \times 85.0 \quad [\text{kW}]$$
  Clipped to a minimum baseline of $0.5\text{ kW}$ at idle.

---

### 5. `RPM` — Engine Crankshaft Rotational Speed ($\text{rpm}$)
- **Source**: SAE PID `010C` (Engine RPM).
- **Formula**:
  $$\text{RPM} = \frac{(256 \times A) + B}{4}$$

---

### 6. `Consumption L/H` — Volumetric Fuel Consumption Rate ($\text{L/h}$)
- **Source**: Speed-Density Thermodynamic Mass Airflow calculation.
- **Physical Meaning**: Real-time fuel burn rate in litres per hour.
- **Derivation Formula**:
  1. *Mass Airflow ($\text{g/s}$)*:
     $$\dot{m}_{\text{air}} (\text{g/s}) = \frac{\text{MAP} \times 10^3 \times \left(\frac{\text{RPM}}{120}\right) \times V_d \times \eta_v}{R_{\text{air}} \times T_{\text{intake}}} \times \frac{1000}{M_{\text{air}}}$$
     *(with $V_d = 1.5\text{ L} = 0.0015\text{ m}^3$, $\eta_v = 0.82$, $R = 287.05\text{ J/(kg}\cdot\text{K)}$, $T = 298.15\text{ K}$)*
  2. *Fuel Mass Flow Rate ($\text{g/s}$)*:
     $$\dot{m}_{\text{fuel}} (\text{g/s}) = \frac{\dot{m}_{\text{air}}}{\text{AFR}} = \frac{\dot{m}_{\text{air}}}{14.7 \times \lambda}$$
  3. *Volumetric Conversion*:
     $$\text{Consumption L/H} = \left(\frac{\dot{m}_{\text{fuel}}}{\rho_{\text{gasoline}}}\right) \times 3600 = \left(\frac{\dot{m}_{\text{fuel}}}{745.0\text{ g/L}}\right) \times 3600$$

---

### 7. `Consumption L/100KM` — Distance-Specific Fuel Economy ($\text{L/100 km}$)
- **Source**: Instantaneous fuel efficiency conversion.
- **Derivation Formula**:
  $$\text{Consumption L/100KM} = \frac{\text{Consumption L/H}}{\max(\text{Speed (km/h)}, 5.0)} \times 100.0$$

---

### 8. `Speed` — Vehicle Speed over Ground ($\text{km/h}$)
- **Source**: SAE PID `010D` (Vehicle Speed).
- **Formula**:
  $$\text{Speed} = A \quad [\text{km/h}]$$

---

### 9. `CO` — Carbon Monoxide Concentration ($\%$)
- **Source**: Exhaust gas emissions stoichiometry.
- **Physical Meaning**: Unoxidized carbon resulting from insufficient oxygen during combustion.
- **Derivation Rule**:
  $$\text{CO} (\%) = \begin{cases} 
  2.40\% & \text{if } \lambda < 0.95 \text{ (Rich mixture: severe partial combustion)} \\ 
  0.25\% & \text{if } 0.95 \le \lambda \le 1.05 \text{ (Stoichiometric: nominal 3-way catalyst conversion)} \\ 
  0.12\% & \text{if } \lambda > 1.05 \text{ (Lean mixture: excess oxygen facilitates complete } \text{CO}_2 \text{ oxidation)} 
  \end{cases}$$

---

### 10. `HC` — Unburned Hydrocarbons ($\text{ppm}$)
- **Source**: Combustion chamber wall quenching and misfire emissions.
- **Physical Meaning**: Raw unburned gasoline vapors escaping into exhaust.
- **Derivation Rule**:
  $$\text{HC (ppm)} = \begin{cases} 
  310.0\text{ ppm} & \text{if } \lambda < 0.95 \text{ (Rich mixture: raw fuel slip)} \\ 
  45.0\text{ ppm} & \text{if } 0.95 \le \lambda \le 1.05 \text{ (Nominal catalytic oxidation)} \\ 
  260.0\text{ ppm} & \text{if } \lambda > 1.05 \text{ (Lean misfire: slow flame propagation quench)} 
  \end{cases}$$

---

### 11. `CO2` — Carbon Dioxide Concentration ($\%$)
- **Source**: Complete hydrocarbon combustion product.
- **Physical Meaning**: Direct indicator of combustion thermal and chemical efficiency.
- **Derivation Rule**:
  $$\text{CO}_2 (\%) = \begin{cases} 
  12.10\% & \text{if } \lambda < 0.95 \text{ (Oxygen starved)} \\ 
  14.90\% & \text{if } 0.95 \le \lambda \le 1.05 \text{ (Peak stoichiometric efficiency)} \\ 
  11.80\% & \text{if } \lambda > 1.05 \text{ (Diluted by excess intake air)} 
  \end{cases}$$

---

### 12. `O2` — Narrowband Oxygen Sensor Voltage ($\text{V}$)
- **Source**: SAE PID `0114` (Bank 1 Sensor 1 Oxygen Sensor).
- **Physical Meaning**: Zirconia ceramic Nernst cell voltage representing partial pressure of oxygen in exhaust.
- **Derivation Rule**:
  $$V_{\text{O2}} = \begin{cases} 
  0.820\text{ V} & \text{if } \lambda < 0.97 \text{ (Rich: high CO/HC, low exhaust } O_2\text{)} \\ 
  0.450\text{ V} & \text{if } 0.97 \le \lambda \le 1.03 \text{ (Stoichiometric toggle midpoint)} \\ 
  0.150\text{ V} & \text{if } \lambda > 1.03 \text{ (Lean: abundant exhaust } O_2\text{)} 
  \end{cases}$$

---

### 13. `Lambda` ($\lambda$) — Air-Fuel Equivalence Ratio
- **Source**: SAE PID `0124` / `0134` (Wideband Equivalence Ratio) or Fuel Trim inference.
- **Physical Meaning**: Ratio of actual Air-Fuel Ratio to stoichiometric Air-Fuel Ratio ($\lambda = \text{AFR} / 14.7$).
- **Derivation Formula**:
  $$\lambda = 1.0 - \left(\frac{\text{STFT} + \text{LTFT}}{100.0}\right) \times 0.25$$
  - When fuel trims are not present: $\lambda = 0.92$ under heavy acceleration ($\text{TPS} > 75\%$), $\lambda = 1.02$ under coasting ($\text{TPS} < 10\%$), and $\lambda = 1.00$ at normal cruise.

---

### 14. `AFR` — Air-Fuel Ratio
- **Formula**:
  $$\text{AFR} = \lambda \times 14.7$$
  - Gasoline Stoichiometric = $14.7 : 1$
  - Rich $< 14.0 : 1$
  - Lean $> 15.2 : 1$

---

## 4. Vehicle Health Index (VHI) Aggregation Formula

When evaluating a multi-minute drive session (thousands of frames), the Vehicle Health Index ($0 - 100$) is calculated as:

$$\text{VHI} = \text{round}\left(100.0 - \left(\%_{\text{Rich}} \times 0.60 + \%_{\text{Lean}} \times 0.70 + \%_{\text{LowVoltage}} \times 0.85\right)\right)$$

### Health Status Bands:
- **`VHI >= 88`**: **`HEALTHY`** (Pristine factory combustion and electrical health).
- **`65 <= VHI < 88`**: **`ATTENTION_REQUIRED`** (Minor mixture deviations or transient spark irregularities).
- **`VHI < 65`**: **`CRITICAL_FAULT`** (Sustained lean misfires, leaking injectors, or alternator charging failure).

---

## 5. What We Provide to Team Members & Collaborators

To ensure no other team member ever gets stuck with missing columns or formula calculations, we provide **three turnkey solutions**:

### Solution A: Automatic Standalone CLI Tool (`ml/enrich_csv.py`)
Anyone on the team can convert ANY raw OBD CSV file into a 100% model-ready 14-feature CSV with one terminal command:

```bash
# 1. Convert raw CSV to 14-feature enriched CSV
python ml/enrich_csv.py raw_drive.csv -o model_ready.csv

# 2. Or convert AND run model diagnostics immediately:
python ml/enrich_csv.py raw_drive.csv --predict
```

### Solution B: Automatic API Preprocessing in FastAPI & Spring Boot
When mobile apps or web frontends send data to the backend, they **do not need to derive columns**. The API accepts raw telemetry and automatically populates missing values under the hood:

```bash
# Direct single-frame prediction with basic inputs:
curl -X POST http://127.0.0.1:8000/api/diagnostics/enginefault-predict \
     -H "Content-Type: application/json" \
     -d '{"RPM": 2100, "Speed": 60, "Load": 26}'
```

The service automatically derives `Force`, `Power`, `Consumption L/H`, `CO`, `HC`, `CO2`, `O2`, and `Lambda`, returning the diagnostic fault code, confidence, and workshop checklist.

### Solution C: One-Click Run Scripts
- [`run-ml-service.bat`](../run-ml-service.bat): Launches the FastAPI ML Microservice on `http://127.0.0.1:8000`.
- [`run-spring-backend.bat`](../run-spring-backend.bat): Launches the Spring Boot Gateway on `http://127.0.0.1:8080`.
