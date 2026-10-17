import { useState } from 'react';
import PropTypes from 'prop-types';
import { Alert, Box, Button, Card, Stack, TextField, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import HttpService from '../../services/HttpService';
import { errorKey, money, parseAmount } from '../../utils/wallet';

// "In what order do I pay my loans off, and what does it save?" (ADR 0013). Reorders money already paid plus the
// extra the person types; nothing here offers credit.
function Row({ title, plan, saves, lang, t }) {
  return (
    <Box sx={{ py: 1.5, borderTop: '1px solid', borderColor: 'divider' }}>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={0.5}>
        <Typography variant="subtitle2">{title}</Typography>
        <Typography variant="body2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
          {plan.months == null
            ? t('commitments.payoff.never')
            : t('commitments.payoff.months', { count: plan.months, interest: money(plan.interest, lang) })}
        </Typography>
      </Stack>
      {saves != null && Number(saves) > 0 && (
        <Typography variant="caption" color="success.main" component="p">
          {t('commitments.payoff.saves', { amount: money(saves, lang) })}
        </Typography>
      )}
    </Box>
  );
}

Row.propTypes = {
  title: PropTypes.string.isRequired,
  plan: PropTypes.shape({
    months: PropTypes.number,
    interest: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  }).isRequired,
  saves: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  lang: PropTypes.string.isRequired,
  t: PropTypes.func.isRequired,
};

export default function DebtPayoff() {
  const { t, i18n } = useTranslation();
  const lang = i18n.language;
  const [extra, setExtra] = useState('');
  const [state, setState] = useState({ loading: false, error: null, answer: null });
  const cents = extra === '' ? 0 : parseAmount(extra);
  const invalid = cents === null || cents > 100000000;

  const calculate = async (event) => {
    event.preventDefault();
    setState({ loading: true, error: null, answer: null });
    try {
      const answer = await HttpService.getWithAuth(`/future/debt-payoff?extra=${cents / 100}`);
      setState({ loading: false, error: null, answer });
    } catch (failure) {
      setState({ loading: false, error: errorKey(failure), answer: null });
    }
  };

  const { answer } = state;
  const plan = answer?.plan;
  return (
    <Card component="section" aria-labelledby="debt-payoff" sx={{ p: 3 }}>
      <Typography id="debt-payoff" variant="h6" component="h2">
        {t('commitments.payoff.title')}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        {t('commitments.payoff.help')}
      </Typography>
      <Stack component="form" direction={{ xs: 'column', sm: 'row' }} spacing={1.5} onSubmit={calculate}>
        <TextField
          size="small"
          label={t('commitments.payoff.extra')}
          value={extra}
          onChange={(event) => setExtra(event.target.value)}
          inputProps={{ inputMode: 'decimal' }}
          error={invalid}
          helperText={invalid ? t('commitments.payoff.extraInvalid') : undefined}
        />
        <Button type="submit" variant="contained" disabled={invalid || state.loading} sx={{ flexShrink: 0 }}>
          {t('commitments.payoff.calculate')}
        </Button>
      </Stack>

      <Box aria-live="polite" sx={{ mt: 2 }}>
        {state.error && <Alert severity="error">{t(state.error)}</Alert>}
        {plan && plan.avalanche.order.length === 0 && (
          <Typography variant="body2">{t('commitments.payoff.noLoans')}</Typography>
        )}
        {plan && plan.avalanche.order.length > 0 && (
          <>
            <Row title={t('commitments.payoff.baseline')} plan={plan.baseline} lang={lang} t={t} />
            <Row
              title={t('commitments.payoff.avalanche')}
              plan={plan.avalanche}
              saves={plan.avalancheSaves}
              lang={lang}
              t={t}
            />
            <Row
              title={t('commitments.payoff.snowball')}
              plan={plan.snowball}
              saves={plan.snowballSaves}
              lang={lang}
              t={t}
            />
            <Typography variant="subtitle2" sx={{ mt: 2 }}>
              {t('commitments.payoff.orderTitle')}
            </Typography>
            <Box component="ol" sx={{ m: 0, pl: 3 }}>
              {plan.avalanche.order.map((payoff) => (
                <Typography component="li" variant="body2" key={payoff.id}>
                  {payoff.label}
                  {payoff.month != null ? `: ${t('commitments.payoff.endsIn', { count: payoff.month })}` : ''}
                </Typography>
              ))}
            </Box>
          </>
        )}
        {answer?.loansWithoutRate > 0 && (
          <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 1 }}>
            {t('commitments.payoff.withoutRate', { count: answer.loansWithoutRate })}
          </Typography>
        )}
        {plan && (
          <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 1 }}>
            {t('commitments.payoff.note')}
          </Typography>
        )}
      </Box>
    </Card>
  );
}
