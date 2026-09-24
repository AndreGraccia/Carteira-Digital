import { useEffect, useState } from 'react';
import { Button, FormControlLabel, Link, Stack, Switch, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useSnackbar } from 'notistack';
import HttpService from '../../services/HttpService';
import { errorKey } from '../../utils/wallet';
import { currentSubscription, keyBytes, pushSupported, staleKey } from '../../utils/push';

/**
 * Opt-in: one alert per new negative window, calculated by the same engine as this page. It goes by
 * e-mail once the address is verified and to every browser the user turned on here.
 */
export default function AlertToggle() {
  const { t } = useTranslation();
  const { enqueueSnackbar } = useSnackbar();
  const [enabled, setEnabled] = useState(null);
  const [deliverable, setDeliverable] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  // null: push off on the server or unsupported here; otherwise {publicKey, here}.
  const [push, setPush] = useState(null);

  useEffect(() => {
    let active = true;
    // On failure the switch stays disabled: showing "off" for a user who turned it on would be a lie.
    HttpService.getWithAuth('/future/alerts')
      .then((settings) => {
        if (!active) return;
        setEnabled(settings.enabled);
        setDeliverable(settings.deliverable !== false);
      })
      .catch((failure) => active && setError(errorKey(failure)));
    if (pushSupported()) {
      HttpService.getWithAuth('/future/push')
        .then(async (settings) => {
          if (!settings.publicKey) return;
          let existing = await currentSubscription();
          // Made with an old server key: the push service will refuse it forever, so start over.
          if (existing && staleKey(existing, settings.publicKey)) {
            await existing.unsubscribe().catch(() => {});
            existing = null;
          }
          let here = false;
          // Browsers rotate subscriptions; re-sending keeps the server copy current. "refresh" never takes a
          // browser that belongs to another account (a shared device after someone else logged in).
          if (existing && Notification.permission === 'granted') {
            const state = await HttpService.postWithAuth('/future/push/subscriptions', {
              ...existing.toJSON(),
              refresh: true,
            }).catch(() => null);
            here = Boolean(state?.active);
          }
          if (active) setPush({ publicKey: settings.publicKey, here });
        })
        .catch(() => {});
    }
    return () => {
      active = false;
    };
  }, []);

  const change = async (value) => {
    setBusy(true);
    try {
      const settings = await HttpService.putWithAuth('/future/alerts', { enabled: value });
      setEnabled(settings.enabled);
      setDeliverable(settings.deliverable !== false);
      setError(null);
      enqueueSnackbar(t(value ? 'future.alerts.on' : 'future.alerts.off'), { variant: 'success' });
    } catch (failure) {
      const key = errorKey(failure);
      if (key === 'app.errors.ALERT_EMAIL_NOT_VERIFIED') setError(key);
      else enqueueSnackbar(t(key), { variant: 'error' });
    } finally {
      setBusy(false);
    }
  };

  const turnOnHere = async () => {
    setBusy(true);
    try {
      if ((await Notification.requestPermission()) !== 'granted') {
        enqueueSnackbar(t('future.alerts.pushBlocked'), { variant: 'warning' });
        return;
      }
      const registration = await navigator.serviceWorker.register('/sw.js');
      await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: keyBytes(push.publicKey),
      });
      await HttpService.postWithAuth('/future/push/subscriptions', subscription.toJSON());
      setPush({ ...push, here: true });
      setDeliverable(true);
      setError(null);
      enqueueSnackbar(t('future.alerts.pushOn'), { variant: 'success' });
    } catch (failure) {
      enqueueSnackbar(t(failure.response ? errorKey(failure) : 'future.alerts.pushFailed'), { variant: 'error' });
    } finally {
      setBusy(false);
    }
  };

  const turnOffHere = async () => {
    setBusy(true);
    try {
      const subscription = await currentSubscription();
      if (subscription) {
        await HttpService.postWithAuth('/future/push/unsubscribe', { endpoint: subscription.endpoint });
        await subscription.unsubscribe();
      }
      setPush({ ...push, here: false });
      // It may have been the only channel: ask the server instead of guessing.
      const settings = await HttpService.getWithAuth('/future/alerts').catch(() => null);
      if (settings) setDeliverable(settings.deliverable !== false);
      enqueueSnackbar(t('future.alerts.pushOff'), { variant: 'success' });
    } catch (failure) {
      enqueueSnackbar(t(errorKey(failure)), { variant: 'error' });
    } finally {
      setBusy(false);
    }
  };

  // On but with nowhere to go: say it, instead of a switch that silently does nothing.
  const silent = enabled && !deliverable;
  const message = error || (silent ? 'future.alerts.silent' : null);

  return (
    <Stack spacing={0.5}>
      <FormControlLabel
        control={
          <Switch
            checked={Boolean(enabled)}
            disabled={enabled === null || busy}
            onChange={(event) => change(event.target.checked)}
            inputProps={{ 'aria-describedby': 'alerts-help' }}
          />
        }
        label={t('future.alerts.label')}
      />
      <Typography id="alerts-help" variant="caption" color="text.secondary" sx={{ pl: 6 }}>
        {t('future.alerts.help')}
      </Typography>
      {push && (
        <Stack direction="row" alignItems="center" flexWrap="wrap" sx={{ pl: 6, gap: 1 }}>
          <Typography variant="caption" color="text.secondary">
            {t(push.here ? 'future.alerts.pushHere' : 'future.alerts.pushOffer')}
          </Typography>
          <Button size="small" disabled={busy} onClick={push.here ? turnOffHere : turnOnHere} sx={{ py: 0 }}>
            {t(push.here ? 'future.alerts.pushTurnOff' : 'future.alerts.pushTurnOn')}
          </Button>
        </Stack>
      )}
      {message && (
        <Typography variant="caption" color="warning.main" role="status" sx={{ pl: 6 }}>
          {t(message)}{' '}
          {(message === 'app.errors.ALERT_EMAIL_NOT_VERIFIED' || silent) && (
            <Link component={RouterLink} to="/open-finance">
              {t('future.alerts.verify')}
            </Link>
          )}
        </Typography>
      )}
    </Stack>
  );
}
