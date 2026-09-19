from pathlib import Path
files={}
files['frontend/src/pages/transfer/BasicTabs.js']=r'''import { useRef, useState } from 'react';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom';
import { Alert, Box, Button, Card, Container, Dialog, DialogActions, DialogContent, DialogTitle, Divider, MenuItem, Stack, Tab, Tabs, TextField, Typography } from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { enqueueSnackbar } from 'notistack';
import { useWallets } from '../../context/WalletContext';
import { DataState, EmptyState, PageHeading, WalletIcon } from '../../components/wallet-ui';
import HttpService from '../../services/HttpService';
import { errorKey, money, parseAmount } from '../../utils/wallet';

const modes = { transfer: { endpoint: 'transfer', label: 'app.send' }, deposit: { endpoint: 'addFunds', label: 'app.add' }, withdraw: { endpoint: 'withdrawFunds', label: 'app.withdraw' } };
export default function BasicTabs() {
  const { t, i18n } = useTranslation();
  const { wallets, refresh } = useWallets();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const mode = modes[params.get('mode')] ? params.get('mode') : 'transfer';
  const initialWallet = params.get('wallet') || '';
  const [selectedId, setSelectedId] = useState(initialWallet);
  const [amount, setAmount] = useState('');
  const [recipient, setRecipient] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState(null);
  const [review, setReview] = useState(false);
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  const requestId = useRef(null);
  const wallet = wallets.find((item) => String(item.id) === selectedId) || (!selectedId ? wallets[0] : null);
  const cents = parseAmount(amount);
  const destination = recipient.replace(/\s/g, '').toUpperCase();
  const reset = () => { requestId.current = null; setError(null); };
  const openReview = (event) => {
    event.preventDefault();
    if (!wallet) { setError('app.chooseWallet'); return; }
    if (cents === null || cents <= 0) { setError('app.errors.INVALID_AMOUNT'); return; }
    if (mode !== 'deposit' && cents > Math.round(Number(wallet.balance) * 100)) { setError('app.errors.balance'); return; }
    if (mode === 'transfer' && !/^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/.test(destination)) { setError('app.errors.INVALID_WALLET'); return; }
    if (mode === 'transfer' && wallet.iban === destination) { setError('app.errors.SAME_WALLET'); return; }
    setError(null); setReview(true);
  };
  const confirm = async () => {
    if (submitting.current) return;
    submitting.current = true; setBusy(true);
    if (!requestId.current) requestId.current = crypto.randomUUID();
    try {
      await HttpService.postWithAuth(`/wallets/${modes[mode].endpoint}`, {
        amount: (cents / 100).toFixed(2), description: description.trim(),
        fromWalletIban: wallet.iban, toWalletIban: mode === 'transfer' ? destination : wallet.iban,
        typeId: 1, requestId: requestId.current,
      });
      enqueueSnackbar(t('app.operationSuccess'), { variant: 'success' });
      setReview(false); await refresh(); navigate(`/wallets/${wallet.id}`);
    } catch (failure) { setReview(false); setError(errorKey(failure)); }
    finally { submitting.current = false; setBusy(false); }
  };
  return <><Helmet><title>{t('transfer.pageTitle')}</title></Helmet><Container maxWidth="md"><PageHeading title={t('transfer.heading')} description={t('app.transferHelp')} /><DataState>{!wallets.length ? <Card><EmptyState title={t('app.firstWallet')} description={t('app.firstWalletHelp')} action={<Button component={RouterLink} to="/wallets/new">{t('wallet.newWallet')}</Button>} /></Card> : <Card><Tabs value={mode} variant="fullWidth" onChange={(_, value) => { setParams({ mode: value, ...(wallet ? { wallet: wallet.id } : {}) }); reset(); }} aria-label={t('transfer.heading')} sx={{ borderBottom: '1px solid', borderColor: 'divider', px: 1 }}>{Object.entries(modes).map(([key, value]) => <Tab key={key} value={key} label={t(value.label)} sx={{ minWidth: 0, px: 1 }} />)}</Tabs><Box component="form" onSubmit={openReview} sx={{ p: { xs: 3, sm: 5 } }}><Stack spacing={3}>{error && <Alert severity="error" role="alert">{t(error)}</Alert>}<TextField select fullWidth label={t(mode === 'deposit' ? 'transfer.receiverWallet' : 'transfer.senderWallet')} value={wallet ? String(wallet.id) : ''} onChange={(event) => { setSelectedId(event.target.value); reset(); }} required>{wallets.map((item) => <MenuItem key={item.id} value={String(item.id)}>{item.name}</MenuItem>)}</TextField>{wallet && <Typography variant="body2" color="text.secondary">{t('wallet.availableBalance')}: <strong>{money(wallet.balance, i18n.language)}</strong></Typography>}<TextField label={t('transfer.amount')} placeholder="0,00" value={amount} onChange={(event) => { setAmount(event.target.value); reset(); }} required fullWidth inputProps={{ inputMode: 'decimal', maxLength: 13 }} InputProps={{ startAdornment: <Typography sx={{ mr: 1 }} color="text.secondary">R$</Typography> }} helperText={t('app.amountHelp')} />{mode === 'transfer' && <TextField label={t('app.recipientCode')} value={recipient} onChange={(event) => { setRecipient(event.target.value.toUpperCase()); reset(); }} fullWidth required inputProps={{ maxLength: 42 }} helperText={t('app.recipientHelp')} />}<TextField label={t('app.note')} value={description} onChange={(event) => { setDescription(event.target.value); reset(); }} fullWidth inputProps={{ maxLength: 50 }} helperText={`${description.length}/50`} /><Typography variant="caption" color="text.secondary">{t('app.demoNotice')}</Typography><Button type="submit" size="large" variant="contained" endIcon={<WalletIcon name="arrow" />}>{t('app.review')}</Button></Stack></Box></Card>}</DataState></Container><Dialog open={review} onClose={busy ? undefined : () => setReview(false)} fullWidth maxWidth="xs" aria-labelledby="review-title"><DialogTitle id="review-title">{t('app.review')}</DialogTitle><DialogContent><Typography variant="h3" sx={{ mt: 1, mb: 3 }}>{money((cents || 0) / 100, i18n.language)}</Typography><Stack spacing={2} divider={<Divider />}><Box><Typography variant="caption" color="text.secondary">{t('app.operation')}</Typography><Typography>{t(modes[mode].label)}</Typography></Box><Box><Typography variant="caption" color="text.secondary">{t('transfer.wallet')}</Typography><Typography>{wallet?.name}</Typography></Box>{mode === 'transfer' && <Box><Typography variant="caption" color="text.secondary">{t('app.recipientCode')}</Typography><Typography sx={{ wordBreak: 'break-all' }}>{destination}</Typography></Box>}<Box><Typography variant="caption" color="text.secondary">{t('app.balanceAfter')}</Typography><Typography>{money(Number(wallet?.balance || 0) + (mode === 'deposit' ? 1 : -1) * (cents || 0) / 100, i18n.language)}</Typography></Box></Stack><Alert severity="info" sx={{ mt: 3 }}>{t('app.demoNotice')}</Alert></DialogContent><DialogActions sx={{ p: 3 }}><Button disabled={busy} onClick={() => setReview(false)}>{t('app.edit')}</Button><LoadingButton variant="contained" loading={busy} onClick={confirm}>{t('app.confirm')}</LoadingButton></DialogActions></Dialog></>;
}
'''
files['frontend/src/pages/wallet/NewWallet.js']=r'''import { useRef, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Card, Container, Stack, TextField, Typography } from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { enqueueSnackbar } from 'notistack';
import { useWallets } from '../../context/WalletContext';
import { PageHeading, WalletIcon } from '../../components/wallet-ui';
import AuthService from '../../services/AuthService';
import HttpService from '../../services/HttpService';
import { errorKey } from '../../utils/wallet';
export default function NewWallet() {
  const { t } = useTranslation();
  const { refresh } = useWallets();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const sending = useRef(false);
  const submit = async (event) => {
    event.preventDefault(); if (sending.current) return;
    if (name.trim().length < 3) { setError('app.nameHelp'); return; }
    sending.current = true; setBusy(true); setError(null);
    try { const response = await HttpService.postWithAuth('/wallets', { name: name.trim(), balance: '0.00', userId: AuthService.getCurrentUser()?.id }); await refresh(); enqueueSnackbar(t('wallet.create.success'), { variant: 'success' }); navigate(`/wallets/${response.id}`); }
    catch (failure) { setError(errorKey(failure)); }
    finally { setBusy(false); sending.current = false; }
  };
  return <><Helmet><title>{t('wallet.create.pageTitle')}</title></Helmet><Container maxWidth="sm"><Button component={RouterLink} to="/wallets" startIcon={<WalletIcon name="back" />} sx={{ mb: 3 }}>{t('navigation.wallets')}</Button><PageHeading title={t('wallet.newWallet')} description={t('app.newWalletHelp')} /><Card sx={{ p: { xs: 3, sm: 4 } }}><Box sx={{ width: 56, height: 56, borderRadius: '18px', bgcolor: '#1D1D1F', color: 'white', display: 'grid', placeItems: 'center', mb: 4 }}><WalletIcon name="wallet" sx={{ fontSize: 30 }} /></Box><Box component="form" onSubmit={submit}><Stack spacing={3}>{error && <Alert severity="error">{t(error)}</Alert>}<TextField label={t('wallet.create.name')} placeholder={t('wallet.create.namePlaceholder')} value={name} onChange={(event) => setName(event.target.value)} required autoFocus inputProps={{ minLength: 3, maxLength: 50 }} helperText={t('app.nameHelp')} /><Typography variant="body2" color="text.secondary">{t('app.zeroBalanceHelp')}</Typography><LoadingButton type="submit" variant="contained" loading={busy}>{t('wallet.create.save')}</LoadingButton></Stack></Box></Card></Container></>;
}
'''
files['frontend/src/pages/About.js']=r'''import { Box, Card, Container, Link, Stack, Typography } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { PageHeading } from '../components/wallet-ui';
export default function About() {
  const { t } = useTranslation();
  return <><Helmet><title>{t('app.about')} | e-wallet</title></Helmet><Container maxWidth="md"><PageHeading title={t('app.about')} description={t('app.demoNotice')} /><Stack spacing={3}>{['simulation','apple','bcb'].map((section) => <Card key={section} sx={{ p: 3 }}><Typography variant="h6" gutterBottom>{t(`app.aboutSections.${section}.title`)}</Typography><Typography color="text.secondary">{t(`app.aboutSections.${section}.body`)}</Typography></Card>)}<Box><Typography variant="subtitle2" gutterBottom>{t('app.sources')}</Typography><Stack spacing={1}><Link href="https://www.apple.com/br/wallet/" target="_blank" rel="noreferrer">Apple Wallet</Link><Link href="https://developer.apple.com/help/account/capabilities/configure-apple-pay-on-the-web" target="_blank" rel="noreferrer">Apple Pay — Web</Link><Link href="https://www.bcb.gov.br/estabilidadefinanceira/instituicaopagamento" target="_blank" rel="noreferrer">Banco Central — Instituições de pagamento</Link><Link href="https://www.bcb.gov.br/estabilidadefinanceira/pix" target="_blank" rel="noreferrer">Banco Central — Pix</Link></Stack></Box></Stack></Container></>;
}
'''
for name,content in files.items():
    p=Path(name);p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content,encoding='utf-8')
p=Path('frontend/src/App.js');s=p.read_text();s=s.replace("import { Route, Routes }", "import { Navigate, Route, Routes }");s=s.replace("import AddFunds from './pages/wallet/AddFunds';", "import About from './pages/About';");s=s.replace('<Route path="addFunds" element={<AddFunds />} />','<Route path="addFunds" element={<Navigate to="/transfers?mode=deposit" replace />} />\n              <Route path=":walletId" element={<Wallet />} />');s=s.replace('<Route path="transactions" element={<Transaction />} />','<Route path="transactions" element={<Transaction />} />\n            <Route path="about" element={<About />} />');p.write_text(s,encoding='utf-8')
p=Path('frontend/src/services/axios.js');p.write_text('''import axios from 'axios';
const instance = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});
export default instance;
''',encoding='utf-8')
