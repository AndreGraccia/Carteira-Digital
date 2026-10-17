import { useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { Alert, Box, Button, Stack, TextField, Typography } from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { useTranslation } from 'react-i18next';
import AuthService from '../../services/AuthService';
import { errorKey } from '../../utils/wallet';

const secondsUntil = (instant, now) =>
  instant ? Math.max(0, Math.ceil((new Date(instant).getTime() - now) / 1000)) : 0;

/** When the server says "wait", Retry-After (seconds) says until when. */
const retryAfter = (failure) => {
  const seconds = Number(failure.response?.headers?.['retry-after']);
  return Number.isFinite(seconds) && seconds > 0 ? new Date(Date.now() + seconds * 1000).toISOString() : null;
};

/**
 * Second step of sign-up (and of a login to an unverified account): the 8-digit code sent by e-mail.
 * On success `onVerified` signs in with the password typed a moment ago, which lives only in the parent's
 * state (ADR 0008: the code is never a password substitute, so verify returns no token).
 */
export default function EmailCode({ login, maskedEmail, resendAvailableAt, onVerified, onBack }) {
  const { t } = useTranslation();
  const [code, setCode] = useState('');
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [busy, setBusy] = useState(false);
  const [resending, setResending] = useState(false);
  const [resendAt, setResendAt] = useState(resendAvailableAt);
  const [now, setNow] = useState(Date.now());
  const input = useRef(null);
  const wait = secondsUntil(resendAt, now);

  // Countdown until the next code may be requested: a timer is an external system, hence the effect.
  useEffect(() => {
    if (!resendAt) return undefined;
    setNow(Date.now());
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [resendAt]);

  const submit = async (event) => {
    event.preventDefault();
    if (busy || code.length !== 8) return;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await AuthService.verifyEmail(login, code);
      await onVerified();
    } catch (failure) {
      const key = errorKey(failure);
      // Verified in another tab or a previous attempt: nothing left to confirm, go on to sign in.
      if (key === 'app.errors.EMAIL_ALREADY_VERIFIED') {
        await onVerified();
        return;
      }
      setError(key);
      input.current?.focus();
    } finally {
      setBusy(false);
    }
  };

  const resend = async () => {
    if (resending || wait > 0) return;
    setResending(true);
    setError(null);
    setNotice(null);
    try {
      const answer = await AuthService.resendCode(login);
      setResendAt(answer?.resendAvailableAt || null);
      setCode('');
      setNotice('auth.verify.resent');
      input.current?.focus();
    } catch (failure) {
      setError(errorKey(failure));
      const until = retryAfter(failure);
      if (until) setResendAt(until);
    } finally {
      setResending(false);
    }
  };

  return (
    <Box component="form" onSubmit={submit} noValidate>
      <Stack spacing={2.5}>
        <Typography color="text.secondary">
          {maskedEmail ? t('auth.verify.sentTo', { email: maskedEmail }) : t('auth.verify.sentUnknown', { login })}
        </Typography>
        {error && <Alert severity="error">{t(error)}</Alert>}
        {notice && (
          <Alert severity="success" role="status">
            {t(notice)}
          </Alert>
        )}
        <TextField
          inputRef={input}
          autoFocus
          label={t('auth.verify.code')}
          name="code"
          value={code}
          onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 8))}
          helperText={t('auth.verify.codeHelp')}
          error={Boolean(error)}
          inputProps={{
            inputMode: 'numeric',
            autoComplete: 'one-time-code',
            pattern: '[0-9]{8}',
            maxLength: 8,
            style: { letterSpacing: '0.3em', fontVariantNumeric: 'tabular-nums' },
          }}
          fullWidth
        />
        <LoadingButton type="submit" variant="contained" size="large" loading={busy} disabled={code.length !== 8}>
          {t('auth.verify.submit')}
        </LoadingButton>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ gap: 1, flexWrap: 'wrap' }}>
          <Button onClick={onBack} color="inherit">
            {t('auth.verify.back')}
          </Button>
          <LoadingButton onClick={resend} loading={resending} disabled={wait > 0}>
            {wait > 0 ? t('auth.verify.resendIn', { seconds: wait }) : t('auth.verify.resend')}
          </LoadingButton>
        </Stack>
        <Typography variant="caption" color="text.secondary">
          {t('auth.verify.spam')}
        </Typography>
      </Stack>
    </Box>
  );
}
EmailCode.propTypes = {
  login: PropTypes.string.isRequired,
  maskedEmail: PropTypes.string,
  resendAvailableAt: PropTypes.string,
  onVerified: PropTypes.func.isRequired,
  onBack: PropTypes.func.isRequired,
};
