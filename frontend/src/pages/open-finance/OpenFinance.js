import { alpha } from '@mui/material/styles';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { enqueueSnackbar } from 'notistack';
import { PageHeading, PageSkeleton, WalletIcon, IconTile } from '../../components/wallet-ui';
import { useWallets } from '../../context/WalletContext';
import HttpService from '../../services/HttpService';
import { dateTime, errorKey } from '../../utils/wallet';

export default function OpenFinance() {
  const { t, i18n } = useTranslation();
  const { refresh } = useWallets();
  const [status, setStatus] = useState(null);
  const [verification, setVerification] = useState(null);
  const [code, setCode] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [clock, setClock] = useState(Date.now());
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);
  const [error, setError] = useState(null);
  const [verificationError, setVerificationError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const nextStatus = await HttpService.getWithAuth('/open-finance/status');
      setStatus(nextStatus);
      setVerification(nextStatus.verification?.pending ? nextStatus.verification : null);
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

  useEffect(() => {
    if (!dialogOpen && !verification?.pending) return undefined;
    setClock(Date.now());
    const timer = window.setInterval(() => setClock(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [dialogOpen, verification?.pending]);

  const resendSeconds = useMemo(() => {
    if (!verification?.resendAvailableAt) return 0;
    return Math.max(0, Math.ceil((new Date(verification.resendAvailableAt).getTime() - clock) / 1000));
  }, [clock, verification]);

  const verificationActive = verification?.pending && new Date(verification.expiresAt).getTime() > clock;

  const notifySync = (result) => {
    enqueueSnackbar(
      t('openFinance.syncSuccess', {
        accounts: result.accountsSynced,
        transactions: result.newTransactions,
      }),
      { variant: 'success' }
    );
  };

  const sync = async () => {
    if (busyRef.current) return;
    busyRef.current = true;
    setBusy(true);
    setError(null);
    try {
      const result = await HttpService.postWithAuth('/open-finance/sync', {});
      await Promise.all([load(), refresh()]);
      notifySync(result);
    } catch (failure) {
      await load();
      setError(errorKey(failure));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  };

  const requestCode = async () => {
    if (busyRef.current) return;
    if (verification?.pending && new Date(verification.expiresAt).getTime() > Date.now()) {
      setVerificationError(null);
      setDialogOpen(true);
      return;
    }
    busyRef.current = true;
    setBusy(true);
    setError(null);
    try {
      const challenge = await HttpService.postWithAuth('/open-finance/connection/challenge', {});
      setVerification({ ...challenge, pending: true });
      setCode('');
      setVerificationError(null);
      setDialogOpen(true);
      enqueueSnackbar(t('openFinance.verification.sent'), { variant: 'success' });
      await load();
    } catch (failure) {
      await load();
      setError(errorKey(failure));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  };

  const resendCode = async () => {
    if (busyRef.current || resendSeconds > 0) return;
    busyRef.current = true;
    setBusy(true);
    setVerificationError(null);
    try {
      const challenge = await HttpService.postWithAuth('/open-finance/connection/challenge', {});
      setVerification({ ...challenge, pending: true });
      setCode('');
      setClock(Date.now());
      enqueueSnackbar(t('openFinance.verification.resent'), { variant: 'success' });
      await load();
    } catch (failure) {
      setVerificationError(errorKey(failure));
      await load();
      applyRetryDelay(failure);
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  };

  const applyRetryDelay = (failure) => {
    const seconds = Number(failure.response?.headers?.['retry-after']);
    if (seconds > 0) {
      setClock(Date.now());
      setVerification((current) => ({
        ...current,
        resendAvailableAt: new Date(Date.now() + seconds * 1000).toISOString(),
      }));
    }
  };

  const confirmConnection = async () => {
    if (busyRef.current || !/^\d{8}$/.test(code) || !verificationActive) return;
    busyRef.current = true;
    setBusy(true);
    setVerificationError(null);
    try {
      const result = await HttpService.postWithAuth('/open-finance/connection/confirm', { code });
      setDialogOpen(false);
      setVerification(null);
      setCode('');
      await Promise.all([load(), refresh()]);
      notifySync(result);
    } catch (failure) {
      setVerificationError(errorKey(failure));
      await load();
      applyRetryDelay(failure);
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  };

  const disconnect = async () => {
    if (busyRef.current) return;
    busyRef.current = true;
    setBusy(true);
    setError(null);
    try {
      await HttpService.deleteWithAuth('/open-finance/connection');
      setVerification(null);
      setDialogOpen(false);
      await load();
      enqueueSnackbar(t('openFinance.revoked'), { variant: 'success' });
    } catch (failure) {
      await load();
      setError(errorKey(failure));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  };

  const connected = status?.status === 'AUTHORISED';
  const pending = !connected && verificationActive;
  const connectionStatus = pending
    ? 'PENDING_EMAIL_VERIFICATION'
    : status?.status === 'PENDING_EMAIL_VERIFICATION'
    ? 'DISCONNECTED'
    : status?.status || 'DISCONNECTED';
  const statusColor = connected ? 'success' : pending ? 'warning' : 'default';

  return (
    <>
      <Helmet>
        <title>{t('openFinance.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="lg">
        <PageHeading title={t('openFinance.heading')} description={t('openFinance.description')} />
        {error && (
          <Alert
            severity="error"
            sx={{ mb: 3 }}
            action={
              // Without a status there is nothing else on screen to act on: offer the retry here.
              status ? undefined : (
                <Button color="inherit" onClick={load} disabled={loading}>
                  {t('app.retry')}
                </Button>
              )
            }
          >
            {t(error)}
          </Alert>
        )}
        <Grid container spacing={3}>
          <Grid item xs={12} md={7}>
            {/* No status yet: nothing is known about the connection, so the card claims nothing. */}
            {!status && loading && <PageSkeleton rows={0} />}
            {status && (
              <Card sx={{ p: { xs: 3, sm: 4 }, overflow: 'hidden', position: 'relative' }}>
                <Box
                  sx={{
                    position: 'absolute',
                    width: 220,
                    height: 220,
                    borderRadius: '50%',
                    background: (theme) =>
                      `radial-gradient(circle, ${alpha(theme.palette.brand.horizon, 0.16)}, transparent 68%)`,
                    right: -70,
                    top: -90,
                  }}
                />
                <Stack spacing={3} sx={{ position: 'relative' }}>
                  <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={2}>
                    <IconTile name="bank" />
                    <Chip color={statusColor} label={t(`openFinance.status.${connectionStatus}`)} />
                  </Stack>
                  <Box>
                    <Typography variant="overline" color="text.secondary">
                      {status.simulated ? t('openFinance.contractSandbox') : t('openFinance.participantSandbox')}
                    </Typography>
                    <Typography variant="h4" sx={{ mt: 0.5 }}>
                      {status.institutionName || t('openFinance.bankPlaceholder')}
                    </Typography>
                    <Typography color="text.secondary" sx={{ mt: 1.5, maxWidth: 580 }}>
                      {t(status.simulated ? 'openFinance.notice.contract' : 'openFinance.notice.participant')}
                    </Typography>
                  </Box>
                  {connected && (
                    <Stack
                      direction={{ xs: 'column', sm: 'row' }}
                      spacing={3}
                      divider={<Divider orientation="vertical" flexItem />}
                    >
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
                    <LoadingButton
                      loading={busy || loading}
                      variant="contained"
                      onClick={connected ? sync : requestCode}
                      startIcon={<WalletIcon name={connected ? 'sync' : 'shield'} />}
                    >
                      {t(
                        connected
                          ? 'openFinance.sync'
                          : pending
                          ? 'openFinance.verification.enterCode'
                          : 'openFinance.connect'
                      )}
                    </LoadingButton>
                    {connected && (
                      <Button color="inherit" disabled={busy} onClick={disconnect}>
                        {t('openFinance.disconnect')}
                      </Button>
                    )}
                  </Stack>
                </Stack>
              </Card>
            )}
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

      <Dialog
        open={dialogOpen}
        onClose={busy ? undefined : () => setDialogOpen(false)}
        fullWidth
        maxWidth="xs"
        PaperProps={{ sx: { borderRadius: 4, p: 1 } }}
      >
        <DialogTitle>{t('openFinance.verification.title')}</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            <Typography color="text.secondary">
              {t('openFinance.verification.sentTo', { email: verification?.maskedEmail || '' })}
            </Typography>
            {verificationError && <Alert severity="error">{t(verificationError)}</Alert>}
            {!verificationActive && !verificationError && (
              <Alert severity="warning">{t('app.errors.OPEN_FINANCE_CODE_EXPIRED')}</Alert>
            )}
            <TextField
              autoFocus
              fullWidth
              label={t('openFinance.verification.code')}
              value={code}
              disabled={busy || !verificationActive}
              onKeyDown={(event) => {
                if (event.key === 'Enter') confirmConnection();
              }}
              onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 8))}
              autoComplete="one-time-code"
              inputProps={{ inputMode: 'numeric', pattern: '[0-9]*', maxLength: 8 }}
              helperText={t('openFinance.verification.codeHelp')}
            />
            <Alert severity="warning" icon={<WalletIcon name="shield" />}>
              {t('openFinance.verification.safety')}
            </Alert>
            {verification?.expiresAt && (
              <Typography variant="caption" color="text.secondary">
                {t('openFinance.verification.expires', {
                  date: dateTime(verification.expiresAt, i18n.language),
                })}
              </Typography>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5, justifyContent: 'space-between' }}>
          <Button disabled={busy || resendSeconds > 0} onClick={resendCode}>
            {resendSeconds > 0
              ? t('openFinance.verification.resendIn', { seconds: resendSeconds })
              : t('openFinance.verification.resend')}
          </Button>
          <Stack direction="row" spacing={1}>
            <Button color="inherit" disabled={busy} onClick={() => setDialogOpen(false)}>
              {t('common.cancel')}
            </Button>
            <LoadingButton
              loading={busy}
              disabled={code.length !== 8 || !verificationActive}
              variant="contained"
              onClick={confirmConnection}
            >
              {t('openFinance.verification.confirm')}
            </LoadingButton>
          </Stack>
        </DialogActions>
      </Dialog>
    </>
  );
}
