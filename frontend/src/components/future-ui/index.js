import PropTypes from 'prop-types';
import { Box, Chip, Stack, Tooltip, Typography } from '@mui/material';
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
          {event.label}
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
