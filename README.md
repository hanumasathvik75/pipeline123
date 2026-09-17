# IgnitionAI 2.0 — Pre-Owned Vehicle Diagnostic Pipeline

A modular knowledge engineering and vehicle health assessment system. Each directory maps 1-to-1 to a named box in the architecture diagram, enforcing strict boundaries across all 7 phases of the pipeline.

## Repository Structure

The complete Phase 1-7 integrated pipeline is fully implemented on the `master` branch.

```
IgnitionAI 2.0/
├── technical-sources/        Phase 1 – immutable source records and addressable passages
├── fact-extraction/          Phase 1 – schema-constrained candidate fact extraction
├── consistency-checks/       Phase 1 – structural validation and contradiction detection
├── linked-registries/        Phase 1 – four linked, versioned knowledge registries
├── obd-input-and-pre-processing/ Phase 2 – OBD Stream Normalization, Bluetooth Adapter, Local Storage
├── tools/obd-generator/      Phase 2 – Configurable Stream Generator for testing
├── vehicle-context/          Phase 3 – Vehicle Identity, Context, and Operating Regime State
├── direct-features/          Phase 3 – Rolling Window Feature Computation
├── virtual-sensors/          Phase 3 – Model-driven Sensor Generation
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
├── trend-and-persistence-analyzer/ Phase 5 – State space models predicting future trajectories
├── event-risk-estimator/     Phase 5 – Survival/Hazard models projecting risk horizons
├── degradation-evidence-store/ Phase 5 – Evidence linkage for degradation models
├── phase6-contract/          Phase 6 – Vehicle Health Assessment Schema and Output Contract
├── severity-indicator/       Phase 6 – Rules-based categorization of subsystem impact
├── subsystem-health-score/   Phase 6 – Component-level 0-100 scoring based on Phase 5 data
├── vehicle-health-index/     Phase 6 – Weighted aggregation of subsystems into overall VHI
├── certificate-snapshot/     Phase 7 – Immutable representation of the Phase 6 health assessment
├── latex-certificate-generator/ Phase 7 – Dynamic LaTeX rendering of the certificate
├── pre-owned-dealership-app/ Phase 7 – Dealership Dashboard MVP and Historical Comparisons
└── tools/pipeline-runner/    CLI testing tools, Integration Adapters, and the master runner
```

## Running the Complete Integrated Pipeline

You can run the end-to-end `IgnitionAiPipelineRunner`, which drives data sequentially from Phase 2 (OBD) through Phase 3 (Context & Features), Phase 4 (Anomaly Detection), Phase 5 (Degradation & Risk), Phase 6 (Health Scoring), and outputs a final Phase 7 LaTeX Certificate and Dealership Dashboard view.

First, ensure the project compiles successfully:
```bash
powershell -ExecutionPolicy Bypass -File compile.ps1
```

Or just run the test suite (which compiles and executes all phase tests):
```bash
build_and_test.bat
```

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
