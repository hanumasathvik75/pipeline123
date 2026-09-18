# Native MVP delivery

User goal: finish the complete app, address its shortcomings, push to GitHub, and deliver native Java Windows and Android apps plus a native Java simulator GUI. The attached audit requirements remain the acceptance checklist. OEM diagnostic planning and the pre-owned assessment pipeline both remain in scope.

## Baseline verified 2026-09-18

- Release branch is `master`, commit `11b399c`; only this branch remains remotely.
- The current suite passes, but the cumulative demo forces anomaly state, substitutes a healthy score, and exports after certificate validation fails.
- The dealership application is console-only. No Android or Windows graphical app exists in this repository.
- Phase 5 emits normalized degradation scores, whereas Phase 6 assumes percent scores. VHI inserts invented odometer and identity values.
- Application packaging, UI workflows, real cross-platform import/export, hardware capture verification, and customer validation are unfinished.

## Delivery sequence and evidence gates

1. Replace the cumulative demo with a reusable, causal application service; retain source/quality/identity through every phase. No invented health, vehicle identity, mileage, or calibrated probabilities.
2. Native Swing Windows assessment application and configurable simulator GUI; asynchronous work, replay import/export, history and certificate workflow. Verify actual graphical controls and packaged launch.
3. Native Java Android Activities plus Bluetooth connection/session lifecycle, app-private storage, replay/import/export, shared core, simulator UI, and installable APK. Verify build and emulator workflows; report physical-device checks separately.
4. Durable evidence/snapshots, bounded retention, strict contracts, scoring coverage and scale fixes, deterministic certificates and actual PDF compilation.
5. OEM hypothesis and diagnostic-test planning engine with explicit evidence and unknown hypotheses; UI presents algorithmic decisions and test feedback. Do not fabricate validated repair knowledge.
6. Clean checkout build, all meaningful suites, UI/runtime verification, APK and Windows distribution, published GitHub source/artifacts, final requirement-by-requirement audit.

No claim of customer-deliverable completion until these gates are evidenced. Simulation validates software behavior, not real vehicle diagnosis or predictive calibration.

## Verified preview checkpoint — 2026-09-18

Concurrent Antigravity changes appeared at commit `741d891`; they were preserved. Continued in an isolated `codex/native-mvp-delivery` worktree. User confirmed Antigravity is still working. No merge or deletion of its branches is performed.

Implemented a shared application orchestration service, native Swing assessment/simulator/history UI, native Java Android Activity and foreground Bluetooth capture service, strict JSON replay, configurable trajectory generation, sampling fixes, score-scale and coverage corrections, explicit unknowns, preliminary source-labelled reports and data-dependent certificate identifiers. Sustained anomalies quarantine their baseline from suspect samples. Report LaTeX escapes metacharacters and wraps evidence references.

Verification performed on the isolated worktree:

- `build_and_test.bat`: passes existing module/integration suites and `NativeWorkflowTest`.
- `NativeWorkflowTest`: deterministic generation/replay, fractional sampling, DTC recovery, invalid input, unknown identity/mileage/risk, sustained-fault detection, source labels, distinct identifiers for different evidence.
- `DesktopSmokeTest`: opens real Swing controls and generates an assessment through the backend. Passed again using the packaged runtime and application JARs.
- Packaged `IgnitionAI.exe`: launched and stayed running; closed after launch verification. Package includes a Java runtime and discoverable sensor catalog.
- Android `assembleDebug` and `assembleDebugAndroidTest`: pass. Runtime test remains NOT RUN: API 34 emulator exits/crashes before instrumentation can execute, including hardware/software acceleration attempts.
- Actual `pdflatex` invocation: generates the preliminary report successfully, with no overfull-box warnings after table wrapping changes. This verifies compilation, not all report layouts.
- Real Bluetooth/Nexon capture, Android lifecycle/export behavior, calibrated prediction, OEM diagnosis and full customer acceptance remain unverified or unfinished. Detailed open gates are in `NATIVE_PREVIEW.md`.

Local outputs are under `releases/` (Windows application image, Windows ZIP, Android debug APK). Logs and UI evidence are under `out/` and local build logs. These are generated outputs, not substitutes for acceptance evidence on physical hardware.
