# IgnitionAI: full rebuild action plan (post-feedback)

Prepared 19 September 2026, after being told the app was a toy and the certificate was unusable. This doc: (1) answers the integration question directly, with evidence; (2) lists what was fixed in the last hour, verified; (3) gives a real plan and real hour estimates for everything demanded — sensor/feature expansion, a real test suite, a real certificate, BMS predictive degradation, extra features — instead of promising speed I can't back up.

## 1. "Is the backend actually integrated, or are you just manipulating the frontend?"

Real answer: integrated. One code path, no shortcuts. Concretely:

- `IgnitionDesktop`'s "Open OBD session" button calls `AssessmentHistory(...).assess(SessionFiles.read(path))`. `AssessmentHistory` calls `InspectionService.assess()`. That is the exact same method [`tools/pipeline-runner/CertificateDemoRunner.java`](tools/pipeline-runner/CertificateDemoRunner.java) calls, the exact same method every automated test in `build_and_test.bat` calls, and the exact same method the new standalone simulator's output feeds once you open its saved session file. There is no second, simplified, or mocked path for the UI.
- Proof you can run yourself right now: change any signal's trajectory in a scenario file, generate a new log with the standalone simulator, open it in the main app, and the VHI/Diagnostic Index/DTCs will be different numbers — because they're computed from whatever bytes are in that file, not from a template.
- Independent proof I ran this session: encoded 411 real sensor readings into actual ELM327 hex byte frames (SAE J1979 formulas), decoded them back with the published formula, saved them as a session file, fed that file through `InspectionService` exactly as the app would, and got a real, non-canned VHI (74.44) and 9 real Diagnostic Index results out the other end. Round-trip mismatches: 0.

## 2. Fixed in the last hour, verified against real output (not asserted)

