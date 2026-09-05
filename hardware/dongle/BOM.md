# IgnitionAI Diagnostic Dongle & Bench Peer: Bill of Materials (BOM)

**Budget Target:** INR 5,000 - 10,000  
**Provisional Allocation:** INR 8,000  
**Status:** Component costing based on Indian electronic component distributors (Element14 India, Mouser India, Robu.in).

---

## 1. Unit 1: Custom Diagnostic Dongle (Vehicle Unit)

| Item | Component / Part Number | Description & Specification | Qty | Unit Price (INR) | Total (INR) |
|---|---|---|---|---|---|
| U1 | **ESP32-S3-WROOM-1-N8R2** | Dual-core 240MHz, 8MB Flash, 2MB PSRAM, BLE 5.0, TWAI CAN | 1 | 380 | 380 |
| U2 | **TCAN1042HGV-Q1** | Automotive CAN transceiver, AEC-Q100, $\pm 58 \text{V}$ fault protection | 1 | 210 | 210 |
| U3 | **LM5164-Q1** | Wide input buck regulator (6V-65V to 3.3V / 1A), automotive grade | 1 | 290 | 290 |
| D1 | **SMBJ28CA** | 600W Bi-directional TVS surge diode, 28V standoff, 45V max clamp | 1 | 45 | 45 |
| Q1 | **DMP4015SPSQ-13** | 40V Automotive P-Channel MOSFET for reverse polarity protection | 1 | 65 | 65 |
| F1 | **MF-MSMF050-2** | 500mA hold current resettable PTC fuse (1812) | 1 | 35 | 35 |
| L1, C1-C8 | Passives & Filters | Automotive grade inductors, X7R ceramic capacitors, resistors | - | 220 | 220 |
| J1 | **J1962M-90** | Right-angle male OBD-II 16-pin connector with strain relief | 1 | 450 | 450 |
| PCB1 | Custom 2-Layer PCB | FR4, 1.6mm, 1oz copper, ENIG finish (prototype fab batch) | 1 | 750 | 750 |
| ENC1 | Custom 3D/Molded Case | Heat-resistant ABS enclosure, snap-fit with status light pipe | 1 | 650 | 650 |
| **Subtotal Dongle Unit** | | | | | **3,095** |

---

## 2. Unit 2: Bench ECU Simulator Peer & Test Harness

| Item | Component / Part Number | Description & Specification | Qty | Unit Price (INR) | Total (INR) |
|---|---|---|---|---|---|
| U4 | **ESP32 DevKit V1** | Standard ESP32 microcontroller for bench ECU simulation | 1 | 380 | 380 |
| U5 | **SN65HVD230 / VP230** | 3.3V CAN transceiver breakout module for bench testing | 1 | 160 | 160 |
| R_TERM | 120 $\Omega$ 1% Metal Film | CAN bus termination resistor on bench harness | 2 | 10 | 20 |
| J2 | J1962F Harness | Female OBD-II test socket with 4mm banana jacks for bench DC supply | 1 | 550 | 550 |
| PS1 | 12V 2A Regulated Adapter | Current-limited bench supply adapter for test rig | 1 | 450 | 450 |
| **Subtotal Bench Rig** | | | | | **1,560** |

---

## 3. Tooling, Shipping & Contingency Reserve

| Item | Description | Allocation (INR) |
|---|---|---|
| Domestic Shipping & Taxes | Freight from distributors & PCB fab | 950 |
| Solder & Assembly Consumables | Low-temp lead-free paste, flux, rework wick, thermal pads | 450 |
| Prototype Rework Reserve | Contingency for layout adjustment or component substitution | 1,945 |
| **Total Prototype Package** | **Dongle + Bench ECU Peer + Harness + Contingency** | **INR 8,000** |

> [!NOTE]
> **Budget Compliance:** The total prototype package is firmly within the INR 8,000 provisional allocation and below the INR 10,000 ceiling. Existing workshop tools (oscilloscope, multimeter, soldering station, and Android phone) are leveraged without budget deduction.
