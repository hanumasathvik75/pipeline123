# IgnitionAI Vehicle Support Matrix (Initial BS4 Candidate Profiles)

Status: Candidate profiles for qualification. Real vehicle testing requires physical bench / vehicle verification before promotion to `Validated`.

| Vehicle Profile ID | Make & Model | Engine / Fuel | ECU / Protocol | Supported PIDs Target | Validation Status |
|---|---|---|---|---|---|
| VP-MS-K12M | Maruti Suzuki Swift / Dzire (2012-2018) | 1.2L K12M Petrol (Naturally Aspirated) | Bosch ME17 / ISO 15765-4 (CAN 11-bit 500k) | RPM, VSS, ECT, MAP, IAT, SPARK, STFT1, LTFT1, O2S11, TP, VPWR | Candidate (Simulated) |
| VP-MS-D13A | Maruti Suzuki Swift / Dzire (2012-2018) | 1.3L DDiS MultiJet Diesel (Turbo) | Magneti Marelli MJD / ISO 15765-4 (CAN 11-bit 500k) | RPM, VSS, ECT, MAP, MAF, FRP, BOOST, VPWR (Trims N/A for Diesel) | Candidate (Simulated) |
| VP-HY-K12 | Hyundai i20 / Grand i10 (2013-2019) | 1.2L Kappa VTVT Petrol | Kefico / Continental / ISO 15765-4 (CAN 11-bit 500k) | RPM, VSS, ECT, MAP, IAT, STFT1, LTFT1, TP, VPWR | Candidate (Simulated) |
| VP-TA-REV | Tata Tiago / Tigor (2016-2020) | 1.2L Revotron Petrol | Bosch / ISO 15765-4 (CAN 11-bit 500k) | RPM, VSS, ECT, MAP, IAT, STFT1, LTFT1, VPWR | Candidate (Simulated) |

### Profile Validation Checklist
- [ ] Stationary Protocol Handshake & Timing Jitter <= 50ms
- [ ] Verified PID Scaling and Units against Service Manual / Reference Tool
- [ ] Fuel Trim Convention confirmed (Positive = Lean correction, Negative = Rich correction)
- [ ] MAP vs Barometric Pressure scaling verified
- [ ] No dashboard warning or CAN bus error frames induced during 10-minute query loop
