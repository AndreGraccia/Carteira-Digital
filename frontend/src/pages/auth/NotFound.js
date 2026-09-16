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
  return (
    <>
      <Helmet>
        <title>Página não encontrada | Carteira Digital</title>
      </Helmet>

      <Container>
        <StyledContent>
          <Typography variant="h3" paragraph>
            Página não encontrada
          </Typography>

          <Typography sx={{ color: 'text.secondary' }}>
            O endereço informado não existe ou foi removido.
          </Typography>

          <Box
            component="img"
            src="/assets/illustrations/illustration_404.svg"
            alt="Página não encontrada"
            sx={{ height: 260, mx: 'auto', my: { xs: 5, sm: 8 } }}
          />

          <Button to="/" size="large" variant="contained" component={RouterLink}>
            Voltar para o início
          </Button>
        </StyledContent>
      </Container>
    </>
  );
}