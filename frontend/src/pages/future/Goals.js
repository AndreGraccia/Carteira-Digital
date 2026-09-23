import { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Alert, Box, Button, IconButton, Stack, TextField, Tooltip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useSnackbar } from 'notistack';
import HttpService from '../../services/HttpService';
import { SectionCard, WalletIcon } from '../../components/wallet-ui';
import { dayLabel, errorKey, money, parseAmount } from '../../utils/wallet';

const EMPTY = { name: '', amount: '', day: '' };

/**
 * Monthly goals the user sets. Each one leaves the spendable balance on its day, so the daily limit
 * already answers "how much can I spend without giving up the goal?".
 */
export default function Goals({ onChanged }) {
  const { t, i18n } = useTranslation();
  const { enqueueSnackbar } = useSnackbar();
  const lang = i18n.language;
  const [goals, setGoals] = useState([]);
  const [error, setError] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setGoals(await HttpService.getWithAuth('/future/goals'));
      setError(null);
    } catch (failure) {
      setError(errorKey(failure));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const cents = parseAmount(form.amount);
  const day = Number(form.day);
  const dayValid = Number.isInteger(day) && day >= 1 && day <= 31;
  const valid = form.name.trim() && cents !== null && cents > 0 && dayValid;

  const run = async (action, message) => {
    setBusy(true);
    try {
      await action();
      enqueueSnackbar(t(message), { variant: 'success' });
      await load();
      onChanged();
    } catch (failure) {
      enqueueSnackbar(t(errorKey(failure)), { variant: 'error' });
    } finally {
      setBusy(false);
    }
  };

  const add = (event) => {
    event.preventDefault();
    if (!valid || busy) return;
    run(async () => {
      await HttpService.postWithAuth('/future/goals', {
        name: form.name.trim(),
        monthlyAmount: cents / 100,
        dayOfMonth: day,
      });
      setForm(EMPTY);
    }, 'future.goals.saved');
  };

  return (
    <SectionCard title={t('future.goals.title')}>
      <Typography variant="body2" color="text.secondary" sx={{ px: 3, pb: 2 }}>
        {t('future.goals.help')}
      </Typography>
      {error && (
        <Alert
          severity="error"
          sx={{ mx: 3, mb: 2 }}
          action={
            <Button color="inherit" onClick={load}>
              {t('app.retry')}
            </Button>
          }
        >
          {t(error)}
        </Alert>
      )}
      {!error && !goals.length && (
        <Typography variant="body2" sx={{ px: 3, pb: 2 }}>
          {t('future.goals.empty')}
        </Typography>
      )}
      {goals.map((goal) => (
        <Stack
          key={goal.id}
          direction="row"
          alignItems="center"
          spacing={2}
          sx={{ px: 3, py: 1.5, borderTop: '1px solid', borderColor: 'divider' }}
        >
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography variant="subtitle2" noWrap>
              {goal.name}
            </Typography>
            <Typography variant="caption" color="text.secondary" component="p">
              {t('future.goals.perMonth', { amount: money(goal.monthlyAmount, lang), day: goal.dayOfMonth })}
              {goal.nextDate ? ` · ${t('future.goals.next', { date: dayLabel(goal.nextDate, lang) })}` : ''}
            </Typography>
          </Box>
          <Tooltip title={t('future.goals.remove')}>
            <span>
              <IconButton
                aria-label={t('future.goals.removeLabel', { name: goal.name })}
                disabled={busy}
                onClick={() => run(() => HttpService.deleteWithAuth(`/future/goals/${goal.id}`), 'future.goals.removed')}
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
          label={t('future.goals.name')}
          placeholder={t('future.goals.namePlaceholder')}
          value={form.name}
          onChange={(event) => setForm({ ...form, name: event.target.value })}
          inputProps={{ maxLength: 60 }}
          sx={{ flex: 2 }}
        />
        <TextField
          size="small"
          label={t('future.goals.amount')}
          value={form.amount}
          onChange={(event) => setForm({ ...form, amount: event.target.value })}
          inputProps={{ inputMode: 'decimal' }}
          error={form.amount !== '' && (cents === null || cents <= 0)}
          sx={{ flex: 1 }}
        />
        <TextField
          size="small"
          label={t('future.goals.day')}
          value={form.day}
          onChange={(event) => setForm({ ...form, day: event.target.value.replace(/\D/g, '').slice(0, 2) })}
          inputProps={{ inputMode: 'numeric' }}
          error={form.day !== '' && !dayValid}
          sx={{ width: { sm: 110 } }}
        />
        <Button type="submit" variant="contained" disabled={!valid || busy} sx={{ flexShrink: 0 }}>
          {t('future.goals.add')}
        </Button>
      </Stack>
    </SectionCard>
  );
}
Goals.propTypes = { onChanged: PropTypes.func.isRequired };
