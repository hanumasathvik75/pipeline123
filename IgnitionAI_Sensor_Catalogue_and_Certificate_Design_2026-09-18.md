# IgnitionAI sensor catalogue: feasibility, phasing, and certificate design

Prepared 18 September 2026, as a follow-up to the deep audit and demo action plan. Covers: (1) your original ~100-column sensor list, categorized and phased so the MVP is a foundation to iterate on rather than a one-off demo; (2) what the new certificate actually shows and why, including the code changes made in this session.

## Update — 19 September 2026: 3 research-grounded sensors added, thresholds moved out of code

Following review of two literature audits (`cluster_A_automotive.md`: 14 automotive sensor/diagnostics papers; `cluster_B_soft_sensor_methods.md`: 27 chemical-process soft-sensor papers) against IgnitionAI's `DiagnosticIndexPlugin` contract:

- **3 new diagnostic indices shipped**, bringing the total to **8**: `Air Intake` (Air-Intake Consistency), `Timing Compliance`, `Driving Behavior`. Each is grounded in a specific cited paper — see §5 below for the research basis, adaptation, and honest limitations of each (none is a verbatim reproduction; all three needed adaptation because the source papers assume calibration data IgnitionAI doesn't have yet, stated explicitly in each plugin's Javadoc and in §5).
- **Every numeric threshold across all 8 indices was moved out of Java code** into `virtual-sensors/src/main/resources/config/diagnostic-index-config.json`, a versioned, human-readable config file with a stated rationale next to every value. No plugin has a hardcoded `private static final double` threshold anymore — this directly implements the seven-phase plan's requirement that weights/thresholds live in a "versioned policy," which was previously not done (flagged in the original deep audit).
- The legacy raw-signal `InspectionService` floors and VHI weights (previously `Map.of("engine_rpm", 150.0, ...)` and `weights.put(id, 1.0)` inline in Java) now also read from the same config file, under `monitored_indicators`. The config file states plainly that these weights are **provisional equal weights pending real fleet/domain calibration** — not a fabricated "precise" number, and not a silent magic constant either.
- The Diagnostic Index certificate table no longer prints raw `key=value` evidence as its main content — each finding now leads with one plain-English sentence (e.g. *"Engine speed took about 1.7s to respond after a throttle input — slower than the 1.5s expected for a healthy air path"*), with the technical evidence trace kept underneath in small type for a technician. This was a real gap the previous version had: it read as exactly the "raw text dump" you asked not to ship.
- **BMS predictive degradation is intentionally not touched.** Neither literature cluster contained a battery/BMS paper, and you said you have your own fleet data for this and would rather supply it than have anything invented. The existing `degradation-state-estimator`/`event-risk-estimator` contracts (Phase 5) are already vehicle-subsystem-agnostic, so a BMS index can plug into the same `DiagnosticIndexPlugin` contract later without new architecture — flagged as open, not started.

## 1. What changed in the code this session (verifiable, not aspirational)

- Added a new `com.ignitionai.virtualsensors.index` package (in the `virtual-sensors` module) with a `DiagnosticIndexPlugin` interface, discovered automatically via `java.util.ServiceLoader` — see the companion doc, [IgnitionAI_Virtual_Sensor_Authoring_Guide_2026-09-18.md](IgnitionAI_Virtual_Sensor_Authoring_Guide_2026-09-18.md), for the exact contract.
- Implemented **5 real composite diagnostic indices** against that interface: Combustion, Fuel, Thermal, Load Response, Charging. Each computes its result from actual telemetry statistics (rolling-window RPM variance, fuel-trim exceedance, warm-up slope + steady-state variance, throttle-to-RPM cross-signal lag, voltage band + ripple) — not placeholder formulas. Full method notes are in each class's Javadoc under `virtual-sensors/src/main/java/com/ignitionai/virtualsensors/index/`.
- Extended `CertificateSnapshot` (non-breaking — existing constructors and tests are untouched) with an evidence appendix: DTCs, anomaly episodes, degradation/risk outputs, the 5 diagnostic-index results, and a ranked fault-candidate list.
- Wired `InspectionService` to compute all of this and attach it to every certificate.
- Rewrote `LatexGenerator` for a real formatted certificate (see §3).
- Fixed a real bug found in the same pass: `VirtualSensorRuntime` never actually registered any `implementation_type=plugin` sensor (zero were ever wired anywhere in the codebase) — `discoverPlugins()` now does this via ServiceLoader too.
- Fixed a real build gap: neither `compile.ps1` (the actual `run-desktop.bat` build path) nor `native-core/build.gradle` copied `src/main/resources` onto the runtime classpath, so `ServiceLoader` and the logo asset would have silently found nothing at runtime. Both are now fixed.
- Added a demo scenario, `fixtures/demo-diagnostic-index-showcase.properties` (load via Simulator → Load scenario), built to exercise all 5 new indices with a genuine multi-second throttle response lag and elevated cruise-RPM noise, without hand-faking the resulting numbers.

