import { useCallback, useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { Link as RouterLink } from 'react-router-dom';
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  Card,
  Collapse,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import HttpService from '../../services/HttpService';
import ProjectionChart from '../../components/projection-chart';
import { DailyLimitCard, EventRow, ExitPlanList, StatBlock } from '../../components/future-ui';
import { EmptyState, PageHeading, SectionCard, WalletIcon } from '../../components/wallet-ui';
import { dayLabel, errorKey, money } from '../../utils/wallet';
import Questions from './Questions';
import Simulator from './Simulator';
import Goals from './Goals';
import AlertToggle from './AlertToggle';
import Accuracy from './Accuracy';

function WhyDialog({ risk, exitPlan, onClose }) {
  const { t, i18n } = useTranslation();
  return (
    <Dialog open={Boolean(risk)} onClose={onClose} fullWidth maxWidth="sm" aria-labelledby="why-title">
      {risk && (
        <>
          <DialogTitle id="why-title">{t('future.whyTitle')}</DialogTitle>
          <DialogContent>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              {t('future.whyIntro')}
            </Typography>
            {risk.causes.map((event) => (
              <EventRow key={`${event.reference}-${event.date}`} event={event} dense />
            ))}
            <Alert severity={risk.relief ? 'info' : 'warning'} sx={{ mt: 2 }}>
              {risk.relief
                ? t('future.whyRelief', {
                    label: risk.relief.label,
                    date: dayLabel(risk.relief.date, i18n.language),
                    amount: money(risk.relief.amount, i18n.language),
                  })
                : t('future.whyNoRelief')}
            </Alert>
            <ExitPlanList actions={exitPlan} />
            <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 2 }}>
              {t('future.whyFooter')}
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={onClose}>{t('common.close')}</Button>
          </DialogActions>
        </>
      )}
    </Dialog>
  );
}

WhyDialog.propTypes = { risk: PropTypes.object, exitPlan: PropTypes.array, onClose: PropTypes.func.isRequired };

