import {
  Box,
  Card,
  Chip,
  Divider,
  Stack,
  Typography,
} from '@mui/material';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import Iconify from '../iconify';

function getDirection(transaction, wallet) {
  const walletIban = wallet?.iban;
  const sourceIban = transaction.fromWallet?.iban;
  const targetIban = transaction.toWallet?.iban;

  if (
    sourceIban === walletIban &&
    targetIban !== walletIban
  ) {
    return 'outgoing';
  }

  if (
    targetIban === walletIban &&
    sourceIban !== walletIban
  ) {
    return 'incoming';
  }

  return 'adjustment';
}

function belongsToWallet(transaction, wallet) {
  return (
    transaction.fromWallet?.iban === wallet?.iban ||
    transaction.toWallet?.iban === wallet?.iban
  );
}

function formatDate(value, language) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(language, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export default function WalletStatement({
  wallet,
  transactions,
}) {
  const { t, i18n } = useTranslation();

  const language = i18n.resolvedLanguage?.startsWith('en')
    ? 'en-US'
    : 'pt-BR';

  const formatAmount = (value) =>
    new Intl.NumberFormat(language, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Number(value) || 0);

  if (!wallet) {
    return (
      <Card
        sx={{
          minHeight: 420,
          fontVariantNumeric: 'tabular-nums',
          p: 4,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
          borderRadius: '28px',
          bgcolor: 'background.paper',
          border: '1px solid rgba(29,29,31,0.08)',
          boxShadow: 'none',
        }}
      >
        <Stack
          alignItems="center"
          spacing={2}
          sx={{ maxWidth: 330 }}
        >
          <Box
            sx={{
              width: 64,
              height: 64,
              display: 'grid',
              placeItems: 'center',
              borderRadius: '20px',
              color: '#0A84FF',
              bgcolor: 'rgba(10,132,255,0.1)',
            }}
          >
            <Iconify
              icon="material-symbols:wallet-rounded"
              width={32}
            />
          </Box>

          <Typography variant="h6">
            {t('wallet.statement.selectTitle')}
          </Typography>

          <Typography
            variant="body2"
            sx={{ color: 'text.secondary' }}
          >
            {t('wallet.statement.selectDescription')}
          </Typography>
        </Stack>
      </Card>
    );
  }

  const walletTransactions = transactions
    .filter((transaction) =>
      belongsToWallet(transaction, wallet)
    )
    .slice()
    .reverse();

  const successfulTransactions =
    walletTransactions.filter(
      (transaction) =>
        !transaction.status ||
        transaction.status === 'SUCCESS'
    );

  const spent = successfulTransactions
    .filter(
      (transaction) =>
        getDirection(transaction, wallet) === 'outgoing'
    )
    .reduce(
      (total, transaction) =>
        total + Number(transaction.amount || 0),
      0
    );

  const received = successfulTransactions
    .filter(
      (transaction) =>
        getDirection(transaction, wallet) === 'incoming'
    )
    .reduce(
      (total, transaction) =>
        total + Number(transaction.amount || 0),
      0
    );

  return (
    <Card
      sx={{
        minHeight: 420,
        fontVariantNumeric: 'tabular-nums',
        p: { xs: 2.5, sm: 4 },
        borderRadius: '28px',
        bgcolor: 'background.paper',
        border: '1px solid rgba(29,29,31,0.08)',
        boxShadow: '0 4px 24px rgba(29,29,31,0.04)',
        animation: 'statementIn 280ms ease both',

        '@keyframes statementIn': {
          from: {
            opacity: 0,
            transform: 'translateY(14px)',
          },
          to: {
            opacity: 1,
            transform: 'translateY(0)',
          },
        },
      }}
    >
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        spacing={2}
        sx={{ mb: 3 }}
      >
        <Box>
          <Typography
            variant="h5"
            sx={{ fontWeight: 700 }}
          >
            {t('wallet.statement.title')}
          </Typography>

          <Typography
            variant="body2"
            sx={{ color: 'text.secondary' }}
          >
            {wallet.name}
          </Typography>
        </Box>

        <Chip
          label={wallet.iban}
          sx={{
            maxWidth: 250,
            fontFamily: 'monospace',
            bgcolor: 'rgba(15,23,42,0.06)',
          }}
        />
      </Stack>

      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{ mb: 4 }}
      >
        <Box
          sx={{
            flex: 1,
            p: 2.25,
            borderRadius: '18px',
            bgcolor: 'background.default',
            minWidth: 0,
            overflowWrap: 'anywhere',
          }}
        >
          <Typography
            variant="caption"
            sx={{ color: 'text.secondary' }}
          >
            {t('wallet.statement.spent')}
          </Typography>

          <Typography
            variant="h5"
            sx={{ color: '#D92D20', fontWeight: 700 }}
          >
            − {formatAmount(spent)}
          </Typography>
        </Box>

        <Box
          sx={{
            flex: 1,
            p: 2.25,
            borderRadius: '18px',
            bgcolor: 'background.default',
            minWidth: 0,
            overflowWrap: 'anywhere',
          }}
        >
          <Typography
            variant="caption"
            sx={{ color: 'text.secondary' }}
          >
            {t('wallet.statement.received')}
          </Typography>

          <Typography
            variant="h5"
            sx={{ color: '#15803D', fontWeight: 700 }}
          >
            + {formatAmount(received)}
          </Typography>
        </Box>
      </Stack>

      <Typography
        variant="subtitle1"
        sx={{ mb: 1.5, fontWeight: 700 }}
      >
        {t('wallet.statement.recent')}
      </Typography>

      {walletTransactions.length === 0 ? (
        <Box
          sx={{
            py: 5,
            textAlign: 'center',
            color: 'text.secondary',
          }}
        >
          <Typography variant="body2">
            {t('wallet.statement.empty')}
          </Typography>
        </Box>
      ) : (
        <Stack divider={<Divider flexItem />}>
          {walletTransactions
            .slice(0, 8)
            .map((transaction) => {
              const direction = getDirection(
                transaction,
                wallet
              );

              const outgoing = direction === 'outgoing';
              const incoming = direction === 'incoming';

              return (
                <Stack
                  key={
                    transaction.id ??
                    transaction.referenceNumber
                  }
                  direction="row"
                  alignItems="center"
                  spacing={2}
                  sx={{ py: 1.75 }}
                >
                  <Box
                    sx={{
                      width: 42,
                      height: 42,
                      flexShrink: 0,
                      display: 'grid',
                      placeItems: 'center',
                      borderRadius: '14px',
                      color: outgoing
                        ? '#D92D20'
                        : '#15803D',
                      bgcolor: outgoing
                        ? 'rgba(255,69,58,0.09)'
                        : 'rgba(48,209,88,0.1)',
                    }}
                  >
                    <Iconify
                      icon={
                        outgoing
                          ? 'material-symbols:arrow-outward-rounded'
                          : 'material-symbols:south-west-rounded'
                      }
                      width={22}
                    />
                  </Box>

                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography
                      variant="subtitle2"
                      noWrap
                    >
                      {transaction.description ||
                        t(
                          `wallet.statement.${direction}`
                        )}
                    </Typography>

                    <Typography
                      variant="caption"
                      sx={{ color: 'text.secondary' }}
                    >
                      {formatDate(
                        transaction.createdAt,
                        language
                      )}
                    </Typography>
                  </Box>

                  <Typography
                    variant="subtitle2"
                    sx={{
                      flexShrink: 0,
                      color: outgoing
                        ? '#D92D20'
                        : incoming
                          ? '#15803D'
                          : 'text.primary',
                    }}
                  >
                    {outgoing
                      ? '−'
                      : incoming
                        ? '+'
                        : ''}{' '}
                    {formatAmount(transaction.amount)}
                  </Typography>
                </Stack>
              );
            })}
        </Stack>
      )}
    </Card>
  );
}

WalletStatement.propTypes = {
  transactions: PropTypes.array,
  wallet: PropTypes.object,
};

WalletStatement.defaultProps = {
  transactions: [],
  wallet: null,
};