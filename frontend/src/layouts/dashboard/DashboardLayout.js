import { useState } from 'react';
import { Link as RouterLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Avatar,
  BottomNavigation,
  BottomNavigationAction,
  Box,
  Button,
  Chip,
  Container,
  Divider,
  IconButton,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { WalletProvider } from '../../context/WalletContext';
import LanguageSelector from '../../components/language-selector';
import { WalletIcon } from '../../components/wallet-ui';
import AuthService from '../../services/AuthService';

export default function DashboardLayout() {
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const user = AuthService.getCurrentUser();
  const [accountOpen, setAccountOpen] = useState(false);
  const items = [
    { path: '/', label: 'navigation.dashboard', icon: 'home' },
    { path: '/wallets', label: 'navigation.wallets', icon: 'wallet' },
    { path: '/transfers', label: 'navigation.transfers', icon: 'send' },
    { path: '/transactions', label: 'navigation.transactions', icon: 'history' },
    { path: '/insights', label: 'navigation.insights', icon: 'insights' },
  ];
  const current =
    items.find((item) => (item.path === '/' ? pathname === '/' : pathname.startsWith(item.path)))?.path || false;
  const logout = () => {
    AuthService.logout();
    navigate('/login', { replace: true });
  };
  return (
    <WalletProvider>
      <Box sx={{ minHeight: '100vh', display: 'flex' }}>
        <Box
          component="aside"
          sx={{
            width: 240,
            position: 'fixed',
            inset: '0 auto 0 0',
            bgcolor: '#EEEEF1',
            borderRight: '1px solid',
            borderColor: 'divider',
            p: 2.5,
            display: { xs: 'none', lg: 'flex' },
            flexDirection: 'column',
          }}
        >
          <Stack direction="row" spacing={1.5} alignItems="center" sx={{ px: 1, py: 2, mb: 4 }}>
            <Box
              sx={{
                width: 38,
                height: 38,
                borderRadius: '12px',
                bgcolor: '#1D1D1F',
                color: 'white',
                display: 'grid',
                placeItems: 'center',
              }}
            >
              <WalletIcon name="wallet" />
            </Box>
            <Typography variant="h6">e-wallet</Typography>
          </Stack>
          <Stack component="nav" spacing={0.75} aria-label={t('app.navigation')}>
            {items.map((item) => (
              <Button
                key={item.path}
                component={RouterLink}
                to={item.path}
                aria-current={current === item.path ? 'page' : undefined}
                startIcon={<WalletIcon name={item.icon} />}
                sx={{
                  justifyContent: 'flex-start',
                  px: 2,
                  gap: 1,
                  color: current === item.path ? 'text.primary' : 'text.secondary',
                  bgcolor: current === item.path ? 'white' : 'transparent',
                  boxShadow: current === item.path ? '0 2px 8px #00000006' : 'none',
                }}
              >
                {t(item.label)}
              </Button>
            ))}
          </Stack>
          <Box sx={{ flexGrow: 1 }} />
          <Chip label={t('app.demo')} size="small" sx={{ alignSelf: 'flex-start', mb: 1.5 }} />
          <Typography variant="caption" color="text.secondary" sx={{ px: 0.5, mb: 2 }}>
            {t('app.demoNotice')}
          </Typography>
          <Button
            component={RouterLink}
            to="/about"
            startIcon={<WalletIcon name="info" />}
            sx={{ justifyContent: 'flex-start', color: 'text.secondary' }}
          >
            {t('app.about')}
          </Button>
        </Box>
        <Box sx={{ flex: 1, minWidth: 0, ml: { lg: '240px' } }}>
          <Box
            component="header"
            sx={{
              position: 'sticky',
              top: 0,
              zIndex: 1100,
              bgcolor: 'rgba(245,245,247,0.88)',
              backdropFilter: 'blur(20px)',
              borderBottom: '1px solid',
              borderColor: 'divider',
            }}
          >
            <Container maxWidth="lg">
              <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ minHeight: 76 }}>
                <Stack direction="row" spacing={1} alignItems="center">
                  <Typography variant="subtitle1" sx={{ display: { lg: 'none' } }}>
                    e-wallet
                  </Typography>
                  <Chip size="small" label={t('app.demo')} sx={{ bgcolor: 'background.neutral' }} />
                </Stack>
                <Stack direction="row" spacing={1.5} alignItems="center">
                  <LanguageSelector />
                  <Tooltip title={t('common.account')}>
                    <IconButton
                      onClick={() => setAccountOpen(!accountOpen)}
                      aria-label={t('common.account')}
                      aria-expanded={accountOpen}
                    >
                      <Avatar sx={{ width: 34, height: 34, bgcolor: '#DEDEE4', color: '#48484A', fontSize: 14 }}>
                        {user?.firstName?.[0] || 'U'}
                      </Avatar>
                    </IconButton>
                  </Tooltip>
                </Stack>
              </Stack>
              {accountOpen && (
                <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} spacing={1} sx={{ pb: 2 }}>
                  <Typography variant="body2" sx={{ flex: 1 }}>
                    {user?.firstName} {user?.lastName}
                  </Typography>
                  <Button component={RouterLink} to="/about" onClick={() => setAccountOpen(false)}>
                    {t('app.about')}
                  </Button>
                  <Button onClick={logout} startIcon={<WalletIcon name="logout" />}>
                    {t('actions.logout')}
                  </Button>
                </Stack>
              )}
            </Container>
          </Box>
          <Box component="main" sx={{ pt: { xs: 3, md: 5 }, pb: { xs: 14, lg: 6 } }}>
            <Outlet />
          </Box>
        </Box>
        <Box
          sx={{
            display: { lg: 'none' },
            position: 'fixed',
            bottom: 0,
            right: 0,
            left: 0,
            zIndex: 1200,
            pb: 'env(safe-area-inset-bottom)',
            bgcolor: 'rgba(250,250,252,0.95)',
            backdropFilter: 'blur(20px)',
          }}
        >
          <Divider />
          <BottomNavigation showLabels value={current} sx={{ height: 70, bgcolor: 'transparent' }}>
            {items.map((item) => (
              <BottomNavigationAction
                key={item.path}
                component={RouterLink}
                to={item.path}
                value={item.path}
                label={t(item.label)}
                icon={<WalletIcon name={item.icon} />}
                sx={{ minWidth: 0, px: 0.5, '& .MuiBottomNavigationAction-label': { fontSize: '0.7rem' } }}
              />
            ))}
          </BottomNavigation>
        </Box>
      </Box>
    </WalletProvider>
  );
}
