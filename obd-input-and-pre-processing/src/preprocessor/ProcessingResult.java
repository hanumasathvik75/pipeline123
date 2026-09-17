package com.ignitionai.obdinput.preprocessor;

import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.schema.QualityEvent;
import java.util.List;

public class ProcessingResult {
    private final ObdMessage message;
    private final List<QualityEvent> events;

    public ProcessingResult(ObdMessage message, List<QualityEvent> events) {
        this.message = message;
        this.events = events;
    }

    public ObdMessage getMessage() { return message; }
    public List<QualityEvent> getEvents() { return events; }
}
