import { useTranslation } from 'react-i18next';
import { useState } from 'react';

import { useNavigate } from 'react-router-dom';
import { Avatar, Box, Divider, IconButton, MenuItem, Popover, Stack, Typography } from '@mui/material';
import { alpha } from '@mui/material/styles';
import AuthService from '../../../services/AuthService';
import account from '../../../_mock/account';

const MENU_OPTIONS = [
  {
    label: 'common.home',
    icon: 'eva:home-fill',
  },
  {
    label: 'common.profile',
    icon: 'eva:person-fill',
  },
  {
    label: 'common.settings',
    icon: 'eva:settings-2-fill',
  },
];

export default function AccountPopover() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(null);
  const navigate = useNavigate();
  const currentUser = AuthService.getCurrentUser();

  const handleOpen = (event) => {
    setOpen(event.currentTarget);
  };

  const handleClose = () => {
    setOpen(null);
  };

  const handleLogout = () => {
    AuthService.logout();
    navigate('/login');
  };

  return (
    <>
      <IconButton
        onClick={handleOpen}
        aria-label={t('common.account')}
        sx={{
          p: 0,
          ...(open && {
            '&:before': {
              zIndex: 1,
              content: "''",
              width: '100%',
              height: '100%',
              borderRadius: '50%',
              position: 'absolute',
              bgcolor: (theme) => alpha(theme.palette.grey[900], 0.8),
            },
          }),
        }}
      >
        <Avatar src={account.photoURL} alt={t('common.account')} />
      </IconButton>
      <Popover
        open={Boolean(open)}
        anchorEl={open}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{
          sx: {
            p: 0,
            mt: 1.5,
            ml: 0.75,
            width: 180,
            '& .MuiMenuItem-root': {
              typography: 'body2',
              borderRadius: 0.75,
            },
          },
        }}
      >
        <Box sx={{ my: 1.5, px: 2.5 }}>
          <Typography variant="subtitle2" noWrap>
            {currentUser?.firstName} {currentUser?.lastName}
          </Typography>
        </Box>
        <Divider sx={{ borderStyle: 'dashed' }} />
        <Stack sx={{ p: 1 }}>
          {MENU_OPTIONS.map((option) => (
            <MenuItem key={t(option.label)} onClick={handleClose} disabled>
              {t(option.label)}
            </MenuItem>
          ))}
        </Stack>
        <Divider sx={{ borderStyle: 'dashed' }} />
        <MenuItem onClick={handleLogout} sx={{ m: 1 }}>
          {t('actions.logout')}{' '}
        </MenuItem>
      </Popover>
    </>
  );
}
