import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import AuthService from '../services/AuthService';
import HttpService from '../services/HttpService';
import { errorKey, normalizeTransactions } from '../utils/wallet';

const WalletContext = createContext(null);
// The statement is paged (100 by default, 500 at most); the screens compute over the whole history.
const PAGE_SIZE = 500;
// ponytail: 10.000 rows in the browser. Past that, the screens should ask the API for aggregates instead.
const MAX_PAGES = 20;

export async function fetchAllTransactions(userId) {
  const rows = [];
  for (let page = 0; page < MAX_PAGES; page += 1) {
    // eslint-disable-next-line no-await-in-loop
    const response = await HttpService.getWithAuth(`/transactions/users/${userId}?page=${page}&size=${PAGE_SIZE}`);
    if (Array.isArray(response)) return response;
    const content = response?.content || [];
    rows.push(...content);
    const info = response?.page;
    if (!info || info.number + 1 >= info.totalPages || content.length < PAGE_SIZE) break;
  }
  return rows;
}

export function WalletProvider({ children }) {
  const user = AuthService.getCurrentUser();
  const userId = user?.id;
  const [data, setData] = useState({ wallets: [], transactions: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [wallets, transactions] = await Promise.all([
        HttpService.getWithAuth(`/wallets/users/${userId}`),
        fetchAllTransactions(userId),
      ]);
      setData({ wallets: Array.isArray(wallets) ? wallets : [], transactions: normalizeTransactions(transactions) });
    } catch (failure) {
      // 401 is handled once for every screen in HttpService.
      setError(errorKey(failure));
    } finally {
      setLoading(false);
    }
  }, [userId]);
  useEffect(() => {
    refresh();
  }, [refresh]);
  const value = useMemo(() => ({ ...data, loading, error, refresh }), [data, loading, error, refresh]);
  return <WalletContext.Provider value={value}>{children}</WalletContext.Provider>;
}
WalletProvider.propTypes = { children: PropTypes.node };
export const useWallets = () => useContext(WalletContext);
