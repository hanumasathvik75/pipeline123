# Native application preview

This branch supplies a working desktop assessment/simulator flow and a buildable Android application using the same Java backend. It is a development preview, not a validated vehicle-health product.

## Windows

- From source: install JDK 17 or later, then double-click `run-desktop.bat`.
- Packaged build: extract the complete Windows ZIP, keep its `app` and `runtime` folders beside the executable, and launch `IgnitionAI.exe`. No separate Java installation is required.
- Open **Simulator**, edit the configuration and select **Generate and assess**. View Assessment, Telemetry, and Evidence tabs. Save a session to replay it or compare it through History.
- Scenario settings accept arbitrary piecewise signal trajectories, per-signal sampling rates, missing-data fraction, noise variance, bias, drift, stuck values, fault timing, DTCs and deterministic random seeds. Times are milliseconds. They generate test inputs; they do not simulate validated vehicle physics.
- Export certificate creates LaTeX and compiles a PDF when `pdflatex` is on PATH. Installing MiKTeX or TeX Live is required for desktop PDF compilation. A compilation failure is shown explicitly.
- Local sessions are under `%USERPROFILE%/IgnitionAI/sessions`. Session deletion is available in History. Files are plaintext local data; encryption and automatic retention are not yet integrated into this workflow.
- Sensor manifests are discovered from `app/sensors` in the package; adding a valid manifest requires no core-code edit. Restart/reassess to load it. From source, the catalog is `virtual-sensors/src/main/resources/sensors`.

## Android

- Build using JDK 17, Android SDK 36 and the checked-in wrapper: `gradlew.bat :android-app:assembleDebug`.
- Install `android-app/build/outputs/apk/debug/android-app-debug.apk`. It is a debug-signed preview, not a Play Store release.
- The native Activity exposes configurable simulation, assessment, session import/export, history, LaTeX export and Android-native PDF export. Android PDF export uses the structured assessment; it does not compile the LaTeX file.
- Pair a Classic Bluetooth SPP ELM327 adapter in system settings, enter a vehicle reference, and select **Connect paired Bluetooth dongle**. Capture uses a foreground service and app-private session/raw-response files. **Stop capture** closes the connection. Open the saved session through History.
- The transport does not support BLE adapters. Actual dongle compatibility, signal availability and capture recovery need physical-device testing.
- Android and Windows exchange newline-delimited `obd-input.v1` session files. Android stores sessions in private app storage; export before uninstalling.

## Reproducible checks

```text
build_and_test.bat
java -ea -cp out com.ignitionai.desktop.DesktopSmokeTest
gradlew.bat :android-app:assembleDebug :android-app:assembleDebugAndroidTest
powershell -ExecutionPolicy Bypass -File package-windows.ps1 -OutputName IgnitionAI
```

The Windows packaging script needs a full JDK with `jpackage` and `jlink`. Use a fresh output name when rebuilding; it deliberately does not delete existing releases.

Android UI instrumentation, once an emulator or device is available:

```text
adb install -r android-app/build/outputs/apk/debug/android-app-debug.apk
adb install -r android-app/build/outputs/apk/androidTest/debug/android-app-debug-androidTest.apk
adb shell am instrument -w ai.ignition.android.test/com.ignitionai.nativeapp.NativeUiTest
```

Require `PASS NativeUiTest` in output. An APK build alone is not a runtime test.

## Remaining release gates

- Android UI/device verification is uncompleted: the available emulator exits before instrumentation can run. Physical Bluetooth capture has not been tested against the Nexon.
- Expected-value baselines, uncertainty floors and indicator mappings are preliminary. Suspect observations are excluded from baseline updates, but operating-regime changes, missing intervals and vehicle-specific calibration still need evaluation. Direct features and virtual sensors are computed, while current anomaly tracking covers five raw indicators; their complete model wiring is unfinished.
- The app reports a low-confidence preliminary indicator score when the five required signals are observed, and marks the certificate `PARTIAL` with `PRELIMINARY_INDICATOR_ONLY`. It reports unknown health when a signal is absent. It must not substitute a VIN, mileage, or a calibrated failure probability. The five indicator scores are not validated component-health measurements.
- Previous phone-local sessions from the same vehicle and source are now replayed causally into degradation evidence, with a 365-day/16 MB analysis bound, duplicate and future-session rejection, corrupt-file warnings, and cross-session evidence fingerprints. Complete Phase 1 registry-to-model integration, confidence calibration and retention/encryption require further implementation.
- OEM causal diagnosis, test selection, algorithmic repair planning and repair-outcome learning are not implemented in this application workflow.
- Existing `desktop-app` and the old `ai.ignition.android` source contain another agent's prototype. They are preserved for integration review but are not the documented launchers. Do not distribute their hard-coded assessment screens.
- Installer signing, Android release signing, lifecycle/reconnection testing, user acceptance and the final requirements audit remain open.

Concurrent Antigravity work is preserved. Integrate this branch after reviewing its changes against the other agent's branch; do not overwrite the live working folder or delete branches while that work continues.
