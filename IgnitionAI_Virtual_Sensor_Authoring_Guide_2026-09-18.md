# IgnitionAI virtual sensor authoring guide

For anyone (human or agent) adding a new sensor to IgnitionAI 2.0 in parallel with other work on the main pipeline. Follow this exactly and your sensor plugs in with **zero edits to any existing file** — `InspectionService.java`, `VirtualSensorRuntime.java`, `DiagnosticIndexRuntime.java`, `native-desktop`, none of it changes. You only ever add new files.

## 0. Pick the right contract

There are three ways to add a sensor. Pick based on what it needs.

| Contract | Use when | Output shape | Needs a `.properties` manifest? |
|---|---|---|---|
| **Formula manifest** | The value is one arithmetic expression between two already-available signals at the same instant | One derived signal value per timestamp | Yes — no Java code at all |
| **`VirtualSensorPlugin`** | You need real logic (a lookup table, a small state machine, rounding rules) but the output is still one instant value per timestamp for one derived signal | One derived signal value per timestamp | Yes, plus one Java class |
| **`DiagnosticIndexPlugin`** | The finding only makes sense over the whole drive — a rolling-window statistic, a cross-signal lag, a warm-up slope, anything with "fraction of the session," "confidence," or "state" in its definition | One session-level Health/Confidence/State/Evidence result | No — pure Java, self-describing |

The 8 sensors shipped so far (Combustion, Fuel, Thermal, Load Response, Charging, Air Intake, Timing Compliance, Driving Behavior — all in `virtual-sensors/src/main/java/com/ignitionai/virtualsensors/index/`) are all `DiagnosticIndexPlugin`. That is almost certainly what you want for anything from the "Model-derived" tier of the sensor catalogue doc ([IgnitionAI_Sensor_Catalogue_and_Certificate_Design_2026-09-18.md](IgnitionAI_Sensor_Catalogue_and_Certificate_Design_2026-09-18.md)). Use the formula manifest only for genuinely trivial per-instant math (e.g. `BoostGauge_kPa = MAP - Baro`).

## 1. Formula manifest (no Java)

File: `virtual-sensors/src/main/resources/sensors/<sensor_id>.properties` (or, when packaged, `app/sensors/<sensor_id>.properties` next to the installed app — see `sensorDirectory()` in `IgnitionDesktop.java`).

```properties
sensor_id=boost_gauge
sensor_version=1.0
display_name=Boost Gauge
observation_kind=MODEL_DERIVED
input_signal_ids=manifold_absolute_pressure,barometric_pressure
input_feature_ids=
output_signal_ids=boost_gauge_kpa
units=kPa
formula_or_model_reference=diff(manifold_absolute_pressure, barometric_pressure)
sampling_requirements=1Hz
uncertainty_definition=low
implementation_type=formula
status=active
```

The evaluator (`MathExpressionEvaluator.java`) supports `diff(a,b)` and exactly one of `+ - * /` between two named inputs. It does not support parentheses, more than one operator, or functions. If your formula needs more than that, use a `VirtualSensorPlugin` instead — do not try to abuse the string format.

