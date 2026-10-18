package ai.ignition.spring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Incoming request for live single-frame diagnostic evaluation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveTelemetryRequest {

    @JsonProperty("RPM")
    private Double rpm;

    @JsonProperty("Speed")
    private Double speed;

    @JsonProperty("MAP")
    private Double map;

    @JsonProperty("TPS")
    private Double tps;

    @JsonProperty("Coolant")
    private Double coolant;

    @JsonProperty("Load")
    private Double load;

    @JsonProperty("O2")
    private Double o2;

    @JsonProperty("Lambda")
    private Double lambda;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("extraSignals")
    private Map<String, Double> extraSignals;

    public LiveTelemetryRequest() {}

    public Double getRpm() { return rpm; }
    public void setRpm(Double rpm) { this.rpm = rpm; }

    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }

    public Double getMap() { return map; }
    public void setMap(Double map) { this.map = map; }

    public Double getTps() { return tps; }
    public void setTps(Double tps) { this.tps = tps; }

    public Double getCoolant() { return coolant; }
    public void setCoolant(Double coolant) { this.coolant = coolant; }

    public Double getLoad() { return load; }
    public void setLoad(Double load) { this.load = load; }

    public Double getO2() { return o2; }
    public void setO2(Double o2) { this.o2 = o2; }

    public Double getLambda() { return lambda; }
    public void setLambda(Double lambda) { this.lambda = lambda; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Map<String, Double> getExtraSignals() { return extraSignals; }
    public void setExtraSignals(Map<String, Double> extraSignals) { this.extraSignals = extraSignals; }
}
