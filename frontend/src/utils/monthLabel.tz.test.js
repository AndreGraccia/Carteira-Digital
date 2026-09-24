// Brasil é UTC-3: meia-noite UTC do dia 1 ainda é o último dia do mês anterior aqui.
process.env.TZ = 'America/Sao_Paulo';

// eslint-disable-next-line import/first
import { monthLabel } from './wallet';

test('the timezone of this suite really is behind UTC', () => {
  expect(new Date(Date.UTC(2026, 8, 1)).getMonth()).toBe(7);
});

test('a calendar month keeps its month in UTC-3 (Insights monthly flow)', () => {
  expect(monthLabel('2026-09', 'pt-BR', { month: 'short', year: '2-digit' })).toMatch(/^set/);
  expect(monthLabel('2026-09', 'en', { month: 'short', year: '2-digit' })).toMatch(/^Sep/);
  expect(monthLabel('2026-01', 'pt-BR')).toBe('janeiro de 2026');
});