Field notes:
- `input_signal_ids` — canonical signal IDs the sensor needs. Check [`SignalIds.java`](obd-input-contract/src/main/java/com/ignitionai/obd/SignalIds.java) for existing aliases before inventing a new ID; if your input might arrive under more than one name, add the alias there (that file is small and stable — this is the one shared-file edit that's expected, not a violation of "zero edits").
- `app_*` / `pre_*` prefixed keys become vehicle-applicability / operating-precondition checks (e.g. `app_engine_family=EA888`, `pre_engine_running=true`). See `VirtualSensorRuntime.evaluate()` for exactly what's checked.
- `status=active` vs `status=experimental` is documentation only right now — both run. Use `experimental` honestly for anything you haven't validated against real data.

## 2. `VirtualSensorPlugin` (Java, per-instant)

Two files:

**Manifest** — same shape as above, but `implementation_type=plugin`.

**Plugin class** — anywhere under `virtual-sensors/src/main/java/com/ignitionai/virtualsensors/plugins/<your_sensor_id>/`:

```java
package com.ignitionai.virtualsensors.plugins.example;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.virtualsensors.VirtualSensorPlugin;
import java.util.List;
import java.util.Map;

public final class ExamplePlugin implements VirtualSensorPlugin {
    @Override public String getSensorId() { return "example_sensor"; } // must match the manifest's sensor_id

    @Override
    public AnalyticalObservation execute(Map<String, AnalyticalObservation> inputs, VehicleContext context) {
        AnalyticalObservation a = inputs.get("some_signal_id"); // already validated present+AVAILABLE by the runtime
        double result = a.getValue() * 2.0; // your real logic here
        return new AnalyticalObservation("example_sensor", result, "unit", a.getTimestampMs(),
            QualityState.AVAILABLE, List.of(), a.getContextVersion(), "example.v1", "uncertainty unavailable", "plugin evaluation");
    }
}
```

**Registration** — add (or create) `virtual-sensors/src/main/resources/META-INF/services/com.ignitionai.virtualsensors.VirtualSensorPlugin`, one fully-qualified class name per line:

```
com.ignitionai.virtualsensors.plugins.example.ExamplePlugin
```

If the file already exists (other plugins registered), append your line — don't overwrite it.

## 3. `DiagnosticIndexPlugin` (Java, session-level — the flagship contract)

One file, no manifest. Interface (already in the repo, do not copy it — implement it):

```java
package com.ignitionai.virtualsensors.index;

public interface DiagnosticIndexPlugin {
    String getIndexId();                 // stable lowercase id, e.g. "boost"
    String getDisplayName();              // shown on the certificate, e.g. "Boost"
    String getPluginVersion();            // e.g. "boost-residual.v1" — bump when you change the method
    List<String> getRequiredSignalIds();  // canonical ids you need present to run at all
    DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context);
}
```

`DiagnosticIndexResult` is immutable and built via static factories — never construct it by hand:
- `DiagnosticIndexResult.of(indexId, displayName, healthPct, confidencePct, state, contributingPids, evidenceMap, pluginVersion)` — a real result. `state` is `NORMAL` or `FLAGGED`.
- `DiagnosticIndexResult.insufficientData(indexId, displayName, contributingPids, reason, pluginVersion)` — not enough samples/events. **Use this instead of guessing.**
- `DiagnosticIndexResult.unsupported(indexId, displayName, contributingPids, reason, pluginVersion)` — required signal(s) genuinely absent from this session.

Registration: add your class's fully-qualified name to `virtual-sensors/src/main/resources/META-INF/services/com.ignitionai.virtualsensors.index.DiagnosticIndexPlugin` (append, don't overwrite).

### 3.1 Worked example — the simplest shipped plugin, in full

`ChargingIndexPlugin.java` (real file, copy this pattern):

```java
package com.ignitionai.virtualsensors.index;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obdinput.schema.ObdMessage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class ChargingIndexPlugin implements DiagnosticIndexPlugin {
    private static final double MIN_NORMAL_V = 12.4;
    private static final double MAX_NORMAL_V = 14.9;
    private static final double RIPPLE_NORMAL_V = 0.30;
    private static final double RIPPLE_FLAG_V = 0.60;

    @Override public String getIndexId() { return "charging"; }
    @Override public String getDisplayName() { return "Charging"; }
    @Override public String getPluginVersion() { return "voltage-band-and-ripple.v1"; }
    @Override public List<String> getRequiredSignalIds() { return List.of("control_module_voltage"); }

    @Override
    public DiagnosticIndexResult evaluate(List<ObdMessage> session, VehicleContext context) {
        List<SeriesUtil.Sample> voltage = SeriesUtil.series(session, "control_module_voltage");
        if (voltage.size() < 10) {
            return DiagnosticIndexResult.insufficientData(getIndexId(), getDisplayName(), getRequiredSignalIds(),
                "Fewer than 10 valid control_module_voltage samples", getPluginVersion());
        }
        double meanV = voltage.stream().mapToDouble(s -> s.v).average().orElse(0);
        // ... rolling-ripple computation, health scoring, evidence map ...
        // See the real file for the complete, runnable version.
        return null; // placeholder — read the actual class, don't paste this stub
    }
}
```

Read the real files for the complete versions — this excerpt is trimmed for the doc. All 5 are in `virtual-sensors/src/main/java/com/ignitionai/virtualsensors/index/`: `CombustionIndexPlugin`, `FuelIndexPlugin`, `ThermalIndexPlugin`, `LoadResponseIndexPlugin`, `ChargingIndexPlugin`. `SeriesUtil.java` in the same package has the shared helpers you'll want (`series()`, `mean()`, `stddev()`, `median()`, `clamp01()`) — use them, don't reimplement.

### 3.2 Rules that are not optional

