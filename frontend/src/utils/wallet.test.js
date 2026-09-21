import { projectRegularDeposits } from './wallet';

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
