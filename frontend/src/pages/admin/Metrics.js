import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Container,
  Grid,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { useSnackbar } from 'notistack';
import HttpService from '../../services/HttpService';
import { StatBlock } from '../../components/future-ui';
import { PageHeading, SectionCard } from '../../components/wallet-ui';
import { errorKey, money } from '../../utils/wallet';

const PIX = ['PIX_PREPARE', 'PIX_CONFIRM'];

/**
 * Product numbers for the team: the north star (negative days avoided per active user per month), the
 * misses next to it, what people ask and how many want the Pix for real. Aggregates only.
 */
export default function Metrics() {
  const { t, i18n } = useTranslation();
  const { enqueueSnackbar } = useSnackbar();
  const lang = i18n.language;
  const [days, setDays] = useState(30);
  const [state, setState] = useState({ loading: true, error: null, data: null });
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setState((previous) => ({ ...previous, loading: true, error: null }));
    try {
      setState({ loading: false, error: null, data: await HttpService.getWithAuth(`/admin/metrics?days=${days}`) });
    } catch (failure) {
      setState({ loading: false, error: errorKey(failure), data: null });
    }
  }, [days]);

  useEffect(() => {
    load();
  }, [load]);

  const runSync = async () => {
    setBusy(true);
    try {
      const result = await HttpService.postWithAuth('/admin/open-finance/daily-sync');
      enqueueSnackbar(t('metrics.syncDone', { synced: result.synced, skipped: result.skipped }), {
        variant: 'success',
      });
      await load();
    } catch (failure) {
      enqueueSnackbar(t(errorKey(failure)), { variant: 'error' });
    } finally {
      setBusy(false);
    }
  };

  const { data } = state;
  const star = data?.northStar;
  const usage = data?.usage || [];
  const questions = usage.filter((row) => !PIX.includes(row.intent));
  const asked = questions.reduce((sum, row) => sum + row.count, 0);
  const unknown = questions.find((row) => row.intent === 'UNKNOWN')?.count || 0;
  const pix = (step) => usage.find((row) => row.intent === step)?.count || 0;

  return (
    <>
      <Helmet>
        <title>{t('metrics.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="lg">
        <PageHeading
          title={t('metrics.heading')}
          description={t('metrics.description')}
          action={
            <ToggleButtonGroup exclusive size="small" value={days} onChange={(_, value) => value && setDays(value)}>
              {[7, 30, 90].map((option) => (
                <ToggleButton key={option} value={option} aria-label={t('future.horizonOption', { days: option })}>
                  {t('future.horizonShort', { days: option })}
                </ToggleButton>
              ))}
            </ToggleButtonGroup>
          }
        />
        {state.error && (
          <Alert
            severity="error"
            action={
              <Button color="inherit" onClick={load}>
                {t('app.retry')}
              </Button>
            }
          >
            {t(state.error)}
          </Alert>
        )}
        {star && (
          <Stack spacing={3} aria-busy={state.loading}>
            <Card sx={{ p: { xs: 3, md: 4 } }}>
              <StatBlock
                label={t('metrics.northStar')}
                value={
                  star.avoidedPerActiveUserMonth == null
                    ? '—'
                    : Number(star.avoidedPerActiveUserMonth).toLocaleString(lang)
                }
                help={t('metrics.northStarHelp')}
                emphasis
              />
              <Grid container spacing={3} sx={{ mt: 1 }}>
                {[
                  ['activeUsers', star.activeUsers],
                  ['gradedUsers', star.gradedUsers],
                  ['avoided', star.avoided],
                  ['missed', star.missed],
                  ['meanError', star.meanAbsoluteError == null ? '—' : money(star.meanAbsoluteError, lang)],
                ].map(([key, value]) => (
                  <Grid item xs={6} md={2.4} key={key}>
                    <StatBlock
                      label={t(`metrics.${key}`)}
                      value={String(value)}
                      negative={key === 'missed' && value > 0}
                    />
                  </Grid>
                ))}
              </Grid>
            </Card>

            {/* Box keeps Stack's margin reset off the Grid's negative margins. */}
            <Box>
              <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                  <SectionCard title={t('metrics.pixTitle')}>
                    <Box sx={{ px: 3, pb: 3 }}>
                      <Typography variant="body2" color="text.secondary">
                        {t('metrics.pixHelp')}
                      </Typography>
                      <Grid container spacing={2} sx={{ mt: 1 }}>
                        <Grid item xs={6}>
                          <StatBlock label={t('metrics.pixPrepare')} value={String(pix('PIX_PREPARE'))} />
                        </Grid>
                        <Grid item xs={6}>
                          <StatBlock label={t('metrics.pixConfirm')} value={String(pix('PIX_CONFIRM'))} />
                        </Grid>
                      </Grid>
                    </Box>
                  </SectionCard>
                </Grid>
                <Grid item xs={12} md={6}>
                  <SectionCard title={t('metrics.askTitle')}>
                    <Typography variant="body2" color="text.secondary" sx={{ px: 3 }}>
                      {asked
                        ? t('metrics.askUnknown', { percent: Math.round((unknown / asked) * 100), total: asked })
                        : t('metrics.askNone')}
                    </Typography>
                    <TableContainer sx={{ px: 1, pb: 1, overflowX: 'auto' }}>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>{t('metrics.intent')}</TableCell>
                            <TableCell align="right">{t('metrics.count')}</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {questions.map((row) => (
                            <TableRow key={row.intent}>
                              <TableCell>{t(`metrics.intents.${row.intent}`, { defaultValue: row.intent })}</TableCell>
                              <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                                {row.count}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </SectionCard>
                </Grid>
              </Grid>
            </Box>

            <Box>
              <Button variant="outlined" onClick={runSync} disabled={busy}>
                {t('metrics.runSync')}
              </Button>
              <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 1 }}>
                {t('metrics.runSyncHelp')}
              </Typography>
            </Box>
          </Stack>
        )}
      </Container>
    </>
  );
}
