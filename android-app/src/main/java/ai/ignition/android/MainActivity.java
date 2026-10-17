package ai.ignition.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import ai.ignition.android.diagnostics.HardwareTestReport;
import ai.ignition.android.diagnostics.HardwareTestRunner;
import ai.ignition.android.obd.LivePidDecoder;
import ai.ignition.android.util.RedactionUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Entry-point Activity for the native Android client, managing permissions,
 * paired Classic dongle selection, BLE peripheral scanning, and running
 * the real ELM327 hardware verification workflow.
 */
@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 201;
    private static final long BLE_SCAN_PERIOD_MS = 10000;

    private TextView tvStatus;
    private TextView tvRpm;
    private TextView tvSpeed;
    private TextView tvCoolant;
    private TextView tvLoad;
    private TextView tvVhiScore;
    private TextView tvConsole;
    private Button btnConnect;
    private Button btnAssess;

    private HardwareTestRunner testRunner;
    private final StringBuilder consoleLog = new StringBuilder();

    // BLE scanning state
    private BluetoothLeScanner bleScanner;
    private boolean isScanning = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private final Map<String, BluetoothDevice> discoveredBleDevices = new LinkedHashMap<>();
    private ArrayAdapter<String> bleListAdapter;
    private AlertDialog bleScanDialog;

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
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvRpm = findViewById(R.id.tvRpm);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvCoolant = findViewById(R.id.tvCoolant);
        tvLoad = findViewById(R.id.tvLoad);
        tvVhiScore = findViewById(R.id.tvVhiScore);
        tvConsole = findViewById(R.id.tvConsole);
        btnConnect = findViewById(R.id.btnConnect);
        btnAssess = findViewById(R.id.btnAssess);

        if (tvConsole != null) {
            tvConsole.setMovementMethod(new ScrollingMovementMethod());
        }

        testRunner = new HardwareTestRunner(this);

        btnConnect.setOnClickListener(v -> checkPermissionsAndSelectDongle());
        btnAssess.setOnClickListener(v -> {
            Toast.makeText(this, "Backend assessment integration will follow after hardware milestone verification", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkPermissionsAndSelectDongle() {
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

        showTransportSelectionDialog();
    }

    private void showTransportSelectionDialog() {
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;

        if (adapter == null) {
            tvStatus.setText("Status: Bluetooth not supported on this device");
            tvStatus.setTextColor(0xFFD32F2F);
            Toast.makeText(this, "This device does not have a Bluetooth adapter", Toast.LENGTH_LONG).show();
            return;
        }

        if (!adapter.isEnabled()) {
            tvStatus.setText("Status: Bluetooth is disabled");
            tvStatus.setTextColor(0xFFD32F2F);
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return;
        }

        String[] options = new String[]{
                "Select Paired Dongle (Bluetooth Classic / Paired BLE)",
                "Scan for Nearby BLE Dongles (Low Energy)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Connect to ELM327 Adapter")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showPairedDonglePicker(adapter);
                    } else {
                        startBleScanFlow(adapter);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPairedDonglePicker(BluetoothAdapter adapter) {
        List<BluetoothDevice> paired = new ArrayList<>(adapter.getBondedDevices());
        if (paired.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No Paired Devices")
                    .setMessage("No paired Bluetooth dongles were found in Android Settings.\n\n" +
                            "If your adapter is Bluetooth Classic, pair it in Android Bluetooth Settings first.\n" +
                            "If your adapter is BLE (Bluetooth Low Energy), choose 'Scan for Nearby BLE Dongles'.")
                    .setPositiveButton("Scan BLE", (d, w) -> startBleScanFlow(adapter))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        String[] displayNames = new String[paired.size()];
        for (int i = 0; i < paired.size(); i++) {
            BluetoothDevice dev = paired.get(i);
            String name = dev.getName() != null ? dev.getName() : "Unknown Dongle";
            displayNames[i] = name + "\n" + RedactionUtil.redactMac(dev.getAddress());
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Paired Dongle")
                .setItems(displayNames, (dialog, which) -> {
                    BluetoothDevice selected = paired.get(which);
                    startHardwareVerification(selected);
                })
                .setNeutralButton("Scan BLE Instead", (d, w) -> startBleScanFlow(adapter))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startBleScanFlow(BluetoothAdapter adapter) {
        bleScanner = adapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            Toast.makeText(this, "BLE Scanner not available on this device", Toast.LENGTH_LONG).show();
            return;
        }

        discoveredBleDevices.clear();
        bleListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());

        bleScanDialog = new AlertDialog.Builder(this)
                .setTitle("Scanning BLE Dongles (10s)…")
                .setAdapter(bleListAdapter, (dialog, which) -> {
                    stopBleScan();
                    List<BluetoothDevice> devices = new ArrayList<>(discoveredBleDevices.values());
                    if (which >= 0 && which < devices.size()) {
                        BluetoothDevice selected = devices.get(which);
                        appendLog("Disclaimer: Discovered BLE device '" + selected.getName() + "' does not prove OBD capability until verified.");
                        startHardwareVerification(selected);
                    }
                })
                .setNegativeButton("Stop Scan", (dialog, which) -> stopBleScan())
                .setOnDismissListener(dialog -> stopBleScan())
                .create();

        bleScanDialog.show();

        isScanning = true;
        bleScanner.startScan(bleScanCallback);
        appendLog("Started BLE discovery scan (timeout in 10s)…");

        // Enforce 10-second automated timeout
        scanHandler.postDelayed(this::stopBleScan, BLE_SCAN_PERIOD_MS);
    }

    private void stopBleScan() {
        if (isScanning && bleScanner != null) {
            isScanning = false;
            try {
                bleScanner.stopScan(bleScanCallback);
            } catch (Exception ignored) {}
            scanHandler.removeCallbacksAndMessages(null);
            appendLog("BLE scan stopped. Discovered " + discoveredBleDevices.size() + " peripheral(s).");
            if (bleScanDialog != null && bleScanDialog.isShowing()) {
                bleScanDialog.setTitle("Discovered BLE Devices (" + discoveredBleDevices.size() + ")");
            }
        }
    }

    private void startHardwareVerification(BluetoothDevice device) {
        btnConnect.setEnabled(false);
        tvStatus.setText("Status: Initializing verification pipeline…");
        tvStatus.setTextColor(0xFF1976D2);
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
                        tvRpm.setText(extractSignalDisplay(line, "RPM: "));
                    } else if (line.contains("Vehicle Speed") && line.contains("km/h")) {
                        tvSpeed.setText(extractSignalDisplay(line, "Speed: "));
                    } else if (line.contains("Coolant Temperature") && line.contains("°C")) {
                        tvCoolant.setText(extractSignalDisplay(line, "Coolant: "));
                    } else if (line.contains("Calculated Engine Load") && line.contains("%")) {
                        tvLoad.setText(extractSignalDisplay(line, "Load: "));
                    }
                });
            }

            @Override
            public void onComplete(HardwareTestReport report, File savedReportFile) {
                runOnUiThread(() -> {
                    btnConnect.setEnabled(true);
                    if (report.outcome == HardwareTestReport.VerificationOutcome.PASS) {
                        tvStatus.setText("Status: ✅ Physical Verification PASSED");
                        tvStatus.setTextColor(0xFF388E3C);
                    } else if (report.outcome == HardwareTestReport.VerificationOutcome.PARTIAL) {
                        tvStatus.setText("Status: ⚠️ Verification PARTIAL — " + report.outcomeReason);
                        tvStatus.setTextColor(0xFFF57C00);
                    } else {
                        tvStatus.setText("Status: ❌ Verification FAILED — " + report.outcomeReason);
                        tvStatus.setTextColor(0xFFD32F2F);
                    }

                    for (LivePidDecoder.PidReading r : report.liveReadings) {
                        if (r.valid && r.value != null) {
                            if (r.pid.equals("010C")) tvRpm.setText(String.format(Locale.ROOT, "RPM: %.0f rpm", r.value));
                            if (r.pid.equals("010D")) tvSpeed.setText(String.format(Locale.ROOT, "Speed: %.0f km/h", r.value));
                            if (r.pid.equals("0105")) tvCoolant.setText(String.format(Locale.ROOT, "Coolant Temp: %.1f °C", r.value));
                            if (r.pid.equals("0104")) tvLoad.setText(String.format(Locale.ROOT, "Engine Load: %.1f %%", r.value));
                        }
                    }

                    Toast.makeText(MainActivity.this, "Verification report generated: " + savedReportFile.getName(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnConnect.setEnabled(true);
                    tvStatus.setText("Status: ❌ Error");
                    tvStatus.setTextColor(0xFFD32F2F);
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
                showTransportSelectionDialog();
            } else {
                tvStatus.setText("Status: Bluetooth permissions denied");
                tvStatus.setTextColor(0xFFD32F2F);
                Toast.makeText(this, "Bluetooth permissions are required to connect to the ELM327 dongle", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBleScan();
    }

    @Override
    protected void onDestroy() {
        stopBleScan();
        if (testRunner != null) {
            testRunner.shutdown();
        }
        super.onDestroy();
    }
}
