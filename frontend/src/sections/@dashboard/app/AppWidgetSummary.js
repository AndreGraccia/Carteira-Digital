import { Card, Typography } from '@mui/material';
import { alpha, styled } from '@mui/material/styles';
import PropTypes from 'prop-types';
import Iconify from '../../../components/iconify';
import { fShortenNumber } from '../../../utils/formatNumber';

const StyledIcon = styled('div')(({ theme }) => ({
  width: 48,
  height: 48,
  display: 'flex',
  borderRadius: 14,
  alignItems: 'center',
  justifyContent: 'center',
  marginBottom: theme.spacing(3),
}));

AppWidgetSummary.propTypes = {
  color: PropTypes.string,
  icon: PropTypes.string,
  title: PropTypes.string.isRequired,
  total: PropTypes.number.isRequired,
  sx: PropTypes.object,
};

export default function AppWidgetSummary({
  title,
  total,
  icon,
  color = 'primary',
  sx,
  ...other
}) {
  return (
    <Card
      sx={{
        p: 3,
        minHeight: 190,
        position: 'relative',
        overflow: 'hidden',
        borderRadius: '24px',
        color: 'text.primary',
        bgcolor: 'background.paper',
        fontVariantNumeric: 'tabular-nums',

        ...sx,
      }}
      {...other}
    >
      <StyledIcon
        sx={{
          color: (theme) => theme.palette[color].main,
          bgcolor: (theme) => alpha(theme.palette[color].main, 0.12),
        }}
      >
        <Iconify icon={icon} width={25} height={25} />
      </StyledIcon>

      <Typography
        variant="h3"
        sx={{
          mb: 0.75,
          fontWeight: 700,
          letterSpacing: '-0.04em',
        }}
      >
        {fShortenNumber(total)}
      </Typography>

      <Typography
        variant="subtitle2"
        sx={{
          color: 'text.secondary',
          fontWeight: 500,
        }}
      >
        {title}
      </Typography>
    </Card>
  );
}