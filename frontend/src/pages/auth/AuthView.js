import { useRef, useState } from 'react';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
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
import { WalletIcon } from '../../components/wallet-ui';
import AuthService from '../../services/AuthService';
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
        const from = location.state?.from;
        navigate(
          typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') && !from.startsWith('/login')
            ? from
            : '/',
          { replace: true }
        );
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
            bgcolor: '#1D1D1F',
            color: 'white',
            p: 6,
            flexDirection: 'column',
            justifyContent: 'space-between',
            overflow: 'hidden',
          }}
        >
          <Stack direction="row" alignItems="center" spacing={1.5}>
            <WalletIcon name="wallet" />
            <Typography variant="h6">e-wallet</Typography>
          </Stack>
          <Box>
            <Box aria-hidden="true" sx={{ position: 'relative', height: 230, mb: 6 }}>
              {['#899C89', '#738DAB', '#D6CAB6'].map((color, index) => (
                <Box
                  key={color}
                  sx={{
                    height: 175,
                    position: 'absolute',
                    inset: `${index * 28}px 0 auto`,
                    transform: `rotate(${-10 + index * 6}deg)`,
                    borderRadius: '24px',
                    background: `linear-gradient(120deg,${color},#35373D)`,
                    boxShadow: '0 16px 32px #00000030',
                    p: 3,
                  }}
                >
                  <WalletIcon name="wallet" sx={{ opacity: 0.7 }} />
                </Box>
              ))}
            </Box>
            <Typography variant="h2" sx={{ fontSize: { md: 42, lg: 54 }, maxWidth: 450 }}>
              {t('app.authHeadline')}
            </Typography>
            <Typography sx={{ mt: 2, color: '#FFFFFFAD', maxWidth: 380 }}>{t('app.authDescription')}</Typography>
          </Box>
          <Typography variant="caption" sx={{ color: '#FFFFFF99' }}>
            {t('app.demoNotice')}
          </Typography>
        </Box>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ p: 3 }}>
            <Typography variant="subtitle1" sx={{ visibility: { md: 'hidden' } }}>
              e-wallet
            </Typography>
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
                      minLength: signup ? (field === 'password' || field === 'email' ? 6 : 3) : 1,
                      maxLength: field === 'password' ? 100 : field === 'username' ? 20 : 50,
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
