package com.ignitionai.obdgenerator.config;

import java.util.List;
import java.util.Map;

public class GeneratorConfig {
    private String vehicleId;
    private List<String> supportedSignals = new java.util.ArrayList<>();
    private Map<String, Double> samplingRatesHz = new java.util.HashMap<>();
    private long durationMs;
    private long randomSeed;
    private java.time.Instant startTime = java.time.Instant.parse("2026-09-17T10:00:00Z");
    public java.time.Instant getStartTime() { return startTime; }
    public void setStartTime(java.time.Instant value) { startTime = java.util.Objects.requireNonNull(value); }
    private ScenarioType scenarioType = ScenarioType.CITY_DRIVING;
    
    // Anomaly simulation
    private Map<String, Double> bias = new java.util.HashMap<>();
    private Map<String, Double> noiseVariance = new java.util.HashMap<>();
    private Map<String, Double> driftPerSecond = new java.util.HashMap<>();
    private Map<String, Double> stuckValues = new java.util.HashMap<>();
    private List<String> injectedDtcs = new java.util.ArrayList<>();
    private double missingnessProbability;
    private Map<String, SignalProgram> signalPrograms = new java.util.TreeMap<>();
    private long faultStartMs = 0;
    private long faultEndMs = Long.MAX_VALUE;

    public Map<String, SignalProgram> getSignalPrograms() { return signalPrograms; }
    public void setSignalPrograms(Map<String, SignalProgram> value) { signalPrograms = new java.util.TreeMap<>(value); }
    public long getFaultStartMs() { return faultStartMs; }
    public long getFaultEndMs() { return faultEndMs; }
    public void setFaultWindow(long start, long end) {
        if (start < 0 || end <= start) throw new IllegalArgumentException("Fault end must follow start");
        faultStartMs = start; faultEndMs = end;
    }
    
    // Getters and Setters
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    
    public List<String> getSupportedSignals() { return supportedSignals; }
    public void setSupportedSignals(List<String> signals) { this.supportedSignals = signals; }
    
    public Map<String, Double> getSamplingRatesHz() { return samplingRatesHz; }
    public void setSamplingRatesHz(Map<String, Double> rates) { this.samplingRatesHz = rates; }
    
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    
    public long getRandomSeed() { return randomSeed; }
    public void setRandomSeed(long randomSeed) { this.randomSeed = randomSeed; }
    
    public ScenarioType getScenarioType() { return scenarioType; }
    public void setScenarioType(ScenarioType type) { this.scenarioType = type; }
    
    public Map<String, Double> getBias() { return bias; }
    public void setBias(Map<String, Double> bias) { this.bias = bias; }
    
    public Map<String, Double> getNoiseVariance() { return noiseVariance; }
    public void setNoiseVariance(Map<String, Double> noiseVariance) { this.noiseVariance = noiseVariance; }
    
    public Map<String, Double> getDriftPerSecond() { return driftPerSecond; }
    public void setDriftPerSecond(Map<String, Double> drift) { this.driftPerSecond = drift; }
    
    public Map<String, Double> getStuckValues() { return stuckValues; }
    public void setStuckValues(Map<String, Double> stuck) { this.stuckValues = stuck; }
    
    public List<String> getInjectedDtcs() { return injectedDtcs; }
    public void setInjectedDtcs(List<String> dtcs) { this.injectedDtcs = dtcs; }
    
    public double getMissingnessProbability() { return missingnessProbability; }
    public void setMissingnessProbability(double prob) { this.missingnessProbability = prob; }
}
