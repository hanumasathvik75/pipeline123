package ai.ignition.spring.controller;

import ai.ignition.spring.dto.DiagnosticReportDto;
import ai.ignition.spring.dto.LiveTelemetryRequest;
import ai.ignition.spring.dto.SessionAssessmentRequest;
import ai.ignition.spring.service.MlDiagnosticsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API Gateway Controller exposed to Rahul's Android App.
 * Routes assessment requests to the Python FastAPI ML Diagnostics Service
 * running the Real Hybrid XGBoost engine.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentController.class);
    private final MlDiagnosticsClient mlClient;

    public AssessmentController(MlDiagnosticsClient mlClient) {
        this.mlClient = mlClient;
    }

    /**
     * Gateway and downstream ML service health check.
     * GET /api/v1/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("gateway_status", "UP");
        response.put("service", "IgnitionAI Spring Boot Gateway");

        Map<String, Object> mlHealth = mlClient.checkHealth();
        response.put("ml_service_health", mlHealth);

        return ResponseEntity.ok(response);
    }

    /**
     * Primary endpoint invoked by Android App (MainActivity btnAssess).
     * POST /api/v1/assess/session
     */
    @PostMapping(value = "/assess/session", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DiagnosticReportDto> assessSession(@RequestBody SessionAssessmentRequest request) {
        logger.info("Received session assessment request: sessionId={}, hasRawJsonl={}, recordCount={}",
                request.getSessionId(),
                request.getRawJsonl() != null && !request.getRawJsonl().isEmpty(),
                request.getRecords() != null ? request.getRecords().size() : 0);

        DiagnosticReportDto report = mlClient.evaluateSession(request);
        return ResponseEntity.ok(report);
    }

    /**
     * Live single-frame assessment during drive capture.
     * POST /api/v1/assess/live
     */
    @PostMapping(value = "/assess/live", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> assessLive(@RequestBody LiveTelemetryRequest request) {
        Map<String, Object> result = mlClient.evaluateLiveTelemetry(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Multipart file upload endpoint accepting session.jsonl directly.
     * POST /api/v1/assess/upload
     */
    @PostMapping(value = "/assess/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DiagnosticReportDto> uploadAndAssessSession(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "session_id", required = false) String sessionIdParam) {

        String sessionId = (sessionIdParam != null && !sessionIdParam.isBlank())
                ? sessionIdParam
                : (file.getOriginalFilename() != null ? file.getOriginalFilename().replace(".jsonl", "") : "uploaded-session");

        logger.info("Received session file upload: name={}, size={} bytes, sessionId={}",
                file.getOriginalFilename(), file.getSize(), sessionId);

        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            SessionAssessmentRequest req = new SessionAssessmentRequest(sessionId, sb.toString());
            DiagnosticReportDto report = mlClient.evaluateSession(req);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Failed to read uploaded session file: {}", e.getMessage());
            DiagnosticReportDto errorDto = new DiagnosticReportDto();
            errorDto.setSessionId(sessionId);
            errorDto.setStatus("ERROR");
            errorDto.setMessage("Failed to process session file: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorDto);
        }
    }
}
