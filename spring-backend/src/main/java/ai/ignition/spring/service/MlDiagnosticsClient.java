package ai.ignition.spring.service;

import ai.ignition.spring.dto.DiagnosticReportDto;
import ai.ignition.spring.dto.LiveTelemetryRequest;
import ai.ignition.spring.dto.SessionAssessmentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service client communicating with Python FastAPI ML Diagnostics Service
 * which hosts the trained Real Hybrid XGBoost + Physics Invariants model.
 */
@Service
public class MlDiagnosticsClient {

    private static final Logger logger = LoggerFactory.getLogger(MlDiagnosticsClient.class);

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MlDiagnosticsClient(RestTemplate restTemplate,
                               @Value("${ml.service.url:http://127.0.0.1:8000}") String mlServiceUrl) {
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
    }

    public String getMlServiceUrl() {
        return mlServiceUrl;
    }

    /**
     * Checks health and readiness of the FastAPI ML microservice.
     */
    public Map<String, Object> checkHealth() {
        String url = mlServiceUrl + "/health";
        try {
            logger.info("Pinging ML service health at: {}", url);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("status", "UP");
        } catch (Exception e) {
            logger.warn("ML Service unreachable at {}: {}", url, e.getMessage());
            return Map.of("status", "DOWN", "error", e.getMessage(), "url", url);
        }
    }

    /**
     * Evaluates a full drive session (JSONL content or parsed records) using Real Hybrid Model.
     */
    public DiagnosticReportDto evaluateSession(SessionAssessmentRequest request) {
        String url = mlServiceUrl + "/api/diagnostics/evaluate-session";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SessionAssessmentRequest> entity = new HttpEntity<>(request, headers);

        try {
            logger.info("Forwarding session assessment request for session '{}' to FastAPI: {}",
                    request.getSessionId(), url);
            ResponseEntity<DiagnosticReportDto> response = restTemplate.postForEntity(url, entity, DiagnosticReportDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                DiagnosticReportDto dto = response.getBody();
                dto.setMessage("Diagnostic assessment completed successfully via Real Hybrid XGBoost Engine.");
                return dto;
            } else {
                throw new RuntimeException("Unexpected response from ML service: HTTP " + response.getStatusCode());
            }
        } catch (RestClientException rce) {
            logger.error("Failed to call ML Service at {}: {}", url, rce.getMessage());
            // Fallback diagnostic response gracefully
            DiagnosticReportDto fallback = new DiagnosticReportDto();
            fallback.setSessionId(request.getSessionId() != null ? request.getSessionId() : "session-error");
            fallback.setVhiScore(0);
            fallback.setHealthStatus("SERVICE_UNAVAILABLE");
            fallback.setDominantFault("Evaluation Failed: ML Service Unreachable");
            fallback.setStatus("ERROR");
            fallback.setMessage("FastAPI ML service on " + mlServiceUrl + " is unreachable: " + rce.getMessage());
            fallback.setWorkshopInspectionPlan(List.of("Ensure the Python FastAPI service is running: python ml/ml_service.py"));
            return fallback;
        }
    }

    /**
     * Evaluates a single live frame/telemetry packet.
     */
    public Map<String, Object> evaluateLiveTelemetry(LiveTelemetryRequest request) {
        String url = mlServiceUrl + "/api/diagnostics/enginefault-predict";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        if (request.getRpm() != null) body.put("RPM", request.getRpm());
        if (request.getSpeed() != null) body.put("Speed", request.getSpeed());
        if (request.getMap() != null) body.put("MAP", request.getMap());
        if (request.getTps() != null) body.put("TPS", request.getTps());
        if (request.getCoolant() != null) body.put("Coolant", request.getCoolant());
        if (request.getLoad() != null) body.put("Load", request.getLoad());
        if (request.getO2() != null) body.put("O2", request.getO2());
        if (request.getLambda() != null) body.put("Lambda", request.getLambda());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            logger.info("Evaluating live frame at FastAPI: {}", url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return response.getBody() != null ? response.getBody() : Collections.emptyMap();
        } catch (RestClientException rce) {
            logger.error("Live telemetry prediction failed: {}", rce.getMessage());
            return Map.of(
                    "status", "ERROR",
                    "error", rce.getMessage(),
                    "fault_name", "UNKNOWN",
                    "confidence", 0.0
            );
        }
    }
}
