package com.ignitionai.obdinput.adapter;

import com.ignitionai.obdinput.schema.ObdMessage;
import java.util.Optional;

public class BluetoothAdapterStub implements ObdAdapter {
    
    private final String adapterId;
    
    public BluetoothAdapterStub(String adapterId) {
        this.adapterId = adapterId;
    }

    @Override
    public void startSession(String sessionId) {
        throw new UnsupportedDeviceException("Live Bluetooth integration is not supported until tested with physical hardware.");
    }

    @Override
    public Optional<AdapterResult> pollNextMessage() {
        throw new UnsupportedDeviceException("Live Bluetooth integration is not supported until tested with physical hardware.");
    }

    @Override
    public void endSession() {
        // No-op
    }
}
