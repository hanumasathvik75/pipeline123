package ai.ignition.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

import ai.ignition.android.diagnostics.HardwareTestReport;
import ai.ignition.android.diagnostics.HardwareTestRunner;
import ai.ignition.android.obd.LivePidDecoder;
import ai.ignition.android.recording.CaptureState;
import ai.ignition.android.recording.DriveCaptureEngine;
import ai.ignition.android.recording.DriveRecordingService;
import ai.ignition.android.recording.PayloadReader;
import ai.ignition.android.recording.SessionEvent;
import ai.ignition.android.recording.SessionExporter;
import ai.ignition.android.recording.SessionManifest;
import ai.ignition.android.recording.SessionRepository;
import ai.ignition.android.recording.SessionStatus;
import ai.ignition.android.recording.SessionValidator;
import ai.ignition.android.recording.ValidationResult;
import ai.ignition.android.util.RedactionUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entry-point Activity for the native Android client, managing edge-to-edge system insets,
 * Material You / Material 3 styling, runtime permissions, paired Classic dongle discovery,
 * BLE scanning, hardware verification, and local JSONL recording.
 */
@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 201;
    private static final int REQ_EXPORT_JSONL = 301;
    private static final int REQ_EXPORT_MANIFEST = 302;
    private static final int REQ_EXPORT_ZIP = 303;
    private static final long BLE_SCAN_PERIOD_MS = 10000;

    // Hardware Preflight Views
    private TextView tvHardwareBadge;
    private TextView tvSelectedDongle;
    private TextView tvStatus;
    private TextView tvPreflightReason;
    private MaterialButton btnConnect;
    private MaterialButton btnRefreshDongle;

    // Live Telemetry Views
    private TextView tvRpm;
    private TextView tvSpeed;
    private TextView tvCoolant;
    private TextView tvLoad;

    // VHI Assessment Views
    private TextView tvVhiScore;
    private MaterialButton btnAssess;

    // Drive Recording Views (Phase 2)
    private TextView tvRecordingBadge;
    private TextView tvRecordingStatus;
    private TextView tvActiveSessionInfo;
    private TextView tvRecordingMetrics;
    private MaterialButton btnDriveRecord;
    private MaterialButton btnViewPayload;
    private MaterialButton btnSessionHistory;
    private MaterialButton btnExportSession;

    // Console View
    private TextView tvConsole;

    // Controllers & State
    private BluetoothDevice activeDevice;
    private boolean isAdapterConnected = false;
    private SessionRepository sessionRepository;
    private HardwareTestRunner testRunner;
    private final StringBuilder consoleLog = new StringBuilder();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    // Export tracking
    private String pendingExportSessionId;

    // BLE scanning state
    private BluetoothLeScanner bleScanner;
    private boolean isScanning = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private final Map<String, BluetoothDevice> discoveredBleDevices = new LinkedHashMap<>();
    private ArrayAdapter<String> bleListAdapter;
    private AlertDialog bleScanDialog;

    // Live metrics tracking
    private String lastObservationSignal = "None";
    private String lastFinalizedSessionId = null;

    private final Runnable elapsedTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (DriveRecordingService.isRecordingActive()) {
                updateRecordingUi();
                scanHandler.postDelayed(this, 1000);
            }
        }
    };

    private final DriveCaptureEngine.CaptureListener recordingListener = new DriveCaptureEngine.CaptureListener() {
        @Override
        public void onStateChanged(CaptureState state) {
            runOnUiThread(() -> updateRecordingUi());
        }

        @Override
        public void onObservation(SessionEvent event) {
            runOnUiThread(() -> {
                if (event.getSignalName() != null) {
                    lastObservationSignal = event.getSignalName();
                }
                if (event.getValue() != null && event.getSignalName() != null) {
                    if (event.getSignalName().contains("RPM")) {
                        tvRpm.setText(String.format(Locale.ROOT, "%.0f rpm", event.getValue()));
                    } else if (event.getSignalName().contains("Speed")) {
                        tvSpeed.setText(String.format(Locale.ROOT, "%.0f km/h", event.getValue()));
                    } else if (event.getSignalName().contains("Coolant")) {
                        tvCoolant.setText(String.format(Locale.ROOT, "%.1f °C", event.getValue()));
                    } else if (event.getSignalName().contains("Load")) {
                        tvLoad.setText(String.format(Locale.ROOT, "%.1f %%", event.getValue()));
                    }
                }
            });
        }

        @Override
        public void onConnectionLost(String reason) {
            runOnUiThread(() -> {
                appendLog("⚠️ [Recorder] Connection lost: " + reason);
                updateRecordingUi();
            });
        }

        @Override
        public void onConnectionRestored(long gapDurationMs) {
            runOnUiThread(() -> {
                appendLog("✅ [Recorder] Connection restored! Data gap: " + gapDurationMs + " ms");
                updateRecordingUi();
            });
        }

        @Override
        public void onError(String error) {
            runOnUiThread(() -> {
                appendLog("❌ [Recorder] Error: " + error);
                updateRecordingUi();
            });
        }

        @Override
        public void onFinalized(SessionManifest manifest) {
            runOnUiThread(() -> {
                lastFinalizedSessionId = manifest.getSessionId();
                appendLog("🏁 [Recorder] Session finalized: " + manifest.getSessionId() +
                        " [status=" + manifest.getStatus() +
                        ", valid=" + manifest.getValidRecordCount() +
                        ", invalid=" + manifest.getInvalidRecordCount() +
                        ", size=" + manifest.getFileSizeBytes() + " B]");
                updateRecordingUi();
            });
        }
    };

    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result == null || result.getDevice() == null) return;
            BluetoothDevice device = result.getDevice();
            String address = device.getAddress();
            if (address != null && !discoveredBleDevices.containsKey(address)) {
                discoveredBleDevices.put(address, device);
                String name = device.getName() != null ? device.getName() : "Unnamed BLE Device";
                String entry = name + " [RSSI " + result.getRssi() + " dBm]\n" + RedactionUtil.redactMac(address);
                if (bleListAdapter != null) {
                    bleListAdapter.add(entry);
                    bleListAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            appendLog("BLE scan failed with error code: " + errorCode);
            stopBleScan();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure edge-to-edge system window insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // Apply insets padding to avoid status-bar / navigation-bar overlapping
        View rootLayout = findViewById(R.id.rootLayout);
        View appBarLayout = findViewById(R.id.appBarLayout);
        View scrollView = findViewById(R.id.scrollViewContent);

        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
                Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
                Insets navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());

                if (appBarLayout != null) {
                    appBarLayout.setPadding(0, statusBars.top, 0, 0);
                }
                if (scrollView != null) {
                    scrollView.setPadding(
                            scrollView.getPaddingLeft(),
                            scrollView.getPaddingTop(),
                            scrollView.getPaddingRight(),
                            navBars.bottom
                    );
                }
                return windowInsets;
            });
        }

        // Configure status and navigation bar icon contrast for Light vs Dark theme
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            boolean isNightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            insetsController.setAppearanceLightStatusBars(!isNightMode);
            insetsController.setAppearanceLightNavigationBars(!isNightMode);
        }

        // Hardware Preflight Bindings
        tvHardwareBadge = findViewById(R.id.tvHardwareBadge);
        tvSelectedDongle = findViewById(R.id.tvSelectedDongle);
        tvStatus = findViewById(R.id.tvStatus);
        tvPreflightReason = findViewById(R.id.tvPreflightReason);
        btnConnect = findViewById(R.id.btnConnect);
        btnRefreshDongle = findViewById(R.id.btnRefreshDongle);

        // Live Telemetry Bindings
        tvRpm = findViewById(R.id.tvRpm);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvCoolant = findViewById(R.id.tvCoolant);
        tvLoad = findViewById(R.id.tvLoad);

        // VHI Assessment Bindings
        tvVhiScore = findViewById(R.id.tvVhiScore);
        btnAssess = findViewById(R.id.btnAssess);

        // Drive Recording Bindings
        tvRecordingBadge = findViewById(R.id.tvRecordingBadge);
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus);
        tvActiveSessionInfo = findViewById(R.id.tvActiveSessionInfo);
        tvRecordingMetrics = findViewById(R.id.tvRecordingMetrics);
        btnDriveRecord = findViewById(R.id.btnDriveRecord);
        btnViewPayload = findViewById(R.id.btnViewPayload);
        btnSessionHistory = findViewById(R.id.btnSessionHistory);
        btnExportSession = findViewById(R.id.btnExportSession);

        // Console Binding
        tvConsole = findViewById(R.id.tvConsole);
        if (tvConsole != null) {
            tvConsole.setMovementMethod(new ScrollingMovementMethod());
        }

        testRunner = new HardwareTestRunner(this);
        sessionRepository = new SessionRepository(getFilesDir());

        // Startup recovery for interrupted sessions
        List<SessionManifest> recovered = sessionRepository.detectAndRecoverInterruptedSessions();
        if (!recovered.isEmpty()) {
            appendLog("Interrupted session recovery: Recovered " + recovered.size() + " incomplete session(s) from previous run.");
        }

        // Action Listeners
        btnConnect.setOnClickListener(v -> {
            if (isAdapterConnected) {
                handleDisconnectAction();
            } else {
                startConnectionFlow();
            }
        });

        btnRefreshDongle.setOnClickListener(v -> startConnectionFlow());

        btnAssess.setOnClickListener(v -> {
            Toast.makeText(this, "Backend assessment integration will follow after hardware milestone verification", Toast.LENGTH_SHORT).show();
        });

        btnDriveRecord.setOnClickListener(v -> {
            if (DriveRecordingService.isRecordingActive()) {
                Intent stopIntent = new Intent(this, DriveRecordingService.class);
                stopIntent.setAction(DriveRecordingService.ACTION_STOP_RECORDING);
                startService(stopIntent);
            } else {
                if (activeDevice == null) {
                    Toast.makeText(this, "Select and connect an ELM327 adapter first", Toast.LENGTH_SHORT).show();
                    startConnectionFlow();
                    return;
                }
                Intent startIntent = new Intent(this, DriveRecordingService.class);
                startIntent.setAction(DriveRecordingService.ACTION_START_RECORDING);
                startIntent.putExtra(DriveRecordingService.EXTRA_DEVICE, activeDevice);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(startIntent);
                } else {
                    startService(startIntent);
                }
            }
        });

        btnViewPayload.setOnClickListener(v -> {
            String targetSessionId = getTargetSessionId();
            if (targetSessionId != null) {
                showPayloadViewerDialog(targetSessionId);
            } else {
                Toast.makeText(this, "No active or recorded sessions found to view", Toast.LENGTH_SHORT).show();
            }
        });

        btnExportSession.setOnClickListener(v -> {
            String targetSessionId = getTargetSessionId();
            if (targetSessionId != null) {
                showExportOptionsDialog(targetSessionId);
            } else {
                Toast.makeText(this, "No session available to export", Toast.LENGTH_SHORT).show();
            }
        });

        btnSessionHistory.setOnClickListener(v -> showSessionHistoryDialog());
    }

    private void handleDisconnectAction() {
        if (DriveRecordingService.isRecordingActive()) {
            new AlertDialog.Builder(this)
                    .setTitle("Drive Recording In Progress")
                    .setMessage("Drive recording is currently active. Please stop drive recording before disconnecting the adapter.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        if (testRunner != null) {
            testRunner.shutdown();
        }
        activeDevice = null;
        isAdapterConnected = false;
        btnConnect.setText("Connect ELM327");
        btnConnect.setIconResource(R.drawable.ic_bluetooth);
        tvHardwareBadge.setText("DISCONNECTED");
        tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFECEFF1));
        tvHardwareBadge.setTextColor(0xFF607D8B);
        tvSelectedDongle.setText("No adapter selected");
        tvStatus.setText("Status: Disconnected");
        tvPreflightReason.setVisibility(View.GONE);
        appendLog("Disconnected from ELM327 adapter.");
    }

    private String getTargetSessionId() {
        if (DriveRecordingService.isRecordingActive()) {
            return DriveRecordingService.getActiveSessionId();
        }
        if (lastFinalizedSessionId != null) {
            return lastFinalizedSessionId;
        }
        List<SessionManifest> sessions = sessionRepository.listSessions();
        if (!sessions.isEmpty()) {
            return sessions.get(0).getSessionId();
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // Bluetooth Adapter Discovery & Connection Flow
    // ---------------------------------------------------------------------------------------------

    private void startConnectionFlow() {
        List<String> neededPermissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[0]), REQ_PERMISSIONS);
            return;
        }

        discoverAndSelectAdapter();
    }

    private void discoverAndSelectAdapter() {
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;

        if (adapter == null) {
            tvHardwareBadge.setText("UNAVAILABLE");
            tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFFEBEE));
            tvHardwareBadge.setTextColor(0xFFC62828);
            tvStatus.setText("Status: Bluetooth hardware not supported on this device/emulator");
            appendLog("❌ BluetoothAdapter is null on this device.");

            new AlertDialog.Builder(this)
                    .setTitle("Bluetooth Unavailable")
                    .setMessage("This device or emulator does not have a Bluetooth controller.\n\nTo test hardware communication, run IgnitionAI on a physical Android device or pair an ELM327 dongle with your host system.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        if (!adapter.isEnabled()) {
            tvHardwareBadge.setText("DISABLED");
            tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFFF3E0));
            tvHardwareBadge.setTextColor(0xFFE65100);
            tvStatus.setText("Status: Bluetooth is turned off");
            appendLog("⚠️ Bluetooth adapter is currently disabled.");

            new AlertDialog.Builder(this)
                    .setTitle("Bluetooth Disabled")
                    .setMessage("Bluetooth is turned off. Please turn on Bluetooth to discover and connect your ELM327 adapter.")
                    .setPositiveButton("Turn On", (d, which) -> {
                        startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                    })
                    .setNeutralButton("Settings", (d, which) -> {
                        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        Set<BluetoothDevice> bondedSet = null;
        try {
            bondedSet = adapter.getBondedDevices();
        } catch (SecurityException e) {
            appendLog("SecurityException querying bonded devices: " + e.getMessage());
        }

        List<BluetoothDevice> paired = (bondedSet != null) ? new ArrayList<>(bondedSet) : Collections.emptyList();

        if (paired.isEmpty()) {
            tvHardwareBadge.setText("NO DEVICES");
            tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFFF3E0));
            tvHardwareBadge.setTextColor(0xFFE65100);
            tvStatus.setText("Status: No paired Bluetooth devices found");
            appendLog("⚠️ No paired Bluetooth Classic devices found in Android settings.");

            new AlertDialog.Builder(this)
                    .setTitle("No Paired ELM327 Devices")
                    .setMessage("No paired Bluetooth devices were found.\n\n" +
                            "To pair your ELM327 adapter:\n" +
                            "1. Plug the adapter into your vehicle's OBD-II port.\n" +
                            "2. Ensure the adapter's power LED is lit.\n" +
                            "3. Open Android Bluetooth Settings and pair with 'OBDII', 'ELM327', or your adapter name (PIN: 1234 or 0000).\n" +
                            "4. Return to IgnitionAI and tap 'Refresh'.")
                    .setPositiveButton("Bluetooth Settings", (d, which) -> {
                        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                    })
                    .setNeutralButton("Scan Nearby BLE", (d, which) -> {
                        startBleScanFlow(adapter);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        // Sort so that devices with OBD/ELM in the name appear first
        paired.sort((a, b) -> {
            boolean aObd = isLikelyObdDongle(a.getName());
            boolean bObd = isLikelyObdDongle(b.getName());
            if (aObd && !bObd) return -1;
            if (!aObd && bObd) return 1;
            String aName = a.getName() != null ? a.getName() : "";
            String bName = b.getName() != null ? b.getName() : "";
            return aName.compareToIgnoreCase(bName);
        });

        String[] displayEntries = new String[paired.size()];
        for (int i = 0; i < paired.size(); i++) {
            BluetoothDevice d = paired.get(i);
            String name = d.getName() != null ? d.getName() : "Unknown Device";
            String tag = isLikelyObdDongle(name) ? " [Recommended OBD]" : "";
            displayEntries[i] = name + tag + "\n" + RedactionUtil.redactMac(d.getAddress());
        }

        new AlertDialog.Builder(this)
                .setTitle("Select ELM327 Bluetooth Adapter")
                .setItems(displayEntries, (dialog, which) -> {
                    BluetoothDevice selected = paired.get(which);
                    selectDeviceAndConnect(selected);
                })
                .setNeutralButton("Scan Nearby BLE", (d, which) -> {
                    startBleScanFlow(adapter);
                })
                .setPositiveButton("Bluetooth Settings", (d, which) -> {
                    startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean isLikelyObdDongle(String name) {
        if (name == null) return false;
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.contains("OBD") || upper.contains("ELM") || upper.contains("V-LINK") ||
                upper.contains("SCAN") || upper.contains("VGATE") || upper.contains("CAR");
    }

    private void selectDeviceAndConnect(BluetoothDevice device) {
        this.activeDevice = device;
        String name = device.getName() != null ? device.getName() : "Unknown Adapter";
        String redactedMac = RedactionUtil.redactMac(device.getAddress());

        tvSelectedDongle.setText("Selected: " + name + " (" + redactedMac + ")");
        tvHardwareBadge.setText("CONNECTING");
        tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFE3F2FD));
        tvHardwareBadge.setTextColor(0xFF1565C0);
        tvStatus.setText("Connecting to " + name + "…");
        btnConnect.setText("Connecting…");
        btnConnect.setEnabled(false);

        runHardwareVerificationPipeline(device);
    }

    private void startBleScanFlow(BluetoothAdapter adapter) {
        bleScanner = adapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            Toast.makeText(this, "Bluetooth LE Scanner is unavailable on this hardware", Toast.LENGTH_LONG).show();
            return;
        }

        discoveredBleDevices.clear();
        bleListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Scanning for Nearby BLE Dongles…")
                .setAdapter(bleListAdapter, (dialog, which) -> {
                    stopBleScan();
                    List<BluetoothDevice> devices = new ArrayList<>(discoveredBleDevices.values());
                    if (which >= 0 && which < devices.size()) {
                        BluetoothDevice selected = devices.get(which);
                        selectDeviceAndConnect(selected);
                    }
                })
                .setNegativeButton("Stop Scan", (dialog, which) -> stopBleScan());

        bleScanDialog = builder.create();
        bleScanDialog.show();

        isScanning = true;
        bleScanner.startScan(bleScanCallback);
        appendLog("Started BLE discovery scan (timeout " + (BLE_SCAN_PERIOD_MS / 1000) + "s)…");

        scanHandler.postDelayed(this::stopBleScan, BLE_SCAN_PERIOD_MS);
    }

    private void stopBleScan() {
        if (isScanning && bleScanner != null) {
            isScanning = false;
            try {
                bleScanner.stopScan(bleScanCallback);
            } catch (Exception ignored) {}
            appendLog("BLE discovery scan stopped.");
            if (bleScanDialog != null && bleScanDialog.isShowing()) {
                bleScanDialog.setTitle("BLE Scan Completed (Select Dongle)");
            }
        }
    }

    private void runHardwareVerificationPipeline(BluetoothDevice device) {
        this.activeDevice = device;
        btnConnect.setEnabled(false);
        tvHardwareBadge.setText("INITIALIZING");
        tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFE3F2FD));
        tvHardwareBadge.setTextColor(0xFF1565C0);
        tvStatus.setText("Status: Initializing verification pipeline…");
        tvPreflightReason.setVisibility(View.GONE);
        consoleLog.setLength(0);
        updateConsole();

        testRunner.runHardwareTest(device, new HardwareTestRunner.TestCallback() {
            @Override
            public void onProgress(String status) {
                runOnUiThread(() -> tvStatus.setText("Status: " + status));
            }

            @Override
            public void onLog(String line) {
                runOnUiThread(() -> {
                    appendLog(line);
                    if (line.contains("Engine RPM") && line.contains("rpm")) {
                        tvRpm.setText(extractSignalDisplay(line, ""));
                    } else if (line.contains("Vehicle Speed") && line.contains("km/h")) {
                        tvSpeed.setText(extractSignalDisplay(line, ""));
                    } else if (line.contains("Coolant Temperature") && line.contains("°C")) {
                        tvCoolant.setText(extractSignalDisplay(line, ""));
                    } else if (line.contains("Calculated Engine Load") && line.contains("%")) {
                        tvLoad.setText(extractSignalDisplay(line, ""));
                    }
                });
            }

            @Override
            public void onComplete(HardwareTestReport report, File savedReportFile) {
                runOnUiThread(() -> {
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Disconnect");
                    btnConnect.setIconResource(R.drawable.ic_stop);
                    isAdapterConnected = true;

                    if (report.outcome == HardwareTestReport.VerificationOutcome.PASS) {
                        tvHardwareBadge.setText("VERIFIED PASS");
                        tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFE6F4EA));
                        tvHardwareBadge.setTextColor(0xFF1E8E3E);
                        tvStatus.setText("Status: Preflight Passed — All checks verified");
                        tvPreflightReason.setVisibility(View.GONE);
                    } else if (report.outcome == HardwareTestReport.VerificationOutcome.PARTIAL) {
                        tvHardwareBadge.setText("PREFLIGHT PARTIAL");
                        tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFEF7E0));
                        tvHardwareBadge.setTextColor(0xFFE37400);
                        tvStatus.setText("Status: Preflight Verified (Partial Diagnostics)");
                        tvPreflightReason.setVisibility(View.VISIBLE);
                        tvPreflightReason.setText("Preflight Diagnostic Note: " + report.outcomeReason +
                                "\n(OBD adapter communication and live data polling remain fully functional for recording)");
                    } else {
                        tvHardwareBadge.setText("FAILED");
                        tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFCE8E6));
                        tvHardwareBadge.setTextColor(0xFFD93025);
                        tvStatus.setText("Status: Hardware Communication FAILED");
                        tvPreflightReason.setVisibility(View.VISIBLE);
                        tvPreflightReason.setText("Failure: " + report.outcomeReason);
                    }

                    for (LivePidDecoder.PidReading r : report.liveReadings) {
                        if (r.valid && r.value != null) {
                            if (r.pid.equals("010C")) tvRpm.setText(String.format(Locale.ROOT, "%.0f rpm", r.value));
                            if (r.pid.equals("010D")) tvSpeed.setText(String.format(Locale.ROOT, "%.0f km/h", r.value));
                            if (r.pid.equals("0105")) tvCoolant.setText(String.format(Locale.ROOT, "%.1f °C", r.value));
                            if (r.pid.equals("0104")) tvLoad.setText(String.format(Locale.ROOT, "%.1f %%", r.value));
                        }
                    }

                    Toast.makeText(MainActivity.this, "Verification report generated: " + savedReportFile.getName(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Connect ELM327");
                    btnConnect.setIconResource(R.drawable.ic_bluetooth);
                    isAdapterConnected = false;

                    tvHardwareBadge.setText("ERROR");
                    tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFCE8E6));
                    tvHardwareBadge.setTextColor(0xFFD93025);
                    tvStatus.setText("Status: Error — " + error);
                    tvPreflightReason.setVisibility(View.VISIBLE);
                    tvPreflightReason.setText("Communication Error: " + error);
                    appendLog("\nERROR: " + error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String extractSignalDisplay(String line, String prefix) {
        int idx = line.indexOf("->");
        if (idx != -1) return prefix + line.substring(idx + 2).trim();
        return prefix + line.trim();
    }

    private void appendLog(String line) {
        consoleLog.append(line).append("\n");
        updateConsole();
    }

    private void updateConsole() {
        if (tvConsole != null) {
            tvConsole.setText(consoleLog.toString());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                appendLog("✅ Bluetooth permissions granted.");
                discoverAndSelectAdapter();
            } else {
                tvHardwareBadge.setText("DENIED");
                tvHardwareBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFCE8E6));
                tvHardwareBadge.setTextColor(0xFFD93025);
                tvStatus.setText("Status: Bluetooth permissions denied");
                Toast.makeText(this, "Bluetooth permissions are required to connect to the ELM327 dongle", Toast.LENGTH_LONG).show();

                new AlertDialog.Builder(this)
                        .setTitle("Permissions Required")
                        .setMessage("IgnitionAI requires Bluetooth permissions to connect to your ELM327 adapter. Please grant permissions in App Settings.")
                        .setPositiveButton("App Settings", (d, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.fromParts("package", getPackageName(), null));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DriveRecordingService.addStateListener(recordingListener);
        updateRecordingUi();
        if (DriveRecordingService.isRecordingActive()) {
            scanHandler.post(elapsedTimerRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBleScan();
        scanHandler.removeCallbacks(elapsedTimerRunnable);
        DriveRecordingService.removeStateListener(recordingListener);
    }

    private void updateRecordingUi() {
        if (tvRecordingStatus == null || btnDriveRecord == null) return;
        boolean isRecording = DriveRecordingService.isRecordingActive();
        CaptureState state = DriveRecordingService.getActiveState();
        String sessionId = DriveRecordingService.getActiveSessionId();
        long recordCount = DriveRecordingService.getActiveRecordCount();

        if (isRecording || state == CaptureState.RECORDING || state == CaptureState.RECONNECTING || state == CaptureState.INITIALIZING) {
            btnDriveRecord.setText("Stop Drive Recording");
            btnDriveRecord.setIconResource(R.drawable.ic_stop);
            btnDriveRecord.setEnabled(true);

            long elapsedSec = 0;
            if (DriveRecordingService.getSessionStartTimeMs() > 0) {
                elapsedSec = (System.currentTimeMillis() - DriveRecordingService.getSessionStartTimeMs()) / 1000;
            }
            long minutes = elapsedSec / 60;
            long seconds = elapsedSec % 60;
            String elapsedStr = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);

            if (state == CaptureState.RECONNECTING) {
                tvRecordingBadge.setText("RECONNECTING");
                tvRecordingBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFEF7E0));
                tvRecordingBadge.setTextColor(0xFFE37400);
                tvRecordingStatus.setText("Status: Connection dropped; reconnecting adapter…");
            } else if (state == CaptureState.INITIALIZING) {
                tvRecordingBadge.setText("INITIALIZING");
                tvRecordingBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFE8F0FE));
                tvRecordingBadge.setTextColor(0xFF1A73E8);
                tvRecordingStatus.setText("Status: Initializing session writer & transport…");
            } else {
                tvRecordingBadge.setText("RECORDING");
                tvRecordingBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFCE8E6));
                tvRecordingBadge.setTextColor(0xFFD93025);
                tvRecordingStatus.setText("Status: Live Recording In Progress");
            }

            long validCount = DriveRecordingService.getActiveValidObservations();
            long unavailCount = DriveRecordingService.getActiveUnavailableObservations();
            long cycleDuration = DriveRecordingService.getLastCycleDurationMs();
            long lastRequestMs = DriveRecordingService.getLastRequestDurationMs();

            tvActiveSessionInfo.setText("Active Session: " + (sessionId != null ? sessionId : "none"));
            tvRecordingMetrics.setText(String.format(Locale.ROOT,
                    "Total Events: %,d | Valid: %,d | N/A: %,d | Cycle: %d ms | Latency: %d ms | Elapsed: %s",
                    recordCount, validCount, unavailCount, cycleDuration, lastRequestMs, elapsedStr));
        } else {
            btnDriveRecord.setText("Start Drive Recording");
            btnDriveRecord.setIconResource(R.drawable.ic_record);

            if (lastFinalizedSessionId != null) {
                SessionManifest manifest = sessionRepository.getSessionManifest(lastFinalizedSessionId);
                if (manifest != null) {
                    tvRecordingBadge.setText(manifest.getStatus().name());
                    if (manifest.getStatus() == SessionStatus.COMPLETE) {
                        tvRecordingBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFE6F4EA));
                        tvRecordingBadge.setTextColor(0xFF1E8E3E);
                        tvRecordingStatus.setText("Status: Finalized — COMPLETE (" + manifest.getValidRecordCount() + " records)");
                    } else if (manifest.getStatus() == SessionStatus.PARTIAL) {
                        tvRecordingBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFEF7E0));
                        tvRecordingBadge.setTextColor(0xFFE37400);
                        tvRecordingStatus.setText("Status: Finalized — PARTIAL (" + manifest.getValidationSummary() + ")");
                    } else if (manifest.getStatus() == SessionStatus.INTERRUPTED) {
                        tvRecordingBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFEF7E0));
                        tvRecordingBadge.setTextColor(0xFFE37400);
                        tvRecordingStatus.setText("Status: Interrupted — Preserved (" + manifest.getValidRecordCount() + " records)");
                    } else {
                        tvRecordingBadge.setText("INVALID");
                        tvRecordingBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFCE8E6));
                        tvRecordingBadge.setTextColor(0xFFD93025);
                        tvRecordingStatus.setText("Status: Finalized — INVALID (" + manifest.getValidationSummary() + ")");
                    }
                    tvActiveSessionInfo.setText("Last Session: " + lastFinalizedSessionId);
                    tvRecordingMetrics.setText(String.format(Locale.ROOT, "Total Records: %,d | File Size: %,d bytes",
                            manifest.getRecordCount(), manifest.getFileSizeBytes()));
                    return;
                }
            }

            tvRecordingBadge.setText("IDLE");
            tvRecordingBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFF1F3F4));
            tvRecordingBadge.setTextColor(0xFF5F6368);
            tvRecordingStatus.setText("Status: Idle (Not recording)");
            tvActiveSessionInfo.setText("Active Session: none");
            tvRecordingMetrics.setText("Records: 0 | Elapsed: 00:00 | Ready to record");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Payload Viewer Dialog
    // ---------------------------------------------------------------------------------------------

    private void showPayloadViewerDialog(String sessionId) {
        File jsonlFile = sessionRepository.getSessionJsonlFile(sessionId);
        if (jsonlFile == null || !jsonlFile.exists()) {
            Toast.makeText(this, "Session JSONL file not found for " + sessionId, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payload_viewer, null);
        TextView tvPayloadSessionId = dialogView.findViewById(R.id.tvPayloadSessionId);
        TextView tvPayloadSummary = dialogView.findViewById(R.id.tvPayloadSummary);
        TextView tvPayloadContent = dialogView.findViewById(R.id.tvPayloadContent);
        TextView tvPageIndicator = dialogView.findViewById(R.id.tvPageIndicator);
        MaterialButton btnViewFormatted = dialogView.findViewById(R.id.btnViewFormatted);
        MaterialButton btnViewRaw = dialogView.findViewById(R.id.btnViewRaw);
        MaterialButton btnPrevPage = dialogView.findViewById(R.id.btnPrevPage);
        MaterialButton btnNextPage = dialogView.findViewById(R.id.btnNextPage);
        MaterialButton btnRefreshPayload = dialogView.findViewById(R.id.btnRefreshPayload);
        MaterialButton btnExportFromPayload = dialogView.findViewById(R.id.btnExportFromPayload);
        MaterialButton btnClosePayload = dialogView.findViewById(R.id.btnClosePayload);

        tvPayloadSessionId.setText("Session: " + sessionId);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        final int[] currentPage = {0};
        final boolean[] isRaw = {false};

        Runnable loadPageAction = () -> {
            tvPayloadContent.setText("Reading session records from storage…");
            ioExecutor.execute(() -> {
                try {
                    PayloadReader.PayloadPage page = PayloadReader.readPage(jsonlFile, currentPage[0], PayloadReader.DEFAULT_PAGE_SIZE);
                    ValidationResult validation = SessionValidator.validateFile(jsonlFile, sessionId);

                    StringBuilder sb = new StringBuilder();
                    if (isRaw[0]) {
                        for (PayloadReader.PayloadRecord r : page.getRecords()) {
                            sb.append(r.getRawJson()).append("\n");
                        }
                    } else {
                        for (PayloadReader.PayloadRecord r : page.getRecords()) {
                            sb.append(r.getFormattedDisplay()).append("\n\n");
                        }
                    }
                    if (page.getRecords().isEmpty()) {
                        sb.append("(No records found in this page or file is empty)");
                    }

                    runOnUiThread(() -> {
                        tvPayloadSummary.setText(String.format(Locale.ROOT,
                                "Records: %,d valid, %,d invalid (%,d B) | Status: %s",
                                validation.getValidRecordCount(),
                                validation.getInvalidRecordCount(),
                                validation.getFileSizeBytes(),
                                validation.isValid() ? "PASSED" : "PARTIAL / INVALID"));

                        tvPageIndicator.setText(String.format(Locale.ROOT, "Page %d of %d (%,d records)",
                                page.getPageIndex() + 1, page.getTotalPages(), page.getTotalLines()));
                        btnPrevPage.setEnabled(page.hasPreviousPage());
                        btnNextPage.setEnabled(page.hasNextPage());
                        tvPayloadContent.setText(sb.toString());
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> tvPayloadContent.setText("Failed to read payload: " + e.getMessage()));
                }
            });
        };

        btnViewFormatted.setOnClickListener(v -> {
            if (isRaw[0]) {
                isRaw[0] = false;
                loadPageAction.run();
            }
        });

        btnViewRaw.setOnClickListener(v -> {
            if (!isRaw[0]) {
                isRaw[0] = true;
                loadPageAction.run();
            }
        });

        btnPrevPage.setOnClickListener(v -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPageAction.run();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            currentPage[0]++;
            loadPageAction.run();
        });

        btnRefreshPayload.setOnClickListener(v -> loadPageAction.run());

        btnExportFromPayload.setOnClickListener(v -> {
            showExportOptionsDialog(sessionId);
        });

        btnClosePayload.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        loadPageAction.run();
    }

    // ---------------------------------------------------------------------------------------------
    // Session History Dialog
    // ---------------------------------------------------------------------------------------------

    private void showSessionHistoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_session_history, null);
        LinearLayout container = dialogView.findViewById(R.id.layoutSessionListContainer);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmptyHistory);
        MaterialButton btnRefresh = dialogView.findViewById(R.id.btnRefreshHistory);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnCloseHistory);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Runnable populateHistory = () -> {
            container.removeAllViews();
            List<SessionManifest> sessions = sessionRepository.listSessions();
            if (sessions.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                container.addView(tvEmpty);
            } else {
                tvEmpty.setVisibility(View.GONE);
                for (SessionManifest m : sessions) {
                    View itemView = LayoutInflater.from(this).inflate(R.layout.item_session_history, container, false);
                    TextView tvSessionId = itemView.findViewById(R.id.tvHistorySessionId);
                    TextView tvStatusBadge = itemView.findViewById(R.id.tvHistoryStatusBadge);
                    TextView tvTimestamps = itemView.findViewById(R.id.tvHistoryTimestamps);
                    TextView tvMetrics = itemView.findViewById(R.id.tvHistoryMetrics);
                    TextView tvValidation = itemView.findViewById(R.id.tvHistoryValidation);
                    TextView tvInterrupted = itemView.findViewById(R.id.tvHistoryInterruptedTag);
                    MaterialButton btnView = itemView.findViewById(R.id.btnHistoryViewPayload);
                    MaterialButton btnExport = itemView.findViewById(R.id.btnHistoryExport);

                    tvSessionId.setText(m.getSessionId());
                    tvStatusBadge.setText(m.getStatus().name());

                    if (m.getStatus() == SessionStatus.COMPLETE) {
                        tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFE6F4EA));
                        tvStatusBadge.setTextColor(0xFF1E8E3E);
                    } else if (m.getStatus() == SessionStatus.PARTIAL) {
                        tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFEF7E0));
                        tvStatusBadge.setTextColor(0xFFE37400);
                    } else if (m.getStatus() == SessionStatus.INTERRUPTED) {
                        tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFEF7E0));
                        tvStatusBadge.setTextColor(0xFFE37400);
                        tvInterrupted.setVisibility(View.VISIBLE);
                    } else {
                        tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(0xFFFCE8E6));
                        tvStatusBadge.setTextColor(0xFFD93025);
                    }

                    tvTimestamps.setText(String.format(Locale.ROOT, "Start: %s | Duration: %,d ms",
                            m.getStartTime() != null ? m.getStartTime() : "unknown", m.getDurationMs()));

                    tvMetrics.setText(String.format(Locale.ROOT, "Records: %,d (Valid: %,d, Invalid: %,d) | Size: %,d B",
                            m.getRecordCount(), m.getValidRecordCount(), m.getInvalidRecordCount(), m.getFileSizeBytes()));

                    tvValidation.setText("Validation: " + m.getValidationSummary());

                    btnView.setOnClickListener(v -> showPayloadViewerDialog(m.getSessionId()));
                    btnExport.setOnClickListener(v -> showExportOptionsDialog(m.getSessionId()));

                    container.addView(itemView);
                }
            }
        };

        btnRefresh.setOnClickListener(v -> populateHistory.run());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        populateHistory.run();
    }

    // ---------------------------------------------------------------------------------------------
    // SAF Save & Export Workflow
    // ---------------------------------------------------------------------------------------------

    private void showExportOptionsDialog(String sessionId) {
        this.pendingExportSessionId = sessionId;

        String[] options = new String[]{
                "Export session.jsonl (Raw telemetry events)",
                "Export manifest.json (Session metadata & validation)",
                "Export Complete Session ZIP (Contains JSONL + Manifest)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Save / Export Drive Session")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        launchSafExport("session_" + sessionId + ".jsonl", "application/x-ndjson", REQ_EXPORT_JSONL);
                    } else if (which == 1) {
                        launchSafExport("manifest_" + sessionId + ".json", "application/json", REQ_EXPORT_MANIFEST);
                    } else {
                        launchSafExport("session_" + sessionId + ".zip", "application/zip", REQ_EXPORT_ZIP);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void launchSafExport(String suggestedFilename, String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, suggestedFilename);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            if (requestCode == REQ_EXPORT_JSONL || requestCode == REQ_EXPORT_MANIFEST || requestCode == REQ_EXPORT_ZIP) {
                appendLog("Export cancelled by user.");
            }
            return;
        }

        Uri destinationUri = data.getData();
        String sessionId = pendingExportSessionId;
        if (sessionId == null) return;

        ioExecutor.execute(() -> {
            try {
                long bytes;
                String typeName;
                if (requestCode == REQ_EXPORT_JSONL) {
                    bytes = SessionExporter.exportJsonlToUri(this, sessionRepository, sessionId, destinationUri);
                    typeName = "session.jsonl";
                } else if (requestCode == REQ_EXPORT_MANIFEST) {
                    bytes = SessionExporter.exportManifestToUri(this, sessionRepository, sessionId, destinationUri);
                    typeName = "manifest.json";
                } else if (requestCode == REQ_EXPORT_ZIP) {
                    bytes = SessionExporter.exportSessionZipToUri(this, sessionRepository, sessionId, destinationUri);
                    typeName = "session ZIP archive";
                } else {
                    return;
                }

                runOnUiThread(() -> {
                    String msg = String.format(Locale.ROOT, "Exported %s successfully (%,d bytes written)!", typeName, bytes);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    appendLog("Export: " + msg);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    String err = "Export failed: " + e.getMessage();
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    appendLog("Error: " + err);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        stopBleScan();
        scanHandler.removeCallbacks(elapsedTimerRunnable);
        if (testRunner != null) {
            testRunner.shutdown();
        }
        ioExecutor.shutdown();
        super.onDestroy();
    }
}
