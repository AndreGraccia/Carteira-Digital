import {
  Box,
  Drawer,
} from '@mui/material';
import PropTypes from 'prop-types';
import { useEffect } from 'react';
import {
  useLocation,
} from 'react-router-dom';
import Logo from '../../../components/logo';
import NavSection from '../../../components/nav-section';
import Scrollbar from '../../../components/scrollbar';
import useResponsive from '../../../hooks/useResponsive';
import useNavConfig from './config';

const NAV_WIDTH = 280;

const drawerStyle = {
  width: NAV_WIDTH,
  bgcolor: 'rgba(248,248,250,0.72)',

  backdropFilter:
    'blur(34px) saturate(175%)',

  WebkitBackdropFilter:
    'blur(34px) saturate(175%)',

  borderRight:
    '1px solid rgba(255,255,255,0.74)',

  boxShadow: `
    12px 0 45px rgba(15,23,42,0.06),
    inset -1px 0 0 rgba(142,142,147,0.08)
  `,
};

Nav.propTypes = {
  openNav: PropTypes.bool,
  onCloseNav: PropTypes.func,
};

export default function Nav({
  openNav,
  onCloseNav,
}) {
  const { pathname } = useLocation();
  const navConfig = useNavConfig();
  const isDesktop = useResponsive('up', 'lg');

  useEffect(() => {
    if (openNav) {
      onCloseNav();
    }

    // eslint-disable-next-line
  }, [pathname]);

  const renderContent = (
    <Scrollbar
      sx={{
        height: 1,

        '& .simplebar-content': {
          height: 1,
          display: 'flex',
          flexDirection: 'column',
        },
      }}
    >
      <Box
        sx={{
          px: 3,
          pt: 3.5,
          pb: 2.5,
          display: 'inline-flex',
        }}
      >
        <Logo />
      </Box>

      <NavSection data={navConfig} />

      <Box sx={{ flexGrow: 1 }} />
    </Scrollbar>
  );

  return (
    <Box
      component="nav"
      sx={{
        flexShrink: { lg: 0 },
        width: { lg: NAV_WIDTH },
      }}
    >
      {isDesktop ? (
        <Drawer
          open
          variant="permanent"
          PaperProps={{
            sx: drawerStyle,
          }}
        >
          {renderContent}
        </Drawer>
      ) : (
        <Drawer
          open={openNav}
          onClose={onCloseNav}
          ModalProps={{
            keepMounted: true,

            BackdropProps: {
              sx: {
                bgcolor: 'rgba(15,23,42,0.22)',
                backdropFilter: 'blur(8px)',
              },
            },
          }}
          PaperProps={{
            sx: {
              ...drawerStyle,
              borderRadius: '0 28px 28px 0',
            },
          }}
        >
          {renderContent}
        </Drawer>
      )}
    </Box>
  );
}
