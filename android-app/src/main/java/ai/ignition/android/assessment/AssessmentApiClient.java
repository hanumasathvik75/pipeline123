package ai.ignition.android.assessment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Robust HTTP client that communicates with the Spring Boot Gateway service
 * (which routes requests to the FastAPI Real Hybrid ML service).
 * Uses standard HttpURLConnection without external dependencies.
 */
public class AssessmentApiClient {

    private static final String TAG = "AssessmentApiClient";
    private static final String PREFS_NAME = "ignition_backend_prefs";
    private static final String KEY_BACKEND_URL = "backend_base_url";

    // 10.0.2.2 is Android Emulator's loopback to host 127.0.0.1
    public static final String DEFAULT_EMULATOR_URL = "http://10.0.2.2:8080";
    public static final String DEFAULT_LOCALHOST_URL = "http://127.0.0.1:8080";

    public interface AssessmentCallback {
        void onSuccess(AssessmentResult result);
        void onError(String errorMessage);
    }

    public interface HealthCallback {
        void onResult(boolean isOnline, String details);
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AssessmentApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getBackendBaseUrl() {
        return prefs.getString(KEY_BACKEND_URL, DEFAULT_EMULATOR_URL);
    }

    public void setBackendBaseUrl(String url) {
        if (url != null && !url.trim().isEmpty()) {
            String clean = url.trim();
            if (clean.endsWith("/")) {
                clean = clean.substring(0, clean.length() - 1);
            }
            prefs.edit().putString(KEY_BACKEND_URL, clean).apply();
        }
    }

    /**
     * Checks health and reachability of the Spring Boot gateway service.
     */
    public void checkHealth(HealthCallback callback) {
        networkExecutor.execute(() -> {
            String baseUrl = getBackendBaseUrl();
            String endpoint = baseUrl + "/api/v1/health";
            try {
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    String body = readStream(conn.getInputStream());
                    mainHandler.post(() -> callback.onResult(true, "Gateway Online: " + body));
                } else {
                    mainHandler.post(() -> callback.onResult(false, "HTTP " + code + " from " + endpoint));
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> callback.onResult(false, "Unreachable: " + e.getMessage()));
            }
        });
    }

    /**
     * Submits drive session JSONL content to Spring Boot Gateway for Real Hybrid ML evaluation.
     */
    public void assessSession(String sessionId, String rawJsonl, AssessmentCallback callback) {
        networkExecutor.execute(() -> {
            String baseUrl = getBackendBaseUrl();
            String endpoint = baseUrl + "/api/v1/assess/session";
            HttpURLConnection conn = null;

            try {
                JSONObject payload = new JSONObject();
                payload.put("session_id", sessionId != null ? sessionId : "session-live");
                payload.put("raw_jsonl", rawJsonl);

                byte[] postBytes = payload.toString().getBytes(StandardCharsets.UTF_8);

                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(45000); // Allow sufficient window for ML processing

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postBytes);
                    os.flush();
                }

                int statusCode = conn.getResponseCode();
                InputStream is = (statusCode >= 200 && statusCode < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                String responseBody = readStream(is);

                if (statusCode >= 200 && statusCode < 300) {
                    AssessmentResult result = parseAssessmentResponse(responseBody, sessionId);
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    String err = "Spring Boot Gateway returned HTTP " + statusCode + ": " + responseBody;
                    Log.e(TAG, err);
                    mainHandler.post(() -> callback.onError(err));
                }

            } catch (Exception e) {
                Log.e(TAG, "Assessment request failed: " + e.getMessage(), e);
                String friendlyMsg = "Unable to connect to Spring Boot Gateway at " + baseUrl +
                        ".\nEnsure the backend server is running (run-spring-backend.bat) and the phone/emulator can reach this host.\n\nError: " + e.getMessage();
                mainHandler.post(() -> callback.onError(friendlyMsg));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    /**
     * Submits live PID readings for snapshot diagnostic prediction.
     */
    public void assessLiveTelemetry(Double rpm, Double speed, Double coolant, Double load, AssessmentCallback callback) {
        networkExecutor.execute(() -> {
            String baseUrl = getBackendBaseUrl();
            String endpoint = baseUrl + "/api/v1/assess/live";
            HttpURLConnection conn = null;

            try {
                JSONObject payload = new JSONObject();
                if (rpm != null) payload.put("RPM", rpm);
                if (speed != null) payload.put("Speed", speed);
                if (coolant != null) payload.put("Coolant", coolant);
                if (load != null) payload.put("Load", load);

                byte[] postBytes = payload.toString().getBytes(StandardCharsets.UTF_8);

                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postBytes);
                    os.flush();
                }

                int statusCode = conn.getResponseCode();
                String responseBody = readStream(statusCode == 200 ? conn.getInputStream() : conn.getErrorStream());

                if (statusCode == 200) {
                    JSONObject json = new JSONObject(responseBody);
                    String fault = json.optString("fault_name", "Normal");
                    double conf = json.optDouble("confidence", 0.95);
                    String action = json.optString("recommended_action", "Nominal parameters.");
                    int vhi = "No Fault (Normal)".equals(fault) ? 100 : 75;

                    AssessmentResult res = new AssessmentResult(
                            "live-snapshot",
                            vhi,
                            vhi >= 85 ? "HEALTHY" : "ATTENTION_REQUIRED",
                            fault,
                            conf,
                            1,
                            "No Fault (Normal)".equals(fault) ? 1 : 0,
                            "No Fault (Normal)".equals(fault) ? 0 : 1,
                            Collections.emptyMap(),
                            Collections.emptyMap(),
                            List.of(action),
                            "SUCCESS",
                            "Live snapshot assessment completed."
                    );
                    mainHandler.post(() -> callback.onSuccess(res));
                } else {
                    mainHandler.post(() -> callback.onError("Live assessment failed: HTTP " + statusCode));
                }

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Live assessment failed: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private AssessmentResult parseAssessmentResponse(String jsonStr, String fallbackSessionId) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            String sessionId = obj.optString("session_id", fallbackSessionId);
            int vhi = obj.optInt("vhi_score", 100);
            String health = obj.optString("health_status", "HEALTHY");
            String fault = obj.optString("dominant_fault", "No Fault (Normal)");
            double conf = obj.optDouble("confidence", 1.0);
            int frames = obj.optInt("frames_evaluated", 0);
            int clean = obj.optInt("clean_frames", 0);
            int anomalies = obj.optInt("anomaly_frames", 0);
            String status = obj.optString("status", "SUCCESS");
            String message = obj.optString("message", "");

            List<String> plan = new ArrayList<>();
            JSONArray planArr = obj.optJSONArray("workshop_inspection_plan");
            if (planArr != null) {
                for (int i = 0; i < planArr.length(); i++) {
                    plan.add(planArr.getString(i));
                }
            }

            return new AssessmentResult(
                    sessionId, vhi, health, fault, conf, frames, clean, anomalies,
                    Collections.emptyMap(), Collections.emptyMap(), plan, status, message
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse assessment response: " + e.getMessage(), e);
            return new AssessmentResult(
                    fallbackSessionId, 0, "PARSE_ERROR", "Parse Error: " + e.getMessage(),
                    0.0, 0, 0, 0, Collections.emptyMap(), Collections.emptyMap(),
                    List.of(), "ERROR", e.getMessage()
            );
        }
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
