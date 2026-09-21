import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  Container,
  Divider,
  Grid,
  Stack,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { enqueueSnackbar } from 'notistack';
import { PageHeading, WalletIcon } from '../../components/wallet-ui';
import { useWallets } from '../../context/WalletContext';
import HttpService from '../../services/HttpService';
import { dateTime, errorKey } from '../../utils/wallet';

export default function OpenFinance() {
  const { t, i18n } = useTranslation();
  const { refresh } = useWallets();
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setStatus(await HttpService.getWithAuth('/open-finance/status'));
      setError(null);
    } catch (failure) {
      setError(errorKey(failure));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const sync = async () => {
    setBusy(true);
    setError(null);
    try {
      const result = await HttpService.postWithAuth('/open-finance/sync', {});
      await Promise.all([load(), refresh()]);
      enqueueSnackbar(
        t('openFinance.syncSuccess', { accounts: result.accountsSynced, transactions: result.newTransactions }),
        { variant: 'success' }
      );
    } catch (failure) {
      setError(errorKey(failure));
    } finally {
      setBusy(false);
    }
  };

  const disconnect = async () => {
    setBusy(true);
    setError(null);
    try {
      await HttpService.deleteWithAuth('/open-finance/connection');
      await load();
      enqueueSnackbar(t('openFinance.revoked'), { variant: 'success' });
    } catch (failure) {
      setError(errorKey(failure));
    } finally {
      setBusy(false);
    }
  };

  const connected = status?.status === 'AUTHORISED';
  return (
    <>
      <Helmet>
        <title>{t('openFinance.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="lg">
        <PageHeading title={t('openFinance.heading')} description={t('openFinance.description')} />
        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {t(error)}
          </Alert>
        )}
        <Grid container spacing={3}>
          <Grid item xs={12} md={7}>
            <Card sx={{ p: { xs: 3, sm: 4 }, overflow: 'hidden', position: 'relative' }}>
              <Box
                sx={{
                  position: 'absolute',
                  width: 220,
                  height: 220,
                  borderRadius: '50%',
                  background: 'radial-gradient(circle, rgba(0,113,227,.16), transparent 68%)',
                  right: -70,
                  top: -90,
                }}
              />
              <Stack spacing={3} sx={{ position: 'relative' }}>
                <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={2}>
                  <Box
                    sx={{
                      width: 54,
                      height: 54,
                      borderRadius: '18px',
                      bgcolor: '#1D1D1F',
                      color: 'white',
                      display: 'grid',
                      placeItems: 'center',
                    }}
                  >
                    <WalletIcon name="bank" />
                  </Box>
                  <Chip
                    color={connected ? 'success' : 'default'}
                    label={t(`openFinance.status.${status?.status || 'DISCONNECTED'}`)}
                  />
                </Stack>
                <Box>
                  <Typography variant="overline" color="text.secondary">
                    {status?.simulated ? t('openFinance.contractSandbox') : t('openFinance.participantSandbox')}
                  </Typography>
                  <Typography variant="h4" sx={{ mt: 0.5 }}>
                    {status?.institutionName || t('openFinance.bankPlaceholder')}
                  </Typography>
                  <Typography color="text.secondary" sx={{ mt: 1.5, maxWidth: 580 }}>
                    {status?.notice || t('openFinance.loading')}
                  </Typography>
                </Box>
                {connected && (
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3} divider={<Divider orientation="vertical" flexItem />}>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        {t('openFinance.accounts')}
                      </Typography>
                      <Typography variant="h5">{status.accountCount}</Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        {t('openFinance.lastSync')}
                      </Typography>
                      <Typography variant="subtitle1">{dateTime(status.lastSyncedAt, i18n.language)}</Typography>
                    </Box>
                  </Stack>
                )}
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                  <LoadingButton loading={busy || loading} variant="contained" onClick={sync} startIcon={<WalletIcon name="sync" />}>
                    {t(connected ? 'openFinance.sync' : 'openFinance.connect')}
                  </LoadingButton>
                  {connected && (
                    <Button color="inherit" disabled={busy} onClick={disconnect}>
                      {t('openFinance.disconnect')}
                    </Button>
                  )}
                </Stack>
              </Stack>
            </Card>
          </Grid>
          <Grid item xs={12} md={5}>
            <Stack spacing={3}>
              <Card sx={{ p: 3 }}>
                <Typography variant="h6">{t('openFinance.permissionsTitle')}</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 2.5 }}>
                  {t('openFinance.permissionsHelp')}
                </Typography>
                <Stack direction="row" gap={1} flexWrap="wrap">
                  {(status?.permissions || []).map((permission) => (
                    <Chip key={permission} size="small" label={t(`openFinance.permissions.${permission}`)} />
                  ))}
                </Stack>
              </Card>
              <Alert severity="info" icon={<WalletIcon name="shield" />}>
                <Typography variant="subtitle2">{t('openFinance.credentialTitle')}</Typography>
                <Typography variant="body2">{t('openFinance.credentialHelp')}</Typography>
              </Alert>
              <Button
                href="https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/17369300/Dados+do+Cliente+-+DC"
                target="_blank"
                rel="noreferrer"
                endIcon={<WalletIcon name="arrow" />}
              >
                {t('openFinance.officialDocs')}
              </Button>
            </Stack>
          </Grid>
        </Grid>
      </Container>
    </>
  );
}
