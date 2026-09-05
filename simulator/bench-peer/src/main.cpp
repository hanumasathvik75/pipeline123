/**
 * IgnitionAI Bench ECU Simulator Peer Firmware
 * Target: ESP32 / ESP32-S3 using ESP-IDF / Arduino Framework
 *
 * Simulates an ISO 15765-4 classical CAN (500 kbps, 11-bit ID) Engine Control Unit (0x7E8)
 * Responds to diagnostic queries on 0x7DF and 0x7E0.
 */

#include <Arduino.h>
#include "driver/twai.h"

// CAN GPIO definitions (Adjust according to board pinout)
#define CAN_TX_PIN GPIO_NUM_4
#define CAN_RX_PIN GPIO_NUM_5

// Standard CAN IDs
#define OBD_BROADCAST_ID   0x7DF
#define ECU_PHYSICAL_ID    0x7E0
#define ECU_RESPONSE_ID    0x7E8

// Fault injection state
enum FaultProfile {
    FAULT_NONE,
    FAULT_AIR_LEAK,
    FAULT_THERMOSTAT_STUCK,
    FAULT_VOLTAGE_SAG
};

static FaultProfile currentFault = FAULT_NONE;
static uint32_t sessionStartTimeMs = 0;

void setupCAN() {
    twai_general_config_t g_config = TWAI_GENERAL_CONFIG_DEFAULT(CAN_TX_PIN, CAN_RX_PIN, TWAI_MODE_NORMAL);
    twai_timing_config_t t_config = TWAI_TIMING_CONFIG_500KBITS();
    
    // Accept all 11-bit standard diagnostic IDs
    twai_filter_config_t f_config = TWAI_FILTER_CONFIG_ACCEPT_ALL();

    if (twai_driver_install(&g_config, &t_config, &f_config) == ESP_OK) {
        Serial.println("[ECU-SIM] TWAI Driver installed successfully.");
    } else {
        Serial.println("[ECU-SIM] Failed to install TWAI driver.");
        return;
    }

    if (twai_start() == ESP_OK) {
        Serial.println("[ECU-SIM] TWAI Driver started at 500 kbps.");
    } else {
        Serial.println("[ECU-SIM] Failed to start TWAI driver.");
    }
}

void sendCANResponse(uint8_t length, const uint8_t* payload) {
    twai_message_t tx_msg;
    tx_msg.identifier = ECU_RESPONSE_ID;
    tx_msg.extd = 0; // Standard 11-bit ID
    tx_msg.rtr = 0;
    tx_msg.data_length_code = 8;

    // ISO 15765-4 Single Frame formatting
    tx_msg.data[0] = length; // Number of valid bytes
    for (int i = 0; i < 7; i++) {
        tx_msg.data[i + 1] = (i < length) ? payload[i] : 0xAA; // 0xAA padding
    }

    esp_err_t err = twai_transmit(&tx_msg, pdMS_TO_TICKS(50));
    if (err != ESP_OK) {
        Serial.printf("[ECU-SIM] Transmit failed: 0x%X\n", err);
    }
}

