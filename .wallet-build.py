from pathlib import Path
files = {}
files['frontend/src/utils/wallet.js'] = r'''export const localeFor = (language) => (language?.startsWith('en') ? 'en-US' : 'pt-BR');
export const money = (value, language) => new Intl.NumberFormat(localeFor(language), { style: 'currency', currency: 'BRL' }).format(Number(value) || 0);
export const dateTime = (value, language) => {
  const date = new Date(value);
  return value && !Number.isNaN(date.getTime()) ? new Intl.DateTimeFormat(localeFor(language), { dateStyle: 'medium', timeStyle: 'short' }).format(date) : '—';
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
  if (transaction.type?.name === 'Deposit' || transaction.type?.name === 'Initial balance') return to ? 'incoming' : 'adjustment';
  if (transaction.type?.name === 'Withdrawal') return from ? 'outgoing' : 'adjustment';
  if (from && to) return 'internal';
  if (from) return 'outgoing';
  if (to) return 'incoming';
  return 'adjustment';
}
export function totalsFor(transactions, wallets) {
  return transactions.filter((item) => item.status === 'SUCCESS').reduce((totals, item) => {
    const direction = directionFor(item, wallets);
    if (direction === 'incoming' || direction === 'outgoing') totals[direction] += Math.round(Number(item.amount) * 100);
    return totals;
  }, { incoming: 0, outgoing: 0 });
}
export function normalizeTransactions(response) {
  const rows = Array.isArray(response) ? response : response?.content || [];
  return [...new Map(rows.map((row) => [row.id, row])).values()].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt) || b.id - a.id);
}
export function errorKey(error) {
  const code = error.response?.data?.code;
  if (['INVALID_AMOUNT', 'SAME_WALLET', 'INVALID_WALLET', 'IMMUTABLE_WALLET', 'IDEMPOTENCY_CONFLICT'].includes(code)) return `app.errors.${code}`;
  const status = error.response?.status;
  if (status === 401) return 'app.errors.session';
  if (status === 403) return 'app.errors.forbidden';
  if (status === 404) return 'app.errors.notFound';
  if (status === 409) return 'app.errors.conflict';
  if (status === 412) return 'app.errors.balance';
  if (status === 400 || status === 422) return 'app.errors.validation';
  return error.response ? 'app.errors.server' : 'app.errors.network';
}
'''
files['frontend/src/context/WalletContext.js'] = r'''import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import { useNavigate } from 'react-router-dom';
import AuthService from '../services/AuthService';
import HttpService from '../services/HttpService';
import { errorKey, normalizeTransactions } from '../utils/wallet';

const WalletContext = createContext(null);
export function WalletProvider({ children }) {
  const navigate = useNavigate();
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
      setError(errorKey(failure));
      if (failure.response?.status === 401) { AuthService.logout(); navigate('/login', { replace: true }); }
    } finally { setLoading(false); }
  }, [navigate, userId]);
  useEffect(() => { refresh(); }, [refresh]);
  const value = useMemo(() => ({ ...data, loading, error, refresh }), [data, loading, error, refresh]);
  return <WalletContext.Provider value={value}>{children}</WalletContext.Provider>;
}
WalletProvider.propTypes = { children: PropTypes.node };
export const useWallets = () => useContext(WalletContext);
'''
files['frontend/src/components/wallet-ui/index.js'] = r'''import { Alert, Box, Button, Card, CircularProgress, Stack, SvgIcon, Typography } from '@mui/material';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { useWallets } from '../../context/WalletContext';
import { dateTime, directionFor, money } from '../../utils/wallet';

const paths = {
  wallet: 'M3 7h17v13H3V4h14v3M20 11h-5v5h5M16.5 13.5h.01',
  home: 'm3 10 9-7 9 7M5 9v12h5v-7h4v7h5V9',
  history: 'M3 11a9 9 0 1 1 2 7M3 4v7h7M12 7v5l3 2',
  send: 'm4 4 17 8-17 8 3-8-3-8Zm3 8h14',
  plus: 'M12 5v14M5 12h14',
  down: 'M12 4v16m-6-6 6 6 6-6',
  up: 'M12 20V4m-6 6 6-6 6 6',
  arrow: 'M5 12h14m-5-5 5 5-5 5',
  back: 'M19 12H5m5-5-5 5 5 5',
  check: 'm5 12 4 4L19 6',
  user: 'M20 21v-2a6 6 0 0 0-6-6h-4a6 6 0 0 0-6 6v2M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  logout: 'M9 4H4v16h5m5-13 5 5-5 5M8 12h11',
  eye: 'M2 12s3-7 10-7 10 7 10 7-3 7-10 7S2 12 2 12Zm13 0a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  info: 'M12 11v6m0-10h.01M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
  copy: 'M9 9h12v12H9V9ZM15 9V3H3v12h6',
  swap: 'M4 7h16m-4-4 4 4-4 4M20 17H4m4-4-4 4 4 4',
};
export function WalletIcon({ name, ...props }) {
  return <SvgIcon {...props} viewBox="0 0 24 24"><path d={paths[name] || paths.wallet} fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /></SvgIcon>;
}
WalletIcon.propTypes = { name: PropTypes.string };
export function PageHeading({ title, description, action }) {
  return <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={2} sx={{ mb: { xs: 3, md: 4 } }}><Box sx={{ minWidth: 0 }}><Typography variant="h3" component="h1">{title}</Typography>{description && <Typography color="text.secondary" sx={{ mt: 0.75 }}>{description}</Typography>}</Box>{action}</Stack>;
}
PageHeading.propTypes = { title: PropTypes.string, description: PropTypes.string, action: PropTypes.node };
export function EmptyState({ title, description, action, icon = 'wallet' }) {
  return <Stack alignItems="center" spacing={2} sx={{ py: 6, px: 3, textAlign: 'center' }}><Box sx={{ p: 2, borderRadius: '22px', bgcolor: 'background.neutral', color: 'text.secondary' }}><WalletIcon name={icon} sx={{ fontSize: 34 }} /></Box><Typography variant="h6">{title}</Typography><Typography color="text.secondary" variant="body2" sx={{ maxWidth: 340 }}>{description}</Typography>{action}</Stack>;
}
EmptyState.propTypes = { title: PropTypes.string, description: PropTypes.string, action: PropTypes.node, icon: PropTypes.string };
export function DataState({ children }) {
  const { t } = useTranslation();
  const { loading, error, refresh } = useWallets();
  if (loading) return <Box role="status" sx={{ textAlign: 'center', py: 10 }}><CircularProgress size={28} /><Typography sx={{ mt: 2 }} color="text.secondary">{t('common.loading')}</Typography></Box>;
  if (error) return <Alert severity="error" action={<Button color="inherit" onClick={refresh}>{t('app.retry')}</Button>}>{t(error)}</Alert>;
  return children;
}
DataState.propTypes = { children: PropTypes.node };
export function ActivityList({ transactions, wallets, onSelect }) {
  const { t, i18n } = useTranslation();
  if (!transactions.length) return <EmptyState icon="history" title={t('app.noActivity')} description={t('app.noActivityHelp')} />;
  return <Box>{transactions.map((item) => {
    const direction = directionFor(item, wallets);
    const success = item.status === 'SUCCESS';
    const color = success && direction === 'incoming' ? 'success.dark' : 'text.primary';
    const title = t(`app.types.${item.type?.name}`, { defaultValue: t('app.types.Transfer') });
    return <Box key={item.id} component="button" type="button" onClick={() => onSelect(item)} sx={{ width: '100%', display: 'flex', alignItems: 'center', gap: { xs: 1.5, sm: 2 }, py: 2, px: { xs: 2, sm: 3 }, border: 0, borderBottom: '1px solid', borderColor: 'divider', bgcolor: 'transparent', color: 'text.primary', textAlign: 'left', cursor: 'pointer', '&:last-child': { borderBottom: 0 }, '&:hover': { bgcolor: 'action.hover' }, '&:focus-visible': { outline: '2px solid', outlineColor: 'primary.main', outlineOffset: -2 } }}><Box sx={{ width: 42, height: 42, flexShrink: 0, borderRadius: '14px', bgcolor: 'background.neutral', display: 'grid', placeItems: 'center', color }}><WalletIcon name={direction === 'incoming' ? 'down' : direction === 'outgoing' ? 'up' : 'swap'} /></Box><Box sx={{ flex: 1, minWidth: 0 }}><Typography variant="subtitle2" noWrap>{title}</Typography><Typography variant="caption" color="text.secondary">{dateTime(item.createdAt, i18n.language)}</Typography></Box><Box sx={{ textAlign: 'right', minWidth: 0 }}><Typography variant="subtitle2" sx={{ color, fontVariantNumeric: 'tabular-nums', overflowWrap: 'anywhere' }}>{success && direction === 'outgoing' ? '− ' : success && direction === 'incoming' ? '+ ' : ''}{money(item.amount, i18n.language)}</Typography><Typography variant="caption" color="text.secondary">{t(`transactions.status.${item.status}`, { defaultValue: item.status })}</Typography></Box></Box>;
  })}</Box>;
}
ActivityList.propTypes = { transactions: PropTypes.array.isRequired, wallets: PropTypes.array.isRequired, onSelect: PropTypes.func.isRequired };
export function SectionCard({ title, action, children }) {
  return <Card><Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1} sx={{ p: 3, pb: 1.5 }}><Typography variant="h6">{title}</Typography>{action}</Stack>{children}</Card>;
}
SectionCard.propTypes = { title: PropTypes.string, action: PropTypes.node, children: PropTypes.node };
'''
files['frontend/src/components/transaction-detail/index.js'] = r'''import { Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, Stack, Typography } from '@mui/material';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { dateTime, money } from '../../utils/wallet';
export default function TransactionDetail({ transaction, onClose }) {
  const { t, i18n } = useTranslation();
  if (!transaction) return null;
  const rows = [ ['transfer.senderWallet', transaction.fromWallet?.name], ['transfer.receiverWallet', transaction.toWallet?.name], ['transactions.columns.createdAt', dateTime(transaction.createdAt, i18n.language)], ['transactions.columns.status', t(`transactions.status.${transaction.status}`, { defaultValue: transaction.status })], ['transfer.description', transaction.description === 'Initial balance' ? t('app.types.Initial balance') : transaction.description || '—'], ['app.reference', transaction.referenceNumber] ];
  return <Dialog open onClose={onClose} fullWidth maxWidth="xs" aria-labelledby="detail-title"><DialogTitle id="detail-title">{t('app.details')}</DialogTitle><DialogContent><Typography variant="h3" sx={{ py: 2, fontVariantNumeric: 'tabular-nums' }}>{money(transaction.amount, i18n.language)}</Typography><Typography color="text.secondary" sx={{ mb: 3 }}>{t(`app.types.${transaction.type?.name}`, { defaultValue: t('app.types.Transfer') })}</Typography><Stack divider={<Divider />} spacing={1.5}>{rows.map(([label, value]) => <Box key={label}><Typography variant="caption" color="text.secondary">{t(label)}</Typography><Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>{value}</Typography></Box>)}</Stack><Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 3 }}>{t('app.demoNotice')}</Typography></DialogContent><DialogActions><Button onClick={onClose}>{t('common.close')}</Button></DialogActions></Dialog>;
}
TransactionDetail.propTypes = { transaction: PropTypes.object, onClose: PropTypes.func.isRequired };
'''
files['frontend/src/layouts/dashboard/DashboardLayout.js'] = r'''import { useState } from 'react';
import { Link as RouterLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Avatar, BottomNavigation, BottomNavigationAction, Box, Button, Chip, Container, Divider, IconButton, Stack, Tooltip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { WalletProvider } from '../../context/WalletContext';
import LanguageSelector from '../../components/language-selector';
import { WalletIcon } from '../../components/wallet-ui';
import AuthService from '../../services/AuthService';

export default function DashboardLayout() {
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const user = AuthService.getCurrentUser();
  const [accountOpen, setAccountOpen] = useState(false);
  const items = [{ path: '/', label: 'navigation.dashboard', icon: 'home' }, { path: '/wallets', label: 'navigation.wallets', icon: 'wallet' }, { path: '/transfers', label: 'navigation.transfers', icon: 'send' }, { path: '/transactions', label: 'navigation.transactions', icon: 'history' }];
  const current = items.find((item) => item.path === '/' ? pathname === '/' : pathname.startsWith(item.path))?.path || false;
  const logout = () => { AuthService.logout(); navigate('/login', { replace: true }); };
  return <WalletProvider><Box sx={{ minHeight: '100vh', display: 'flex' }}><Box component="aside" sx={{ width: 240, position: 'fixed', inset: '0 auto 0 0', bgcolor: '#EEEEF1', borderRight: '1px solid', borderColor: 'divider', p: 2.5, display: { xs: 'none', lg: 'flex' }, flexDirection: 'column' }}><Stack direction="row" spacing={1.5} alignItems="center" sx={{ px: 1, py: 2, mb: 4 }}><Box sx={{ width: 38, height: 38, borderRadius: '12px', bgcolor: '#1D1D1F', color: 'white', display: 'grid', placeItems: 'center' }}><WalletIcon name="wallet" /></Box><Typography variant="h6">e-wallet</Typography></Stack><Stack component="nav" spacing={0.75} aria-label={t('app.navigation')}>{items.map((item) => <Button key={item.path} component={RouterLink} to={item.path} aria-current={current === item.path ? 'page' : undefined} startIcon={<WalletIcon name={item.icon} />} sx={{ justifyContent: 'flex-start', px: 2, gap: 1, color: current === item.path ? 'text.primary' : 'text.secondary', bgcolor: current === item.path ? 'white' : 'transparent', boxShadow: current === item.path ? '0 2px 8px #00000006' : 'none' }}>{t(item.label)}</Button>)}</Stack><Box sx={{ flexGrow: 1 }} /><Chip label={t('app.demo')} size="small" sx={{ alignSelf: 'flex-start', mb: 1.5 }} /><Typography variant="caption" color="text.secondary" sx={{ px: 0.5, mb: 2 }}>{t('app.demoNotice')}</Typography><Button component={RouterLink} to="/about" startIcon={<WalletIcon name="info" />} sx={{ justifyContent: 'flex-start', color: 'text.secondary' }}>{t('app.about')}</Button></Box><Box sx={{ flex: 1, minWidth: 0, ml: { lg: '240px' } }}><Box component="header" sx={{ position: 'sticky', top: 0, zIndex: 1100, bgcolor: 'rgba(245,245,247,0.88)', backdropFilter: 'blur(20px)', borderBottom: '1px solid', borderColor: 'divider' }}><Container maxWidth="lg"><Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ minHeight: 76 }}><Stack direction="row" spacing={1} alignItems="center"><Typography variant="subtitle1" sx={{ display: { lg: 'none' } }}>e-wallet</Typography><Chip size="small" label={t('app.demo')} sx={{ bgcolor: 'background.neutral' }} /></Stack><Stack direction="row" spacing={1.5} alignItems="center"><LanguageSelector /><Tooltip title={t('common.account')}><IconButton onClick={() => setAccountOpen(!accountOpen)} aria-label={t('common.account')} aria-expanded={accountOpen}><Avatar sx={{ width: 34, height: 34, bgcolor: '#DEDEE4', color: '#48484A', fontSize: 14 }}>{user?.firstName?.[0] || 'U'}</Avatar></IconButton></Tooltip></Stack></Stack>{accountOpen && <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} spacing={1} sx={{ pb: 2 }}><Typography variant="body2" sx={{ flex: 1 }}>{user?.firstName} {user?.lastName}</Typography><Button component={RouterLink} to="/about" onClick={() => setAccountOpen(false)}>{t('app.about')}</Button><Button onClick={logout} startIcon={<WalletIcon name="logout" />}>{t('actions.logout')}</Button></Stack>}</Container></Box><Box component="main" sx={{ pt: { xs: 3, md: 5 }, pb: { xs: 14, lg: 6 } }}><Outlet /></Box></Box><Box sx={{ display: { lg: 'none' }, position: 'fixed', bottom: 0, right: 0, left: 0, zIndex: 1200, pb: 'env(safe-area-inset-bottom)', bgcolor: 'rgba(250,250,252,0.95)', backdropFilter: 'blur(20px)' }}><Divider /><BottomNavigation showLabels value={current} sx={{ height: 70, bgcolor: 'transparent' }}>{items.map((item) => <BottomNavigationAction key={item.path} component={RouterLink} to={item.path} value={item.path} label={t(item.label)} icon={<WalletIcon name={item.icon} />} sx={{ minWidth: 0, px: 0.5, '& .MuiBottomNavigationAction-label': { fontSize: '0.7rem' } }} />)}</BottomNavigation></Box></Box></WalletProvider>;
}
'''
for name,content in files.items():
    p=Path(name); p.parent.mkdir(parents=True,exist_ok=True); p.write_text(content,encoding='utf-8')
print('Shared wallet interface and data layer written')
