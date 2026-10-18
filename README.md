# IgnitionAI 2.0 — Pre-Owned Vehicle Diagnostic Pipeline

A modular knowledge engineering and vehicle health assessment system. Each directory maps 1-to-1 to a named box in the architecture diagram, enforcing strict boundaries across all 7 phases of the pipeline.

## Repository Structure

This is a development preview. Module presence and passing synthetic tests do not establish customer or vehicle validation. See [native preview instructions and current limitations](docs/NATIVE_PREVIEW.md).

The native Swing Windows app and Java Android app call the same `application` service. Run `run-desktop.bat` from source, or use the packaged Windows executable. The Android Gradle module is `android-app`; its launcher is `com.ignitionai.nativeapp.MainActivity`. Telemetry input to the desktop app comes only from a file ("Open OBD session"), a genuinely independent third-party ELM327 emulator over TCP ("Capture live ELM327"), or a real dongle capture saved elsewhere — the app cannot generate its own favorable data. Synthetic test data is produced by the separate `IgnitionSimulator` tool (`run-simulator.bat`), never by the assessment app itself.

```
IgnitionAI 2.0/
├── technical-sources/        Phase 1 – immutable source records and addressable passages
├── fact-extraction/          Phase 1 – schema-constrained candidate fact extraction
├── consistency-checks/       Phase 1 – structural validation and contradiction detection
├── linked-registries/        Phase 1 – four linked, versioned knowledge registries
├── obd-input-and-pre-processing/ Phase 2 – OBD Stream Normalization, Bluetooth Adapter, Local Storage
├── tools/obd-generator/      Phase 2 – Configurable Stream Generator for testing, incl. real SAE J1979/J2012 ELM327 protocol encode/decode
├── vehicle-context/          Phase 3 – Vehicle Identity, Context, and Operating Regime State
├── direct-features/          Phase 3 – Rolling Window Feature Computation (~30 signal-derived features)
├── virtual-sensors/          Phase 3 – Model-driven Sensor Generation, plus a Diagnostic Index plugin system: 11 composite condition indices (combustion, fuel, thermal, load response, charging, air intake, timing, driving behaviour, signal quality, hybrid battery health via the standard SAE J1979 PID 0x5B, overall condition) and a probabilistic fault graph (`FaultCandidateRanker`) combining index findings and DTCs into ranked root-cause candidates
├── phase4-contract/          Phase 4 – Phase 4 Data Transfer Objects and States
├── expected-behaviour-model/ Phase 4 – Dynamic Baselines (Regime/Rolling)
├── residual-calculation/     Phase 4 – Calculates deviation (Observed - Expected)
├── uncertainty-calculation/  Phase 4 – Defines dynamic noise/variance bounds
├── anomaly-monitoring/       Phase 4 – Thresholds, Rolling Windows, Debounce Logic
├── episode-store/            Phase 4 – Append-only anomaly state transitions
├── evidence-store/           Phase 4 – Append-only evidence tracing
├── phase5-contract/          Phase 5 – Output contracts for Degradation and Risk
├── degradation-state-estimator/ Phase 5 – Maps anomaly episodes to explicit state classes
├── degradation-feature-builder/ Phase 5 – Constructs severity/duration/recurrence feature vectors
├── trend-and-persistence-analyzer/ Phase 5 – State-space (Kalman filter) model predicting future trajectories, reused for both OBD degradation trend and BMS Pack SoH trend
├── event-risk-estimator/     Phase 5 – Survival/Hazard models projecting risk horizons
├── degradation-evidence-store/ Phase 5 – Evidence linkage for degradation models
├── bms-diagnostics/          Battery Management System diagnostics: per-session cell-imbalance/SoC-estimator-disagreement/thermal-spread checks and a cross-session Pack SoH degradation trend, independent of the OBD-II signal path
├── phase6-contract/          Phase 6 – Vehicle Health Assessment Schema and Output Contract
├── severity-indicator/       Phase 6 – Rules-based categorization of subsystem impact
├── subsystem-health-score/   Phase 6 – Component-level 0-100 scoring based on Phase 5 data
├── vehicle-health-index/     Phase 6 – Weighted aggregation of subsystems into overall VHI
├── certificate-snapshot/     Phase 7 – Immutable representation of the Phase 6 health assessment
├── latex-certificate-generator/ Phase 7 – Renders three separate PDFs from the same snapshot: the Vehicle Health Certificate, a Root Cause Analysis & Stepwise Repair Plan, and a BMS Diagnostics Report
├── pre-owned-dealership-app/ Phase 7 – Dealership Dashboard MVP and Historical Comparisons
└── tools/pipeline-runner/    CLI testing tools, Integration Adapters, the master runner, the comprehensive 7-scenario test suite, and the live ELM327 capture runner
```

