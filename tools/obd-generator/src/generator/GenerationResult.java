package com.ignitionai.obdgenerator.generator;

import com.ignitionai.obdinput.schema.ObdMessage;
import java.util.List;

public class GenerationResult {
    private final List<ObdMessage> publicStream;
    private final List<GroundTruthRecord> groundTruth;

    public GenerationResult(List<ObdMessage> publicStream, List<GroundTruthRecord> groundTruth) {
        this.publicStream = publicStream;
        this.groundTruth = groundTruth;
    }

    public List<ObdMessage> getPublicStream() { return publicStream; }
    public List<GroundTruthRecord> getGroundTruth() { return groundTruth; }
}
