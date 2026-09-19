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
  if (['INVALID_AMOUNT', 'SAME_WALLET', 'INVALID_WALLET', 'IMMUTABLE_WALLET', 'IDEMPOTENCY_CONFLICT'].includes(code))
    return `app.errors.${code}`;
  const status = error.response?.status;
  if (status === 401) return 'app.errors.session';
  if (status === 403) return 'app.errors.forbidden';
  if (status === 404) return 'app.errors.notFound';
  if (status === 409) return 'app.errors.conflict';
  if (status === 412) return 'app.errors.balance';
  if (status === 400 || status === 422) return 'app.errors.validation';
  return error.response ? 'app.errors.server' : 'app.errors.network';
}
