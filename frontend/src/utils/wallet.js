export const localeFor = (language) => (language?.startsWith('en') ? 'en-US' : 'pt-BR');
export const money = (value, language) =>
  new Intl.NumberFormat(localeFor(language), { style: 'currency', currency: 'BRL' }).format(Number(value) || 0);
export const dateTime = (value, language) => {
  const date = new Date(value);
  return value && !Number.isNaN(date.getTime())
    ? new Intl.DateTimeFormat(localeFor(language), { dateStyle: 'medium', timeStyle: 'short' }).format(date)
    : '—';
};
export function parseAmount(value) {
  const normalized = String(value).trim().replace(',', '.');
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(normalized)) return null;
  const [whole, fraction = ''] = normalized.split('.');
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
  return Number.isSafeInteger(cents) ? cents : null;
}
export function projectRegularDeposits(monthlyAmount, years, annualRatePercent) {
  const contribution = Number(String(monthlyAmount).replace(',', '.'));
  const duration = Number(years);
  const rate = Number(String(annualRatePercent).replace(',', '.')) / 100;
  const months = duration * 12;
  if (
    !Number.isFinite(contribution) ||
    contribution < 0 ||
    !Number.isInteger(months) ||
    months <= 0 ||
    !Number.isFinite(rate) ||
    rate < 0 ||
    rate > 1
  )
    return { contributed: 0, earnings: 0, futureValue: 0 };
  const monthlyRate = (1 + rate) ** (1 / 12) - 1;
  const futureValue =
    monthlyRate === 0
      ? contribution * months
      : contribution * (1 + monthlyRate) * (((1 + monthlyRate) ** months - 1) / monthlyRate);
  const contributed = contribution * months;
  return { contributed, earnings: futureValue - contributed, futureValue };
}
export function directionFor(transaction, wallets) {
  const owned = new Set(wallets.map((wallet) => wallet.iban));
  const from = owned.has(transaction.fromWallet?.iban);
  const to = owned.has(transaction.toWallet?.iban);
  if (transaction.type?.name === 'Deposit' || transaction.type?.name === 'Initial balance')
    return to ? 'incoming' : 'adjustment';
  if (transaction.type?.name === 'Withdrawal') return from ? 'outgoing' : 'adjustment';
  if (from && to) return 'internal';
  if (from) return 'outgoing';
  if (to) return 'incoming';
  return 'adjustment';
}
export function totalsFor(transactions, wallets) {
  return transactions
    .filter((item) => item.status === 'SUCCESS')
    .reduce(
      (totals, item) => {
        const direction = directionFor(item, wallets);
        if (direction === 'incoming' || direction === 'outgoing')
          totals[direction] += Math.round(Number(item.amount) * 100);
        return totals;
      },
      { incoming: 0, outgoing: 0 }
    );
}
export function normalizeTransactions(response) {
  const rows = Array.isArray(response) ? response : response?.content || [];
  return [...new Map(rows.map((row) => [row.id, row])).values()].sort(
    (a, b) => new Date(b.createdAt) - new Date(a.createdAt) || b.id - a.id
  );
}
export function errorKey(error) {
  const code = error.response?.data?.code;
  if (code === 'AUTH_RATE_LIMIT') return 'app.errors.authRateLimit';
  if (code === 'INSIGHT_RATE_LIMIT') return 'app.errors.insightRateLimit';
  if (code === 'OPEN_FINANCE_UNAVAILABLE') return 'app.errors.openFinance';
  if (
    [
      'INVALID_AMOUNT',
      'SAME_WALLET',
      'INVALID_WALLET',
      'IMMUTABLE_WALLET',
      'IDEMPOTENCY_CONFLICT',
      'OPEN_FINANCE_READ_ONLY',
      'OPEN_FINANCE_CODE_RESEND_WAIT',
      'OPEN_FINANCE_VERIFICATION_REQUIRED',
      'OPEN_FINANCE_CODE_EXPIRED',
      'OPEN_FINANCE_CODE_ATTEMPTS_EXCEEDED',
      'OPEN_FINANCE_CODE_INVALID',
      'OPEN_FINANCE_EMAIL_UNAVAILABLE',
      'OPEN_FINANCE_ALREADY_CONNECTED',
      'RECURRENCE_NOT_FOUND',
      'RECURRENCE_CATEGORY_MISMATCH',
      'SCENARIO_NO_CONFIRMED_INCOME',
      'INVALID_HORIZON',
    ].includes(code)
  )
    return `app.errors.${code}`;
  const status = error.response?.status;
  if (status === 401) return 'app.errors.session';
  if (status === 403) return 'app.errors.forbidden';
  if (status === 404) return 'app.errors.notFound';
  if (status === 409) return 'app.errors.conflict';
  if (status === 412) return 'app.errors.balance';
  if (status === 429) return 'app.errors.rateLimit';
  if (status === 400 || status === 422) return 'app.errors.validation';
  return error.response ? 'app.errors.server' : 'app.errors.network';
}

// API dates without time ("2026-10-03") are calendar days. `new Date("2026-10-03")` would be UTC
// midnight, which is the previous day in Brazil, so they are built as local dates.
export function localDate(isoDay) {
  const [year, month, day] = String(isoDay).split('-').map(Number);
  return new Date(year, month - 1, day || 1);
}
export const dayLabel = (isoDay, language, options = { day: '2-digit', month: '2-digit' }) =>
  isoDay ? new Intl.DateTimeFormat(localeFor(language), options).format(localDate(isoDay)) : '—';
export const monthLabel = (isoMonth, language) =>
  new Intl.DateTimeFormat(localeFor(language), { month: 'long', year: 'numeric' }).format(localDate(`${isoMonth}-01`));
