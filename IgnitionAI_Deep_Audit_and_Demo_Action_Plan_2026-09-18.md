# IgnitionAI 2.0 — Deep audit and GoMechanic-demo action plan

Prepared 18 September 2026. Repository: `shankscodz/IgnitionAI-2.0`, branch `master`, commit `94271b6` (merge of PR #1, `codex/native-mvp-delivery`). Reviewed against `IgnitionAI_Seven_Phase_MVP_Action_Plan.md`, `IgnitionAI_Technical_Architecture_and_Deployment_Specification.pdf`, and the `deck_iqoo.pdf` pitch deck. Target: demonstrable to the GoMechanic CEO on 22 September 2026 — **4 days from this audit**.

## 0. Bottom line

This is not vibecoded slop in the sense of "nothing works." The module boundaries genuinely match the architecture diagram, the code compiles (`:native-core:compileJava` — BUILD SUCCESSFUL), and the full test suite passes (`build_and_test.bat` — ALL TESTS PASSED, including a real end-to-end run: 178 records, 2 episodes, a real `CertificateSnapshot`, a real `pdflatex` compile). Several AI agents (ChatGPT-driven work, "Antigravity," and a "Codex" agent) did produce a working skeleton.

But the **specific story you need to tell GoMechanic — "virtual sensors make us smarter than a raw OBD scan" — is not true of the code as it stands.** Virtual sensors and direct features are computed and then thrown away before scoring. The analytical pipeline that actually drives severity, degradation, and the Vehicle Health Index runs on **5 raw PIDs only**. The certificate that would land in the CEO's hand contains almost none of what the spec (and the pitch deck) promises. And there are three competing, overlapping app implementations in this repo, two of which are dead weight.

This is fixable in 4 days **only if you cut scope hard** and focus every hour on making the virtual-sensor story real, not on chasing full spec completeness. Section 4 is the plan. Section 1–3 is the evidence.

## 1. What is actually real (verified, not assumed)

- **It compiles.** `native-core` (which source-includes all 32 phase modules) builds clean under JDK 17/25.
- **It runs end to end.** The cumulative pipeline test produces a real `CertificateSnapshot`, generates LaTeX, and — verified in this audit's environment — the worklog records an actual successful `pdflatex` invocation.
- **The full suite passes**, including Phase 6 (7/7) and Phase 7 (9/9) tests, `NativeWorkflowTest`, and `HistoryWorkflowTest`.
- **The OBD generator is genuinely configurable**, not a hard-coded demo: arbitrary signal list, per-signal piecewise trajectories, noise/bias/drift/stuck-value injection, missing-data fraction, DTC injection, deterministic seeds — all via a scenario file, no code changes needed. This is a real, reusable asset.
- **VHI aggregation logic itself (`VhiCalculator`) is sound**: weighted mean over eligible subsystems, explicit coverage fraction, exclusion reasons per missing subsystem, no fabricated identity (`vehicleId`/VIN are explicitly `"UNKNOWN"`/`"UNAVAILABLE"` when not resolved — the earlier "invented odometer" bug flagged in your own worklog has been fixed).
- **Phase 4/5 wiring for the 5 tracked signals is real**: `RollingWindowDetector` → `AnomalyEpisode` → `DegradationFeatureBuilder` → `TrendAnalyzer` → `DegradationStateEstimator` → `EventRiskEstimator` is genuinely called per-signal inside `InspectionService.assess()`, not stubbed.

## 2. What is not real — the gap between the pitch and the code

### 2.1 Virtual sensors are decorative, not functional

This is the single most important finding, because it's exactly what you told GoMechanic you're doing differently.

[`InspectionService.java:126-131`](application/src/main/java/com/ignitionai/application/InspectionService.java#L126-L131) computes Direct Features and Virtual Sensor outputs into a map called `all`. That map is:
- **Never used for anomaly detection** — the tracker loop only watches `floors.keySet()` = `{engine_rpm, coolant_temperature, vehicle_speed, calculated_engine_load, throttle_position}` (5 raw PIDs).
- **Never used for degradation, severity, or VHI** — those all key off `floors.keySet()` too.
- **Never included in the certificate** — `CertificateSnapshot` doesn't reference `observations` at all.
- **Only consumed in one place**: [`IgnitionDesktop.java:116`](native-desktop/src/main/java/com/ignitionai/desktop/IgnitionDesktop.java#L116) dumps them as `signal_id = value unit [quality]` into a plain-text "Evidence" tab.

Your own `docs/NATIVE_PREVIEW.md` (line 56, written by the agent that did this work) already says it in plain language: *"Direct features and virtual sensors are computed, while current anomaly tracking covers five raw indicators; their complete model wiring is unfinished."* That's the honest status. If you demo this today, "we use virtual sensors to see what raw OBD can't" is a claim the running code cannot back up — the virtual sensor numbers are printed, not acted on.

Only **3 virtual sensors exist**, all `implementation_type=formula` and all a bare subtraction (`diff(a, b)`) — not the pitch deck's "multi-sensor correlation" (fuel trims + MAF + O2 + RPM + load) or physics-based estimates:
- `charging_voltage_stability` = `control_module_voltage - battery_voltage`
- `fuel_trim_balance` = `long_term_fuel_trim_bank1 - long_term_fuel_trim_bank2`
- `thermal_response_proxy` = `engine_coolant_temperature - ambient_air_temperature` — **and this one has a bug**: its manifest declares input `engine_coolant_temperature`, but the canonical signal ID used everywhere else in the codebase (`SignalIds.java`, the generator, direct features) is `coolant_temperature`. This sensor can never resolve its input and will always report `unsupported`/`missing_inputs`.

### 2.2 "Subsystem" scoring is one raw signal per subsystem, not grouped indicators

The spec (Phase 6, §9.2) requires grouping *multiple* indicators under a named subsystem (e.g., "Engine," "Cooling," "Electrical") with internal weights and coverage. The live code instead does `subs.add(...)` once per raw PID in `floors.keySet()`, naming the "subsystem" after the PID itself (e.g. a subsystem literally called `engine_rpm`). There is no grouping step. The scoring engine (`SubsystemScoreCalculator`, `VhiCalculator`) *can* do real grouping correctly — proven by the Phase 7 test fixture demo in the test output, which shows a properly named `Engine: 65.00` / `Transmission: 75.00` breakdown — but that's fixture data exercising the module in isolation. The live orchestration (`InspectionService`) never calls it that way.

### 2.3 The certificate — the actual demo artifact — is a near-empty shell

`CertificateSnapshot.java` holds exactly: certificate ID, schema version, the health assessment (VHI + per-"subsystem" scores), source type, session ID, an evidence digest hash. Compare to spec §10.1, which requires: vehicle identity/config, capture duration and achieved operating conditions, supported/assessed/unassessed systems, DTCs, findings, anomaly episodes, evidence references, degradation and event-risk output with endpoint/horizon/uncertainty, recommendations, and per-module version stamps.

`LatexGenerator.java` (131 lines total) renders: header, vehicle identity (mostly `UNAVAILABLE`), VHI + band + severity, and the subsystem table. **No DTCs, no anomaly episodes, no evidence appendix, no degradation/risk section appear in the PDF at all**, even though `InspectionResult` (the object one level up) actually *has* `episodes` and `degradation` populated — they're just never passed into the certificate. This is a wiring gap, not a missing-feature gap, and it's the cheapest high-value fix in this whole audit.

### 2.4 Phase 1 knowledge engineering is fully disconnected from the runtime

`grep` for any reference to `RegistryQueryService`, `ObservationRegistry`, `FaultKnowledgeModel`, or `VehicleApplicability` inside `application/` or `native-desktop/`: **zero hits.** The four-registry knowledge engine (Technical Sources → Fact Extraction → Consistency Checks → Linked Registries) exists, has its own tests, and was itself separately audited on 17 September and found to have a build blocker plus several correctness defects (see `IgnitionAI_Phase1_Audit_and_Sources_2026-09-17.md` in this repo — still valid, nothing in Phase 1 has changed since). But even a fixed Phase 1 wouldn't matter for the demo: nothing in the live inspection path queries it. All thresholds (`floors` map) are hard-coded literals in `InspectionService`, not looked up from the Observation Registry as the architecture requires.

### 2.5 Three competing app implementations, two of them dead

- `native-desktop/IgnitionDesktop.java` (205 lines, Swing) — **this is the real, wired, launchable app** (`run-desktop.bat`).
- `pre-owned-dealership-app/` — the module the architecture diagram and README actually name for Phase 7.3. It's a **console-only MVP, not included in `settings.gradle` or `native-core`'s module list. Orphaned.**
- `desktop-app/DealershipDesktopApp.java` — a single-file leftover from a different agent ("Antigravity"). Your own `NATIVE_PREVIEW.md` says outright: *"preserved for integration review but are not the documented launchers. Do not distribute their hard-coded assessment screens."* **Dead code sitting in the repo you're about to hand to a CEO.**
- `android-app/` similarly contains two packages: `com.ignitionai.nativeapp` (real, wired, builds) and a leftover `ai.ignition.android` (old prototype, unused).

None of this breaks the build, but it means anyone opening this repo — including you, mid-crunch — can easily grab the wrong file, and it visibly signals "multiple people built this without merging" to any technical reviewer who browses the source.

### 2.6 The pitch deck oversells the current build materially

Reading `deck_iqoo.pdf` against the code:
- Deck: *"Multi-parameter scan... more than 370 parameters... multi-sensor correlation (fuel trims + MAF + O2 + RPM + load)."* Code: 5 raw PIDs drive every decision; 3 trivial two-input subtraction sensors exist and are unused.
- Deck: polished branded mobile "Health Certificate" mockup and laptop "Interactive Dashboard." Code: a functional but visually raw Swing app (plain `JTable`, `JTextArea` log dump, hand-drawn line chart) with no branding, and an Android app whose emulator instrumentation has never successfully run (`NATIVE_PREVIEW.md`: *"the available emulator exits before instrumentation can run"*).
- Deck: *"Structured Diagnosis Plan — flags internal issues... immediately understand and act on."* Code: this is explicitly out of scope per your own seven-phase plan (Structured Repair Plan is an OEM-branch module, not implemented), and the current certificate shows `"Recommendations unavailable"` territory, not a diagnosis plan.

None of this is a reason to panic — it's normal for a pitch deck to describe the vision. But if the CEO has seen this deck (or one like it) before the demo, showing the current raw Swing debug UI with 5 signals will read as a step backward from what was promised, unless you frame the demo correctly (see §4).

## 3. Root cause

Multiple AI coding agents worked on this repo in parallel over a short period (commit history shows a `codex/native-mvp-delivery` branch merged over/alongside "Antigravity" work that was deliberately preserved rather than deleted). Each agent respected the module *boundaries* (the architecture diagram mapping is genuinely honored — that part of your seven-phase plan worked) but the **orchestration layer that wires modules together was built fast and shallow**: it satisfies "does it compile and pass tests" without satisfying "does the data actually flow through every module the way the architecture diagram draws it." That's precisely how you get 40 well-organized directories that still add up to a 5-signal demo.

## 4. Action plan for 22 September (4 days)

**Reality check first:** your own seven-phase plan estimates 125–191 focused hours for the full scope. You have roughly 25–35 realistic focused hours before the 22nd. Do not attempt full spec completeness. The goal is: **make the virtual-sensor/intelligence story true for a small, real, well-chosen set of indicators, and make the certificate reflect what the pipeline actually finds.** Everything below is scoped to fit that.

### Day 1 (19th) — Cut dead weight, fix the wiring bug, make virtual sensors count

1. **Delete or clearly quarantine** `desktop-app/` and the old `ai.ignition.android` package. Keep `pre-owned-dealership-app/` only if you decide to demo through it instead of `native-desktop`; otherwise delete it too or mark it `ARCHIVED — superseded by native-desktop` in a README so nobody opens it by mistake before the demo.
2. **Fix the `thermal_response_proxy` input-ID bug**: change its manifest to `input_signal_ids=coolant_temperature,ambient_air_temperature` to match `SignalIds.java`.
3. **Wire virtual sensors and direct features into the real pipeline.** In `InspectionService.assess()`, extend the tracked-indicator set (currently `floors`) so it also covers the virtual sensor outputs (`voltage_stability_index`, `fuel_trim_balance_pct`, `thermal_response_index`) and at least one or two direct features. This means: give them baseline/expected-value tracking, feed them through the same `RollingWindowDetector` → episode → degradation chain the 5 raw PIDs already get. This is the highest-value fix in the whole plan — it's what turns "we compute virtual sensors" into "we detect faults raw OBD can't see."
4. **Define real subsystem grouping.** Replace the 1-PID-per-subsystem loop with an explicit map, e.g.:
   - `Engine/Combustion`: `engine_rpm`, `calculated_engine_load`, `throttle_position`, `fuel_trim_balance_pct`
   - `Cooling/Thermal`: `coolant_temperature`, `thermal_response_index`
   - `Electrical`: `voltage_stability_index` (plus raw `control_module_voltage` if you add it as a tracked signal)
   - `Drivetrain`: `vehicle_speed` (thin for now — fine to label low-coverage)

### Day 2 (20th) — New sensors, and make the certificate tell the truth

5. **Add 3–4 more sensors** using signals your generator can already emit with zero new adapter code (it's a config-only change — see `ScenarioFile.example()`), for example:
   - Real PID: `manifold_absolute_pressure`, `mass_airflow_rate`, `short_term_fuel_trim_bank1` (direct feature: windowed variability).
   - Virtual sensor, slightly more than a diff — e.g. **idle RPM stability index** (rolling stddev of `engine_rpm` while `vehicle_speed≈0` and `throttle_position` low — this is literally the spec's suggested starter feature in §6.2) and **warm-up rate** (`coolant_temperature` slope during the warm-up regime — also spec-suggested, and `CoolantWarmupRateFeature.java` already exists in `direct-features/`, just needs to be wired into scoring like the rest).
   - Keep formulas honest: label uncertainty and status exactly as the existing manifests do. Don't invent physics you can't defend under CEO questioning — a real automotive person will ask "how did you calibrate that."
6. **Wire `InspectionResult.episodes`, `.degradation`, and DTC state into `CertificateSnapshot`**, and extend `LatexGenerator` to render: DTC list, anomaly episodes (signal, onset, duration, evidence refs), and the degradation/risk block per assessed subsystem, each carrying its existing `"PRELIMINARY_INDICATOR_ONLY"` / `"RISK_UNAVAILABLE"` honesty labels. Don't strip those labels — they're your credibility shield, not a weakness, when talking to someone who runs inspection workshops for a living.

### Day 3 (21st) — Make it demoable, not just correct

7. Minimum UI pass on `native-desktop`: add the IgnitionAI logo/wordmark, basic color, and clean up the "Evidence" tab so virtual sensor rows are visually distinguished from raw PIDs (e.g., a "Derived" column) — you don't need the deck's mobile mockup, you need the existing Swing app to look intentional for 15 minutes on a projector, not like a debug console.
8. **Build and rehearse two scenario files**: one clean/healthy run, one with an injected fault that specifically shows a virtual sensor catching something the raw PID list alone would miss (e.g., inject a `long_term_fuel_trim_bank1` vs `bank2` imbalance with both individually "in range," so `fuel_trim_balance_pct` fires but neither raw PID alone would flag anything). This is the actual "we're smarter than a raw scan" moment — build the demo around proving it, don't just narrate it.
9. Generate the actual certificate PDF for both runs ahead of time as a fallback in case `pdflatex` misbehaves live.

### Day 4 (22nd) — Freeze and rehearse

10. Code freeze by midday. Full `build_and_test.bat` clean run as the final gate. No new features.
11. Dry-run the exact demo script twice, timed.
12. Prepare the honesty framing explicitly: *"This is a working preliminary pipeline on simulated and generator-driven data; virtual sensors are wired into detection for these specific indicators; scores are labeled preliminary/uncalibrated because we haven't validated against real fleet outcomes yet."* That sentence, said confidently, reads as engineering maturity to an operations-minded CEO — much better than an unlabeled score that invites "how do you know this number is right?"

## 5. What to explicitly not promise on the 22nd

- Don't claim "370+ parameters" or the deck's full multi-sensor breadth — you'll have ~8–12 real indicators wired, say that number.
- Don't demo the Android app live unless you get a real device or working emulator between now and then — instrumentation has never passed (`NATIVE_PREVIEW.md`). Desktop + PDF certificate is the safe demo surface.
- Don't present any VHI number as a calibrated failure probability. Keep every "preliminary"/"uncalibrated" label visible on screen — it's true, and it's the correct posture for a first pilot conversation with a company that will actually use this to buy/sell cars.
- Don't show the repo file tree to the CEO before Day 1's cleanup — the dead `desktop-app`/orphaned modules undercut the "not vibecoded" argument you want to make.

## 6. Beyond the 22nd (not for this demo, but next)

- Reconnect Phase 1 (knowledge registries) to the runtime so thresholds/applicability come from versioned knowledge instead of hard-coded literals — this is real work (Phase 1's own audit lists a build blocker and several correctness defects to fix first).
- Get an Android emulator or physical device instrumentation run to actually pass, and do one real Bluetooth ELM327 capture against a real vehicle.
- Decide, permanently, whether `pre-owned-dealership-app` or `native-desktop` is the canonical app and delete the loser rather than "preserving for review" indefinitely.
