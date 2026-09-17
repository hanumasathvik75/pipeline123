package com.ignitionai.obdinput.adapter;

import com.ignitionai.obdinput.schema.ObdMessage;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class TestAdapter implements ObdAdapter {
    
    private final Queue<ObdMessage> messageQueue;
    private boolean sessionActive = false;
    
    public TestAdapter(List<ObdMessage> initialMessages) {
        this.messageQueue = new LinkedList<>(initialMessages);
    }
    
    public void enqueueMessage(ObdMessage msg) {
        this.messageQueue.add(msg);
    }

    @Override
    public void startSession(String sessionId) {
        this.sessionActive = true;
    }

    @Override
    public Optional<AdapterResult> pollNextMessage() {
        if (!sessionActive) return Optional.empty();
        ObdMessage msg = messageQueue.poll();
        if (msg == null) return Optional.empty();
        return Optional.of(new AdapterResult(msg, "{\"test_raw\":\"" + msg.getMessageId() + "\"}"));
    }

    @Override
    public void endSession() {
        this.sessionActive = false;
    }
}
