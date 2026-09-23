import { dayLabel, localDate, monthLabel, projectRegularDeposits } from './wallet';

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
