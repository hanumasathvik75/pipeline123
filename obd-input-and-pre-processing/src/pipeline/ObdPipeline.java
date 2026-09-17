package com.ignitionai.obdinput.pipeline;

import com.ignitionai.obdinput.adapter.ObdAdapter;
import com.ignitionai.obdinput.adapter.AdapterResult;
import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.preprocessor.ObdPreProcessor;
import com.ignitionai.obdinput.preprocessor.ProcessingResult;
import com.ignitionai.obdinput.preprocessor.SessionInspector;
import com.ignitionai.obdinput.preprocessor.SessionInspectionReport;
import com.ignitionai.obdinput.storage.StorageProvider;

import java.util.Optional;

public class ObdPipeline {
    
    private final ObdAdapter adapter;
    private final StorageProvider storage;
    private final ObdPreProcessor preProcessor;
    private final SessionInspector inspector;
    
    public ObdPipeline(ObdAdapter adapter, StorageProvider storage, ObdPreProcessor preProcessor, SessionInspector inspector) {
        this.adapter = adapter;
        this.storage = storage;
        this.preProcessor = preProcessor;
        this.inspector = inspector;
    }

    public void processSession(String sessionId) {
        adapter.startSession(sessionId);
        
        Optional<AdapterResult> nextRes = adapter.pollNextMessage();
        while (nextRes.isPresent()) {
            AdapterResult res = nextRes.get();
            ObdMessage rawMsg = res.getNormalizedMessage();
            
            storage.persistRawRecord(sessionId, res.getRawPayload());
            
            ProcessingResult result = preProcessor.process(rawMsg);
            ObdMessage normalizedMsg = result.getMessage();
            
            storage.persistNormalizedMessage(normalizedMsg);
            inspector.observe(normalizedMsg);
            
            nextRes = adapter.pollNextMessage();
        }
        
        adapter.endSession();
        SessionInspectionReport report = inspector.generateReport(sessionId);
        System.out.println("Session " + sessionId + " finished with status: " + report.getInspectionAcceptanceStatus());
    }
}