void handleDiagnosticRequest(const twai_message_t& rx_msg) {
    if (rx_msg.identifier != OBD_BROADCAST_ID && rx_msg.identifier != ECU_PHYSICAL_ID) {
        return; // Ignore frames not addressed to ECU
    }

    uint8_t pci_len = rx_msg.data[0];
    if (pci_len < 1) return;

    uint8_t service = rx_msg.data[1];
    uint8_t pid = (pci_len >= 2) ? rx_msg.data[2] : 0x00;

    // Reject prohibited services (e.g. 0x04 clear DTCs)
    if (service == 0x04) {
        Serial.println("[ECU-SIM] REJECTED: Mode 0x04 Clear DTC prohibited in bench test.");
        uint8_t nrc[3] = { 0x7F, 0x04, 0x12 }; // Negative Response Code: Subfunction not supported
        sendCANResponse(3, nrc);
        return;
    }

    uint32_t elapsedMs = millis() - sessionStartTimeMs;
    float elapsedMinutes = elapsedMs / 60000.0f;

    // Mode 01: Current Powertrain Diagnostic Data
    if (service == 0x01) {
        uint8_t responseService = 0x41; // Positive response = Service + 0x40

        switch (pid) {
            case 0x00: { // Supported PIDs [01-20]
                uint8_t resp[6] = { responseService, pid, 0xBE, 0x3F, 0xA8, 0x13 };
                sendCANResponse(6, resp);
                break;
            }
            case 0x05: { // Engine Coolant Temperature (ECT)
                // Normal: warm up to 88C (A = ECT + 40)
                int ect = (int)min(88.0f, 35.0f + (elapsedMinutes * 11.0f));
                if (currentFault == FAULT_THERMOSTAT_STUCK) {
                    ect = (int)min(54.0f, 30.0f + (elapsedMinutes * 4.0f));
                }
                uint8_t a = (uint8_t)(ect + 40);
                uint8_t resp[3] = { responseService, pid, a };
                sendCANResponse(3, resp);
                break;
            }
            case 0x06: { // Short Term Fuel Trim (STFT1)
                // Normal: ~0% (A = 128)
                int stftPercent = 0;
                if (currentFault == FAULT_AIR_LEAK) {
                    stftPercent = 16; // Lean bias at idle
                }
                uint8_t a = (uint8_t)((stftPercent + 100) * 1.28f);
                uint8_t resp[3] = { responseService, pid, a };
                sendCANResponse(3, resp);
                break;
            }
            case 0x07: { // Long Term Fuel Trim (LTFT1)
                int ltftPercent = 0;
                if (currentFault == FAULT_AIR_LEAK) {
                    ltftPercent = 10;
                }
                uint8_t a = (uint8_t)((ltftPercent + 100) * 1.28f);
                uint8_t resp[3] = { responseService, pid, a };
                sendCANResponse(3, resp);
                break;
            }
            case 0x0B: { // Intake Manifold Absolute Pressure (MAP)
                uint8_t mapKpa = 34; // Idle vacuum ~34 kPa
                uint8_t resp[3] = { responseService, pid, mapKpa };
                sendCANResponse(3, resp);
                break;
            }
            case 0x0C: { // Engine RPM: ((A * 256) + B) / 4
                uint16_t rpm = 750; // Idle
                uint16_t rawRpm = rpm * 4;
                uint8_t resp[4] = { responseService, pid, (uint8_t)(rawRpm >> 8), (uint8_t)(rawRpm & 0xFF) };
                sendCANResponse(4, resp);
                break;
            }
            case 0x0D: { // Vehicle Speed (km/h)
                uint8_t speed = 0; // Idle
                uint8_t resp[3] = { responseService, pid, speed };
                sendCANResponse(3, resp);
                break;
            }
            case 0x42: { // Control Module Voltage: ((A * 256) + B) / 1000
                float vpwr = (currentFault == FAULT_VOLTAGE_SAG) ? 12.2f : 14.1f;
                uint16_t rawV = (uint16_t)(vpwr * 1000.0f);
                uint8_t resp[4] = { responseService, pid, (uint8_t)(rawV >> 8), (uint8_t)(rawV & 0xFF) };
                sendCANResponse(4, resp);
                break;
            }
            default: { // Unsupported PID in Mode 01
                uint8_t nrc[3] = { 0x7F, 0x01, 0x12 }; // Subfunction not supported
                sendCANResponse(3, nrc);
                break;
            }
        }
    }
}

void setup() {
    Serial.begin(115200);
    delay(1000);
    Serial.println("\n=======================================================");
    Serial.println("  IGNITIONAI BENCH ECU SIMULATOR PEER (ISO 15765-4)");
    Serial.println("  Commands via Serial: LEAK, THERMO, VOLT, NORMAL");
    Serial.println("=======================================================");

    sessionStartTimeMs = millis();
    setupCAN();
}

void loop() {
    // Process Serial commands for fault injection
    if (Serial.available()) {
        String cmd = Serial.readStringUntil('\n');
        cmd.trim();
        cmd.toUpperCase();

        if (cmd == "LEAK") {
            currentFault = FAULT_AIR_LEAK;
            Serial.println("[ECU-SIM] Injected: AIR_INTAKE_LEAK (+STFT at idle)");
        } else if (cmd == "THERMO") {
            currentFault = FAULT_THERMOSTAT_STUCK;
            Serial.println("[ECU-SIM] Injected: THERMOSTAT_STUCK_OPEN (Slow Warmup)");
        } else if (cmd == "VOLT") {
            currentFault = FAULT_VOLTAGE_SAG;
            Serial.println("[ECU-SIM] Injected: VOLTAGE_SAG (Alternator 12.2V)");
        } else if (cmd == "NORMAL") {
            currentFault = FAULT_NONE;
            Serial.println("[ECU-SIM] Injected: NORMAL_BASELINE (Healthy)");
        }
    }

    // Process incoming CAN frames
    twai_message_t rx_msg;
    if (twai_receive(&rx_msg, pdMS_TO_TICKS(10)) == ESP_OK) {
        handleDiagnosticRequest(rx_msg);
    }
}
