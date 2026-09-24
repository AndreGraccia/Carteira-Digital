import { useRef, useState } from 'react';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Chip,
  Container,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { enqueueSnackbar } from 'notistack';
import PropTypes from 'prop-types';
import LanguageSelector from '../../components/language-selector';
import { WalliaLogo } from '../../components/brand';
import { WalletIcon } from '../../components/wallet-ui';
import AuthService from '../../services/AuthService';
import { safeReturnPath } from '../../services/HttpService';
import { errorKey } from '../../utils/wallet';

export default function AuthView({ signup = false }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const group = signup ? 'signup' : 'login';
  const [values, setValues] = useState({ username: '', password: '', firstName: '', lastName: '', email: '' });
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const sending = useRef(false);
  const submit = async (event) => {
    event.preventDefault();
    if (sending.current) return;
    sending.current = true;
    setBusy(true);
    setError(null);
    try {
      if (signup) {
        await AuthService.signup({
          ...values,
          firstName: values.firstName.trim(),
          lastName: values.lastName.trim(),
          username: values.username.trim(),
          email: values.email.trim(),
          roles: ['ROLE_USER'],
        });
        enqueueSnackbar(t('auth.signup.success'), { variant: 'success' });
        navigate('/login', { replace: true });
      } else {
        await AuthService.login({ username: values.username.trim(), password: values.password });
        // `state.from` comes from PrivateRoute, `?from=` from an expired session (HttpService).
        const from = location.state?.from ?? new URLSearchParams(location.search).get('from');
        navigate(safeReturnPath(from), { replace: true });
      }
    } catch (failure) {
      setError(!signup && failure.response?.status === 401 ? 'app.errors.credentials' : errorKey(failure));
    } finally {
      sending.current = false;
      setBusy(false);
    }
  };
  const fields = signup ? ['firstName', 'lastName', 'username', 'email', 'password'] : ['username', 'password'];
  return (
    <>
      <Helmet>
        <title>{t(`auth.${group}.pageTitle`)}</title>
      </Helmet>
      <Box sx={{ minHeight: '100vh', display: 'flex' }}>
        <Box
          sx={{
            display: { xs: 'none', md: 'flex' },
            width: '45%',
            position: 'relative',
            bgcolor: 'background.paper',
            borderRight: '1px solid',
            borderColor: 'divider',
            p: 6,
            flexDirection: 'column',
            justifyContent: 'space-between',
            overflow: 'hidden',
          }}
        >
          <WalliaLogo size={36} />
          <Box>
            {/* Decorative echo of the mark: past (solid), projection (dots), the future point (volt). */}
            <Box aria-hidden="true" component="svg" viewBox="0 0 420 180" sx={{ width: '100%', maxWidth: 440, mb: 6 }}>
              <Box component="line" x1="0" y1="140" x2="420" y2="140" sx={{ stroke: 'divider' }} strokeWidth="1" />
              <Box
                component="polyline"
                points="0,120 50,96 90,128 140,70 190,104 240,60"
                fill="none"
                strokeWidth="3"
                strokeLinecap="round"
                strokeLinejoin="round"
                sx={{ stroke: (theme) => theme.palette.text.primary }}
              />
              {[
                [275, 72],
                [305, 88],
                [335, 64],
                [365, 46],
              ].map(([cx, cy]) => (
                <Box
                  key={cx}
                  component="circle"
                  cx={cx}
                  cy={cy}
                  r="4"
                  sx={{ fill: (theme) => theme.palette.brand.horizon }}
                />
              ))}
              <Box component="circle" cx="398" cy="26" r="11" sx={{ fill: (theme) => theme.palette.brand.volt }} />
            </Box>
            <Typography variant="h2" component="p" sx={{ fontSize: { md: 40, lg: 52 }, maxWidth: 480 }}>
              {t('app.authHeadline')}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 2, maxWidth: 420 }}>
              {t('app.authDescription')}
            </Typography>
            <Stack direction="row" flexWrap="wrap" sx={{ mt: 4, gap: 1 }}>
              {['clarity', 'control', 'trust'].map((pillar) => (
                <Chip key={pillar} label={t(`app.pillars.${pillar}`)} variant="outlined" />
              ))}
            </Stack>
          </Box>
          <Typography variant="caption" color="text.secondary">
            {t('app.demoNotice')}
          </Typography>
        </Box>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ p: 3 }}>
            <Box sx={{ visibility: { md: 'hidden' } }}>
              <WalliaLogo size={28} />
            </Box>
            <LanguageSelector />
          </Stack>
          <Container maxWidth="sm" sx={{ px: { xs: 3, sm: 6 }, py: { xs: 4, md: 6 } }}>
            <Typography variant="overline" color="text.secondary">
              {t('app.demo')}
            </Typography>
            <Typography variant="h3" component="h1" sx={{ mt: 1, mb: 1 }}>
              {t(`auth.${group}.title`)}
            </Typography>
            <Typography color="text.secondary" sx={{ mb: 4 }}>
              {t(signup ? 'auth.signup.hasAccount' : 'auth.login.noAccount')}{' '}
              <Button component={RouterLink} to={signup ? '/login' : '/signup'} sx={{ p: 0, minHeight: 28 }}>
                {t(signup ? 'auth.signup.login' : 'auth.login.createAccount')}
              </Button>
            </Typography>
            <Box component="form" onSubmit={submit}>
              <Stack spacing={2.5}>
                {error && <Alert severity="error">{t(error)}</Alert>}
                {fields.map((field) => (
                  <TextField
                    key={field}
                    label={t(`auth.${group}.${field}`)}
                    name={field}
                    required
                    fullWidth
                    value={values[field]}
                    onChange={(event) => setValues({ ...values, [field]: event.target.value })}
                    autoComplete={
                      {
                        firstName: 'given-name',
                        lastName: 'family-name',
                        username: 'username',
                        password: signup ? 'new-password' : 'current-password',
                        email: 'email',
                      }[field]
                    }
                    type={field === 'password' ? (visible ? 'text' : 'password') : field === 'email' ? 'email' : 'text'}
                    inputProps={{
                      minLength: signup ? (field === 'password' ? 10 : field === 'email' ? 6 : 3) : 1,
                      maxLength: field === 'password' ? 100 : field === 'username' && signup ? 20 : 50,
                      // Phones capitalize the first letter by default; "Gui_oliveira" used to fail the login.
                      ...(field === 'username' || field === 'email'
                        ? { autoCapitalize: 'none', spellCheck: false }
                        : {}),
                    }}
                    helperText={signup && field === 'password' ? t('app.passwordHelp') : undefined}
                    InputProps={
                      field === 'password'
                        ? {
                            endAdornment: (
                              <InputAdornment position="end">
                                <IconButton
                                  aria-label={t(visible ? 'common.hidePassword' : 'common.showPassword')}
                                  onClick={() => setVisible(!visible)}
                                >
                                  <WalletIcon name="eye" />
                                </IconButton>
                              </InputAdornment>
                            ),
                          }
                        : undefined
                    }
                  />
                ))}
                <LoadingButton type="submit" variant="contained" size="large" loading={busy}>
                  {t(`auth.${group}.submit`)}
                </LoadingButton>
                <Typography variant="caption" color="text.secondary">
                  {t('app.demoNotice')}
                </Typography>
              </Stack>
            </Box>
          </Container>
        </Box>
      </Box>
    </>
  );
}
AuthView.propTypes = { signup: PropTypes.bool };
