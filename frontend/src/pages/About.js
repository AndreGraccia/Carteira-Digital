import { Box, Card, Container, Link, Stack, Typography } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import { PageHeading } from '../components/wallet-ui';

export default function About() {
  const { t } = useTranslation();
  return (
    <>
      <Helmet>
        <title>{t('app.about')} | Wall.ia</title>
      </Helmet>
      <Container maxWidth="md">
        <PageHeading title={t('app.about')} description={t('app.demoNotice')} />
        <Stack spacing={3}>
          {['simulation', 'apple', 'bcb'].map((section) => (
            <Card key={section} sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                {t(`app.aboutSections.${section}.title`)}
              </Typography>
              <Typography color="text.secondary">{t(`app.aboutSections.${section}.body`)}</Typography>
            </Card>
          ))}
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              {t('app.sources')}
            </Typography>
            <Stack spacing={1}>
              <Link href="https://www.apple.com/br/wallet/" target="_blank" rel="noreferrer">
                Apple Wallet
              </Link>
              <Link
                href="https://developer.apple.com/help/account/capabilities/configure-apple-pay-on-the-web"
                target="_blank"
                rel="noreferrer"
              >
                Apple Pay — Web
              </Link>
              <Link
                href="https://www.bcb.gov.br/estabilidadefinanceira/instituicaopagamento"
                target="_blank"
                rel="noreferrer"
              >
                Banco Central — Instituições de pagamento
              </Link>
              <Link href="https://www.bcb.gov.br/estabilidadefinanceira/pix" target="_blank" rel="noreferrer">
                Banco Central — Pix
              </Link>
            </Stack>
          </Box>
        </Stack>
      </Container>
    </>
  );
}
