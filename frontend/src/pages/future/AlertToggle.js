import { useEffect, useState } from 'react';
import { FormControlLabel, Stack, Switch, Typography } from '@mui/material';
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

  useEffect(() => {
    let active = true;
    HttpService.getWithAuth('/future/alerts')
      .then((settings) => active && setEnabled(settings.enabled))
      .catch(() => active && setEnabled(false));
    return () => {
      active = false;
    };
  }, []);

  const change = async (value) => {
    setBusy(true);
    try {
      const settings = await HttpService.putWithAuth('/future/alerts', { enabled: value });
      setEnabled(settings.enabled);
      enqueueSnackbar(t(value ? 'future.alerts.on' : 'future.alerts.off'), { variant: 'success' });
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
          <Switch checked={Boolean(enabled)} disabled={enabled === null || busy} onChange={(event) => change(event.target.checked)} />
        }
        label={t('future.alerts.label')}
      />
      <Typography variant="caption" color="text.secondary" sx={{ pl: 6 }}>
        {t('future.alerts.help')}
      </Typography>
    </Stack>
  );
}
