package com.ignitionai.bms;

import java.time.LocalDateTime;

/** One row of a processed BMS CSV export. Every field is nullable/boxed on purpose -- a blank or
 * unparseable cell in the source file stays absent here rather than being silently coerced to 0,
 * matching the same "missing stays missing" discipline the OBD pipeline already follows. */
public final class BmsReading {
    public final LocalDateTime timestamp;
    public final Double packVoltageV;
    public final Double packCurrentA;
    public final Double[] cellVoltagesV; // index 0 = Cell 1 ... index 13 = Cell 14
    public final Double cellTemp1C, cellTemp2C, cellTemp3C, fetTempC;
    public final Double deratedChargeCurrentA, deratedDischargeCurrentA;
    public final Double socEkfPct, socCoulombPct, voltageImbalanceV, packSocPct, packSohPct;
    public final Double motorSpeedRpm, motorTempC;

    public BmsReading(LocalDateTime timestamp, Double packVoltageV, Double packCurrentA, Double[] cellVoltagesV,
            Double cellTemp1C, Double cellTemp2C, Double cellTemp3C, Double fetTempC,
            Double deratedChargeCurrentA, Double deratedDischargeCurrentA,
            Double socEkfPct, Double socCoulombPct, Double voltageImbalanceV, Double packSocPct, Double packSohPct,
            Double motorSpeedRpm, Double motorTempC) {
        this.timestamp = timestamp;
        this.packVoltageV = packVoltageV;
        this.packCurrentA = packCurrentA;
        this.cellVoltagesV = cellVoltagesV;
        this.cellTemp1C = cellTemp1C; this.cellTemp2C = cellTemp2C; this.cellTemp3C = cellTemp3C; this.fetTempC = fetTempC;
        this.deratedChargeCurrentA = deratedChargeCurrentA; this.deratedDischargeCurrentA = deratedDischargeCurrentA;
        this.socEkfPct = socEkfPct; this.socCoulombPct = socCoulombPct; this.voltageImbalanceV = voltageImbalanceV;
        this.packSocPct = packSocPct; this.packSohPct = packSohPct;
        this.motorSpeedRpm = motorSpeedRpm; this.motorTempC = motorTempC;
    }
}
