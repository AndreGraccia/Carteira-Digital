import { dayLabel, errorKey, subtractMoney, localDate, monthLabel, projectRegularDeposits } from './wallet';

test('projects zero-rate monthly deposits without inventing earnings', () => {
  expect(projectRegularDeposits('100', 1, '0')).toEqual({
    contributed: 1200,
    earnings: 0,
    futureValue: 1200,
  });
});

test('uses the regular deposit formula with deposits at the beginning of each month', () => {
  const annualRateEquivalentToOnePercentMonthly = ((1.01 ** 12 - 1) * 100).toString();
  const result = projectRegularDeposits(100, 1, annualRateEquivalentToOnePercentMonthly);

  expect(result.contributed).toBe(1200);
  expect(result.futureValue).toBeCloseTo(1280.93, 2);
  expect(result.earnings).toBeCloseTo(80.93, 2);
});

test('calendar days from the API keep their day in any timezone', () => {
  // new Date('2026-10-03') is UTC midnight: the 2nd of October in Brazil. localDate must not shift it.
  expect(localDate('2026-10-03').getDate()).toBe(3);
  expect(dayLabel('2026-10-03', 'pt-BR')).toBe('03/10');
  expect(dayLabel('2026-10-03', 'en')).toBe('10/03');
  expect(monthLabel('2026-10', 'pt-BR')).toBe('outubro de 2026');
  expect(dayLabel(null, 'pt-BR')).toBe('—');
});

test('server and client error codes reach their own message', () => {
  expect(errorKey({ response: { status: 400, data: { code: 'INVALID_PERIOD' } } })).toBe('app.errors.INVALID_PERIOD');
  expect(errorKey({ response: { status: 422, data: { code: 'INITIAL_BALANCE_NOT_ALLOWED' } } })).toBe(
    'app.errors.INITIAL_BALANCE_NOT_ALLOWED'
  );
  // A login answer without a token is a contract problem, not "check your network".
  expect(errorKey({ code: 'INVALID_TOKEN' })).toBe('app.errors.INVALID_TOKEN');
  // axios' own codes are not ours: they keep the status/network fallback.
  expect(errorKey({ code: 'ERR_NETWORK' })).toBe('app.errors.network');
  expect(errorKey({ code: 'ERR_BAD_RESPONSE', response: { status: 500 } })).toBe('app.errors.server');
});

test('money differences are exact in cents (no red "-R$ 0,00")', () => {
  expect(0.3 - 0.1 - 0.2).not.toBe(0);
  expect(subtractMoney('0.3', 0.1 + 0.2)).toBe(0);
  expect(subtractMoney('1000.10', '999.99')).toBe(0.11);
});
