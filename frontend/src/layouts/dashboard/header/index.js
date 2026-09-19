import { useTranslation } from 'react-i18next';
import { AppBar, Box, IconButton, Stack, Toolbar } from '@mui/material';
import { styled } from '@mui/material/styles';
import PropTypes from 'prop-types';
import Iconify from '../../../components/iconify';
import LanguageSelector from '../../../components/language-selector';
import AccountPopover from './AccountPopover';
import Searchbar from './Searchbar';

const NAV_WIDTH = 280;
const HEADER_MOBILE = 64;
const HEADER_DESKTOP = 72;

const StyledRoot = styled(AppBar)(({ theme }) => ({
  background: 'transparent',
  boxShadow: 'none',
  pointerEvents: 'none',

  [theme.breakpoints.up('lg')]: {
    width: `calc(100% - ${NAV_WIDTH}px)`,
  },
}));

const StyledToolbar = styled(Toolbar)(({ theme }) => ({
  width: 'auto',
  minHeight: HEADER_MOBILE,
  margin: theme.spacing(1.5, 2),
  padding: theme.spacing(0, 1.25),
  pointerEvents: 'auto',

  borderRadius: 22,
  border: '1px solid rgba(255,255,255,0.72)',

  backgroundColor: 'rgba(250,250,252,0.68)',

  backdropFilter: 'blur(28px) saturate(180%)',

  WebkitBackdropFilter: 'blur(28px) saturate(180%)',

  boxShadow: `
    0 14px 40px rgba(15,23,42,0.08),
    inset 0 1px 0 rgba(255,255,255,0.82)
  `,

  [theme.breakpoints.up('lg')]: {
    minHeight: HEADER_DESKTOP,
    margin: theme.spacing(1.75, 3),
    padding: theme.spacing(0, 2),
  },
}));

Header.propTypes = {
  onOpenNav: PropTypes.func,
};

export default function Header({ onOpenNav }) {
  const { t } = useTranslation();
  return (
    <StyledRoot>
      <StyledToolbar>
        <IconButton
          onClick={onOpenNav}
          aria-label={t('common.openMenu')}
          sx={{
            mr: 1,
            width: 40,
            height: 40,
            color: 'text.primary',
            bgcolor: 'rgba(255,255,255,0.5)',
            display: { lg: 'none' },

            '&:hover': {
              bgcolor: 'rgba(255,255,255,0.82)',
            },
          }}
        >
          <Iconify icon="eva:menu-2-fill" />
        </IconButton>

        <Searchbar />

        <Box sx={{ flexGrow: 1 }} />

        <Stack
          direction="row"
          alignItems="center"
          spacing={{
            xs: 0.75,
            sm: 1.25,
          }}
        >
          <LanguageSelector />
          <AccountPopover />
        </Stack>
      </StyledToolbar>
    </StyledRoot>
  );
}
