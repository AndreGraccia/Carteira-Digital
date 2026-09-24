import { useEffect, useState } from 'react';
import { Button, FormControlLabel, Link, Stack, Switch, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useSnackbar } from 'notistack';
import HttpService from '../../services/HttpService';
import { errorKey } from '../../utils/wallet';

const pushSupported = () =>
  typeof window !== 'undefined' && 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;

/** The VAPID key arrives as base64url; PushManager wants the raw bytes. */
function keyBytes(base64url) {
  const base64 = (base64url + '='.repeat((4 - (base64url.length % 4)) % 4)).replace(/-/g, '+').replace(/_/g, '/');
  return Uint8Array.from(window.atob(base64), (char) => char.charCodeAt(0));
}

async function currentSubscription() {
  const registration = await navigator.serviceWorker.getRegistration('/');
  return registration ? registration.pushManager.getSubscription() : null;
}

/**
 * Opt-in: one alert per new negative window, calculated by the same engine as this page. It goes by
 * e-mail once the address is verified and to every browser the user turned on here.
 */
export default function AlertToggle() {
  const { t } = useTranslation();
  const { enqueueSnackbar } = useSnackbar();
  const [enabled, setEnabled] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  // null: push off on the server or unsupported here; otherwise {publicKey, here}.
  const [push, setPush] = useState(null);

  useEffect(() => {
    let active = true;
    // On failure the switch stays disabled: showing "off" for a user who turned it on would be a lie.
    HttpService.getWithAuth('/future/alerts')
      .then((settings) => active && setEnabled(settings.enabled))
      .catch((failure) => active && setError(errorKey(failure)));
    if (pushSupported()) {
      HttpService.getWithAuth('/future/push')
        .then(async (settings) => {
          if (!settings.publicKey) return;
          const existing = await currentSubscription();
          // Browsers rotate subscriptions; sending it again on each visit keeps the server's copy current.
          if (existing && Notification.permission === 'granted')
            await HttpService.postWithAuth('/future/push/subscriptions', existing.toJSON()).catch(() => {});
          if (active) setPush({ publicKey: settings.publicKey, here: Boolean(existing) });
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
      enqueueSnackbar(t('future.alerts.pushOff'), { variant: 'success' });
    } catch (failure) {
      enqueueSnackbar(t(errorKey(failure)), { variant: 'error' });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Stack spacing={0.5}>
      <FormControlLabel
        control={
          <Switch
            checked={Boolean(enabled)}
            disabled={enabled === null || busy}
            onChange={(event) => change(event.target.checked)}
          />
        }
        label={t('future.alerts.label')}
      />
      <Typography variant="caption" color="text.secondary" sx={{ pl: 6 }}>
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
      {error && (
        <Typography variant="caption" color="error.main" role="alert" sx={{ pl: 6 }}>
          {t(error)}{' '}
          {error === 'app.errors.ALERT_EMAIL_NOT_VERIFIED' && (
            <Link component={RouterLink} to="/open-finance">
              {t('future.alerts.verify')}
            </Link>
          )}
        </Typography>
      )}
    </Stack>
  );
}
