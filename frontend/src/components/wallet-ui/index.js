import { Alert, Box, Button, Card, CircularProgress, Stack, SvgIcon, Typography } from '@mui/material';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { useWallets } from '../../context/WalletContext';
import { dateTime, directionFor, money } from '../../utils/wallet';

const paths = {
  wallet: 'M3 7h17v13H3V4h14v3M20 11h-5v5h5M16.5 13.5h.01',
  home: 'm3 10 9-7 9 7M5 9v12h5v-7h4v7h5V9',
  history: 'M3 11a9 9 0 1 1 2 7M3 4v7h7M12 7v5l3 2',
  send: 'm4 4 17 8-17 8 3-8-3-8Zm3 8h14',
  plus: 'M12 5v14M5 12h14',
  down: 'M12 4v16m-6-6 6 6 6-6',
  up: 'M12 20V4m-6 6 6-6 6 6',
  arrow: 'M5 12h14m-5-5 5 5-5 5',
  back: 'M19 12H5m5-5-5 5 5 5',
  check: 'm5 12 4 4L19 6',
  user: 'M20 21v-2a6 6 0 0 0-6-6h-4a6 6 0 0 0-6 6v2M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  logout: 'M9 4H4v16h5m5-13 5 5-5 5M8 12h11',
  eye: 'M2 12s3-7 10-7 10 7 10 7-3 7-10 7S2 12 2 12Zm13 0a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  info: 'M12 11v6m0-10h.01M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
  copy: 'M9 9h12v12H9V9ZM15 9V3H3v12h6',
  swap: 'M4 7h16m-4-4 4 4-4 4M20 17H4m4-4-4 4 4 4',
  insights: 'M4 19V9m5 10V5m5 14v-7m5 7V3M3 19h18M17 7l2-2 2 2M19 5v6',
  shield: 'M12 3 5 6v5c0 4.6 2.9 8.8 7 10 4.1-1.2 7-5.4 7-10V6l-7-3Zm-3 9 2 2 4-5',
  book: 'M4 5a3 3 0 0 1 3-2h5v17H7a3 3 0 0 0-3 3V5Zm8-2h5a3 3 0 0 1 3 3v17a3 3 0 0 0-3-3h-5',
  bank: 'M3 9h18L12 3 3 9Zm2 3v7m5-7v7m4-7v7m5-7v7M3 21h18',
  sync: 'M20 7h-6V1m6 6-3-3a8 8 0 0 0-13 3m0 10h6v6m-6-6 3 3a8 8 0 0 0 13-3',
  future: 'M3 17l5-6 4 4 3-4M17.5 9.5h.01M20 6.5h.01M3 21h18',
  card: 'M3 6h18v12H3V6Zm0 4h18M7 15h4',
  trash: 'M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6m4-6v6',
  envelope: 'M3 6h18v12H3V6Zm0 0 9 7 9-7',
  ask: 'M4 5h16v11H9l-5 4V5Zm6.5 3.5a1.5 1.5 0 1 1 2.2 1.3c-.5.3-.7.7-.7 1.2M12 13.5h.01',
};
export function WalletIcon({ name, ...props }) {
  return (
    <SvgIcon {...props} viewBox="0 0 24 24">
      <path
        d={paths[name] || paths.wallet}
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </SvgIcon>
  );
}
WalletIcon.propTypes = { name: PropTypes.string };
/** Square icon badge used at the top of cards: the brand accent on a raised surface. */
export function IconTile({ name, size = 54, sx }) {
  return (
    <Box
      aria-hidden="true"
      sx={{
        width: size,
        height: size,
        flexShrink: 0,
        borderRadius: `${Math.round(size / 3)}px`,
        bgcolor: 'background.raised',
        border: '1px solid',
        borderColor: 'divider',
        color: 'primary.main',
        display: 'grid',
        placeItems: 'center',
        ...sx,
      }}
    >
      <WalletIcon name={name} sx={{ fontSize: Math.round(size * 0.5) }} />
    </Box>
  );
}
IconTile.propTypes = { name: PropTypes.string.isRequired, size: PropTypes.number, sx: PropTypes.object };
export function PageHeading({ title, description, action }) {
  return (
    <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={2} sx={{ mb: { xs: 3, md: 4 } }}>
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="h3" component="h1">
          {title}
        </Typography>
        {description && (
          <Typography color="text.secondary" sx={{ mt: 0.75 }}>
            {description}
          </Typography>
        )}
      </Box>
      {action}
    </Stack>
  );
}
PageHeading.propTypes = { title: PropTypes.string, description: PropTypes.string, action: PropTypes.node };
export function EmptyState({ title, description, action, icon = 'wallet' }) {
  return (
    <Stack alignItems="center" spacing={2} sx={{ py: 6, px: 3, textAlign: 'center' }}>
      <Box sx={{ p: 2, borderRadius: '22px', bgcolor: 'background.neutral', color: 'text.secondary' }}>
        <WalletIcon name={icon} sx={{ fontSize: 34 }} />
      </Box>
      <Typography variant="h6" component="h2">
        {title}
      </Typography>
      <Typography color="text.secondary" variant="body2" sx={{ maxWidth: 340 }}>
        {description}
      </Typography>
      {action}
    </Stack>
  );
}
EmptyState.propTypes = {
  title: PropTypes.string,
  description: PropTypes.string,
  action: PropTypes.node,
  icon: PropTypes.string,
};
export function DataState({ children }) {
  const { t } = useTranslation();
  const { loading, error, refresh } = useWallets();
  if (loading)
    return (
      <Box role="status" sx={{ textAlign: 'center', py: 10 }}>
        <CircularProgress size={28} />
        <Typography sx={{ mt: 2 }} color="text.secondary">
          {t('common.loading')}
        </Typography>
      </Box>
    );
  if (error)
    return (
      <Alert
        severity="error"
        action={
          <Button color="inherit" onClick={refresh}>
            {t('app.retry')}
          </Button>
        }
      >
        {t(error)}
      </Alert>
    );
  return children;
}
DataState.propTypes = { children: PropTypes.node };
export function ActivityList({ transactions, wallets, onSelect }) {
  const { t, i18n } = useTranslation();
  if (!transactions.length)
    return <EmptyState icon="history" title={t('app.noActivity')} description={t('app.noActivityHelp')} />;
  return (
    <Box>
      {transactions.map((item) => {
        const direction = directionFor(item, wallets);
        const success = item.status === 'SUCCESS';
        const color = success && direction === 'incoming' ? 'success.dark' : 'text.primary';
        const type = t(`app.types.${item.type?.name}`, { defaultValue: t('app.types.Transfer') });
        // Imported entries carry the counterparty ("Netflix"); that says more than the generic type.
        const named = item.description && item.description !== 'Initial balance';
        const title = named ? item.description : type;
        return (
          <Box
            key={item.id}
            component="button"
            type="button"
            onClick={() => onSelect(item)}
            sx={{
              width: '100%',
              display: 'flex',
              alignItems: 'center',
              gap: { xs: 1.5, sm: 2 },
              py: 2,
              px: { xs: 2, sm: 3 },
              border: 0,
              borderBottom: '1px solid',
              borderColor: 'divider',
              bgcolor: 'transparent',
              color: 'text.primary',
              textAlign: 'left',
              cursor: 'pointer',
              '&:last-child': { borderBottom: 0 },
              '&:hover': { bgcolor: 'action.hover' },
              '&:focus-visible': { outline: '2px solid', outlineColor: 'primary.main', outlineOffset: -2 },
            }}
          >
            <Box
              sx={{
                width: 42,
                height: 42,
                flexShrink: 0,
                borderRadius: '14px',
                bgcolor: 'background.neutral',
                display: 'grid',
                placeItems: 'center',
                color,
              }}
            >
              <WalletIcon name={direction === 'incoming' ? 'down' : direction === 'outgoing' ? 'up' : 'swap'} />
            </Box>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="subtitle2" noWrap>
                {title}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {named ? `${type} · ` : ''}
                {dateTime(item.createdAt, i18n.language)}
              </Typography>
            </Box>
            <Box sx={{ textAlign: 'right', minWidth: 0 }}>
              <Typography
                variant="subtitle2"
                sx={{ color, fontVariantNumeric: 'tabular-nums', overflowWrap: 'anywhere' }}
              >
                {success && direction === 'outgoing' ? '− ' : success && direction === 'incoming' ? '+ ' : ''}
                {money(item.amount, i18n.language)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {t(`transactions.status.${item.status}`, { defaultValue: item.status })}
              </Typography>
            </Box>
          </Box>
        );
      })}
    </Box>
  );
}
ActivityList.propTypes = {
  transactions: PropTypes.array.isRequired,
  wallets: PropTypes.array.isRequired,
  onSelect: PropTypes.func.isRequired,
};
export function SectionCard({ title, action, children }) {
  return (
    <Card>
      <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1} sx={{ p: 3, pb: 1.5 }}>
        {/* h2 under the page h1: screen reader users move through long pages by headings. */}
        <Typography variant="h6" component="h2">
          {title}
        </Typography>
        {action && <Box sx={{ flexShrink: 0, whiteSpace: 'nowrap' }}>{action}</Box>}
      </Stack>
      {children}
    </Card>
  );
}
SectionCard.propTypes = { title: PropTypes.string, action: PropTypes.node, children: PropTypes.node };
