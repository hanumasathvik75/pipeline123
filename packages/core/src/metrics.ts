/**
 * Robust Statistical Metrics & Filtering
 * Section 7.1 of IgnitionAI MVP Implementation Plan
 */

export function computeMedian(values: number[]): number {
  if (values.length === 0) return 0;
  const sorted = [...values].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 !== 0 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
}

export function computeMAD(values: number[], median?: number): number {
  if (values.length === 0) return 0;
  const med = median !== undefined ? median : computeMedian(values);
  const deviations = values.map(v => Math.abs(v - med));
  return computeMedian(deviations);
}

/**
 * Combined Petrol Fuel Trim percentage:
 * Formula: 100 * ((1 + STFT/100) * (1 + LTFT/100) - 1)
 */
export function computeCombinedFuelTrim(stft: number, ltft: number): number {
  const stftFactor = 1 + (stft / 100);
  const ltftFactor = 1 + (ltft / 100);
  return Number((100 * (stftFactor * ltftFactor - 1)).toFixed(2));
}

/**
 * Standardized residual:
 * Scale = max(1.4826 * MAD_healthy, validated_noise_floor)
 * z = (r - median_healthy_residual) / Scale
 */
export function computeStandardizedResidual(
  residual: number,
  healthyMedianResidual: number,
  healthyMAD: number,
  noiseFloor: number = 0.5
): number {
  const scale = Math.max(1.4826 * healthyMAD, noiseFloor);
  return (residual - healthyMedianResidual) / scale;
}

export interface EWMAState {
  value: number;
  lastTimestampMs: number;
}

/**
 * Time-aware EWMA:
 * alpha(dt) = 1 - exp(-dt / tau_seconds)
 * EWMA_t = (1 - alpha) * EWMA_prev + alpha * z_t
 */
export function updateEWMA(
  currentZ: number,
  currentTimestampMs: number,
  previousState: EWMAState | null,
  tauSeconds: number = 3.0
): EWMAState {
  if (!previousState) {
    return { value: currentZ, lastTimestampMs: currentTimestampMs };
  }

  const dtSeconds = Math.max(0.001, (currentTimestampMs - previousState.lastTimestampMs) / 1000);
  const alpha = 1 - Math.exp(-dtSeconds / tauSeconds);
  const newValue = (1 - alpha) * previousState.value + alpha * currentZ;

  return {
    value: newValue,
    lastTimestampMs: currentTimestampMs,
  };
}

export interface CUSUMState {
  positive: number;
  negative: number;
}

/**
 * Two-sided CUSUM:
 * C_pos = max(0, C_pos_prev + z_t - k)
 * C_neg = min(0, C_neg_prev + z_t + k)
 */
export function updateCUSUM(
  currentZ: number,
  previousState: CUSUMState,
  k: number = 0.5
): CUSUMState {
  return {
    positive: Math.max(0, previousState.positive + currentZ - k),
    negative: Math.min(0, previousState.negative + currentZ + k),
  };
}
