import { useState } from 'react';
import PropTypes from 'prop-types';
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Link, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

/**
 * How to cancel a subscription yourself: a ready message and the official complaint channel. The paid
 * version of this (a company cancels for a cut of the savings) would put Wall.ia on the side of a
 * commission; this one costs the user nothing and Wall.ia never acts for them.
 */
export default function CancelGuide({ label }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const message = t('future.cancel.message', { label });

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(message);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  };

  return (
    <>
      <Button size="small" onClick={() => setOpen(true)} sx={{ py: 0, mb: 1 }}>
        {t('future.cancel.open', { label })}
      </Button>
      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm" aria-labelledby="cancel-title">
        <DialogTitle id="cancel-title">{t('future.cancel.title', { label })}</DialogTitle>
        <DialogContent>
          <Typography component="ol" variant="body2" sx={{ m: 0, pl: 2.5, '& li': { py: 0.5 } }}>
            <li>{t('future.cancel.step1')}</li>
            <li>{t('future.cancel.step2')}</li>
            <li>
              {t('future.cancel.step3')}{' '}
              <Link href="https://www.consumidor.gov.br" target="_blank" rel="noopener noreferrer">
                consumidor.gov.br
              </Link>
            </li>
            <li>{t('future.cancel.step4')}</li>
          </Typography>
          <Typography variant="subtitle2" component="h3" sx={{ mt: 2 }}>
            {t('future.cancel.messageTitle')}
          </Typography>
          <Typography
            variant="body2"
            sx={{ mt: 1, p: 2, borderRadius: 2, bgcolor: 'background.neutral', whiteSpace: 'pre-wrap' }}
          >
            {message}
          </Typography>
          {copied && (
            <Alert severity="success" role="status" sx={{ mt: 1.5 }}>
              {t('future.cancel.copied')}
            </Alert>
          )}
          <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 2 }}>
            {t('future.cancel.note')}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>{t('common.close')}</Button>
          <Button variant="contained" onClick={copy}>
            {t('future.cancel.copy')}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
CancelGuide.propTypes = { label: PropTypes.string.isRequired };
