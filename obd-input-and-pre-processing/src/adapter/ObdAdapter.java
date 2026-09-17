package com.ignitionai.obdinput.adapter;

import com.ignitionai.obdinput.schema.ObdMessage;
import java.util.Optional;

public interface ObdAdapter {
    void startSession(String sessionId);
    Optional<AdapterResult> pollNextMessage();
    void endSession();
}
