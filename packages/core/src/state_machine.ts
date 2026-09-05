import { OperatingState, DecodedObservation } from '@ignitionai/contracts';

export interface StateClassifierInputs {
  rpm: number;
  speed: number;
  throttle: number;
  dtSeconds: number;
  previousRpm?: number;
  previousThrottle?: number;
}

export class VehicleStateMachine {
  private currentState: OperatingState = 'unknown';
  private stateDwellSeconds: number = 0;

  public classify(inputs: StateClassifierInputs): OperatingState {
    const { rpm, speed, throttle, dtSeconds, previousRpm = rpm, previousThrottle = throttle } = inputs;
    const dRpmDt = (rpm - previousRpm) / Math.max(0.01, dtSeconds);
    const dThrottleDt = (throttle - previousThrottle) / Math.max(0.01, dtSeconds);

    let nextCandidate: OperatingState = 'unknown';

    if (rpm < 200) {
      nextCandidate = 'key_on_engine_off';
    } else if (rpm >= 550 && rpm <= 1100 && speed <= 3) {
      nextCandidate = 'idle';
    } else if (speed >= 25 && Math.abs(dThrottleDt) <= 3.0 && Math.abs(dRpmDt) <= 120) {
      nextCandidate = 'cruise';
    } else if (dThrottleDt > 4.0 || dRpmDt > 200) {
      nextCandidate = 'acceleration';
    } else if (throttle <= 3.0 && (dRpmDt < -150 || speed > 10)) {
      nextCandidate = 'deceleration';
    } else {
      nextCandidate = 'unknown';
    }

    // Apply 1-second hysteresis for state transitions
    if (nextCandidate === this.currentState) {
      this.stateDwellSeconds += dtSeconds;
    } else {
      if (this.stateDwellSeconds >= 1.0 || this.currentState === 'unknown') {
        this.currentState = nextCandidate;
        this.stateDwellSeconds = dtSeconds;
      } else {
        this.stateDwellSeconds += dtSeconds;
      }
    }

    return this.currentState;
  }

  public getCurrentState(): OperatingState {
    return this.currentState;
  }

  public reset(): void {
    this.currentState = 'unknown';
    this.stateDwellSeconds = 0;
  }
}
