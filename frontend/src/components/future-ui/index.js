import PropTypes from 'prop-types';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Card, Chip, Stack, Tooltip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { dayLabel, money } from '../../utils/wallet';

// Certainty is always shown as text + line style, never colour alone:
// confirmed = solid, estimated = dashed, hypothetical = dotted.
const BORDER = { CONFIRMED: 'solid', ESTIMATED: 'dashed', HYPOTHETICAL: 'dotted' };
const COLOR = { CONFIRMED: 'certainty.confirmed', ESTIMATED: 'certainty.estimated', HYPOTHETICAL: 'certainty.hypothetical' };

export function CertaintyChip({ certainty }) {
  const { t } = useTranslation();
  return (
    <Tooltip title={t(`future.certaintyHelp.${certainty}`)}>
      <Chip
        size="small"
        label={t(`future.certainty.${certainty}`)}
        variant="outlined"
        sx={{
          height: 22,
          fontSize: 11,
          color: COLOR[certainty],
          borderColor: COLOR[certainty],
          borderStyle: BORDER[certainty],
          bgcolor: 'transparent',
        }}
      />
    </Tooltip>
  );
}
CertaintyChip.propTypes = { certainty: PropTypes.oneOf(['CONFIRMED', 'ESTIMATED', 'HYPOTHETICAL']).isRequired };

/** One expected movement with its provenance: when, what, from where, how sure, how much. */
export function EventRow({ event, dense = false }) {
  const { t, i18n } = useTranslation();
  const inflow = Number(event.amount) > 0;
  // Everyday spending arrives folded into one line; its reference carries how many days it covers.
  const label =
    event.category === 'DAILY_SPENDING'
      ? t('future.dailyEvent', { days: Number(event.reference?.split(':')[1]) || 1 })
      : event.label;
  return (
    <Stack
      direction="row"
      alignItems="center"
      spacing={2}
      sx={{ py: dense ? 1 : 1.5, borderBottom: '1px solid', borderColor: 'divider', '&:last-of-type': { borderBottom: 0 } }}
    >
      <Box sx={{ width: 52, flexShrink: 0 }}>
        <Typography variant="subtitle2" sx={{ fontVariantNumeric: 'tabular-nums' }}>
          {dayLabel(event.date, i18n.language)}
        </Typography>
      </Box>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="body2" noWrap>
          {label}
        </Typography>
        <Typography variant="caption" color="text.secondary" noWrap component="div">
          {t(`future.categories.${event.category}`, { defaultValue: event.category })}
          {event.origin ? ` · ${event.origin}` : ''}
        </Typography>
      </Box>
      <Stack alignItems="flex-end" spacing={0.5} sx={{ flexShrink: 0 }}>
        <Typography
          variant="subtitle2"
          sx={{ fontVariantNumeric: 'tabular-nums', color: inflow ? 'success.main' : 'text.primary' }}
        >
          {inflow ? '+ ' : '− '}
          {money(Math.abs(Number(event.amount)), i18n.language)}
        </Typography>
        <CertaintyChip certainty={event.certainty} />
      </Stack>
    </Stack>
  );
}
EventRow.propTypes = { event: PropTypes.object.isRequired, dense: PropTypes.bool };

export function StatBlock({ label, value, help, emphasis = false, children }) {
  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography variant="overline" color="text.secondary">
        {label}
      </Typography>
      <Typography
        component="p"
        sx={{
          fontFamily: (theme) => theme.typography.h2.fontFamily,
          fontWeight: 700,
          fontSize: { xs: 28, md: 32 },
          letterSpacing: '-0.03em',
          lineHeight: 1.15,
          fontVariantNumeric: 'tabular-nums',
          color: emphasis ? 'primary.main' : 'text.primary',
          overflowWrap: 'anywhere',
        }}
      >
        {value}
      </Typography>
      {help && (
        <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 0.5 }}>
          {help}
        </Typography>
      )}
      {children}
    </Box>
  );
}
StatBlock.propTypes = {
  label: PropTypes.string.isRequired,
  value: PropTypes.node.isRequired,
  help: PropTypes.node,
  emphasis: PropTypes.bool,
  children: PropTypes.node,
};

/**
 * The first thing the Future page answers: how much can be spent per day until the next income,
 * or how much is missing when the bills alone do not fit. Everything below it supports this number.
 */
export function DailyLimitCard({ limit, risk, onWhy }) {
  const { t, i18n } = useTranslation();
  const lang = i18n.language;
  const short = Number(limit.shortfall) > 0;
  const over = limit.margin !== null && limit.margin !== undefined && Number(limit.margin) < 0;
  const binding = dayLabel(limit.bindingDate, lang);
  const tone = short || over ? 'warning' : 'success';

  let pace = t('future.limit.paceUnknown');
  if (limit.pace !== null && limit.pace !== undefined) {
    if (short) pace = t('future.limit.shortPace', { pace: money(limit.pace, lang) });
    else if (over)
      pace = t('future.limit.paceOver', {
        pace: money(limit.pace, lang),
        cut: money(Math.abs(Number(limit.margin)), lang),
      });
    else
      pace = t('future.limit.paceOk', { pace: money(limit.pace, lang), margin: money(limit.margin, lang) });
  }

  return (
    <Card
      component="section"
      aria-labelledby="daily-limit-title"
      sx={{
        p: { xs: 3, md: 4 },
        borderLeft: '4px solid',
        borderColor: `${tone}.main`,
      }}
    >
      <Typography variant="overline" color="text.secondary">
        {limit.untilIncome
          ? t('future.limit.until', { date: dayLabel(limit.until, lang) })
          : t('future.limit.untilNoIncome', { days: limit.days })}
      </Typography>
      <Typography
        id="daily-limit-title"
        component="h2"
        sx={{
          fontFamily: (theme) => theme.typography.h2.fontFamily,
          fontWeight: 700,
          fontSize: { xs: 26, md: 34 },
          letterSpacing: '-0.03em',
          lineHeight: 1.15,
          fontVariantNumeric: 'tabular-nums',
          mt: 0.5,
          color: short ? 'warning.main' : 'text.primary',
        }}
      >
        {short
          ? t('future.limit.short', { amount: money(limit.shortfall, lang) })
          : t('future.limit.canSpend', { amount: money(limit.safeDaily, lang) })}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
        {short ? t('future.limit.shortHelp', { binding }) : t('future.limit.canSpendHelp', { binding })}
      </Typography>
      <Typography variant="body2" sx={{ mt: 1.5 }}>
        {pace}
      </Typography>
      {risk && Number(risk.overdraftCostCap) > 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          {t('future.limit.cost', { amount: money(risk.overdraftCostCap, lang) })}
        </Typography>
      )}
      <Stack direction="row" flexWrap="wrap" sx={{ mt: 2, gap: 1 }}>
        {risk && onWhy && (
          <Button variant="outlined" color="inherit" onClick={() => onWhy(risk)}>
            {t('future.whyButton')}
          </Button>
        )}
        <Button component={RouterLink} to="/ask" color="inherit">
          {t('future.limit.ask')}
        </Button>
      </Stack>
    </Card>
  );
}
DailyLimitCard.propTypes = { limit: PropTypes.object.isRequired, risk: PropTypes.object, onWhy: PropTypes.func };
