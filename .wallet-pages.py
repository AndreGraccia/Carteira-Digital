from pathlib import Path
files={}
files['frontend/src/pages/dashboard/Dashboard.js']=r'''import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Card, Container, Grid, IconButton, Stack, Typography } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { useWallets } from '../../context/WalletContext';
import { ActivityList, DataState, EmptyState, PageHeading, SectionCard, WalletIcon } from '../../components/wallet-ui';
import WalletCard from '../../components/wallet-card';
import TransactionDetail from '../../components/transaction-detail';
import AuthService from '../../services/AuthService';
import { money, totalsFor } from '../../utils/wallet';

export default function Dashboard() {
  const { t, i18n } = useTranslation();
  const { wallets, transactions } = useWallets();
  const [hidden, setHidden] = useState(false);
  const [detail, setDetail] = useState(null);
  const totals = totalsFor(transactions, wallets);
  const total = wallets.reduce((sum, wallet) => sum + Math.round(Number(wallet.balance) * 100), 0) / 100;
  const user = AuthService.getCurrentUser();
  return <><Helmet><title>{t('dashboard.pageTitle')}</title></Helmet><Container maxWidth="lg"><PageHeading title={t('app.greeting', { name: user?.firstName || t('app.you') })} description={t('app.overviewHelp')} /><DataState><Grid container spacing={3}><Grid item xs={12} md={5}><Card sx={{ p: { xs: 3, sm: 4 }, mb: 3 }}><Stack direction="row" alignItems="center" justifyContent="space-between"><Typography color="text.secondary" variant="body2">{t('wallet.stack.totalBalance')}</Typography><IconButton aria-label={t(hidden ? 'app.showBalance' : 'app.hideBalance')} aria-pressed={hidden} onClick={() => setHidden(!hidden)}><WalletIcon name="eye" sx={{ fontSize: 20 }} /></IconButton></Stack><Typography variant="h2" sx={{ my: 2, fontSize: { xs: '2.5rem', sm: '3rem' }, overflowWrap: 'anywhere', fontVariantNumeric: 'tabular-nums' }}>{hidden ? '••••••' : money(total, i18n.language)}</Typography><Typography variant="caption" color="text.secondary">{t('app.balanceHelp')}</Typography><Stack direction="row" spacing={1} sx={{ mt: 3 }}><Button component={RouterLink} to="/transfers?mode=transfer" variant="contained" fullWidth startIcon={<WalletIcon name="send" />}>{t('app.send')}</Button><Button component={RouterLink} to="/transfers?mode=deposit" variant="outlined" fullWidth startIcon={<WalletIcon name="plus" />}>{t('app.add')}</Button></Stack><Stack direction="row" spacing={3} sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider' }}>{['incoming','outgoing'].map((key) => <Box key={key} sx={{ flex: 1, minWidth: 0 }}><Typography variant="caption" color="text.secondary">{t(`app.${key}`)}</Typography><Typography variant="subtitle1" sx={{ overflowWrap: 'anywhere' }}>{hidden ? '••••' : money(totals[key] / 100, i18n.language)}</Typography></Box>)}</Stack></Card><SectionCard title={t('navigation.wallets')} action={<Button component={RouterLink} to="/wallets">{t('app.seeAll')}</Button>}>{wallets.length ? <Box sx={{ p: 3, pt: 1 }}>{wallets.slice(0,3).map((wallet,index) => <Box key={wallet.id} sx={{ mt: index ? '-105px' : 0, position: 'relative', zIndex: index, '&:focus-within': { zIndex: 5 } }}><WalletCard wallet={wallet} index={index} hideBalance={hidden} onClick={() => { window.location.hash = ''; }} href={`/wallets/${wallet.id}`} /></Box>)}</Box> : <EmptyState title={t('app.firstWallet')} description={t('app.firstWalletHelp')} action={<Button component={RouterLink} to="/wallets/new">{t('wallet.newWallet')}</Button>} />}</SectionCard></Grid><Grid item xs={12} md={7}><SectionCard title={t('wallet.statement.recent')} action={<Button component={RouterLink} to="/transactions">{t('app.seeAll')}</Button>}><ActivityList transactions={transactions.slice(0,6)} wallets={wallets} onSelect={setDetail} /></SectionCard><Card sx={{ mt: 3, p: 3, bgcolor: '#EDEDF2', boxShadow: 'none', border: 0 }}><Stack direction="row" spacing={2}><WalletIcon name="info" sx={{ color: 'text.secondary' }} /><Box><Typography variant="subtitle2">{t('app.learnTitle')}</Typography><Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 1 }}>{t('app.demoNotice')}</Typography><Button component={RouterLink} to="/about" size="small" sx={{ p: 0, minHeight: 32 }}>{t('app.learnMore')}</Button></Box></Stack></Card></Grid></Grid></DataState></Container><TransactionDetail transaction={detail} onClose={() => setDetail(null)} /></>;
}
'''
files['frontend/src/components/wallet-card/index.js']=r'''import { Box, ButtonBase, Card, IconButton, Stack, Tooltip, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import PropTypes from 'prop-types';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { WalletIcon } from '../wallet-ui';
import { money } from '../../utils/wallet';
const THEMES = ['linear-gradient(120deg,#34373D,#111214)', 'linear-gradient(120deg,#557B9F,#294D70)', 'linear-gradient(120deg,#667B6A,#344D40)', 'linear-gradient(120deg,#9F7F70,#705044)', 'linear-gradient(120deg,#807592,#50465E)'];
export default function WalletCard({ wallet, index=0, selected=false, onClick, href, hideBalance=false }) {
  const { t, i18n } = useTranslation();
  const storageKey = `wallet-card-theme-${wallet.id}`;
  const [color, setColor] = useState(() => { try { const saved = localStorage.getItem(storageKey); const value = Number(saved); if (saved !== null && Number.isInteger(value) && value >= 0 && value < THEMES.length) return value; } catch { /* optional preference */ } return index % THEMES.length; });
  const changeColor = () => { const next = (color + 1) % THEMES.length; setColor(next); try { localStorage.setItem(storageKey, String(next)); } catch { /* optional preference */ } };
  return <Card sx={{ height: 202, borderRadius: '24px', background: THEMES[color], color: '#FFF', border: '1px solid #FFFFFF24', boxShadow: '0 8px 20px #16161D24', overflow: 'visible', position: 'relative', '&:focus-within': { outline: '3px solid #0071E3', outlineOffset: 3 } }}><ButtonBase component={href ? RouterLink : 'button'} to={href} onClick={href ? undefined : onClick} aria-pressed={href ? undefined : selected} aria-label={`${wallet.name}, ${t('wallet.card.clickToOpen')}`} sx={{ p: 3, width: '100%', height: '100%', display: 'block', borderRadius: 'inherit', textAlign: 'left' }}><Stack sx={{ height: '100%' }} justifyContent="space-between"><Stack direction="row" spacing={1} alignItems="center" sx={{ pr: selected ? 5 : 0 }}><Typography variant="subtitle1" noWrap sx={{ flex: 1 }}>{wallet.name}</Typography>{!selected && <WalletIcon name="wallet" sx={{ opacity: 0.6 }} />}</Stack><Box><Typography variant="caption" sx={{ color: '#FFFFFFB8' }}>{t('wallet.availableBalance')}</Typography><Typography sx={{ fontSize: 28, fontWeight: 600, letterSpacing: '-0.035em', fontVariantNumeric: 'tabular-nums', overflowWrap: 'anywhere' }}>{hideBalance ? '••••••' : money(wallet.balance, i18n.language)}</Typography></Box><Stack direction="row" justifyContent="space-between"><Typography variant="caption" sx={{ color: '#FFFFFFB8' }}>{t('app.demo')}</Typography><Typography variant="caption" sx={{ letterSpacing: '0.12em' }}>•••• {wallet.iban?.slice(-4)}</Typography></Stack></Stack></ButtonBase>{selected && <Tooltip title={t('wallet.changeColor')}><IconButton aria-label={t('wallet.changeColor')} onClick={changeColor} sx={{ position: 'absolute', right: 14, top: 14, width: 44, height: 44, color: 'white', bgcolor: '#FFFFFF18', '&:hover': { bgcolor: '#FFFFFF28' } }}><Box sx={{ width: 17, height: 17, borderRadius: '50%', border: '2px solid currentColor', background: 'conic-gradient(#B5C9D5,#D0B8A8,#A5BAA3,#B5C9D5)' }} /></IconButton></Tooltip>}</Card>;
}
WalletCard.propTypes = { wallet: PropTypes.object.isRequired, index: PropTypes.number, selected: PropTypes.bool, onClick: PropTypes.func, href: PropTypes.string, hideBalance: PropTypes.bool };
'''
files['frontend/src/pages/wallet/Wallet.js']=r'''import { useState } from 'react';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { Box, Button, Card, Container, Grid, Stack, Typography } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { enqueueSnackbar } from 'notistack';
import { useWallets } from '../../context/WalletContext';
import WalletCard from '../../components/wallet-card';
import TransactionDetail from '../../components/transaction-detail';
import { ActivityList, DataState, EmptyState, PageHeading, SectionCard, WalletIcon } from '../../components/wallet-ui';
export default function Wallet() {
  const { t } = useTranslation();
  const { wallets, transactions } = useWallets();
  const { walletId } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState(null);
  const selected = wallets.find((item) => String(item.id) === walletId) || (!walletId ? wallets[0] : null);
  const activity = selected ? transactions.filter((item) => item.fromWallet?.id === selected.id || item.toWallet?.id === selected.id) : [];
  const copy = async () => { try { await navigator.clipboard.writeText(selected.iban); enqueueSnackbar(t('app.copied'), { variant: 'success' }); } catch { enqueueSnackbar(t('app.copyHelp'), { variant: 'info' }); } };
  return <><Helmet><title>{t('wallet.pageTitle')}</title></Helmet><Container maxWidth="lg"><PageHeading title={t('wallet.heading')} description={t('app.walletHelp')} action={<Button component={RouterLink} to="/wallets/new" variant="contained" startIcon={<WalletIcon name="plus" />}>{t('wallet.newWallet')}</Button>} /><DataState>{!wallets.length ? <Card><EmptyState title={t('app.firstWallet')} description={t('app.firstWalletHelp')} action={<Button component={RouterLink} to="/wallets/new" variant="contained">{t('wallet.newWallet')}</Button>} /></Card> : !selected ? <EmptyState title={t('app.errors.notFound')} action={<Button component={RouterLink} to="/wallets">{t('navigation.wallets')}</Button>} /> : <Grid container spacing={4}><Grid item xs={12} md={5}><Stack spacing={2}>{wallets.map((wallet,index) => <WalletCard key={wallet.id} wallet={wallet} index={index} selected={selected.id === wallet.id} onClick={() => navigate(`/wallets/${wallet.id}`, { replace: true })} />)}</Stack></Grid><Grid item xs={12} md={7}><Card sx={{ mb: 3, p: 3 }}><Typography variant="h5">{selected.name}</Typography><Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>{t('app.walletCode')}</Typography><Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}><Typography variant="body2" sx={{ flex: 1, wordBreak: 'break-all', fontFamily: 'monospace', userSelect: 'all' }}>{selected.iban}</Typography><Button onClick={copy} aria-label={t('app.copy')} sx={{ minWidth: 44 }}><WalletIcon name="copy" /></Button></Stack><Typography variant="caption" color="text.secondary">{t('app.codeHelp')}</Typography><Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mt: 3 }}>{[['transfer','send','app.send'],['deposit','plus','app.add'],['withdraw','up','app.withdraw']].map(([mode,icon,label]) => <Button key={mode} component={RouterLink} to={`/transfers?mode=${mode}&wallet=${selected.id}`} variant={mode === 'transfer' ? 'contained' : 'outlined'} fullWidth startIcon={<WalletIcon name={icon} />}>{t(label)}</Button>)}</Stack></Card><SectionCard title={t('wallet.statement.title')}><ActivityList transactions={activity.slice(0,8)} wallets={[selected]} onSelect={setDetail} />{activity.length > 8 && <Button fullWidth component={RouterLink} to={`/transactions?wallet=${selected.id}`} sx={{ py: 2 }}>{t('app.seeAll')}</Button>}</SectionCard></Grid></Grid>}</DataState></Container><TransactionDetail transaction={detail} onClose={() => setDetail(null)} /></>;
}
'''
files['frontend/src/pages/transaction/Transaction.js']=r'''import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, Container, MenuItem, Stack, TablePagination, TextField } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { useWallets } from '../../context/WalletContext';
import { ActivityList, DataState, PageHeading } from '../../components/wallet-ui';
import TransactionDetail from '../../components/transaction-detail';
export default function Transaction() {
  const { t } = useTranslation();
  const { wallets, transactions } = useWallets();
  const [params, setParams] = useSearchParams();
  const selected = params.get('wallet') || '';
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [detail, setDetail] = useState(null);
  const visible = transactions.filter((item) => (!selected || String(item.fromWallet?.id) === selected || String(item.toWallet?.id) === selected) && [item.description, item.fromWallet?.name, item.toWallet?.name, t(`app.types.${item.type?.name}`, { defaultValue: '' })].join(' ').toLocaleLowerCase().includes(query.toLocaleLowerCase()));
  const scope = selected ? wallets.filter((wallet) => String(wallet.id) === selected) : wallets;
  const currentPage = Math.min(page, Math.max(0, Math.ceil(visible.length / 10) - 1));
  return <><Helmet><title>{t('transactions.pageTitle')}</title></Helmet><Container maxWidth="lg"><PageHeading title={t('transactions.heading')} description={t('app.activityHelp')} /><DataState><Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3 }}><TextField label={t('common.search')} placeholder={t('app.searchActivity')} value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} fullWidth /><TextField select label={t('transfer.wallet')} value={wallets.some((wallet) => String(wallet.id) === selected) ? selected : ''} onChange={(event) => { setParams(event.target.value ? { wallet: event.target.value } : {}); setPage(0); }} sx={{ minWidth: 220 }}><MenuItem value="">{t('app.allWallets')}</MenuItem>{wallets.map((wallet) => <MenuItem value={String(wallet.id)} key={wallet.id}>{wallet.name}</MenuItem>)}</TextField></Stack><Card><ActivityList transactions={visible.slice(currentPage * 10, currentPage * 10 + 10)} wallets={scope} onSelect={setDetail} /><TablePagination component="div" count={visible.length} rowsPerPage={10} rowsPerPageOptions={[10]} page={currentPage} onPageChange={(_, value) => setPage(value)} /></Card></DataState></Container><TransactionDetail transaction={detail} onClose={() => setDetail(null)} /></>;
}
'''
for name,content in files.items():
    p=Path(name); p.parent.mkdir(parents=True,exist_ok=True); p.write_text(content,encoding='utf-8')
p=Path('frontend/src/pages/dashboard/Dashboard.js');s=p.read_text(encoding='utf-8');s=s.replace(" onClick={() => { window.location.hash = ''; }}",'');p.write_text(s,encoding='utf-8')
