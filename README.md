# IgnitionAI 2.0 — Pre-Owned Vehicle Diagnostic Pipeline

A modular knowledge engineering and vehicle health assessment system. Each directory maps 1-to-1 to a named box in the architecture diagram.

## Repository Structure

```
IgnitionAI 2.0/
├── technical-sources/        Phase 1 – immutable source records and addressable passages
├── fact-extraction/          Phase 1 – schema-constrained candidate fact extraction
├── consistency-checks/       Phase 1 – structural validation and contradiction detection
├── linked-registries/        Phase 1 – four linked, versioned knowledge registries
├── obd-input-and-pre-processing/ Phase 2 – OBD Stream Normalization, Bluetooth Adapter, Local Storage
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
├── tools/
│   ├── pipeline-runner/      CLI testing tools and IntegrationAdapters
│   └── obd-generator/        Phase 2 Configurable Stream Generator
└── docs/
    ├── phase1-decisions.md   Decision log for Phase 1
    └── phase2-input-contract.md OBD Canonical Input Contract

Phases 4–7 modules will be added in their respective phases.
## Phase 1 — Knowledge Engineering

See [`docs/phase1-decisions.md`](docs/phase1-decisions.md) for decisions and [`IgnitionAI_Seven_Phase_MVP_Action_Plan.md`](IgnitionAI_Seven_Phase_MVP_Action_Plan.md) for the full plan.

### Pipeline Flow (Phase 1)

```
TechnicalSource (document + passages)
        │
        ▼
CandidateFact  ◄── FactExtractor (schema-constrained, LLM-assisted)
        │
        ▼
ConsistencyCheckService  ──► quarantine store (contradictions)
        │
        ▼
LinkedRegistries  ──► RegistryRelease (pinned by releaseId)
        │
        ▼
RegistryQueryService  (lookup by vehicle context + observation ID)
```

### Running the Pipeline Runner

```bash
java -cp . tools.PipelineRunner --source-dir sample-sources/ --vehicle-family "VW_EA888_Gen3"
```

## Phase 2 to Phase 3 Integration Flow

The canonical Phase 2 output `ObdMessage` maps directly into Phase 3 representations via the `IntegrationAdapter`, ensuring no redundant schemas are re-parsed.

```
ObdGenerator / Bluetooth Adapter (obd-input.v1)
        │
        ▼
ObdPreProcessor (Duplicate detection, sequencing, quality tagging)
        │
        ▼
IntegrationAdapter (Bridging Phase 2 ObdMessage -> Phase 3 AnalyticalObservation)
        │
        ▼
VehicleContextManager (Tracks Regime, Operating Conditions)
        │
        ▼
FeatureCatalogue (Direct signal features over rolling Windows)
        │
        ▼
VirtualSensorRuntime (Evaluates Virtual Sensors based on context and features)
```

## Module Boundaries

Direct module-to-architecture-box mapping:

| Architecture Box         | Code Module                         | Phase |
|--------------------------|-------------------------------------|-------|
| Technical Sources        | `technical-sources/`                | 1     |
| Fact Extraction          | `fact-extraction/`                  | 1     |
| Consistency Checks       | `consistency-checks/`               | 1     |
| Linked Registries        | `linked-registries/`                | 1     |
| OBD Pre-processing       | `obd-input-and-pre-processing/`     | 2     |
| Vehicle Context          | `vehicle-context/`                  | 3     |
| Direct Features          | `direct-features/`                  | 3     |
| Virtual Sensors          | `virtual-sensors/`                  | 3     |
| Phase 4 Contract         | `phase4-contract/`                  | 4     |
| Expected Behaviour       | `expected-behaviour-model/`         | 4     |
| Residuals                | `residual-calculation/`             | 4     |
| Uncertainty              | `uncertainty-calculation/`          | 4     |
| Anomaly Monitoring       | `anomaly-monitoring/`               | 4     |
| Episode Store            | `episode-store/`                    | 4     |
| Evidence Store           | `evidence-store/`                   | 4     |

Each module exposes an explicit input/output contract. No module merges responsibilities with another.
