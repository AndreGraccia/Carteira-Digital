import { useCallback, useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  Container,
  Grid,
  IconButton,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { useSnackbar } from 'notistack';
import HttpService from '../../services/HttpService';
import { StatBlock } from '../../components/future-ui';
import { PageHeading, PageSkeleton, SectionCard, WalletIcon } from '../../components/wallet-ui';
import { errorKey, money, parseAmount, subtractMoney } from '../../utils/wallet';

const EMPTY = { name: '', amount: '' };
// Envelopes already restored from an undo, per page load.
const restored = new Set();
const MAX_CENTS = 100000000;

/**
 * Money set aside from today's balance for something the projection does not know (a trip, the car tax).
 * Nothing moves: the total becomes a floor, so the daily limit on the Future page is what can be spent
 * without touching the envelopes.
 */
export default function Envelopes() {
  const { t, i18n } = useTranslation();
  const { enqueueSnackbar, closeSnackbar } = useSnackbar();
  const lang = i18n.language;
  const [state, setState] = useState({ loading: true, error: null, data: null });
  const [form, setForm] = useState(EMPTY);
  const [busy, setBusy] = useState(false);
  const [tried, setTried] = useState(false);

  const load = useCallback(async () => {
    setState((previous) => ({ ...previous, loading: true, error: null }));
    try {
      setState({ loading: false, error: null, data: await HttpService.getWithAuth('/future/envelopes') });
    } catch (failure) {
      setState((previous) => ({ ...previous, loading: false, error: errorKey(failure) }));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const cents = parseAmount(form.amount);
  const amountInvalid = form.amount !== '' && (cents === null || cents <= 0 || cents > MAX_CENTS);
  const valid = form.name.trim() && cents !== null && !amountInvalid;
  const nameMissing = tried && !form.name.trim();
  const amountMissing = tried && form.amount === '';

  const run = async (action, message, snackbarAction) => {
    setBusy(true);
    try {
      await action();
      enqueueSnackbar(t(message), { variant: 'success', action: snackbarAction });
      await load();
    } catch (failure) {
      enqueueSnackbar(t(errorKey(failure)), { variant: 'error' });
    } finally {
      setBusy(false);
    }
  };

  const create = (body) => HttpService.postWithAuth('/future/envelopes', body);

  const add = (event) => {
    event.preventDefault();
    // The button stays enabled: a grey button does not say what is missing, the fields do.
    setTried(true);
    if (!valid || busy) return;
    setTried(false);
    run(async () => {
      await create({ name: form.name.trim(), amount: cents / 100 });
      setForm(EMPTY);
    }, 'envelopes.saved');
  };

  const remove = (envelope) =>
    run(
      () => HttpService.deleteWithAuth(`/future/envelopes/${envelope.id}`),
      'envelopes.removed',
      (key) => (
        <Button
          color="inherit"
          size="small"
          onClick={(event) => {
            // closeSnackbar only starts the exit animation: a double click would restore it twice.
            if (restored.has(envelope.id)) return;
            restored.add(envelope.id);
            event.currentTarget.disabled = true;
            closeSnackbar(key);
            run(() => create({ name: envelope.name, amount: envelope.amount }), 'envelopes.restored');
          }}
        >
          {t('future.answers.undo')}
        </Button>
      )
    );

  const { data } = state;
  const free = data ? subtractMoney(data.balance, data.reserved) : 0;

  return (
    <>
      <Helmet>
        <title>{t('envelopes.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="lg">
        <PageHeading title={t('envelopes.heading')} description={t('envelopes.description')} />

        {state.loading && !data && <PageSkeleton rows={3} />}
        {state.error && (
          <Alert
            severity="error"
            action={
              <Button color="inherit" onClick={load}>
                {t('app.retry')}
              </Button>
            }
          >
            {t(state.error)}
          </Alert>
        )}

        {data && (
          <Stack spacing={3}>
            <Card sx={{ p: { xs: 3, md: 4 } }}>
              <Grid container spacing={3}>
                <Grid item xs={12} md={4}>
                  <StatBlock
                    label={t('envelopes.balance')}
                    value={money(data.balance, lang)}
                    help={t('envelopes.balanceHelp')}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <StatBlock
                    label={t('envelopes.reserved')}
                    value={money(data.reserved, lang)}
                    help={t('envelopes.reservedHelp')}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <StatBlock
                    label={t('envelopes.free')}
                    value={money(free, lang)}
                    help={t('envelopes.freeHelp')}
                    negative={free < 0}
                  />
                </Grid>
              </Grid>
              <Button component={RouterLink} to="/future" size="small" sx={{ mt: 2, px: 0 }}>
                {t('envelopes.seeLimit')}
              </Button>
            </Card>

            {Number(data.unbacked) > 0 && (
              <Alert severity="warning">{t('envelopes.unbacked', { amount: money(data.unbacked, lang) })}</Alert>
            )}

            <SectionCard title={t('envelopes.listTitle')}>
              {!data.items.length && (
                <Typography variant="body2" sx={{ px: 3, pb: 2 }}>
                  {t('envelopes.empty')}
                </Typography>
              )}
              {data.items.map((envelope) => (
                <Stack
                  key={envelope.id}
                  direction="row"
                  alignItems="center"
                  spacing={2}
                  sx={{ px: 3, py: 1.5, borderTop: '1px solid', borderColor: 'divider' }}
                >
                  <Box sx={{ color: 'primary.main', display: 'grid' }}>
                    <WalletIcon name="envelope" />
                  </Box>
                  <Typography variant="subtitle2" noWrap sx={{ flex: 1, minWidth: 0 }}>
                    {envelope.name}
                  </Typography>
                  <Typography variant="subtitle2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                    {money(envelope.amount, lang)}
                  </Typography>
                  <Tooltip title={t('envelopes.remove')}>
                    <span>
                      <IconButton
                        aria-label={t('envelopes.removeLabel', { name: envelope.name })}
                        disabled={busy}
                        onClick={() => remove(envelope)}
                      >
                        <WalletIcon name="trash" />
                      </IconButton>
                    </span>
                  </Tooltip>
                </Stack>
              ))}
              <Stack
                component="form"
                direction={{ xs: 'column', sm: 'row' }}
                alignItems={{ sm: 'flex-start' }}
                onSubmit={add}
                sx={{ px: 3, py: 2.5, gap: 1.5, borderTop: '1px solid', borderColor: 'divider' }}
              >
                <TextField
                  size="small"
                  label={t('envelopes.name')}
                  placeholder={t('envelopes.namePlaceholder')}
                  value={form.name}
                  onChange={(event) => setForm({ ...form, name: event.target.value })}
                  inputProps={{ maxLength: 60 }}
                  error={nameMissing}
                  helperText={nameMissing ? t('envelopes.nameMissing') : undefined}
                  sx={{ flex: 2 }}
                />
                <TextField
                  size="small"
                  label={t('envelopes.amount')}
                  value={form.amount}
                  onChange={(event) => setForm({ ...form, amount: event.target.value })}
                  inputProps={{ inputMode: 'decimal' }}
                  error={amountInvalid || amountMissing}
                  helperText={amountInvalid || amountMissing ? t('future.goals.amountInvalid') : undefined}
                  sx={{ flex: 1 }}
                />
                <Button type="submit" variant="contained" disabled={busy} sx={{ flexShrink: 0 }}>
                  {t('envelopes.add')}
                </Button>
              </Stack>
              <Typography variant="caption" color="text.secondary" component="p" sx={{ px: 3, pb: 2.5 }}>
                {t('envelopes.help')}
              </Typography>
            </SectionCard>
          </Stack>
        )}
      </Container>
    </>
  );
}
