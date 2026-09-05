# IgnitionAI Custom Diagnostic Dongle: Hardware & Protection Specification

**Document Version:** 1.0  
**Target Vehicle Scope:** 12 V Indian BS4 Passenger Vehicles (Classical CAN ISO 15765-4)  
**Microcontroller:** Espressif ESP32-S3-WROOM-1 (Classical CAN/TWAI + BLE 5.0)  

---

## 1. J1962 OBD-II Connector Pinout & Termination

| OBD Pin | Signal Name | Connection on Dongle | Notes & Protection |
|---|---|---|---|
| **Pin 16** | Battery Power (12V) | Power Supply Rail | Connected via PTC resettable fuse + P-MOSFET reverse polarity protection + TVS diode clamp. |
| **Pin 4** | Chassis Ground | Ground Plane (Star Point) | Connected to system GND plane. Common reference for electrostatic discharge. |
| **Pin 5** | Signal Ground | Ground Plane (Star Point) | Star-grounded with Pin 4 at connector entry to eliminate ground loop voltage drops. |
| **Pin 6** | CAN High (ISO 15765-4) | CAN Transceiver CANH | Automotive CAN transceiver pin with +/-58V fault protection. **NO terminating resistor** on vehicle dongle. |
| **Pin 14** | CAN Low (ISO 15765-4) | CAN Transceiver CANL | Automotive CAN transceiver pin with +/-58V fault protection. **NO terminating resistor** on vehicle dongle. |
| **Pins 1, 2, 3, 7, 8, 9, 10, 11, 12, 13, 15** | Unused / OEM Proprietary | **UNCONNECTED (High-Z)** | No connection to prevent interference with manufacturer proprietary buses. |

> [!IMPORTANT]
> **Termination Rule:** The vehicle CAN bus already contains 60 $\Omega$ split termination (two 120 $\Omega$ resistors at bus endpoints). The custom dongle **must not** add termination resistors. Termination (120 $\Omega$) is only present on the bench simulator peer harness.

---

## 2. Automotive Electrical Front-End Protection

Vehicles present severe electrical transients (ISO 7637-2). The front-end circuit provides 4-stage hardening:

```text
OBD Pin 16 (12V) 
   ──> [FUSE: 500mA PTC Resettable]
   ──> [TVS: SMBJ28CA Bi-directional 600W Clamp]
   ──> [REVERSE POLARITY: P-Channel MOSFET Low-Loss Gate]
   ──> [BUCK CONVERTER: LM5164-Q1 (6V - 65V input -> 3.3V / 1A output)]
   ──> Clean 3.3V System Rail (ESP32-S3 & CAN Transceiver VIO)
```

### Protection Ratings:
- **Continuous Working Voltage:** 9.0 V to 16.0 V DC.
- **Load Dump & Transient Suppression:** ISO 7637-2 Pulse 1 (-100V inductive spike) and Pulse 2a (+50V switching spike) clamped below 40V by SMBJ28CA TVS.
- **Reverse Battery Protection:** Low $R_{DS(on)}$ P-Channel MOSFET (e.g. DMP4015SPSQ-13) blocks reverse battery connection with minimal voltage drop (< 30 mV at 150 mA).
- **Overcurrent Protection:** 500 mA hold current PTC resettable fuse (1812 footprint).
- **Quiescent Current / Standby:** < 1.0 mA after 15 minutes of bus inactivity (deep sleep triggered via CAN transceiver standby pin).

---

## 3. CAN Transceiver & TWAI Controller Interface

- **Transceiver IC:** Texas Instruments **TCAN1042HGV-Q1** (Automotive AEC-Q100 Qualified)
  - Fault protection: $\pm 58 \text{ V}$ on CANH / CANL.
  - Common mode range: $\pm 30 \text{ V}$.
  - ESD protection: $\pm 16 \text{ kV}$ IEC 61000-4-2.
  - Standby pin controlled via ESP32-S3 GPIO for sleep mode.
- **ESP32-S3 TWAI Pins:**
  - `TWAI_TX` $\rightarrow$ GPIO 4
  - `TWAI_RX` $\rightarrow$ GPIO 5
  - `CAN_STBY` $\rightarrow$ GPIO 6 (Active HIGH for low-power standby)

---

## 4. Mechanical & Packaging Guidelines

- **Form Factor:** Compact right-angle or short-lead OBD-II enclosure to avoid interfering with pedals, knees, or steering columns.
- **Status Indicator:** Dual-color LED (Green = Power / BLE Paired, Blue = CAN Activity, Red = Bus Error).
- **Physical Pairing:** Hardwired serial / QR code on housing for authenticated pairing without default universal PIN.
