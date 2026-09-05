import { RawFrameInput } from '@ignitionai/capture-protocol';

export type ScenarioType = 
  | 'healthy_baseline'
  | 'air_intake_leak'
  | 'coolant_thermostat_stuck_open'
  | 'alternator_voltage_sag'
  | 'protocol_quirks';

export interface GeneratedScenario {
  scenarioType: ScenarioType;
  vehicleProfileId: string;
  totalDurationMs: number;
  rawFrames: RawFrameInput[];
  expectedFindingLevels: Record<string, string>;
}

export function generateScenario(type: ScenarioType, seed: number = 42): GeneratedScenario {
  const frames: RawFrameInput[] = [];
  const durationMs = 600000; // 10 minutes = 600,000 ms
  const stepMs = 500; // 2 Hz sampling
  const nowIso = new Date('2026-09-06T10:00:00.000Z').toISOString();

  let pseudoRandom = seed;
  const rand = () => {
    pseudoRandom = (pseudoRandom * 9301 + 49297) % 233280;
    return pseudoRandom / 233280;
  };

  for (let t = 0; t < durationMs; t += stepMs) {
    const elapsedMinutes = t / 60000;

    // Default vehicle dynamics: 0-1m idle, 1-2m launch, 2-4m cruise, 4-6m accel, 6-7m decel, 7-9m cruise, 9-10m idle
    let speed = 0;
    let rpm = 750;
    let throttle = 14;

    if (elapsedMinutes < 1.0) {
      // Idle
      speed = 0;
      rpm = 750 + (rand() * 20 - 10);
      throttle = 13;
    } else if (elapsedMinutes < 2.0) {
      // Gentle departure
      speed = 20 + (elapsedMinutes - 1.0) * 15;
      rpm = 1600 + (rand() * 50 - 25);
      throttle = 22;
    } else if (elapsedMinutes < 4.0) {
      // Steady Cruise 1
      speed = 45 + (rand() * 2 - 1);
      rpm = 2100 + (rand() * 30 - 15);
      throttle = 20;
    } else if (elapsedMinutes < 6.0) {
      // Smooth Acceleration
      speed = 55 + (rand() * 3);
      rpm = 2800 + (rand() * 60 - 30);
      throttle = 38;
    } else if (elapsedMinutes < 7.0) {
      // Deceleration / Lift-off
      speed = 30 - (elapsedMinutes - 6.0) * 15;
      rpm = 1200 - (elapsedMinutes - 6.0) * 400;
      throttle = 0;
    } else if (elapsedMinutes < 9.0) {
      // Steady Cruise 2
      speed = 40 + (rand() * 2 - 1);
      rpm = 1900 + (rand() * 20 - 10);
      throttle = 18;
    } else {
      // Final Idle
      speed = 0;
      rpm = 740 + (rand() * 20 - 10);
      throttle = 13;
    }

    // Thermal trajectory: Normal warmup from 35C to 88C within 5 minutes
    let ect = Math.min(88, 35 + (elapsedMinutes * 11));
    if (type === 'coolant_thermostat_stuck_open') {
      // Thermostat stuck open: never warms up past 54C
      ect = Math.min(54, 30 + (elapsedMinutes * 4));
    }

    // Fuel Trims
    let stft = rand() * 4 - 2; // Normal ~0%
    let ltft = rand() * 2 - 1; // Normal ~0%

    if (type === 'air_intake_leak') {
      if (speed === 0) {
        // High positive trim at idle (high vacuum pulls unmetered air)
        stft = 12 + (rand() * 4);
        ltft = 8 + (rand() * 2);
      } else {
        // Less impact at cruise (throttle open reduces vacuum relative to metered flow)
        stft = 4 + (rand() * 2);
        ltft = 3 + (rand() * 2);
      }
    }

    // Charging Voltage
    let vpwr = 14.1 + (rand() * 0.2 - 0.1);
    if (type === 'alternator_voltage_sag') {
      vpwr = 12.2 + (rand() * 0.2 - 0.1);
    }

    // Encode RPM: ((A * 256) + B) / 4 = rpm => (A * 256) + B = rpm * 4
    const rawRpmVal = Math.round(rpm * 4);
    const rpmA = Math.floor(rawRpmVal / 256);
    const rpmB = rawRpmVal % 256;

    // Encode Speed: A = speed
    const speedA = Math.min(255, Math.max(0, Math.round(speed)));

    // Encode ECT: A = ECT + 40
    const ectA = Math.min(255, Math.max(0, Math.round(ect + 40)));

    // Encode STFT: (A / 1.28) - 100 = stft => A = (stft + 100) * 1.28
    const stftA = Math.min(255, Math.max(0, Math.round((stft + 100) * 1.28)));
    const ltftA = Math.min(255, Math.max(0, Math.round((ltft + 100) * 1.28)));

    // Encode VPWR: ((A * 256) + B) / 1000 = vpwr => val = vpwr * 1000
    const rawVpwrVal = Math.round(vpwr * 1000);
    const vpwrA = Math.floor(rawVpwrVal / 256);
    const vpwrB = rawVpwrVal % 256;

    // Push standard frames
    frames.push({
      ecuId: '0x7E8',
      service: 0x01,
      pid: 0x0C,
      payloadBytes: [rpmA, rpmB],
      monotonicStartMs: t,
      monotonicEndMs: t + 15,
      utcAnchorIso: nowIso,
      isSimulated: true,
    });

    frames.push({
      ecuId: '0x7E8',
      service: 0x01,
      pid: 0x0D,
      payloadBytes: [speedA],
      monotonicStartMs: t + 20,
      monotonicEndMs: t + 35,
      utcAnchorIso: nowIso,
      isSimulated: true,
    });

    // Slower cadence channels (every 2 seconds)
    if (t % 2000 === 0) {
      frames.push({
        ecuId: '0x7E8',
        service: 0x01,
        pid: 0x05,
        payloadBytes: [ectA],
        monotonicStartMs: t + 40,
        monotonicEndMs: t + 55,
        utcAnchorIso: nowIso,
        isSimulated: true,
      });

      frames.push({
        ecuId: '0x7E8',
        service: 0x01,
        pid: 0x06,
        payloadBytes: [stftA],
        monotonicStartMs: t + 60,
        monotonicEndMs: t + 75,
        utcAnchorIso: nowIso,
        isSimulated: true,
      });

      frames.push({
        ecuId: '0x7E8',
        service: 0x01,
        pid: 0x07,
        payloadBytes: [ltftA],
        monotonicStartMs: t + 80,
        monotonicEndMs: t + 95,
        utcAnchorIso: nowIso,
        isSimulated: true,
      });

      frames.push({
        ecuId: '0x7E8',
        service: 0x01,
        pid: 0x42,
        payloadBytes: [vpwrA, vpwrB],
        monotonicStartMs: t + 100,
        monotonicEndMs: t + 115,
        utcAnchorIso: nowIso,
        isSimulated: true,
      });
    }
  }

  const expectedFindingLevels: Record<string, string> = {
    healthy_baseline: 'subsystem',
    air_intake_leak: 'candidate',
    coolant_thermostat_stuck_open: 'candidate',
    alternator_voltage_sag: 'candidate',
    protocol_quirks: 'insufficient_evidence',
  };

  return {
    scenarioType: type,
    vehicleProfileId: 'VP-MS-K12M',
    totalDurationMs: durationMs,
    rawFrames: frames,
    expectedFindingLevels,
  };
}
