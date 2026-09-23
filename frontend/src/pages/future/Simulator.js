import { useState } from 'react';
import PropTypes from 'prop-types';
import {
  Alert,
  Box,
  Button,
  MenuItem,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { useTranslation } from 'react-i18next';
import HttpService from '../../services/HttpService';
import ProjectionChart from '../../components/projection-chart';
import { SectionCard } from '../../components/wallet-ui';
import { dayLabel, errorKey, money, parseAmount } from '../../utils/wallet';

const TYPES = ['PURCHASE', 'SAVINGS', 'INCOME_LOSS'];

/** Plain-language summary built from the engine's numbers. No AI: the same numbers, the same sentence. */
function summaryLines(result, request, t, language) {
  const lines = [];
  const { scenario } = result;
  if (scenario.negativeDays > 0) {
    lines.push(
      t('future.summaryNegative', {
        count: scenario.negativeDays,
        date: dayLabel(scenario.firstNegativeDate, language),
        amount: money(scenario.minBalance90, language),
      })
    );
  } else {
    lines.push(
      t('future.summaryPositive', {
        amount: money(scenario.minBalance90, language),
        date: dayLabel(scenario.minDate, language),
      })
    );
  }
  if (result.installments > 1) {
    const values = { amount: money(result.newMonthlyCommitment, language), count: result.installments, card: result.cardName };
    lines.push(t(result.cardName ? 'future.summaryInstallments' : 'future.summaryInstallmentsNoCard', values));
  }
  if (request.type === 'SAVINGS') lines.push(t('future.summarySavings', { amount: money(result.newMonthlyCommitment, language) }));
  return lines;
}

export default function Simulator({ incomes, hasCard }) {
  const theme = useTheme();
  const { t, i18n } = useTranslation();
  const [type, setType] = useState('PURCHASE');
  const [form, setForm] = useState({
    label: '',
    amount: '',
    method: hasCard ? 'CARD' : 'CASH',
    installments: 1,
    dayOfMonth: 6,
    incomeKey: '',
    months: 2,
  });
  const [state, setState] = useState({ loading: false, error: null, result: null, request: null });
  const set = (field) => (event) => setForm({ ...form, [field]: event.target.value });

  const amountCents = parseAmount(form.amount);
  const needsAmount = type !== 'INCOME_LOSS';
  const blocked = (needsAmount && !amountCents) || (type === 'INCOME_LOSS' && !incomes.length);

  const simulate = async (event) => {
    event.preventDefault();
    if (blocked) return;
    const request = { type };
    if (type === 'PURCHASE') {
      Object.assign(request, {
        amount: amountCents / 100,
        installments: Number(form.installments),
        method: form.method,
        label: form.label.trim() || undefined,
      });
    }
    if (type === 'SAVINGS') Object.assign(request, { amount: amountCents / 100, dayOfMonth: Number(form.dayOfMonth) });
    if (type === 'INCOME_LOSS') Object.assign(request, { months: Number(form.months), incomeKey: form.incomeKey || undefined });
    setState({ loading: true, error: null, result: null, request });
    try {
      const result = await HttpService.postWithAuth('/future/scenarios', request);
      setState({ loading: false, error: null, result, request });
    } catch (failure) {
      setState({ loading: false, error: errorKey(failure), result: null, request });
    }
  };

  const { result } = state;
  const rows = result && [
    ['min30', result.baseline.minBalance30, result.scenario.minBalance30, true],
    ['min90', result.baseline.minBalance90, result.scenario.minBalance90, true],
    ['negativeDays', result.baseline.negativeDays, result.scenario.negativeDays, false],
    ['monthlyCommitment', 0, result.newMonthlyCommitment, true],
  ];

  return (
    <SectionCard title={t('future.simulatorTitle')}>
      <Typography variant="body2" color="text.secondary" sx={{ px: 3 }}>
        {t('future.simulatorHelp')}
      </Typography>
      <Tabs
        value={type}
        onChange={(_, value) => {
          setType(value);
          setState({ loading: false, error: null, result: null, request: null });
        }}
        variant="scrollable"
        sx={{ px: 2, mt: 1, borderBottom: '1px solid', borderColor: 'divider' }}
      >
        {TYPES.map((item) => (
          <Tab key={item} value={item} label={t(`future.scenarioTypes.${item}`)} />
        ))}
      </Tabs>
      <Box component="form" onSubmit={simulate} noValidate sx={{ p: 3 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
          {type === 'PURCHASE' && (
            <TextField
              label={t('future.purchaseLabel')}
              placeholder={t('future.purchasePlaceholder')}
              value={form.label}
              onChange={set('label')}
              inputProps={{ maxLength: 60 }}
              fullWidth
            />
          )}
          {needsAmount && (
            <TextField
              label={t(type === 'SAVINGS' ? 'future.monthlyAmount' : 'future.amount')}
              value={form.amount}
              onChange={set('amount')}
              inputProps={{ inputMode: 'decimal' }}
              InputProps={{ startAdornment: <Typography sx={{ mr: 1 }}>R$</Typography> }}
              error={Boolean(form.amount) && !amountCents}
              helperText={form.amount && !amountCents ? t('app.errors.INVALID_AMOUNT') : t('app.amountHelp')}
              required
              fullWidth
            />
          )}
          {type === 'PURCHASE' && (
            <>
              <TextField select label={t('future.method')} value={form.method} onChange={set('method')} fullWidth>
                {['CASH', 'CARD'].filter((item) => item === 'CASH' || hasCard).map((item) => (
                  <MenuItem key={item} value={item}>
                    {t(`future.methods.${item}`)}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                select
                label={t('future.installments')}
                value={form.installments}
                onChange={set('installments')}
                sx={{ minWidth: 140 }}
              >
                {[1, 2, 3, 4, 5, 6, 8, 10, 12, 18, 24].map((count) => (
                  <MenuItem key={count} value={count}>
                    {count === 1 ? t('future.installmentsCash') : t('future.installmentsOption', { count })}
                  </MenuItem>
                ))}
              </TextField>
            </>
          )}
          {type === 'SAVINGS' && (
            <TextField
              select
              label={t('future.dayOfMonth')}
              value={form.dayOfMonth}
              onChange={set('dayOfMonth')}
              sx={{ minWidth: 160 }}
            >
              {Array.from({ length: 28 }, (_, index) => index + 1).map((day) => (
                <MenuItem key={day} value={day}>
                  {day}
                </MenuItem>
              ))}
            </TextField>
          )}
          {type === 'INCOME_LOSS' && (
            <>
              <TextField select label={t('future.income')} value={form.incomeKey} onChange={set('incomeKey')} fullWidth
                SelectProps={{ displayEmpty: true }} InputLabelProps={{ shrink: true }}>
                <MenuItem value="">{t('future.allIncomes')}</MenuItem>
                {incomes.map((income) => (
                  <MenuItem key={income.key} value={income.key}>
                    {income.counterparty} · {money(income.typicalAmount, i18n.language)}
                  </MenuItem>
                ))}
              </TextField>
              <TextField select label={t('future.months')} value={form.months} onChange={set('months')} sx={{ minWidth: 180 }}>
                {[1, 2, 3, 6, 12].map((count) => (
                  <MenuItem key={count} value={count}>
                    {t('future.monthsOption', { count })}
                  </MenuItem>
                ))}
              </TextField>
            </>
          )}
        </Stack>
        {type === 'INCOME_LOSS' && !incomes.length && (
          <Alert severity="info" sx={{ mt: 2 }}>
            {t('future.noIncomeToLose')}
          </Alert>
        )}
        <Button type="submit" variant="contained" size="large" disabled={blocked || state.loading} sx={{ mt: 2 }}>
          {state.loading ? t('future.simulating') : t('future.simulate')}
        </Button>
      </Box>

      {state.error && (
        <Alert severity="error" sx={{ mx: 3, mb: 3 }}>
          {t(state.error)}
        </Alert>
      )}

      {result && (
        <Box sx={{ px: 3, pb: 3 }} aria-live="polite">
          <Typography variant="h6" component="h3">
            {t('future.resultTitle')}
          </Typography>
          <Alert severity={result.scenario.negativeDays > 0 ? 'warning' : 'success'} sx={{ mt: 2 }}>
            {summaryLines(result, state.request, t, i18n.language).map((line) => (
              <Typography key={line} variant="body2" sx={{ '& + &': { mt: 0.5 } }}>
                {line}
              </Typography>
            ))}
          </Alert>
          <Table size="small" sx={{ mt: 2 }}>
            <TableHead>
              <TableRow>
                <TableCell />
                <TableCell align="right">{t('future.compareToday')}</TableCell>
                <TableCell align="right">{t('future.compareScenario')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map(([key, before, after, isMoney]) => (
                <TableRow key={key}>
                  <TableCell component="th" scope="row">
                    {t(`future.${key}`)}
                  </TableCell>
                  <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                    {isMoney ? (Number(before) ? money(before, i18n.language) : t('future.noneValue')) : before}
                  </TableCell>
                  <TableCell
                    align="right"
                    sx={{
                      fontVariantNumeric: 'tabular-nums',
                      fontWeight: 600,
                      color: isMoney && Number(after) < 0 ? 'error.main' : 'text.primary',
                    }}
                  >
                    {isMoney ? (Number(after) ? money(after, i18n.language) : t('future.noneValue')) : after}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <Box sx={{ mt: 3 }}>
            <ProjectionChart
              ariaLabel={t('future.chartLabel', {
                amount: money(result.scenario.minBalance90, i18n.language),
                date: dayLabel(result.scenario.minDate, i18n.language),
                end: money(result.scenario.endBalance, i18n.language),
              })}
              series={[
                {
                  key: 'baseline',
                  label: t('future.chartBaseline'),
                  color: theme.palette.chart.projection,
                  days: result.baselineProjection.days.map((day) => ({ date: day.date, balance: Number(day.balance) })),
                },
                {
                  key: 'scenario',
                  label: t('future.chartScenario'),
                  color: theme.palette.chart.scenario,
                  dashed: true,
                  days: result.scenarioProjection.days.map((day) => ({ date: day.date, balance: Number(day.balance) })),
                },
              ]}
              lowest={{ date: result.scenario.minDate, balance: Number(result.scenario.minBalance90) }}
            />
          </Box>
          <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 2 }}>
            {t('future.disclaimer')}
          </Typography>
        </Box>
      )}
    </SectionCard>
  );
}
Simulator.propTypes = { incomes: PropTypes.array.isRequired, hasCard: PropTypes.bool };
