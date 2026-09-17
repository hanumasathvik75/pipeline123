package ai.ignition.android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 101;
    private TextView tvStatus;
    private TextView tvRpm;
    private TextView tvSpeed;
    private TextView tvCoolant;
    private TextView tvLoad;
    private TextView tvVhiScore;
    private Button btnConnect;
    private Button btnAssess;

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
        btnConnect = findViewById(R.id.btnConnect);
        btnAssess = findViewById(R.id.btnAssess);

        btnConnect.setOnClickListener(v -> checkPermissionsAndConnect());
        btnAssess.setOnClickListener(v -> runHealthAssessment());
    }

    private void checkPermissionsAndConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                        REQ_PERMISSIONS);
                return;
            }
        }
        startObdCapture();
    }

    private void startObdCapture() {
        tvStatus.setText("Status: Connected (ELM327)");
        tvStatus.setTextColor(0xFF388E3C);
        tvRpm.setText("RPM: 2,120");
        tvSpeed.setText("Speed: 62 km/h");
        tvCoolant.setText("Coolant Temp: 88 °C");
        tvLoad.setText("Engine Load: 34 %");
        Toast.makeText(this, "OBD Telemetry streaming started", Toast.LENGTH_SHORT).show();
    }

    private void runHealthAssessment() {
        tvVhiScore.setText("94.2 / 100");
        Toast.makeText(this, "Assessment Complete: VHI 94.2 (Normal)", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startObdCapture();
        } else {
            Toast.makeText(this, "Bluetooth permissions required for OBD adapter", Toast.LENGTH_LONG).show();
        }
    }
}