This is a foundation, not filler: every one of the ~100 columns below either already exists, maps directly onto a `direct-features` Feature (pure arithmetic, no statistics), or maps onto the same `DiagnosticIndexPlugin` contract the 5 shipped indices already use. Nothing here requires a new architecture.

## 2. Column-by-column phasing

Legend: **Done** = implemented and wired into the certificate this session. **Wave 2** = trivial to add (new raw PID or pure arithmetic on existing signals, no statistical design). **Wave 3** = composite/model-derived, needs the `DiagnosticIndexPlugin` treatment (see the authoring guide) — several already have a close match among the 5 shipped indices.

### 2.1 Direct signals

| Column | Canonical signal id (proposed) | Status |
|---|---|---|
| RPM | `engine_rpm` | Done |
| Coolant_C | `coolant_temperature` | Done |
| Throttle_pct | `throttle_position` | Done |
| Speed_kmh | `vehicle_speed` | Done |
| Load_pct | `calculated_engine_load` | Done |
| STFT_pct | `short_term_fuel_trim_b1` | Done |
| LTFT_pct | `long_term_fuel_trim_b1` | Done |
| ModuleVoltage_V | `control_module_voltage` | Done |
| Timestamp_ms | (envelope field, not a PID) | Already present in `ObdMessage` |
| OilTemp_C | `oil_temperature` | Wave 2 — add to generator + schema, no logic change |
| MAP_kPa | `manifold_absolute_pressure` | Wave 2 |
| IAT_C | `intake_air_temperature` | Wave 2 |
| TimingAdv_deg | `timing_advance` | Wave 2 |
| AccelPedal_pct | `accelerator_pedal_position` | Wave 2 |
| CmdThrottle_pct | `commanded_throttle_position` | Wave 2 |
| DemandTorque_pct | `demanded_torque_pct` | Wave 2 |
| ActualTorque_pct | `actual_torque_pct` | Wave 2 |
| Baro_kPa | `barometric_pressure` | Wave 2 |
| O2S2_V | `o2_bank1_sensor2_voltage` | Wave 2 |
| O2S2_STFT_pct | `o2_bank1_sensor2_stft` | Wave 2 |
| CmdEquivRatio | `commanded_equivalence_ratio` | Wave 2 |
| CatalystTemp_C | `catalyst_temperature` | Wave 2 — vehicle-dependent support |
| O2_SecLTFT_pct | `long_term_fuel_trim_b2` | Wave 2 |
| FuelLevel_pct | `fuel_level` | Wave 2 |
| FuelRate_Lph | `fuel_rate_lph` | Wave 2 |
| AmbientTemp_C | `ambient_air_temperature` | Wave 2 (already used as an input elsewhere) |
| RefTorque_Nm | `reference_torque_nm` | Wave 2 |
| O2S1_Lambda | `o2_bank1_sensor1_lambda` | Wave 2 |
| O2S1_Current_mA | `o2_bank1_sensor1_current` | Wave 2 |
| AbsLoad_pct | `absolute_load` | Wave 2 |
| EvapPurge_pct | `evap_purge_command` | Wave 2 |
| Gear | `estimated_gear` | Wave 2 — simple lookup table from RPM/speed ratio, see below |

