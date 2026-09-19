import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, Container, MenuItem, Stack, TablePagination, TextField } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { useWallets } from '../../context/WalletContext';
import { ActivityList, DataState, PageHeading } from '../../components/wallet-ui';
import TransactionDetail from '../../components/transaction-detail';

export default function Transaction() {
  const { t } = useTranslation();
  const { wallets, transactions } = useWallets();
  const [params, setParams] = useSearchParams();
  const requestedWallet = params.get('wallet') || '';
  const selected = wallets.some((wallet) => String(wallet.id) === requestedWallet) ? requestedWallet : '';
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [detail, setDetail] = useState(null);
  const visible = transactions.filter(
    (item) =>
      (!selected || String(item.fromWallet?.id) === selected || String(item.toWallet?.id) === selected) &&
      [
        item.description,
        item.fromWallet?.name,
        item.toWallet?.name,
        t(`app.types.${item.type?.name}`, { defaultValue: '' }),
      ]
        .join(' ')
        .toLocaleLowerCase()
        .includes(query.toLocaleLowerCase())
  );
  const scope = selected ? wallets.filter((wallet) => String(wallet.id) === selected) : wallets;
  const currentPage = Math.min(page, Math.max(0, Math.ceil(visible.length / 10) - 1));
  return (
    <>
      <Helmet>
        <title>{t('transactions.pageTitle')}</title>
      </Helmet>
      <Container maxWidth="lg">
        <PageHeading title={t('transactions.heading')} description={t('app.activityHelp')} />
        <DataState>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3 }}>
            <TextField
              label={t('common.search')}
              placeholder={t('app.searchActivity')}
              value={query}
              onChange={(event) => {
                setQuery(event.target.value);
                setPage(0);
              }}
              fullWidth
            />
            <TextField
              select
              label={t('transfer.wallet')}
              value={wallets.some((wallet) => String(wallet.id) === selected) ? selected : ''}
              onChange={(event) => {
                setParams(event.target.value ? { wallet: event.target.value } : {});
                setPage(0);
              }}
              sx={{ minWidth: 220 }}
            >
              <MenuItem value="">{t('app.allWallets')}</MenuItem>
              {wallets.map((wallet) => (
                <MenuItem value={String(wallet.id)} key={wallet.id}>
                  {wallet.name}
                </MenuItem>
              ))}
            </TextField>
          </Stack>
          <Card>
            <ActivityList
              transactions={visible.slice(currentPage * 10, currentPage * 10 + 10)}
              wallets={scope}
              onSelect={setDetail}
            />
            <TablePagination
              component="div"
              count={visible.length}
              rowsPerPage={10}
              rowsPerPageOptions={[10]}
              page={currentPage}
              onPageChange={(_, value) => setPage(value)}
            />
          </Card>
        </DataState>
      </Container>
      <TransactionDetail transaction={detail} onClose={() => setDetail(null)} />
    </>
  );
}
