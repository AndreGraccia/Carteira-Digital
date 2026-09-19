import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Card, Container, Grid, IconButton, Stack, Typography } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { useWallets } from '../../context/WalletContext';
import { ActivityList, DataState, EmptyState, PageHeading, SectionCard, WalletIcon } from '../../components/wallet-ui';
import WalletCard from '../../components/wallet-card';
import TransactionDetail from '../../components/transaction-detail';
import AuthService from '../../services/AuthService';
import { money, totalsFor } from '../../utils/wallet';

export default function Dashboard() {
  const { t, i18n } = useTranslation();
  const { wallets, transactions } = useWallets();
  const [hidden, setHidden] = useState(false);
  const [detail, setDetail] = useState(null);
  const totals = totalsFor(transactions, wallets);
  const total = wallets.reduce((sum, wallet) => sum + Math.round(Number(wallet.balance) * 100), 0) / 100;
  const user = AuthService.getCurrentUser();
  return (
    <>
      <Helmet>
        <title>{t('dashboard.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="lg">
        <PageHeading
          title={t('app.greeting', { name: user?.firstName || t('app.you') })}
          description={t('app.overviewHelp')}
        />
        <DataState>
          <Grid container spacing={3}>
            <Grid item xs={12} md={5}>
              <Card sx={{ p: { xs: 3, sm: 4 }, mb: 3 }}>
                <Stack direction="row" alignItems="center" justifyContent="space-between">
                  <Typography color="text.secondary" variant="body2">
                    {t('wallet.stack.totalBalance')}
                  </Typography>
                  <IconButton
                    aria-label={t(hidden ? 'app.showBalance' : 'app.hideBalance')}
                    aria-pressed={hidden}
                    onClick={() => setHidden(!hidden)}
                  >
                    <WalletIcon name="eye" sx={{ fontSize: 20 }} />
                  </IconButton>
                </Stack>
                <Typography
                  variant="h2"
                  sx={{
                    my: 2,
                    fontSize: { xs: '2.5rem', sm: '3rem' },
                    overflowWrap: 'anywhere',
                    fontVariantNumeric: 'tabular-nums',
                  }}
                >
                  {hidden ? '••••••' : money(total, i18n.language)}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {t('app.balanceHelp')}
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mt: 3 }}>
                  <Button
                    component={RouterLink}
                    to="/transfers?mode=transfer"
                    variant="contained"
                    fullWidth
                    startIcon={<WalletIcon name="send" />}
                  >
                    {t('app.send')}
                  </Button>
                  <Button
                    component={RouterLink}
                    to="/transfers?mode=deposit"
                    variant="outlined"
                    fullWidth
                    startIcon={<WalletIcon name="plus" />}
                  >
                    {t('app.add')}
                  </Button>
                </Stack>
                <Stack
                  direction="row"
                  spacing={3}
                  sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider' }}
                >
                  {['incoming', 'outgoing'].map((key) => (
                    <Box key={key} sx={{ flex: 1, minWidth: 0 }}>
                      <Typography variant="caption" color="text.secondary">
                        {t(`app.${key}`)}
                      </Typography>
                      <Typography variant="subtitle1" sx={{ overflowWrap: 'anywhere' }}>
                        {hidden ? '••••' : money(totals[key] / 100, i18n.language)}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              </Card>
              <SectionCard
                title={t('navigation.wallets')}
                action={
                  <Button component={RouterLink} to="/wallets">
                    {t('app.seeAll')}
                  </Button>
                }
              >
                {wallets.length ? (
                  <Box sx={{ p: 3, pt: 1 }}>
                    {wallets.slice(0, 3).map((wallet, index) => (
                      <Box
                        key={wallet.id}
                        sx={{
                          mt: index ? '-105px' : 0,
                          position: 'relative',
                          zIndex: index,
                          '&:focus-within': { zIndex: 5 },
                        }}
                      >
                        <WalletCard wallet={wallet} index={index} hideBalance={hidden} href={`/wallets/${wallet.id}`} />
                      </Box>
                    ))}
                  </Box>
                ) : (
                  <EmptyState
                    title={t('app.firstWallet')}
                    description={t('app.firstWalletHelp')}
                    action={
                      <Button component={RouterLink} to="/wallets/new">
                        {t('wallet.newWallet')}
                      </Button>
                    }
                  />
                )}
              </SectionCard>
            </Grid>
            <Grid item xs={12} md={7}>
              <SectionCard
                title={t('wallet.statement.recent')}
                action={
                  <Button component={RouterLink} to="/transactions">
                    {t('app.seeAll')}
                  </Button>
                }
              >
                <ActivityList transactions={transactions.slice(0, 6)} wallets={wallets} onSelect={setDetail} />
              </SectionCard>
              <Card sx={{ mt: 3, p: 3, bgcolor: '#EDEDF2', boxShadow: 'none', border: 0 }}>
                <Stack direction="row" spacing={2}>
                  <WalletIcon name="info" sx={{ color: 'text.secondary' }} />
                  <Box>
                    <Typography variant="subtitle2">{t('app.learnTitle')}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 1 }}>
                      {t('app.demoNotice')}
                    </Typography>
                    <Button component={RouterLink} to="/about" size="small" sx={{ p: 0, minHeight: 32 }}>
                      {t('app.learnMore')}
                    </Button>
                  </Box>
                </Stack>
              </Card>
            </Grid>
          </Grid>
        </DataState>
      </Container>
      <TransactionDetail transaction={detail} onClose={() => setDetail(null)} />
    </>
  );
}
