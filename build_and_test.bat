@echo off
echo ===================================================
echo Building IgnitionAI 2.0 Phase 3 Modules...
echo ===================================================

if not exist "out" mkdir "out"

rem Compile all modules
powershell -Command "Get-ChildItem -Recurse -Filter *.java | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' } | Out-File -Encoding ascii sources.txt"
javac -encoding UTF-8 -d out @sources.txt
del sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo Running Tests...
echo ===================================================

echo [TEST] Technical Sources
java -ea -cp out com.ignitionai.technicalsources.test.TechnicalSourceTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Fact Extraction
java -ea -cp out com.ignitionai.factextraction.test.FactExtractionTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Consistency Checks
java -ea -cp out com.ignitionai.consistencychecks.test.ConsistencyCheckTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Linked Registries
java -ea -cp out com.ignitionai.linkedregistries.test.LinkedRegistriesTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Vehicle Context (Phase 3)
java -ea -cp out com.ignitionai.context.VehicleContextTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
java -ea -cp out com.ignitionai.context.ContextAcceptanceTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Direct Features (Phase 3)
java -ea -cp out com.ignitionai.features.DirectFeaturesTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
java -ea -cp out com.ignitionai.features.FeatureAcceptanceTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Virtual Sensors (Phase 3)
java -ea -cp out com.ignitionai.virtualsensors.VirtualSensorsTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
java -ea -cp out com.ignitionai.virtualsensors.SensorAcceptanceTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Phase 3 Pipeline Runner
java -ea -cp out tools.pipeline_runner.Phase3PipelineRunner
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Phase 3 to Phase 4 Contract Integration
java -ea -cp out tools.pipeline_runner.Phase3ToPhase4ContractTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Degradation State (Phase 5)
java -ea -cp out com.ignitionai.degradation.state.DegradationStateTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Trend Analyzer (Phase 5)
java -ea -cp out com.ignitionai.degradation.analyzer.StateSpaceModelTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
java -ea -cp out com.ignitionai.degradation.analyzer.FutureDataLeakageTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
java -ea -cp out com.ignitionai.degradation.analyzer.GroundTruthReconstructionTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
java -ea -cp out com.ignitionai.degradation.analyzer.TrendAnalyzerTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Event Risk (Phase 5)
java -ea -cp out com.ignitionai.degradation.risk.HazardModelTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
java -ea -cp out com.ignitionai.degradation.risk.EventRiskTest
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Phase 5 Pipeline Runner
java -ea -cp out tools.pipeline_runner.Phase5PipelineRunner
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Phase 5 Labelled History Generator
java -ea -cp out tools.pipeline_runner.LabelledHistoryGenerator
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
echo.
echo [TEST] Phase 7 Certificate and App
java -ea -cp out com.ignitionai.phase7.test.Phase7Tests
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo [TEST] Phase 7 Demo
java -ea -cp out tools.pipeline_runner.Phase7DemoRunner
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

echo.
echo ===================================================
echo ALL TESTS PASSED SUCCESSFULLY!
echo ===================================================
exit /b 0
