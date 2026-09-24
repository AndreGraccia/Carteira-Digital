import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import AuthService from '../services/AuthService';
import HttpService from '../services/HttpService';
import { errorKey, normalizeTransactions } from '../utils/wallet';

const WalletContext = createContext(null);
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
        HttpService.getWithAuth(`/transactions/users/${userId}`),
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