export default function Future() {
  const theme = useTheme();
  const { t, i18n } = useTranslation();
  const [horizon, setHorizon] = useState(30);
  const [state, setState] = useState({ loading: true, error: null, overview: null, recurrences: [] });
  const [why, setWhy] = useState(null);
  const [showDays, setShowDays] = useState(false);
  const [openGroup, setOpenGroup] = useState(null);

  // Clicking 30, 90, 60 quickly must not leave the 90-day answer under the 60 button.
  const latest = useRef(0);
  const load = useCallback(async () => {
    latest.current += 1;
    const request = latest.current;
    setState((previous) => ({ ...previous, loading: true, error: null }));
    try {
      const [overview, recurrences] = await Promise.all([
        HttpService.getWithAuth(`/future/overview?horizon=${horizon}`),
        HttpService.getWithAuth('/future/recurrences'),
      ]);
      if (request === latest.current) setState({ loading: false, error: null, overview, recurrences });
    } catch (failure) {
      if (request === latest.current)
        setState((previous) => ({ ...previous, loading: false, error: errorKey(failure), overview: null }));
    }
  }, [horizon]);

  useEffect(() => {
    load();
  }, [load]);

  const { overview, recurrences } = state;
  const lang = i18n.language;
  const projection = overview?.projection;
  const incomes = recurrences.filter((item) => item.direction === 'IN' && item.status === 'CONFIRMED');
  const sources = overview?.sources;
  // Nothing to project from: zeros and a flat line would read as "you have R$ 0", not "we know nothing yet".
  const noData =
    sources &&
    sources.confirmedRecurrences === 0 &&
    sources.pendingQuestions === 0 &&
    sources.cards === 0 &&
    sources.loans === 0;

  return (
    <>
      <Helmet>
        <title>{t('future.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="lg">
        <PageHeading
          title={t('future.heading')}
          description={t('future.description')}
          action={
            <ToggleButtonGroup
              exclusive
              size="small"
              value={horizon}
              onChange={(_, value) => value && setHorizon(value)}
              aria-label={t('future.horizonLabel')}
            >
              {[30, 60, 90].map((days) => (
                <ToggleButton key={days} value={days} sx={{ px: 1.5 }} aria-label={t('future.horizonOption', { days })}>
                  {t('future.horizonShort', { days })}
                </ToggleButton>
              ))}
            </ToggleButtonGroup>
          }
        />

        {state.loading && !overview && (
          <Typography role="status" color="text.secondary">
            {t('common.loading')}
          </Typography>
        )}
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

        {noData && (
          <Card>
            <EmptyState
              icon="bank"
              title={t('future.noData')}
              description={t('future.noDataHelp')}
              action={
                <Button component={RouterLink} to="/open-finance" variant="contained">
                  {t('openFinance.connectShort')}
                </Button>
              }
            />
          </Card>
        )}

        {overview && !noData && (
          <Stack
            spacing={3}
            aria-busy={state.loading}
            sx={{ opacity: state.loading ? 0.6 : 1, transition: 'opacity 150ms' }}
          >
            {/* The one answer people open this page for; the numbers below explain it. */}
            {overview.spendingLimit && overview.sources.confirmedRecurrences > 0 && (
              <>
                <DailyLimitCard limit={overview.spendingLimit} risk={projection.riskWindows[0]} onWhy={setWhy} />
                <AlertToggle />
              </>
            )}
            <Card sx={{ p: { xs: 3, md: 4 } }}>
              <Grid container spacing={3}>
                <Grid item xs={12} md={4}>
                  <StatBlock
                    label={t('future.balanceNow')}
                    value={money(overview.balance, lang)}
                    help={t('future.balanceNowHelp')}
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <StatBlock
                    label={t('future.committed')}
                    value={money(overview.committed.total, lang)}
                    help={t('future.committedHelp', { date: dayLabel(overview.committed.until, lang) })}
                  >
                    <Typography variant="caption" color="text.secondary" component="p">
                      {t('future.committedSplit', {
                        confirmed: money(overview.committed.confirmed, lang),
                        estimated: money(overview.committed.estimated, lang),
                      })}
                    </Typography>
                  </StatBlock>
                </Grid>
                <Grid item xs={12} md={4}>
                  <StatBlock
                    label={t('future.free')}
                    value={money(overview.freeAfterCommitments, lang)}
                    emphasis={Number(overview.freeAfterCommitments) >= 0}
                    negative={Number(overview.freeAfterCommitments) < 0}
                    help={t(Number(overview.freeAfterCommitments) < 0 ? 'future.freeNegativeHelp' : 'future.freeHelp')}
                  />
                </Grid>
              </Grid>
            </Card>

            {projection.riskWindows.map((risk) => (
              <Alert
                key={risk.start}
                severity="warning"
                icon={<WalletIcon name="info" />}
                action={
                  <Button color="inherit" onClick={() => setWhy(risk)} sx={{ whiteSpace: 'nowrap' }}>
                    {t('future.whyButton')}
                  </Button>
                }
              >
                <AlertTitle>{t('future.riskTitle')}</AlertTitle>
                {t(risk.untilHorizon ? 'future.riskUntilHorizon' : 'future.risk', {
                  start: dayLabel(risk.start, lang),
                  end: dayLabel(risk.end, lang),
                  amount: money(Math.abs(Number(risk.lowestBalance)), lang),
                })}{' '}
                {t('future.riskReason')}
                {Number(risk.overdraftCostCap) > 0 &&
                  ` ${t('future.limit.cost', { amount: money(risk.overdraftCostCap, lang) })}`}
              </Alert>
            ))}
            {!projection.riskWindows.length && overview.sources.confirmedRecurrences > 0 && (
              <Alert severity="success">
                <AlertTitle>{t('future.allClear', { days: horizon })}</AlertTitle>
                {t('future.allClearHelp', {
                  amount: money(projection.minBalance, lang),
                  date: dayLabel(projection.minDate, lang),
                })}
              </Alert>
            )}
            {overview.sources.confirmedRecurrences === 0 && overview.sources.pendingQuestions > 0 && (
              <Alert severity="info">
                <AlertTitle>{t('future.needsAnswers')}</AlertTitle>
                {t('future.needsAnswersHelp')}
              </Alert>
            )}

            {/* Box keeps Stack's margin reset off the Grid's negative margins. */}
            <Box>
              <Grid container spacing={3}>
                <Grid item xs={12} md={7}>
                  <SectionCard title={t('future.chartTitle')}>
                    <Box sx={{ px: { xs: 1, sm: 3 }, pb: 2 }}>
                      <ProjectionChart
                        ariaLabel={t('future.chartLabel', {
                          amount: money(projection.minBalance, lang),
                          date: dayLabel(projection.minDate, lang),
                          end: money(projection.endBalance, lang),
                        })}
                        series={[
                          {
                            key: 'projection',
                            color: theme.palette.chart.projection,
                            days: projection.days.map((day) => ({ date: day.date, balance: Number(day.balance) })),
                          },
                        ]}
                        lowest={{ date: projection.minDate, balance: Number(projection.minBalance) }}
                      />
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        component="p"
                        sx={{ mt: 1, px: { xs: 2, sm: 0 } }}
                      >
                        {t('future.chartLegend')}
                      </Typography>
                      <Button
                        size="small"
                        onClick={() => setShowDays(!showDays)}
                        aria-expanded={showDays}
                        sx={{ mt: 1, px: { xs: 2, sm: 0 } }}
                      >
                        {t(showDays ? 'future.hideDays' : 'future.showDays')}
                      </Button>
                      <Collapse in={showDays} unmountOnExit>
                        <Box sx={{ px: { xs: 2, sm: 0 } }}>
                          {projection.events.map((event) => (
                            <EventRow key={`${event.reference}-${event.date}`} event={event} dense />
                          ))}
                        </Box>
                      </Collapse>
                    </Box>
                  </SectionCard>
                </Grid>
                <Grid item xs={12} md={5}>
                  <SectionCard
                    title={t('future.whereItGoes')}
                    action={
                      <Button component={RouterLink} to="/commitments" size="small">
                        {t('navigation.commitments')}
                      </Button>
                    }
                  >
                    {overview.committed.groups.length ? (
                      <Box sx={{ pb: 1 }}>
                        {overview.committed.groups.map((group) => (
                          <Box key={group.category} sx={{ borderTop: '1px solid', borderColor: 'divider' }}>
                            <Button
                              fullWidth
                              color="inherit"
                              onClick={() => setOpenGroup(openGroup === group.category ? null : group.category)}
                              aria-expanded={openGroup === group.category}
                              sx={{ justifyContent: 'space-between', px: 3, py: 1.5, borderRadius: 0 }}
                            >
                              <span>{t(`future.categories.${group.category}`, { defaultValue: group.category })}</span>
                              <Typography
                                component="span"
                                variant="subtitle2"
                                sx={{ fontVariantNumeric: 'tabular-nums' }}
                              >
                                {money(group.total, lang)}
                              </Typography>
                            </Button>
                            <Collapse in={openGroup === group.category} unmountOnExit>
                              <Box sx={{ px: 3, pb: 1 }}>
                                {group.items.map((event) => (
                                  <EventRow key={`${event.reference}-${event.date}`} event={event} dense />
                                ))}
                              </Box>
                            </Collapse>
                          </Box>
                        ))}
                      </Box>
                    ) : (
                      <EmptyState
                        icon="bank"
                        title={t('future.noCommitments')}
                        description={t('future.noCommitmentsHelp')}
                        action={
                          <Button component={RouterLink} to="/open-finance" variant="outlined">
                            {t('openFinance.connectShort')}
                          </Button>
                        }
                      />
                    )}
                  </SectionCard>
                </Grid>
              </Grid>
            </Box>

            <Accuracy />
            <Questions
              recurrences={recurrences}
              dailySpending={overview.dailySpending}
              cardSpending={overview.cardSpending}
              onAnswered={load}
            />
            <Goals onChanged={load} />
            <Simulator incomes={incomes} hasCard={overview.sources.cards > 0} />
          </Stack>
        )}
      </Container>
      <WhyDialog
        risk={why}
        // The plan is calculated for the first window only.
        exitPlan={why && why.start === projection?.riskWindows[0]?.start ? overview?.exitPlan : null}
        onClose={() => setWhy(null)}
      />
    </>
  );
}
