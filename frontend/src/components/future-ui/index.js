import { useState } from 'react';
import PropTypes from 'prop-types';
import { Link as RouterLink } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Link,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { dayLabel, money } from '../../utils/wallet';

// Certainty is always shown as text + line style, never colour alone:
// confirmed = solid, estimated = dashed, hypothetical = dotted.
const BORDER = { CONFIRMED: 'solid', ESTIMATED: 'dashed', HYPOTHETICAL: 'dotted' };
const COLOR = {
  CONFIRMED: 'certainty.confirmed',
  ESTIMATED: 'certainty.estimated',
  HYPOTHETICAL: 'certainty.hypothetical',
};

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
  const days = Number(event.reference?.split(':')[1]) || 1;
  let { label } = event;
  if (event.category === 'DAILY_SPENDING') label = t('future.dailyEvent', { days });
  if (event.category === 'CARD_SPENDING') label = t('future.cardEvent', { days });
  return (
    <Stack
      direction="row"
      alignItems="center"
      spacing={2}
      sx={{
        py: dense ? 1 : 1.5,
        borderBottom: '1px solid',
        borderColor: 'divider',
        '&:last-of-type': { borderBottom: 0 },
      }}
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

export function StatBlock({ label, value, help, emphasis = false, negative = false, children }) {
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
          color: negative ? 'error.main' : emphasis ? 'primary.main' : 'text.primary',
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
  negative: PropTypes.bool,
  children: PropTypes.node,
};

/**
 * "Act on the future", simulated: the engine already knows who, how much and the new date of a moved bill,
 * so the Pix can be prepared from it. Nothing is sent; Wall.ia has no accreditation to start payments yet
 * (see docs/estrategia-produto.md). The dialog says so before and after the confirmation.
 */
function PixProposalDialog({ action, onClose }) {
  const { t, i18n } = useTranslation();
  const lang = i18n.language;
  const [confirmed, setConfirmed] = useState(false);
  const close = () => {
    setConfirmed(false);
    onClose();
  };
  return (
    <Dialog open={Boolean(action)} onClose={close} fullWidth maxWidth="xs" aria-labelledby="pix-title">
      {action && (
        <>
          <DialogTitle id="pix-title">{t('future.pix.title')}</DialogTitle>
          <DialogContent>
            <Box
              component="dl"
              sx={{ m: 0, display: 'grid', gridTemplateColumns: 'auto 1fr', columnGap: 2, rowGap: 1 }}
            >
              {[
                ['future.pix.to', action.label],
                ['future.pix.amount', money(action.amount, lang)],
                ['future.pix.date', dayLabel(action.to, lang)],
                ['future.pix.from', t('future.pix.fromValue')],
              ].map(([label, value]) => (
                <Box key={label} sx={{ display: 'contents' }}>
                  <Typography component="dt" variant="body2" color="text.secondary">
                    {t(label)}
                  </Typography>
                  <Typography component="dd" variant="body2" sx={{ m: 0, fontVariantNumeric: 'tabular-nums' }}>
                    {value}
                  </Typography>
                </Box>
              ))}
            </Box>
            <Typography variant="body2" sx={{ mt: 2 }}>
              {t('future.pix.steps', { label: action.label, date: dayLabel(action.to, lang) })}
            </Typography>
            <Alert severity={confirmed ? 'success' : 'info'} sx={{ mt: 2 }} role="status">
              {confirmed ? t('future.pix.done', { date: dayLabel(action.to, lang) }) : t('future.pix.simulated')}
            </Alert>
          </DialogContent>
          <DialogActions>
            <Button onClick={close}>{t(confirmed ? 'common.close' : 'future.pix.cancel')}</Button>
            {!confirmed && (
              <Button variant="contained" onClick={() => setConfirmed(true)}>
                {t('future.pix.confirm')}
              </Button>
            )}
          </DialogActions>
        </>
      )}
    </Dialog>
  );
}
PixProposalDialog.propTypes = { action: PropTypes.object, onClose: PropTypes.func.isRequired };

/**
 * Calculated ways out of the first negative window. Each line says what changes and what it gives;
 * the Wall.ia never acts for the user.
 */
export function ExitPlanList({ actions }) {
  const { t, i18n } = useTranslation();
  const lang = i18n.language;
  const [pix, setPix] = useState(null);
  if (!actions?.length) return null;
  const sentence = (action) => {
    const result = action.solves
      ? t('future.exit.solves')
      : t('future.exit.lowest', { amount: money(action.resultingLowest, lang) });
    if (action.type === 'POSTPONE')
      return `${t('future.exit.POSTPONE', {
        label: action.label,
        amount: money(action.amount, lang),
        from: dayLabel(action.from, lang),
        to: dayLabel(action.to, lang),
      })} ${result}`;
    if (action.type === 'CUT_DAILY')
      return `${t('future.exit.CUT_DAILY', {
        amount: money(action.amount, lang),
        daily: money(action.newDaily, lang),
        to: dayLabel(action.to, lang),
      })} ${result}`;
    return t('future.exit.GAP', { amount: money(action.amount, lang), to: dayLabel(action.to, lang) });
  };
  return (
    <Box component="section" aria-label={t('future.exit.title')} sx={{ mt: 2 }}>
      <Typography variant="subtitle2" component="h3">
        {t('future.exit.title')}
      </Typography>
      <Box component="ol" sx={{ m: 0, mt: 1, pl: 2.5 }}>
        {actions.map((action) => (
          <Typography component="li" variant="body2" key={`${action.type}-${action.label || ''}`} sx={{ py: 0.5 }}>
            {sentence(action)}
            {/* Only a moved bill has a payee, an amount and a date to pay on. */}
            {action.type === 'POSTPONE' && (
              <Button size="small" onClick={() => setPix(action)} sx={{ ml: 1, py: 0 }}>
                {t('future.pix.prepare')}
              </Button>
            )}
          </Typography>
        ))}
      </Box>
      <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 1 }}>
        {t('future.exit.note')}
      </Typography>
      <PixProposalDialog action={pix} onClose={() => setPix(null)} />
    </Box>
  );
}
ExitPlanList.propTypes = { actions: PropTypes.array };

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
    else pace = t('future.limit.paceOk', { pace: money(limit.pace, lang), margin: money(limit.margin, lang) });
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
      {Number(limit.reserved) > 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          {t('future.limit.reserved', { amount: money(limit.reserved, lang) })}{' '}
          <Link component={RouterLink} to="/envelopes">
            {t('future.limit.reservedLink')}
          </Link>
        </Typography>
      )}
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
