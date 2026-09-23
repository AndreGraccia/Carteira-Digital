import { useState } from 'react';
import PropTypes from 'prop-types';
import { Box, Button, Chip, Collapse, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useSnackbar } from 'notistack';
import HttpService from '../../services/HttpService';
import { SectionCard, WalletIcon } from '../../components/wallet-ui';
import { dayLabel, errorKey, money } from '../../utils/wallet';

// Alternative answer offered next to the suggestion ("é outra renda", "é outra conta fixa").
const ALTERNATIVE = {
  SALARY: ['OTHER_INCOME', 'otherIncome'],
  OTHER_INCOME: ['SALARY', 'SALARY'],
  RENT: ['BILL', 'otherBill'],
  SUBSCRIPTION: ['BILL', 'otherBill'],
  BILL: ['OTHER', 'OTHER'],
  OTHER: ['BILL', 'otherBill'],
};

// Open Finance account names usually already carry the institution ("Banco X • 8392").
const origin = ({ institution, accountName }) =>
  accountName && institution && !accountName.includes(institution)
    ? `${institution} · ${accountName}`
    : accountName || institution;

function Question({ item, onAnswered }) {
  const { t, i18n } = useTranslation();
  const { enqueueSnackbar } = useSnackbar();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const pending = item.status === 'PENDING';
  const suggestion = item.suggestedCategory;
  const [alternative, alternativeLabel] = ALTERNATIVE[suggestion] || ['OTHER', 'OTHER'];
  const first = item.occurrences[0];

  const answer = async (status, category) => {
    setBusy(true);
    try {
      await HttpService.putWithAuth('/future/recurrences/decision', { key: item.key, status, category });
      enqueueSnackbar(t('future.saved'), { variant: 'success' });
      onAnswered();
    } catch (failure) {
      enqueueSnackbar(t(errorKey(failure)), { variant: 'error' });
    } finally {
      setBusy(false);
    }
  };

  const evidence = t(item.fixedAmount ? 'future.evidence' : 'future.evidenceVaries', {
    amount: money(item.typicalAmount, i18n.language),
    day: item.typicalDay,
    count: item.occurrences.length,
    since: dayLabel(first.date, i18n.language, { month: 'long' }),
  });

  return (
    <Box sx={{ px: 3, py: 2.5, borderTop: '1px solid', borderColor: 'divider' }}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} justifyContent="space-between">
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="subtitle1" component="h3">
            {pending ? t(`future.questions.${suggestion}`, { name: item.counterparty }) : item.counterparty}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            {evidence}
          </Typography>
          <Stack direction="row" flexWrap="wrap" sx={{ mt: 1, gap: 1 }}>
            <Chip size="small" label={t(`future.confidence.${item.confidence}`)} />
            <Chip size="small" variant="outlined" label={origin(item)} />
            {!pending && (
              <Chip
                size="small"
                color={item.status === 'CONFIRMED' ? 'primary' : 'default'}
                variant={item.status === 'CONFIRMED' ? 'filled' : 'outlined'}
                label={`${t(`future.status.${item.status}`)}${
                  item.status === 'CONFIRMED' ? ` · ${t(`future.categories.${item.category}`)}` : ''
                }`}
              />
            )}
          </Stack>
        </Box>
        <Stack direction="row" flexWrap="wrap" alignItems="flex-start" sx={{ flexShrink: 0, gap: 1 }}>
          {pending ? (
            <>
              <Button variant="contained" disabled={busy} onClick={() => answer('CONFIRMED', suggestion)}>
                {t(`future.answers.${suggestion}`)}
              </Button>
              <Button variant="outlined" disabled={busy} onClick={() => answer('CONFIRMED', alternative)}>
                {t(`future.answers.${alternativeLabel}`)}
              </Button>
              <Button color="inherit" disabled={busy} onClick={() => answer('REJECTED', null)}>
                {t('future.answers.no')}
              </Button>
            </>
          ) : (
            <Button color="inherit" disabled={busy} onClick={() => answer('PENDING', null)}>
              {t('future.answers.undo')}
            </Button>
          )}
        </Stack>
      </Stack>
      <Button
        size="small"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        startIcon={<WalletIcon name="history" sx={{ fontSize: 18 }} />}
        sx={{ mt: 1, px: 0, color: 'text.secondary' }}
      >
        {t(open ? 'future.hideHistory' : 'future.showHistory')}
      </Button>
      <Collapse in={open} unmountOnExit>
        <Box
          component="table"
          sx={{ mt: 1, width: '100%', borderCollapse: 'collapse', '& td': { py: 0.75, fontSize: 14 } }}
        >
          <caption style={{ textAlign: 'left', captionSide: 'top', paddingBottom: 4 }}>
            <Typography variant="caption" color="text.secondary">
              {t('future.historyTitle')}
            </Typography>
          </caption>
          <tbody>
            {item.occurrences.map((occurrence) => (
              <tr key={`${occurrence.transactionId}-${occurrence.date}`}>
                <td>{dayLabel(occurrence.date, i18n.language, { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                  {item.direction === 'IN' ? '+ ' : '− '}
                  {money(occurrence.amount, i18n.language)}
                </td>
              </tr>
            ))}
          </tbody>
        </Box>
      </Collapse>
    </Box>
  );
}
Question.propTypes = { item: PropTypes.object.isRequired, onAnswered: PropTypes.func.isRequired };

export default function Questions({ recurrences, onAnswered }) {
  const { t } = useTranslation();
  const pending = recurrences.filter((item) => item.status === 'PENDING');
  const answered = recurrences.filter((item) => item.status !== 'PENDING');
  return (
    <SectionCard title={t('future.questionsTitle')}>
      <Typography variant="body2" color="text.secondary" sx={{ px: 3, pb: 2 }}>
        {pending.length ? t('future.questionsHelp') : t('future.noQuestions')}
      </Typography>
      {pending.map((item) => (
        <Question key={item.key} item={item} onAnswered={onAnswered} />
      ))}
      {answered.length > 0 && (
        <>
          <Typography
            variant="overline"
            color="text.secondary"
            component="h3"
            sx={{ display: 'block', px: 3, pt: 3, pb: 1 }}
          >
            {t('future.answeredTitle')}
          </Typography>
          {answered.map((item) => (
            <Question key={item.key} item={item} onAnswered={onAnswered} />
          ))}
        </>
      )}
    </SectionCard>
  );
}
Questions.propTypes = { recurrences: PropTypes.array.isRequired, onAnswered: PropTypes.func.isRequired };
