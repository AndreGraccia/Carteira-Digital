import { useEffect, useState } from 'react';
import { FormControlLabel, Link, Stack, Switch, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useSnackbar } from 'notistack';
import HttpService from '../../services/HttpService';
import { errorKey } from '../../utils/wallet';

/** Opt-in: one e-mail per new negative window, calculated by the same engine as this page. */
export default function AlertToggle() {
  const { t } = useTranslation();
  const { enqueueSnackbar } = useSnackbar();
  const [enabled, setEnabled] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    // On failure the switch stays disabled: showing "off" for a user who turned it on would be a lie.
    HttpService.getWithAuth('/future/alerts')
      .then((settings) => active && setEnabled(settings.enabled))
      .catch((failure) => active && setError(errorKey(failure)));
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
