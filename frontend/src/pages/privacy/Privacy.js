import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import AuthService from '../../services/AuthService';
import HttpService from '../../services/HttpService';
import { PageHeading, SectionCard } from '../../components/wallet-ui';
import { errorKey } from '../../utils/wallet';

// What the account keeps and why (LGPD art. 9), and the way out (art. 18). ADR 0012.
const STORED = ['account', 'wallets', 'openFinance', 'answers', 'alerts'];

export default function Privacy() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [state, setState] = useState({ busy: false, error: null });

  const close = () => {
    setOpen(false);
    setPassword('');
    setState({ busy: false, error: null });
  };

  const remove = async (event) => {
    event.preventDefault();
    setState({ busy: true, error: null });
    try {
      await HttpService.postWithAuth('/account/delete', { password });
    } catch (failure) {
      setState({ busy: false, error: errorKey(failure) });
      return;
    }
    AuthService.logout();
    navigate('/login', { replace: true });
  };

  return (
    <>
      <Helmet>
        <title>{t('privacy.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="md">
        <PageHeading title={t('privacy.heading')} description={t('privacy.description')} />
        <Stack spacing={3}>
          <SectionCard title={t('privacy.storedTitle')}>
            <Box component="ul" sx={{ px: 3, pb: 2, m: 0, pl: 5 }}>
              {STORED.map((item) => (
                <Typography component="li" variant="body2" key={item} sx={{ mb: 1 }}>
                  {t(`privacy.stored.${item}`)}
                </Typography>
              ))}
            </Box>
          </SectionCard>

          <SectionCard title={t('privacy.aiTitle')}>
            <Typography variant="body2" sx={{ px: 3, pb: 2 }}>
              {t('privacy.ai')}
            </Typography>
          </SectionCard>

          <SectionCard
            title={t('privacy.openFinanceTitle')}
            action={
              <Button component={RouterLink} to="/open-finance" size="small">
                {t('navigation.openFinance')}
              </Button>
            }
          >
            <Typography variant="body2" sx={{ px: 3, pb: 2 }}>
              {t('privacy.openFinance')}
            </Typography>
          </SectionCard>

          <SectionCard title={t('privacy.deleteTitle')}>
            <Box sx={{ px: 3, pb: 3 }}>
              <Typography variant="body2" sx={{ mb: 2 }}>
                {t('privacy.deleteText')}
              </Typography>
              <Button color="error" variant="outlined" onClick={() => setOpen(true)}>
                {t('privacy.deleteButton')}
              </Button>
            </Box>
          </SectionCard>
        </Stack>
      </Container>

      <Dialog open={open} onClose={state.busy ? undefined : close} fullWidth maxWidth="xs">
        <Box component="form" onSubmit={remove}>
          <DialogTitle>{t('privacy.dialogTitle')}</DialogTitle>
          <DialogContent>
            <Typography variant="body2" sx={{ mb: 2 }}>
              {t('privacy.dialogText')}
            </Typography>
            {state.error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {t(state.error)}
              </Alert>
            )}
            <TextField
              autoFocus
              fullWidth
              type="password"
              autoComplete="current-password"
              label={t('privacy.password')}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={close} disabled={state.busy}>
              {t('privacy.cancel')}
            </Button>
            <Button type="submit" color="error" variant="contained" disabled={!password || state.busy}>
              {t('privacy.confirm')}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>
    </>
  );
}