### 2.2 Derived (pure arithmetic — implement as `direct-features` Feature classes, same pattern as the existing `CoolantWarmupRateFeature`)

| Group | Columns | Status |
|---|---|---|
| Time/kinematics | Timestamp_s, DeltaT_s, Speed_mps, RPM_radps, Accel_mps2, Accel_smooth_mps2, Jerk_mps3, DistanceDelta_m, TripDistance_km | Wave 2 |
| Drive-state flags | EngineRunning_flag, Idle_flag, Cruise_flag, AccelEvent_flag, DecelEvent_flag | Wave 2 |
| Air path | BoostGauge_kPa (`MAP - Baro`), BoostPressureRatio (`MAP / Baro`), IntakeTemp_K, AmbientTemp_K, IntakeTempRise_C | Wave 2 (needs MAP_kPa, Baro_kPa from §2.1) |
| Thermal deltas | CoolantRise_C, OilRise_C, CoolantMinusAmbient_C, OilMinusAmbient_C, OilMinusCoolant_C, CoolantRate_Cps, OilRate_Cps, IntakeTempRate_Cps, WarmEngine_flag | Wave 2 (WarmEngine_flag is the threshold check already inside the Thermal diagnostic index — worth promoting to its own reusable feature) |
| Fuel/economy | CombinedFuelTrim_pct, FuelTrimImbalance_pct, TargetLambda, LambdaError, EstimatedAFR, FuelRate_Lps, InstantFuelEconomy_kmL, InstantFuelEconomy_L100km, FuelUsedDelta_L, TripFuelUsed_L, FuelCostPerKm, EstimatedCO2_gps, TripCO2_kg | Wave 2 (CombinedFuelTrim_pct is already computed inline inside `FuelIndexPlugin` — Wave 2 work is just promoting it to a standalone Feature so other consumers can reuse it) |
| Torque/response | EstimatedTorque_Nm, EnginePower_kW, TorqueDemandGap_pct, TorqueDeliveryRatio, PedalThrottleGap_pct, ThrottleCommandGap_pct, PedalRate_pctps, ThrottleRate_pctps, TorqueRate_pctps, RPMperKmh | Wave 2 |
| Electrical | VoltageRate_Vps, VoltageMovingAvg_V, VoltageMin_30s_V, VoltageDropUnderLoad_V, VoltageStability_stdV | Wave 2 — largely the same rolling-window math already inside `ChargingIndexPlugin`, just exposed per-instant instead of session-level |
| Data quality | SensorCoverage_pct | Wave 2 |

### 2.3 Model-derived / composite (needs `DiagnosticIndexPlugin`, see the authoring guide)