## Running the Complete Integrated Pipeline

You can run the end-to-end `IgnitionAiPipelineRunner`, which drives data sequentially from Phase 2 (OBD) through Phase 3 (Context & Features), Phase 4 (Anomaly Detection), Phase 5 (Degradation & Risk), Phase 6 (Health Scoring), and outputs a final Phase 7 LaTeX Certificate and Dealership Dashboard view.

First, ensure the project compiles successfully:
```bash
powershell -ExecutionPolicy Bypass -File compile.ps1
```

Or just run the test suite (which compiles and executes all phase tests, including a 7-scenario Diagnostic Index test suite and the BMS trend calibration test):
```bash
build_and_test.bat
```

Other useful CLI tools (`java -ea -cp out <class>` after `compile.ps1`):

- `tools.pipeline_runner.CertificateDemoRunner` — runs a demo fixture end-to-end and compiles both the certificate and repair-plan PDFs to `out/certificates/demo/` (needs `pdflatex` on PATH).
- `tools.pipeline_runner.Elm327LiveCaptureRunner [host] [port] [durationMs]` — captures a real OBD-II session from a genuinely independent third-party ELM327 emulator ([Ircama/ELM327-emulator](https://github.com/Ircama/ELM327-emulator), not written by IgnitionAI) over TCP and feeds it through the unmodified `InspectionService`. Start the emulator first: `pip install elm327-emulator` then `python -m elm -n 35000 -s car`. The desktop app exposes the same capture as a "Capture live ELM327" button.
- `tools.pipeline_runner.BmsAnalysisRunner "<path-to-processed_data>"` — runs BMS per-session diagnostics and the cross-session SoH trend against real BMS CSV exports, and compiles a BMS Diagnostics Report PDF to `out/bms-reports/`.

## Architecture and Data Flow

The architecture operates strictly linearly. No module may bypass its predecessor or read from unstructured data stores directly. 

1. **Phase 1** defines the knowledge registries.
2. **Phase 2** ingests structured telemetry (`ObdMessage`).
3. **Phase 3** enriches telemetry with `VehicleContext` and produces `AnalyticalObservation`.
4. **Phase 4** computes dynamic baselines and creates `AnomalyEpisode`.
5. **Phase 5** tracks temporal trends, translating episodes into `DegradationOutput`.
6. **Phase 6** aggregates degraded subsystems into a `VehicleHealthAssessment` and a 0-100 `VehicleHealthIndex`.
7. **Phase 7** validates the assessment against the schema, generates a deterministic `CertificateSnapshot`, and renders a final LaTeX certificate.

Each module exposes an explicit input/output contract. No module merges responsibilities with another.

---

## Real Hybrid ML Diagnostics & Spring Boot Gateway

- **FastAPI ML Microservice (`ml/`)**: Hosts the Real-World Hybrid XGBoost model (`best_hybrid_enginefault_model.joblib`) trained on 55,999 EngineFaultDB samples.
- **Spring Boot Gateway (`spring-backend/`)**: REST API gateway on port 8080 connecting Rahul's Android App to the ML model.
- **Automatic Physics Feature Derivation (`ml/enrich_csv.py`)**: Automatically computes all 14 physical/stoichiometric parameters from raw OBD logs.
- Full mathematical formulas, PID derivations, and VHI calculations are documented in [**`ml/README.md`**](ml/README.md).
