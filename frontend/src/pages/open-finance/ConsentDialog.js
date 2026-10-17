import PropTypes from 'prop-types';
import { useRef } from 'react';
import { Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, Stack, Typography } from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { useTranslation } from 'react-i18next';
import { IconTile, WalletIcon } from '../../components/wallet-ui';
import { dateTime } from '../../utils/wallet';

const GROUPS = [
  ['accounts', 'ACCOUNTS_'],
  ['cards', 'CREDIT_CARDS_'],
  ['loans', 'LOANS_'],
  ['resources', 'RESOURCES_'],
];

/** Permission codes grouped the way a bank shows them: accounts, cards, loans, resource registry. */
export function groupPermissions(permissions = []) {
  return GROUPS.map(([group, prefix]) => ({
    group,
    items: permissions.filter((permission) => permission.startsWith(prefix)),
  })).filter((entry) => entry.items.length);
}

export function PermissionGroups({ permissions }) {
  const { t } = useTranslation();
  return (
    <Stack spacing={2}>
      {groupPermissions(permissions).map(({ group, items }) => (
        <Box key={group}>
          <Typography variant="subtitle2" component="h3">
            {t(`openFinance.permissionGroups.${group}`)}
          </Typography>
          <Box component="ul" sx={{ m: 0, mt: 0.5, pl: 2.5 }}>
            {items.map((permission) => (
              <Typography component="li" variant="body2" color="text.secondary" key={permission}>
                {t(`openFinance.permissions.${permission}`, { defaultValue: permission })}
              </Typography>
            ))}
          </Box>
        </Box>
      ))}
    </Stack>
  );
}
PermissionGroups.propTypes = { permissions: PropTypes.arrayOf(PropTypes.string) };

/**
 * Stand-in for the institution's authorisation screen. In real Open Finance this is a redirect to the
 * bank's app (FAPI); here the sandbox plays the bank, and the header says so, so nobody mistakes it for
 * Wall.ia asking for bank credentials. It asks for none.
 */
export default function ConsentDialog({ consent, busy, error, onAuthorise, onReject, onClose }) {
  const { t, i18n } = useTranslation();
  const lang = i18n.language;
  // Keep the last consent on screen while the dialog fades out, so it does not flash an empty card.
  const last = useRef(consent);
  if (consent) last.current = consent;
  const shown = consent || last.current;
  return (
    <Dialog
      open={Boolean(consent)}
      onClose={busy ? undefined : onClose}
      fullWidth
      maxWidth="sm"
      aria-labelledby="consent-title"
      PaperProps={{ sx: { overflow: 'hidden' } }}
    >
      {shown && (
        <>
          <Box
            sx={{ px: 3, py: 2.5, bgcolor: 'background.neutral', borderBottom: '1px solid', borderColor: 'divider' }}
          >
            <Stack direction="row" spacing={2} alignItems="center">
              <IconTile name="bank" size={44} />
              <Typography id="consent-title" variant="h6" component="h2" sx={{ minWidth: 0 }}>
                {shown.institutionName}
              </Typography>
            </Stack>
            <Chip
              color="warning"
              variant="outlined"
              size="small"
              icon={<WalletIcon name="shield" />}
              label={t('openFinance.consent.sandbox')}
              sx={{ mt: 1.5, height: 'auto', '& .MuiChip-label': { whiteSpace: 'normal', py: 0.5 } }}
            />
          </Box>
          <DialogContent>
            <Stack spacing={2.5} sx={{ pt: 1 }}>
              {error && <Alert severity="error">{t(error)}</Alert>}
              <Typography variant="subtitle1">{t('openFinance.consent.asks')}</Typography>
              <PermissionGroups permissions={shown.permissions} />
              <Typography variant="body2">{t('openFinance.consent.purpose')}</Typography>
              <Typography variant="body2">
                {t('openFinance.consent.until', { date: dateTime(shown.expiresAt, lang) })}
              </Typography>
              <Alert severity="info" icon={<WalletIcon name="shield" />}>
                {t('openFinance.consent.readOnly')}
              </Alert>
              {shown.authoriseBefore && (
                <Typography variant="caption" color="text.secondary">
                  {t('openFinance.consent.authoriseBefore', { time: dateTime(shown.authoriseBefore, lang) })}
                </Typography>
              )}
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2.5, gap: 1 }}>
            <Button color="inherit" disabled={busy} onClick={onReject}>
              {t('openFinance.consent.reject')}
            </Button>
            <LoadingButton variant="contained" loading={busy} onClick={onAuthorise}>
              {t('openFinance.consent.authorise')}
            </LoadingButton>
          </DialogActions>
        </>
      )}
    </Dialog>
  );
}
ConsentDialog.propTypes = {
  consent: PropTypes.shape({
    consentId: PropTypes.string.isRequired,
    institutionName: PropTypes.string,
    permissions: PropTypes.arrayOf(PropTypes.string),
    expiresAt: PropTypes.string,
    authoriseBefore: PropTypes.string,
  }),
  busy: PropTypes.bool,
  error: PropTypes.string,
  onAuthorise: PropTypes.func.isRequired,
  onReject: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};