| Complaint | Fix | Verified how |
|---|---|---|
| "Simulator should be a separate app, not inbuilt" | Removed the Simulator tab entirely from `IgnitionDesktop`. New standalone app: `IgnitionSimulator` (`run-simulator.bat`). It only generates logs; `IgnitionDesktop` only ingests them via "Open OBD session". This was already the architecture your own Seven-Phase plan specified (§5.3: "Keep it as development tooling... It must not become a second analytical pipeline") — I had violated my own written spec by embedding it. | Full suite passes; the two apps now compile and run as separate `main()` entry points. |
| "I want a real log exactly like what comes out of a USB OBD port" | New `Elm327FrameEncoder`: encodes generated telemetry as actual AT-command/hex-response traffic (`>010C` / `41 0C 1A F8`) per the published SAE J1979 PID formulas, plus Mode 03 DTC frames per SAE J2012. The simulator shows this scrolling live in a terminal-style console and separately saves the structured session file the assessment app reads. | 411-reading round-trip test: encode → decode with the textbook formula → compare to source value. 0 mismatches. |
| "Where is the DTC codes?" | Two separate real gaps, both fixed: (1) `IgnitionDesktop` computed DTCs correctly but never displayed them anywhere in the UI — added a dedicated DTC panel on the Assessment tab. (2) The demo fixture had `dtcs=` empty on purpose (to show the Diagnostic Index adds value beyond a DTC scan) — added a real P0301 back in, timed to still be present at the final scan. | Regenerated certificate, grepped the `.tex`: `Diagnostic Trouble Codes ... P0301` now present. |
| (found while verifying the above, not asked about) | `InspectionService` rebuilds its DTC set from scratch on every complete scan. A fault that's injected mid-drive and resolves before the session ends will legitimately show zero DTCs at the final certificate — accurate to "if you rescanned right now it's clear," not accurate to how a real ECU actually latches a stored code until it's cleared or ages out over ~40 drive cycles. Not fixed (needs a stored-DTC lifecycle model — see §5), but documented so it doesn't surprise you in a live demo with a different fixture. | Found via direct testing: generated `ScenarioFile.example()`'s built-in fault, confirmed 25 real DTC-bearing messages mid-session, confirmed final `dtcCodes` was empty because the fault window ended 15s before the scan did. |
| "Dialog opens for .tex, and .aux/.log/.pdf clutter the save folder" | `exportCertificate()` now compiles in a throwaway temp directory and copies out **only the finished PDF** to wherever you choose. Nothing else touches that folder. Temp directory (with the .tex and pdflatex's .aux/.log) is deleted on success; kept and named in the error message if compilation fails, so you can still debug it. | Ran the export path end-to-end in a test harness: target folder contained exactly one file, `MyCert.pdf`, nothing else. Temp build directory confirmed deleted afterward. |

Full `build_and_test.bat` suite (55+ test groups) re-run clean after every change above.

## 3. The certificate: what "not a generic-ass report" actually requires

Agreed it isn't good enough. Two different problems, both real:

**Layout/production values** — currently a table-heavy academic report, not a certificate. To fix properly:
- Real letterhead treatment: full-width colored header band, not just a logo in a corner.
- A proper "certificate" visual language: border rule around the page, a serif or semi-serif display face for the headline number (not the same sans-serif as body table text), more whitespace, a clear visual hierarchy separating "the one number a buyer looks at" from "the technical appendix a mechanic reads."
- A verification block: certificate ID rendered as both text and a QR code (points to nothing live yet, but looks and functions like Cars24/Spinny's authenticity marks) plus an issue-date/inspector-of-record line.
- Page 1 should be a one-page "at a glance" summary (headline score, band, 2–3 line plain-English verdict, DTC count, indices flagged) — the full Diagnostic Index table, Monitored Indicators, evidence appendix etc. move to page 2+. Right now everything is one undifferentiated flow.
- **Estimate: 6–9 hours.** This is real LaTeX design work (packages: `tikz` for the border/QR, `fontspec` or a serif substitute for headline type, `fancyhdr` for the letterhead band), not a five-minute CSS tweak equivalent.

**Content** — already substantially fixed this session (real Diagnostic Index findings, DTCs, plain-English sentences, VHI honestly demoted) but not yet visually reflecting that hierarchy. The redesign above is what makes the existing real content actually look like what it is.

I'll do this next, right after this message, without waiting — it doesn't depend on anything from you.

## 4. Sensor/feature expansion: ~30 items, not the full ~100

You asked for "around 30-40." Cutting the full catalogue ([`IgnitionAI_Sensor_Catalogue_and_Certificate_Design_2026-09-18.md`](IgnitionAI_Sensor_Catalogue_and_Certificate_Design_2026-09-18.md) §2) down to the ~30 that are genuinely worth having for this demo, ordered by value-per-hour:

**Raw PIDs to add (all trivial — new signal ID, no new logic), ~1 hour total for all 8:**
`accelerator_pedal_position`, `commanded_throttle_position`, `fuel_level`, `fuel_rate_lph`, `ambient_air_temperature`, `o2_bank1_sensor2_voltage`, `o2_bank1_sensor2_stft`, `long_term_fuel_trim_b2`.

**Derived features (pure arithmetic on signals already present after the above), ~4-5 hours for all ~14:**
`Speed_mps`/`RPM_radps`/`Accel_mps2`/`Jerk_mps3` (kinematics, already have the pattern from `SpeedMpsFeature`), `Cruise_flag`/`AccelEvent_flag`/`DecelEvent_flag` (alongside the `EngineRunning_flag`/`Idle_flag` already shipped), `CoolantMinusAmbient_C`/`OilMinusAmbient_C` (now buildable — `ambient_air_temperature` is in this batch), `CombinedFuelTrim_pct`/`FuelTrimImbalance_pct` (promoting math already inline in `FuelIndexPlugin` into a reusable Feature), `InstantFuelEconomy_kmL`, `SensorCoverage_pct`.

**Diagnostic Index composites (Wave 3 — each needs real plugin design, not mechanical):**
- `OverallConditionScore` — coverage-weighted blend of all 9 shipped indices. This is also the real fix for the VHI/Diagnostic-Index split flagged earlier. **3-4 hours.**
- `GearConsistency_flag` — needs `estimated_gear` (RPM/speed ratio lookup table) first. **2-3 hours combined.**
- `ElectricalRisk_flag`/`ThermalStress_flag` — trivial, these are just the existing Charging/Thermal `FLAGGED` state exposed as a standalone boolean column for a simpler dashboard view. **1 hour.**

Total for this slice: **~12-15 hours** of real, individually-tested work, each one verified against regenerated output the same way everything above was, not batch-committed on faith. That gets you to 8 raw + 14 derived + 3 new composites = **25 new items on top of the 22 already shipped (11 signals + 3 features + 9 indices, excluding the legacy 5)** — i.e., landing right around your "30-40" ask once you count everything already live.

## 5. BMS predictive degradation — architecture is ready, blocked on your data

`TrendAnalyzer`/`DegradationStateEstimator`/`EventRiskEstimator` already implement a real state-space trend model + discrete-hazard risk interface — built for the 5 legacy signals, but the interfaces are signal-agnostic. A BMS plugin is a new `DiagnosticIndexPlugin` (or a set of Direct Features feeding the existing degradation pipeline) that needs, from you, before I write a single line against it:

1. **What's in the data**: raw cell/pack voltage, current, temperature time series? Or pre-computed SOH/SOC/internal-impedance per session? Both, at different rates?
2. **Sampling rate and session length** — same question I'd ask of any new signal, but load-bearing here since degradation trend estimation needs enough history to say anything.
3. **Labels, if any** — do any sessions have a known outcome (replaced, degraded-confirmed, healthy) I can use to sanity-check the model isn't just noise? If not, this ships the same way `HazardModelConfig` did: real interface, `isCalibrated=false`, loud warning, no fabricated probability — which is honest but not "predictive" in the sense you're asking for. Calibration needs real outcomes, not more clever math.

Send whatever you have (even a schema without real data yet) and I'll scope the exact plugin and hours instead of guessing.

## 6. Extra feature suggestions (grounded in your own deck and spec, not invented)

Pulled from `deck_iqoo.pdf` and `IgnitionAI_Technical_Architecture_and_Deployment_Specification.pdf`, which I re-read this session rather than re-guessing:

- **"Premium-certified inventory" gate** (deck p.8): a simple, explicit rule — e.g. VHI ≥ threshold AND zero Diagnostic Index flags AND no active DTCs → certificate carries a distinct "IgnitionAI Certified" badge/section, otherwise it doesn't. This is a real product lever the deck names ("can be marketed as a separate 'certified' category") and costs almost nothing to add on top of what already exists — it's just a documented threshold rule over data you already compute. **~2 hours** including the certificate section.
- **Historical comparison, surfaced properly**: `compare()` already exists in the backend (VHI movement between two sessions on the same vehicle) but is desktop-only and text-dump styled. Worth a real certificate section ("this vehicle's 2nd inspection: VHI 82→74, Combustion newly flagged") for vehicles GoMechanic re-inspects — directly matches the deck's "reduce post-sale complaints... detecting developing issues early" pitch. **~2-3 hours.**
- **Fault-relationship count as a headline stat** (deck p.3: "more than 370 PID combinations"): once `OverallConditionScore` (§4) exists, the certificate can honestly state something like "correlated across N signal-pairs this session" as a differentiator line — real, since `FaultCandidateRanker` and the index plugins already do multi-signal correlation, just not currently counted/surfaced as a number.
- Not suggesting the deck's OEM-side items (Probabilistic Fault Graph, Structured Repair Plan, LLM Explanation Layer) — your own architecture spec explicitly scopes those out of the pre-owned MVP, and building them now would be exactly the kind of scope creep that produces half-finished features instead of a solid demo.

## 7. Time reality, stated plainly

Certificate redesign (6-9h) + sensor/feature slice (12-15h) + comprehensive multi-scenario test suite (not scoped above — realistically **6-8h** for 6-8 genuinely different fixtures: healthy baseline, per-subsystem single faults, a multi-fault vehicle, a sensor-dropout/low-quality-data case, and a DTC-heavy case, each hand-verified against its own regenerated certificate) + certified-gate/history features (4-5h) = **28-37 hours** of real work on top of what's already shipped. BMS is unscoped until I have your data.

That does not fit "fast." I'm doing the certificate redesign next regardless, since it doesn't need anything from you. Tell me if you want the sensor slice or the test suite prioritized next — both are ready to start the moment you say so, and I'd rather ask than guess wrong and redo it a third time.