1. **Never fabricate a value.** If required signals are missing or there aren't enough samples, return `insufficientData`/`unsupported`. A raw-OBD scan tool that can't measure something says so; this system must too.
2. **Every threshold lives in `config/diagnostic-index-config.json`, never as a Java literal**, with a one-line `rationale` string in the same JSON section. "Why 1.5 seconds for max lag" should be answerable by reading the config file, not by asking the author or hunting through code. Load it via `DiagnosticIndexConfig.get().number("your_index_id", "your_key")` — see any shipped plugin (e.g. `LoadResponseIndexPlugin`) for the pattern. This became mandatory after the founder explicitly asked for all hardcoded/placeholder values to be removed from the codebase; a plugin with an inline `private static final double` threshold will be sent back.
3. **Health/confidence are 0–100, never null unless the state is INSUFFICIENT_DATA/UNSUPPORTED.**
4. **Don't throw.** `DiagnosticIndexRuntime` catches exceptions defensively, but that's a safety net, not a design target — guard your own edge cases (empty series, zero variance, division by zero).
5. **`likelihoodPct` in a ranked fault candidate (built automatically from a `FLAGGED` result by `FaultCandidateRanker`) is a heuristic ranking, never a calibrated probability.** Don't write evidence text that implies otherwise (no "80% chance of failure" language anywhere).
6. **If you think you need to edit `InspectionService.java`, `VirtualSensorRuntime.java`, or `DiagnosticIndexRuntime.java` — stop.** That almost always means the sensor doesn't fit one of the three contracts above, not that the contract needs changing. Flag it instead of working around it.

## 4. Before you hand it back

1. `powershell -ExecutionPolicy Bypass -File compile.ps1` — must exit 0.
2. `build_and_test.bat` — must still print `ALL TESTS PASSED SUCCESSFULLY!`.
3. Prove your plugin actually fires: write a short throwaway `main()` (delete before submitting, or put it behind a `-ea` assertion in a test file) that builds a session with your required signals present (use `tools/obd-generator` — see `fixtures/demo-diagnostic-index-showcase.properties` for the config format) and prints `new DiagnosticIndexRuntime().evaluateAll(session, context)`, confirming your index appears with a real (non-null) health value on a signal set that should trigger it, and `insufficientData`/`unsupported` on one that shouldn't.
4. State, in one sentence, what a raw single-PID OBD read could *not* tell you that your index does. If you can't answer that, it's a Wave-2 Direct/Derived feature, not a diagnostic index — put it in `direct-features` instead.

## 5. Ready-to-paste brief for a parallel research agent

Everything below this line is meant to be copied as-is into a new agent session.

---

You are adding new virtual sensors to IgnitionAI 2.0, a Java vehicle-diagnostics pipeline. Read `IgnitionAI_Virtual_Sensor_Authoring_Guide_2026-09-18.md` in the repo root in full before writing any code — it defines the exact contract (`DiagnosticIndexPlugin` for session-level composite findings, `VirtualSensorPlugin` for per-instant derived signals, or a formula `.properties` manifest for trivial arithmetic) and the non-negotiable rules (never fabricate a value; every threshold needs a stated rationale; never edit `InspectionService.java`, `VirtualSensorRuntime.java`, or `DiagnosticIndexRuntime.java`).

Also read `IgnitionAI_Sensor_Catalogue_and_Certificate_Design_2026-09-18.md`, section 2.3 ("Model-derived / composite") — that's your backlog. Pick 2–4 candidates not already shipped (the shipped 5 are Combustion, Fuel, Thermal, Load Response, Charging — do not duplicate them). Strong candidates: `SignalQualityScore` (missingness + implausible-value fraction + achieved-vs-requested sampling rate), a MAP/boost residual index (needs `manifold_absolute_pressure` and `barometric_pressure` — check `SignalIds.java` and add them if not present), an O2 sensor response-time/deviation index, or `OverallConditionScore` (a coverage-weighted blend of whatever diagnostic indices exist, same shape as `VhiCalculator` but at index resolution).

For each candidate, before writing Java:
1. State the physical/statistical basis in 2–3 sentences (what does this actually measure, and why does relating these specific signals over time — not a single instant — reveal something a raw scan can't).
2. State every threshold you'll use and why (a plausible engineering rationale is fine; you don't need a lab-validated number, but you must not use unexplained magic constants).
3. State what "insufficient data" means for this specific index (minimum samples/events) and what "unsupported" means (which signals must be present).

Then implement it as a `DiagnosticIndexPlugin` following the `ChargingIndexPlugin`/`CombustionIndexPlugin`/etc. pattern exactly, register it in `META-INF/services/com.ignitionai.virtualsensors.index.DiagnosticIndexPlugin`, and verify per §4 of the authoring guide above. Report back: what you built, the research basis for each threshold, and the actual output of your verification run (not a predicted/expected output — the real printed result from running it).

---
