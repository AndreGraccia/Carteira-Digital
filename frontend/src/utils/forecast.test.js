import { forecastReady, limitSummary } from './forecast';

test('the bank shortfall excludes the envelopes, in cents', () => {
  expect(limitSummary({ safeDaily: 0, shortfall: '800.10', reserved: '500.00' })).toEqual({
    kind: 'short',
    amount: 300.1,
    withEnvelopes: 800.1,
    reserved: 500,
  });
});

test('a shortfall made only of envelopes says so instead of "missing even without spending"', () => {
  expect(limitSummary({ safeDaily: 0, shortfall: 200, reserved: 500 })).toMatchObject({
    kind: 'shortEnvelopes',
    amount: 200,
  });
});

test('without shortfall the headline is the safe daily amount', () => {
  expect(limitSummary({ safeDaily: '42.5', shortfall: 0, reserved: 0 })).toMatchObject({
    kind: 'canSpend',
    amount: 42.5,
  });
  // float noise must not become a shortfall
  expect(limitSummary({ safeDaily: 10, shortfall: 0.1 + 0.2 - 0.3, reserved: 0 }).kind).toBe('canSpend');
});

test('nothing confirmed means no limit to show, on every screen', () => {
  expect(forecastReady({ sources: { confirmedRecurrences: 0 } })).toBe(false);
  expect(forecastReady({ sources: { confirmedRecurrences: 2 } })).toBe(true);
  expect(forecastReady({ limit: { untilIncome: false } })).toBe(false);
  expect(forecastReady({ limit: { untilIncome: true } })).toBe(true);
  expect(forecastReady()).toBe(false);
});
