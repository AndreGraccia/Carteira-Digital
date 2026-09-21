import { Box, ButtonBase, Card, IconButton, Stack, Tooltip, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import PropTypes from 'prop-types';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { WalletIcon } from '../wallet-ui';
import { money } from '../../utils/wallet';

const THEMES = [
  'linear-gradient(120deg,#34373D,#111214)',
  'linear-gradient(120deg,#557B9F,#294D70)',
  'linear-gradient(120deg,#667B6A,#344D40)',
  'linear-gradient(120deg,#9F7F70,#705044)',
  'linear-gradient(120deg,#807592,#50465E)',
];
export default function WalletCard({ wallet, index = 0, selected = false, onClick, href, hideBalance = false }) {
  const { t, i18n } = useTranslation();
  const storageKey = `wallet-card-theme-${wallet.id}`;
  const [color, setColor] = useState(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      const value = Number(saved);
      if (saved !== null && Number.isInteger(value) && value >= 0 && value < THEMES.length) return value;
    } catch {
      /* optional preference */
    }
    return index % THEMES.length;
  });
  const changeColor = () => {
    const next = (color + 1) % THEMES.length;
    setColor(next);
    try {
      localStorage.setItem(storageKey, String(next));
    } catch {
      /* optional preference */
    }
  };
  return (
    <Card
      sx={{
        height: 202,
        borderRadius: '24px',
        background: THEMES[color],
        color: '#FFF',
        border: '1px solid #FFFFFF24',
        boxShadow: '0 8px 20px #16161D24',
        overflow: 'visible',
        position: 'relative',
        '&:focus-within': { outline: '3px solid #0071E3', outlineOffset: 3 },
      }}
    >
      <ButtonBase
        component={href ? RouterLink : 'button'}
        to={href}
        onClick={href ? undefined : onClick}
        aria-pressed={href ? undefined : selected}
        aria-label={`${wallet.name}, ${t('wallet.card.clickToOpen')}`}
        sx={{ p: 3, width: '100%', height: '100%', display: 'block', borderRadius: 'inherit', textAlign: 'left' }}
      >
        <Stack sx={{ height: '100%' }} justifyContent="space-between">
          <Stack direction="row" spacing={1} alignItems="center" sx={{ pr: selected ? 5 : 0 }}>
            <Typography variant="subtitle1" noWrap sx={{ flex: 1 }}>
              {wallet.name}
            </Typography>
            {!selected && <WalletIcon name="wallet" sx={{ opacity: 0.6 }} />}
          </Stack>
          <Box>
            <Typography variant="caption" sx={{ color: '#FFFFFFB8' }}>
              {t('wallet.availableBalance')}
            </Typography>
            <Typography
              sx={{
                fontSize: 28,
                fontWeight: 600,
                letterSpacing: '-0.035em',
                fontVariantNumeric: 'tabular-nums',
                overflowWrap: 'anywhere',
              }}
            >
              {hideBalance ? '••••••' : money(wallet.balance, i18n.language)}
            </Typography>
          </Box>
          <Stack direction="row" justifyContent="space-between">
            <Typography variant="caption" sx={{ color: '#FFFFFFB8' }}>
              {wallet.source === 'OPEN_FINANCE' ? wallet.institutionName : t('app.demo')}
            </Typography>
            <Typography variant="caption" sx={{ letterSpacing: '0.12em' }}>
              •••• {wallet.iban?.slice(-4)}
            </Typography>
          </Stack>
        </Stack>
      </ButtonBase>
      {selected && (
        <Tooltip title={t('wallet.changeColor')}>
          <IconButton
            aria-label={t('wallet.changeColor')}
            onClick={changeColor}
            sx={{
              position: 'absolute',
              right: 14,
              top: 14,
              width: 44,
              height: 44,
              color: 'white',
              bgcolor: '#FFFFFF18',
              '&:hover': { bgcolor: '#FFFFFF28' },
            }}
          >
            <Box
              sx={{
                width: 17,
                height: 17,
                borderRadius: '50%',
                border: '2px solid currentColor',
                background: 'conic-gradient(#B5C9D5,#D0B8A8,#A5BAA3,#B5C9D5)',
              }}
            />
          </IconButton>
        </Tooltip>
      )}
    </Card>
  );
}
WalletCard.propTypes = {
  wallet: PropTypes.object.isRequired,
  index: PropTypes.number,
  selected: PropTypes.bool,
  onClick: PropTypes.func,
  href: PropTypes.string,
  hideBalance: PropTypes.bool,
};
