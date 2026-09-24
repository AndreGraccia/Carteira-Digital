import { useState } from 'react';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { Button, Card, Container, Grid, Stack, Typography } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { enqueueSnackbar } from 'notistack';
import { useWallets } from '../../context/WalletContext';
import WalletCard from '../../components/wallet-card';
import TransactionDetail from '../../components/transaction-detail';
import { ActivityList, DataState, EmptyState, PageHeading, SectionCard, WalletIcon } from '../../components/wallet-ui';

export default function Wallet() {
  const { t } = useTranslation();
  const { wallets, transactions } = useWallets();
  const { walletId } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState(null);
  const selected = wallets.find((item) => String(item.id) === walletId) || (!walletId ? wallets[0] : null);
  const activity = selected
    ? transactions.filter((item) => item.fromWallet?.id === selected.id || item.toWallet?.id === selected.id)
    : [];
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(selected.iban);
      enqueueSnackbar(t('app.copied'), { variant: 'success' });
    } catch {
      enqueueSnackbar(t('app.copyHelp'), { variant: 'info' });
    }
  };
  return (
    <>
      <Helmet>
        <title>{t('wallet.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="lg">
        <PageHeading
          title={t('wallet.heading')}
          description={t('app.walletHelp')}
          action={
            <Button component={RouterLink} to="/wallets/new" variant="contained" startIcon={<WalletIcon name="plus" />}>
              {t('wallet.newWallet')}
            </Button>
          }
        />
        <DataState>
          {!wallets.length ? (
            <Card>
              <EmptyState
                title={t('app.firstWallet')}
                description={t('app.firstWalletHelp')}
                action={
                  <Button component={RouterLink} to="/wallets/new" variant="contained">
                    {t('wallet.newWallet')}
                  </Button>
                }
              />
            </Card>
          ) : !selected ? (
            <EmptyState
              title={t('app.errors.notFound')}
              action={
                <Button component={RouterLink} to="/wallets">
                  {t('navigation.wallets')}
                </Button>
              }
            />
          ) : (
            <Grid container spacing={4}>
              <Grid item xs={12} md={5}>
                <Stack spacing={2}>
                  {wallets.map((wallet, index) => (
                    <WalletCard
                      key={wallet.id}
                      wallet={wallet}
                      index={index}
                      selected={selected.id === wallet.id}
                      onClick={() => navigate(`/wallets/${wallet.id}`, { replace: true })}
                    />
                  ))}
                </Stack>
              </Grid>
              <Grid item xs={12} md={7}>
                <Card sx={{ mb: 3, p: 3 }}>
                  <Typography variant="h5">{selected.name}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    {t('app.walletCode')}
                  </Typography>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                    <Typography
                      variant="body2"
                      sx={{ flex: 1, wordBreak: 'break-all', fontFamily: 'monospace', userSelect: 'all' }}
                    >
                      {selected.iban}
                    </Typography>
                    <Button onClick={copy} aria-label={t('app.copy')} sx={{ minWidth: 44 }}>
                      <WalletIcon name="copy" />
                    </Button>
                  </Stack>
                  <Typography variant="caption" color="text.secondary">
                    {t(selected.source === 'OPEN_FINANCE' ? 'openFinance.readOnlyAccount' : 'app.codeHelp')}
                  </Typography>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mt: 3 }}>
                    {selected.source === 'OPEN_FINANCE' ? (
                      <>
                        <Button
                          component={RouterLink}
                          to="/open-finance"
                          variant="contained"
                          fullWidth
                          startIcon={<WalletIcon name="sync" />}
                        >
                          {t('openFinance.sync')}
                        </Button>
                        <Button
                          component={RouterLink}
                          to="/insights"
                          variant="outlined"
                          fullWidth
                          startIcon={<WalletIcon name="insights" />}
                        >
                          {t('navigation.insights')}
                        </Button>
                      </>
                    ) : (
                      <Button
                        component={RouterLink}
                        to="/envelopes"
                        variant="contained"
                        fullWidth
                        startIcon={<WalletIcon name="envelope" />}
                      >
                        {t('navigation.envelopes')}
                      </Button>
                    )}
                  </Stack>
                </Card>
                <SectionCard title={t('wallet.statement.title')}>
                  <ActivityList transactions={activity.slice(0, 8)} wallets={[selected]} onSelect={setDetail} />
                  {activity.length > 8 && (
                    <Button fullWidth component={RouterLink} to={`/transactions?wallet=${selected.id}`} sx={{ py: 2 }}>
                      {t('app.seeAll')}
                    </Button>
                  )}
                </SectionCard>
              </Grid>
            </Grid>
          )}
        </DataState>
      </Container>
      <TransactionDetail transaction={detail} onClose={() => setDetail(null)} />
    </>
  );
}