| Column | Relationship to what's shipped now |
|---|---|
| EngineAnomalyScore | Closely matches the shipped **Combustion** index (RPM/load/torque/throttle deviation) |
| ThermalAnomalyScore | **= the shipped Thermal index**, already implemented |
| FuelSystemAnomalyScore | **= the shipped Fuel index**, already implemented |
| ElectricalAnomalyScore | **= the shipped Charging index**, already implemented |
| — (not in your original list) | The shipped **Load Response** index (throttle→RPM lag) has no direct counterpart in your original catalogue — it's a genuine gap-fill, closest correlates are PedalThrottleGap_pct/TorqueDemandGap_pct at the Derived level, which report the gap but not a temporal lag |
| OverallConditionScore | Wave 3 — weighted blend of the 5 (or more) diagnostic indices; same shape as VHI but at the "which subsystem" resolution rather than raw-PID resolution |
| ConfidenceScore | Wave 3 — already exists per-index (`getConfidencePct()`); an overall session score is a coverage-weighted average, same pattern as `VhiCalculator`'s `overallConf` |
| SignalQualityScore | Wave 3 — straightforward: missingness fraction + implausible-value fraction + achieved vs. requested sampling rate, already tracked in pieces (the `invalid` counter, `DtcSnapshotCompleteness`) but not unified into one score |
| MAPExpected_kPa / MAPResidual_kPa | Wave 3 — same expected-vs-actual pattern as `ThermalIndexPlugin`'s warm-up model, applied to MAP vs. RPM/load |
| CoolantExpected_C / CoolantResidual_C | Wave 3 — refinement of the shipped Thermal index into an explicit expected-value model instead of a fixed operating-temperature threshold |
| FuelTrimExpected_pct / FuelTrimResidual_pct | Wave 3 — refinement of the shipped Fuel index the same way |
| PowerExpected_kW / PowerResidual_kW | Wave 3 — needs EstimatedTorque_Nm/EnginePower_kW from §2.2 first |
| GearConsistency_flag | Wave 3 — needs `estimated_gear` (§2.1) first; then a simple lookup-table plausibility check |
| ThermalStress_flag, ElectricalRisk_flag | Wave 3 — these are just the `FLAGGED` state already returned by the Thermal/Charging indices, exposed as a boolean flag column for a simpler dashboard view |
| O2ResponseError, O2DownstreamDeviation | Wave 3 — needs O2S1/O2S2 signals from §2.1 first; same residual pattern |

## 3. Certificate design and where the USP actually lives

The rewritten `LatexGenerator` (see [`latex-certificate-generator/.../LatexGenerator.java`](latex-certificate-generator/src/main/java/com/ignitionai/phase7/latex/LatexGenerator.java)) produces, in order:

