import { toCents as cents } from './wallet';

/**
 * Same rule as the Future page: with nothing confirmed, a limit or a "free" amount would read as
 * "you have R$ X" when the engine knows nothing yet, so the three screens hide it.
 *
 * `sources` comes with the overview. The Ask answer has no sources, so there the limit itself says
 * whether a confirmed income bounds it (`untilIncome`).
 */
// ponytail: Ask falls back to untilIncome; if the backend adds `sources` to the answer, pass it and the rule is identical.
export function forecastReady({ sources, limit } = {}) {
  if (sources) return Number(sources.confirmedRecurrences) > 0;
  return Boolean(limit?.untilIncome);
}

/**
 * The headline of the daily limit, shared by the Future card and the Ask answer so the same data never
 * yields two different "missing" amounts.
 *
 * `shortfall` is computed on balance minus envelopes; without the envelopes the bank lacks exactly
 * `shortfall - reserved` (see SpendingLimit). When that is zero, only the envelopes are uncovered.
 *
 * @returns {{kind: 'canSpend'|'short'|'shortEnvelopes', amount: number, withEnvelopes: number, reserved: number}}
 */
export function limitSummary(limit) {
  const reserved = cents(limit?.reserved);
  const withEnvelopes = cents(limit?.shortfall);
  const realShort = Math.max(withEnvelopes - reserved, 0);
  const base = { withEnvelopes: withEnvelopes / 100, reserved: reserved / 100 };
  if (withEnvelopes > 0 && realShort > 0) return { ...base, kind: 'short', amount: realShort / 100 };
  if (withEnvelopes > 0) return { ...base, kind: 'shortEnvelopes', amount: withEnvelopes / 100 };
  return { ...base, kind: 'canSpend', amount: Number(limit?.safeDaily) || 0 };
}
