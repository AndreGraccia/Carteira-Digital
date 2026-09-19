import { useRef, useState } from 'react';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  MenuItem,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { enqueueSnackbar } from 'notistack';
import { useWallets } from '../../context/WalletContext';
import { DataState, EmptyState, PageHeading, WalletIcon } from '../../components/wallet-ui';
import HttpService from '../../services/HttpService';
import { errorKey, money, parseAmount } from '../../utils/wallet';

const modes = {
  transfer: { endpoint: 'transfer', label: 'app.send' },
  deposit: { endpoint: 'addFunds', label: 'app.add' },
  withdraw: { endpoint: 'withdrawFunds', label: 'app.withdraw' },
};
export default function BasicTabs() {
  const { t, i18n } = useTranslation();
  const { wallets, refresh } = useWallets();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const mode = modes[params.get('mode')] ? params.get('mode') : 'transfer';
  const selectedId = params.get('wallet') || '';
  const [amount, setAmount] = useState('');
  const [recipient, setRecipient] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState(null);
  const [review, setReview] = useState(false);
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  const requestId = useRef(null);
  const requestSignature = useRef(null);
  const wallet = wallets.find((item) => String(item.id) === selectedId) || (!selectedId ? wallets[0] : null);
  const cents = parseAmount(amount);
  const destination = recipient.replace(/\s/g, '').toUpperCase();
  const reset = () => {
    requestId.current = null;
    setError(null);
  };
  const openReview = (event) => {
    event.preventDefault();
    if (!wallet) {
      setError('app.chooseWallet');
      return;
    }
    if (cents === null || cents <= 0) {
      setError('app.errors.INVALID_AMOUNT');
      return;
    }
    if (mode !== 'deposit' && cents > Math.round(Number(wallet.balance) * 100)) {
      setError('app.errors.balance');
      return;
    }
    if (mode === 'transfer' && !/^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/.test(destination)) {
      setError('app.errors.INVALID_WALLET');
      return;
    }
    if (mode === 'transfer' && wallet.iban === destination) {
      setError('app.errors.SAME_WALLET');
      return;
    }
    setError(null);
    setReview(true);
  };
  const confirm = async () => {
    if (submitting.current) return;
    submitting.current = true;
    setBusy(true);
    try {
      const signature = JSON.stringify([mode, wallet.iban, destination, cents, description.trim()]);
      if (!requestId.current || requestSignature.current !== signature) {
        requestId.current = crypto.randomUUID();
        requestSignature.current = signature;
      }
      await HttpService.postWithAuth(`/wallets/${modes[mode].endpoint}`, {
        amount: (cents / 100).toFixed(2),
        description: description.trim(),
        fromWalletIban: wallet.iban,
        toWalletIban: mode === 'transfer' ? destination : wallet.iban,
        typeId: 1,
        requestId: requestId.current,
      });
      enqueueSnackbar(t('app.operationSuccess'), { variant: 'success' });
      setReview(false);
      await refresh();
      navigate(`/wallets/${wallet.id}`);
    } catch (failure) {
      setReview(false);
      setError(errorKey(failure));
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  };
  return (
    <>
      <Helmet>
        <title>{t('transfer.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="md">
        <PageHeading title={t('transfer.heading')} description={t('app.transferHelp')} />
        <DataState>
          {!wallets.length ? (
            <Card>
              <EmptyState
                title={t('app.firstWallet')}
                description={t('app.firstWalletHelp')}
                action={
                  <Button component={RouterLink} to="/wallets/new">
                    {t('wallet.newWallet')}
                  </Button>
                }
              />
            </Card>
          ) : (
            <Card>
              <Tabs
                value={mode}
                variant="fullWidth"
                onChange={(_, value) => {
                  setParams({ mode: value, ...(wallet ? { wallet: wallet.id } : {}) });
                  reset();
                }}
                aria-label={t('transfer.heading')}
                sx={{ borderBottom: '1px solid', borderColor: 'divider', px: 1 }}
              >
                {Object.entries(modes).map(([key, value]) => (
                  <Tab key={key} value={key} label={t(value.label)} sx={{ minWidth: 0, px: 1 }} />
                ))}
              </Tabs>
              <Box component="form" onSubmit={openReview} sx={{ p: { xs: 3, sm: 5 } }}>
                <Stack spacing={3}>
                  {error && (
                    <Alert severity="error" role="alert">
                      {t(error)}
                    </Alert>
                  )}
                  <TextField
                    select
                    fullWidth
                    label={t(mode === 'deposit' ? 'transfer.receiverWallet' : 'transfer.senderWallet')}
                    value={wallet ? String(wallet.id) : ''}
                    onChange={(event) => {
                      setParams({ mode, wallet: event.target.value });
                      reset();
                    }}
                    required
                  >
                    {wallets.map((item) => (
                      <MenuItem key={item.id} value={String(item.id)}>
                        {item.name}
                      </MenuItem>
                    ))}
                  </TextField>
                  {wallet && (
                    <Typography variant="body2" color="text.secondary">
                      {t('wallet.availableBalance')}: <strong>{money(wallet.balance, i18n.language)}</strong>
                    </Typography>
                  )}
                  <TextField
                    label={t('transfer.amount')}
                    placeholder="0,00"
                    value={amount}
                    onChange={(event) => {
                      setAmount(event.target.value);
                      reset();
                    }}
                    required
                    fullWidth
                    inputProps={{ inputMode: 'decimal', maxLength: 13 }}
                    InputProps={{
                      startAdornment: (
                        <Typography sx={{ mr: 1 }} color="text.secondary">
                          R$
                        </Typography>
                      ),
                    }}
                    helperText={t('app.amountHelp')}
                  />
                  {mode === 'transfer' && (
                    <TextField
                      label={t('app.recipientCode')}
                      value={recipient}
                      onChange={(event) => {
                        setRecipient(event.target.value.toUpperCase());
                        reset();
                      }}
                      fullWidth
                      required
                      inputProps={{ maxLength: 42 }}
                      helperText={t('app.recipientHelp')}
                    />
                  )}
                  <TextField
                    label={t('app.note')}
                    value={description}
                    onChange={(event) => {
                      setDescription(event.target.value);
                      reset();
                    }}
                    fullWidth
                    inputProps={{ maxLength: 50 }}
                    helperText={`${description.length}/50`}
                  />
                  <Typography variant="caption" color="text.secondary">
                    {t('app.demoNotice')}
                  </Typography>
                  <Button type="submit" size="large" variant="contained" endIcon={<WalletIcon name="arrow" />}>
                    {t('app.review')}
                  </Button>
                </Stack>
              </Box>
            </Card>
          )}
        </DataState>
      </Container>
      <Dialog
        open={review}
        onClose={busy ? undefined : () => setReview(false)}
        fullWidth
        maxWidth="xs"
        aria-labelledby="review-title"
      >
        <DialogTitle id="review-title">{t('app.review')}</DialogTitle>
        <DialogContent>
          <Typography variant="h3" sx={{ mt: 1, mb: 3, overflowWrap: 'anywhere' }}>
            {money((cents || 0) / 100, i18n.language)}
          </Typography>
          <Stack spacing={2} divider={<Divider />}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                {t('app.operation')}
              </Typography>
              <Typography>{t(modes[mode].label)}</Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">
                {t('transfer.wallet')}
              </Typography>
              <Typography>{wallet?.name}</Typography>
            </Box>
            {mode === 'transfer' && (
              <Box>
                <Typography variant="caption" color="text.secondary">
                  {t('app.recipientCode')}
                </Typography>
                <Typography sx={{ wordBreak: 'break-all' }}>{destination}</Typography>
              </Box>
            )}
            <Box>
              <Typography variant="caption" color="text.secondary">
                {t('app.balanceAfter')}
              </Typography>
              <Typography>
                {money(
                  Number(wallet?.balance || 0) + ((mode === 'deposit' ? 1 : -1) * (cents || 0)) / 100,
                  i18n.language
                )}
              </Typography>
            </Box>
          </Stack>
          <Alert severity="info" sx={{ mt: 3 }}>
            {t('app.demoNotice')}
          </Alert>
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <Button disabled={busy} onClick={() => setReview(false)}>
            {t('app.edit')}
          </Button>
          <LoadingButton variant="contained" loading={busy} onClick={confirm}>
            {t('app.confirm')}
          </LoadingButton>
        </DialogActions>
      </Dialog>
    </>
  );
}