1. **Header** — your logo (extracted from `IgnitionAI_Diagnostic_Report_demo.pdf` and placed at `latex-certificate-generator/src/main/resources/branding/ignitionai_logo.png`), certificate title, timestamp.
2. **Summary panel** — VHI headline, vehicle identity, capture duration/record count/source, diagnostic-index coverage. This is the part every dealership report (Cars24, Spinny, this codebase's own reference PDF) already has — familiar, not differentiated.
3. **IgnitionAI Diagnostic Index** — the 5 composite findings (Health/Confidence/State/Basis), explicitly captioned *"Findings from relating multiple signals over the full drive — not a single-PID read."* **This is the differentiation section**, and it is the first substantive thing after the summary, not buried at the back.
4. **Ranked Fault Candidates** — only rendered when something is actually flagged. Explicitly labeled as a heuristic ranking, not a calibrated probability, with the DTC-corroboration text so a technician knows whether a code exists to back it up.
5. **Monitored Indicators** — the existing raw-PID baseline-deviation table, relabeled from "Subsystem Health Scores" to be honest that it's currently one row per raw signal, not a true subsystem rollup (a known limitation flagged in the deep audit — Wave 3 subsystem grouping is separate follow-up work, not done in this session).
6. **DTCs, Anomaly Episodes, Degradation & Predictive Risk** — all now actually rendered from the real `InspectionResult` data, which the *previous* certificate silently dropped (see the deep-audit doc, §2.3) even though the data existed one object away.
7. **Confidence & Coverage, Methodology & Traceability, footer** — versions, certificate ID, evidence digest, and an explicit method-boundary statement: deterministic statistical algorithms, heuristic ranking, no repair recommendation, technician confirmation required. This is the same honesty posture as the rest of the codebase (`PRELIMINARY_INDICATOR_ONLY` etc.) and it is intentional — the target reader runs an inspection business and will discount overclaimed precision immediately, but will trust a system that shows its work and says what it doesn't know.

**Why the USP is "indirect" rather than a marketing banner:** nothing on the certificate says "AI-powered" or similar. The differentiation is that Section 3 exists at all and has real numbers with real supporting evidence (contributing PIDs, thresholds, sample counts) — a raw OBD scan tool or a technician glancing at live PIDs cannot produce "the throttle-to-RPM response lag averaged 3.0 s across 2 tip-in events, exceeding the 1.5 s tolerance" because that requires relating two signals over time across the whole drive, not reading either one at an instant. That is the actual, defensible difference from what GoMechanic's existing process does, and it is what the certificate leads with.

## 4. Recommended next-wave order (not started yet)

Given the 22 September deadline, do not start Wave 2/3 work before the demo — the action plan's Day 1–4 sequence (wire what exists, cut dead code, rehearse) still applies. After the demo, the natural order is:

1. Wave 2 signals that feed Wave 3 composites you want next (MAP_kPa/Baro_kPa before BoostGauge/BoostPressureRatio; O2S1/O2S2 before O2ResponseError).
2. Promote the inline math already inside the 5 shipped plugins (CombinedFuelTrim_pct, WarmEngine_flag, voltage rolling stats) into standalone `direct-features` Features — free reuse, already-tested logic.
3. OverallConditionScore and ConfidenceScore, once there are enough diagnostic indices to make a blended score meaningful (2 is thin, 5–7 is reasonable). **Now more urgent**: with 8 indices live and 2 flagged in the demo run while the headline VHI still reports 100/EXCELLENT, this disconnect is no longer a minor caveat — see §5.4.
4. Subsystem grouping fix flagged in the deep audit (`Monitored Indicators` currently one row per raw PID) — orthogonal to this catalogue but affects how any new Direct/Derived signal should be scored, not just displayed.

## 5. The 3 new research-grounded diagnostic indices

Two literature audits were reviewed against the `DiagnosticIndexPlugin` contract: `cluster_A_automotive.md` (14 automotive sensor/diagnostics papers, ranked by relevance) and `cluster_B_soft_sensor_methods.md` (27 chemical-process soft-sensor papers). Cluster B's own conclusion is blunt and correct: the overwhelming majority of soft-sensor literature assumes a real, lab-or-hardware-measured target and a plant-historian-scale historical dataset (hundreds to thousands of samples across weeks/months) — neither of which IgnitionAI has for a single OBD inspection drive. Its practically-borrowable ideas (session/batch statistical feature extraction, correlation-based variable selection, Hampel/robust outlier filtering) were folded into how the 3 new indices below were built, rather than yielding standalone new sensors themselves. Cluster A, being automotive-specific and several papers using exclusively standard OBD-II PIDs with real fault-injection validation, produced the 3 direct candidates.

### 5.1 Air Intake (`AirIntakeConsistencyIndexPlugin`)

**Research basis:** Two papers converge on the same principle. Komorska, Wołczyński & Borczuch 2018 (*"Fault diagnostics in air intake system of combustion engine using virtual sensors"*, Diagnostyka 19(1)) built empirical MAP/MAF look-up tables from a separate steady-state engine-map calibration sweep on two real engines, and showed the residual signature between predicted and actual MAP/MAF cleanly separated three real injected faults (MAF obscuration, TPS drift, intake leak) — faults the vehicles' own OBD systems flagged **zero** of. Ahmed, Bhatti & Iqbal 2011 (*"Virtual Sensors for Automotive Engine Sensors Fault Diagnosis in Second-Order Sliding Modes"*, IEEE Sensors J. 11(9)) built a physics-based nonlinear observer relating MAP and RPM dynamically, detecting an injected pressure-sensor freeze fault in under 1 second, again with zero OBD DTC raised.

**Honest adaptation:** both papers calibrate their expected-value model from a *separate* dataset (a dedicated engine-map sweep, or an identified physical model with named per-engine constants) before comparing live readings against it. IgnitionAI has no such per-engine calibration data yet. The shipped plugin instead checks whether MAP, MAF and an RPM×load proxy correlate strongly **within the current session's own operating-point spread** — the same underlying physical principle (MAP and MAF are two independent measurements of the same intake airflow, and should co-vary with load), tested session-natively rather than against a pre-calibrated reference. This is weaker evidence than the source papers' versions. The upgrade path (once multi-session calibration data exists) is in the authoring guide.

**Demo result (genuine, not staged):** correlations of 0.96–0.97 in the showcase scenario, correctly returning `NORMAL` — the scenario's MAP/MAF/load trajectories were built to be physically consistent, and the plugin correctly recognized that rather than false-flagging.

### 5.2 Timing Compliance (`TimingComplianceIndexPlugin`)

**Research basis:** Komorska, Wołczyński & Borczuch 2019 (*"Diagnosis of sensor faults in a combustion engine control system with the artificial neural network"*, Diagnostyka 20(4)) electrically shifted 11 different sensor characteristics on a real GDI engine while keeping each reading inside the OBD system's own pass/fail tolerance band — the vehicle's OBD never flagged a single one. The ECU's adaptive control still silently compensated (spark timing, injection duration, resulting λ) to hold its target, and those compensation fingerprints, examined across the RPM/load operating envelope, classified which sensor had drifted with 99.6% accuracy via a trained ANN.

**Honest adaptation:** the paper's 99.6% figure is for a 12-class ANN trained on labelled chassis-dynamometer fault-injection data IgnitionAI does not have. The shipped plugin keeps only the *detection* half: it bins the session by (RPM, load) and checks whether commanded ignition timing is repeatable each time the engine revisits the same operating point. It flags erratic compensation without attempting to name which upstream sensor caused it — a real but deliberately narrower claim than the source paper's fault classifier.

### 5.3 Driving Behavior (`DrivingBehaviorIndexPlugin`)

**Research basis:** Michailidis, Panagiotopoulou & Papadakis 2025's OBD-II ML review (Sensors 25, 4057) cites Rimpas et al. 2022's rule-based event-detection approach (idle/gear-change/acceleration events from RPM, throttle, load), reporting 88–95% accuracy using only standard OBD-II PIDs, with no trained classifier required.

**Honest adaptation:** implemented as named acceleration/deceleration thresholds on `vehicle_speed` alone (simpler than the cited rule set, which also used RPM/throttle/MAP) — a deliberate simplification for a first version, tracked as a Wave 2+ improvement to fold in RPM/throttle for a closer match to the cited method. This index is explicitly a **usage-severity indicator, not a fault index** — its evidence includes the line `interpretation: usage severity indicator, not a mechanical fault` for exactly this reason, and it's the one addition in this set that speaks to pre-owned valuation (how hard was this car driven) rather than mechanical condition.

### 5.4 What's still not resolved: VHI doesn't see the Diagnostic Index

Running the demo scenario end-to-end produces Combustion at 77.0% health (`FLAGGED`) and Load Response at 0.0% health (`FLAGGED`, from a 2-event sample — correctly low-confidence at 21%), while the headline Vehicle Health Index still reports **100.00/100, EXCELLENT**. This is the same structural gap flagged in the original deep audit, now sharper with 8 live indices instead of 5: the Diagnostic Index and the legacy Monitored Indicators/VHI pipeline are still two parallel, unmerged scoring layers. §4 item 3 (OverallConditionScore) is the fix; it was not implemented this session because folding Diagnostic Index results into VHI changes the certificate's headline-number semantics and deserves its own explicit decision rather than a silent side effect of a sensor-rollout task.

### 5.5 BMS predictive degradation — explicitly not started

Neither literature cluster contained a battery/BMS paper, and per your instruction this is intentionally left alone pending the fleet data you have. When ready, the natural integration point is a new `DiagnosticIndexPlugin` (or, if BMS data arrives as pre-computed features rather than raw telemetry, a variant runtime consuming `List<BmsRecord>` instead of `List<ObdMessage>` — the interface shape, not the implementation, is what would need a small extension). The existing `degradation-state-estimator`/`event-risk-estimator` (Phase 5) contracts are already subsystem-agnostic and could host BMS state-of-health trending without new architecture. Tell me what the data looks like (schema, sampling rate, whether it's raw cell voltages/temperatures or already-computed SOH/SOC/impedance) when you're ready and this gets scoped properly rather than guessed at now.
