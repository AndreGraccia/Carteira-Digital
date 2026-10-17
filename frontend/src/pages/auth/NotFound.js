import { useTranslation } from 'react-i18next';
import { Box, Button, Container, Typography } from '@mui/material';
import { styled } from '@mui/material/styles';
import { Helmet } from 'react-helmet-async';
import { Link as RouterLink } from 'react-router-dom';

const StyledContent = styled('div')(({ theme }) => ({
  maxWidth: 480,
  margin: 'auto',
  minHeight: '100vh',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  flexDirection: 'column',
  textAlign: 'center',
  padding: theme.spacing(12, 0),
}));

export default function NotFound() {
  const { t } = useTranslation();
  return (
    <>
      <Helmet>
        <title>{t('errors.notFound.pageTitle')}</title>
      </Helmet>

      <Container>
        <StyledContent>
          <Typography variant="h3" paragraph>
            {t('errors.notFound.title')}{' '}
          </Typography>

          <Typography sx={{ color: 'text.secondary' }}>{t('errors.notFound.description')}</Typography>

          <Box
            component="img"
            src="/assets/illustrations/illustration_404.svg"
            alt={t('errors.notFound.title')}
            sx={{ height: 260, mx: 'auto', my: { xs: 5, sm: 8 } }}
          />

          <Button to="/" size="large" variant="contained" component={RouterLink}>
            {t('actions.goHome')}
          </Button>
        </StyledContent>
      </Container>
    </>
  );
}
