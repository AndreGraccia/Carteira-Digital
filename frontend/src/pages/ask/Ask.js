import { useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { Link as RouterLink } from 'react-router-dom';
import { Alert, Box, Button, Card, Chip, Container, Stack, TextField, Typography } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import HttpService from '../../services/HttpService';
import { EventRow, ExitPlanList } from '../../components/future-ui';
import { PageHeading } from '../../components/wallet-ui';
import { dayLabel, errorKey, money, overdraftCostText } from '../../utils/wallet';
import { forecastReady, limitSummary } from '../../utils/forecast';

const SUGGESTIONS = ['s1', 's2', 's3', 's4', 's5'];
const VERDICT_COLOR = { SAFE: 'success', TIGHT: 'warning', RISK: 'error' };

/** Builds the sentence for each intent from engine numbers only. */
function AnswerBody({ answer }) {
  const { t, i18n } = useTranslation();
  const lang = i18n.language;
  const line = (text) => (
    <Typography variant="body2" sx={{ mt: 1 }}>
      {text}
    </Typography>
  );

  switch (answer.intent) {
    case 'PURCHASE':
      if (answer.verdict === 'NEED_AMOUNT') return line(t('ask.needAmount'));
      if (answer.verdict === 'INVALID_INSTALLMENTS') return line(t('ask.invalidInstallments', { max: 24 }));
      return (
        <>
          <Chip
            color={VERDICT_COLOR[answer.verdict]}
            label={t(`ask.verdict.${answer.verdict}`)}
            sx={{ fontWeight: 600 }}
          />
          {line(
            t('ask.purchaseMin', {
              before: money(answer.before.minBalance90, lang),
              after: money(answer.after.minBalance90, lang),
            })
          )}
          {line(t('ask.purchaseDays', { before: answer.before.negativeDays, after: answer.after.negativeDays }))}
          {answer.installments > 1 &&
            Number(answer.newMonthlyCommitment) > 0 &&
            line(t('ask.purchaseMonthly', { amount: money(answer.newMonthlyCommitment, lang) }))}
          {answer.cardName && line(t('ask.purchaseCard', { card: answer.cardName }))}
        </>
      );
    case 'DAILY_LIMIT': {
      const limit = answer.spendingLimit;
      if (!forecastReady({ sources: answer.sources, limit })) return line(t('ask.dailyNotReady'));
      const summary = limitSummary(limit);
      const values = { date: dayLabel(limit.until, lang), amount: money(summary.amount, lang) };
      if (summary.kind === 'short') return line(t('ask.dailyShort', values));
      if (summary.kind === 'shortEnvelopes') return line(t('ask.dailyShortEnvelopes', values));
      return line(t('ask.dailyOk', values));
    }
    case 'WHY_NEGATIVE':
      if (!answer.risk) return line(t('ask.whyNone'));
      return (
        <>
          {line(
            t('ask.why', {
              start: dayLabel(answer.risk.start, lang),
              end: dayLabel(answer.risk.end, lang),
              amount: money(answer.risk.lowestBalance, lang),
            })
          )}
          <Box sx={{ mt: 1 }}>
            {answer.risk.causes.map((event) => (
              <EventRow key={`${event.reference}-${event.date}`} event={event} dense />
            ))}
          </Box>
          {Number(answer.risk.overdraftCostCap) > 0 &&
            line(overdraftCostText(t, answer.risk, lang))}
          <ExitPlanList actions={answer.exitPlan} />
        </>
      );
    case 'COMMITTED':
      return line(
        t('ask.committed', {
          total: money(answer.committed.total, lang),
          date: dayLabel(answer.committed.until, lang),
          confirmed: money(answer.committed.confirmed, lang),
          estimated: money(answer.committed.estimated, lang),
        })
      );
    case 'NEXT_INCOME':
      return line(
        answer.nextIncome
          ? t('ask.nextIncome', {
              label: answer.nextIncome.label,
              date: dayLabel(answer.nextIncome.date, lang),
              amount: money(answer.nextIncome.amount, lang),
            })
          : t('ask.noIncome')
      );
    default:
      return line(t('ask.unknown'));
  }
}
AnswerBody.propTypes = { answer: PropTypes.object.isRequired };

export default function Ask() {
  const { t } = useTranslation();
  const [question, setQuestion] = useState('');
  const [turns, setTurns] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const input = useRef(null);

  const ask = async (text) => {
    const trimmed = text.trim();
    if (!trimmed || busy) return;
    setBusy(true);
    setError(null);
    try {
      const answer = await HttpService.postWithAuth('/future/ask', { question: trimmed });
      setTurns((previous) => [{ id: Date.now(), question: trimmed, answer }, ...previous]);
      setQuestion('');
    } catch (failure) {
      setError(errorKey(failure));
    } finally {
      setBusy(false);
      input.current?.focus();
    }
  };

  return (
    <>
      <Helmet>
        <title>{t('ask.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="md">
        <PageHeading title={t('ask.heading')} description={t('ask.description')} />

        <Card sx={{ p: { xs: 2, md: 3 } }}>
          <Stack
            component="form"
            direction={{ xs: 'column', sm: 'row' }}
            sx={{ gap: 1.5 }}
            onSubmit={(event) => {
              event.preventDefault();
              ask(question);
            }}
          >
            <TextField
              inputRef={input}
              fullWidth
              label={t('ask.label')}
              placeholder={t('ask.placeholder')}
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              inputProps={{ maxLength: 300 }}
              autoComplete="off"
            />
            <Button type="submit" variant="contained" disabled={busy || !question.trim()} sx={{ flexShrink: 0, px: 3 }}>
              {busy ? t('ask.thinking') : t('ask.send')}
            </Button>
          </Stack>
          <Typography variant="overline" color="text.secondary" component="p" sx={{ mt: 2 }}>
            {t('ask.suggestions')}
          </Typography>
          <Stack direction="row" flexWrap="wrap" sx={{ gap: 1 }}>
            {SUGGESTIONS.map((key) => (
              <Chip key={key} label={t(`ask.${key}`)} onClick={() => ask(t(`ask.${key}`))} disabled={busy} />
            ))}
          </Stack>
        </Card>

        {error && (
          <Alert severity="error" sx={{ mt: 3 }}>
            {t(error)}
          </Alert>
        )}

        <Box aria-live="polite" sx={{ mt: 3 }}>
          {busy && (
            <Typography role="status" color="text.secondary" sx={{ mb: 2 }}>
              {t('ask.thinking')}
            </Typography>
          )}
          {!turns.length && !busy && <Typography color="text.secondary">{t('ask.empty')}</Typography>}
          <Stack spacing={2}>
            {turns.map((turn) => (
              <Card key={turn.id} component="article" sx={{ p: { xs: 2.5, md: 3 } }}>
                <Typography variant="overline" color="text.secondary" component="p">
                  {t('ask.you')}
                </Typography>
                <Typography variant="subtitle1" component="h2" sx={{ mb: 1.5 }}>
                  {turn.question}
                </Typography>
                <AnswerBody answer={turn.answer} />
                {turn.answer.intent !== 'UNKNOWN' && (
                  <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    justifyContent="space-between"
                    alignItems={{ sm: 'center' }}
                    sx={{ mt: 2, pt: 2, gap: 1, borderTop: '1px solid', borderColor: 'divider' }}
                  >
                    <Typography variant="caption" color="text.secondary">
                      {t('ask.source')}
                    </Typography>
                    <Button component={RouterLink} to="/future" size="small" sx={{ flexShrink: 0 }}>
                      {t('ask.openFuture')}
                    </Button>
                  </Stack>
                )}
              </Card>
            ))}
          </Stack>
        </Box>
      </Container>
    </>
  );
}
