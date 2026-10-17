import { useRef, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Card, Container, Stack, TextField, Typography } from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { enqueueSnackbar } from 'notistack';
import { useWallets } from '../../context/WalletContext';
import { IconTile, PageHeading, WalletIcon } from '../../components/wallet-ui';
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
    event.preventDefault();
    if (sending.current) return;
    if (name.trim().length < 3) {
      setError('app.nameHelp');
      return;
    }
    sending.current = true;
    setBusy(true);
    setError(null);
    try {
      const response = await HttpService.postWithAuth('/wallets', {
        name: name.trim(),
        balance: '0.00',
        userId: AuthService.getCurrentUser()?.id,
      });
      await refresh();
      enqueueSnackbar(t('wallet.create.success'), { variant: 'success' });
      navigate(`/wallets/${response.id}`);
    } catch (failure) {
      setError(errorKey(failure));
    } finally {
      setBusy(false);
      sending.current = false;
    }
  };
  return (
    <>
      <Helmet>
        <title>{t('wallet.create.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="sm">
        <Button component={RouterLink} to="/wallets" startIcon={<WalletIcon name="back" />} sx={{ mb: 3 }}>
          {t('navigation.wallets')}
        </Button>
        <PageHeading title={t('wallet.newWallet')} description={t('app.newWalletHelp')} />
        <Card sx={{ p: { xs: 3, sm: 4 } }}>
          <IconTile name="wallet" size={56} sx={{ mb: 4 }} />
          <Box component="form" onSubmit={submit}>
            <Stack spacing={3}>
              {error && <Alert severity="error">{t(error)}</Alert>}
              <TextField
                label={t('wallet.create.name')}
                placeholder={t('wallet.create.namePlaceholder')}
                value={name}
                onChange={(event) => setName(event.target.value)}
                required
                autoFocus
                inputProps={{ minLength: 3, maxLength: 50 }}
                helperText={t('app.nameHelp')}
              />
              <Typography variant="body2" color="text.secondary">
                {t('app.zeroBalanceHelp')}
              </Typography>
              <LoadingButton type="submit" variant="contained" loading={busy}>
                {t('wallet.create.save')}
              </LoadingButton>
            </Stack>
          </Box>
        </Card>
      </Container>
    </>
  );
}
