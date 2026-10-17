import PropTypes from 'prop-types';
import { Box, Stack } from '@mui/material';

// The mark is a "W" drawn as a balance line; its last point is the "." of Wall.ia: the future,
// projected. Same geometry as public/favicon.svg — change both together.
export function WalliaMark({ size = 32, title }) {
  return (
    <Box
      component="svg"
      viewBox="0 0 32 32"
      width={size}
      height={size}
      role={title ? 'img' : undefined}
      aria-label={title}
      aria-hidden={title ? undefined : true}
      sx={{ display: 'block', flexShrink: 0 }}
    >
      <Box component="rect" width="32" height="32" rx="9" sx={{ fill: (theme) => theme.palette.background.neutral }} />
      <Box
        component="polyline"
        points="6.5,10 11,21.5 16,13.5 21,21.5"
        fill="none"
        strokeWidth="2.6"
        strokeLinecap="round"
        strokeLinejoin="round"
        sx={{ stroke: (theme) => theme.palette.text.primary }}
      />
      {[
        [22.55, 17.75],
        [24.05, 14.15],
      ].map(([cx, cy]) => (
        <Box
          key={cx}
          component="circle"
          cx={cx}
          cy={cy}
          r="1.3"
          sx={{ fill: (theme) => theme.palette.brand.horizon }}
        />
      ))}
      <Box component="circle" cx="25.6" cy="10.4" r="2.6" sx={{ fill: (theme) => theme.palette.brand.volt }} />
    </Box>
  );
}
WalliaMark.propTypes = { size: PropTypes.number, title: PropTypes.string };

export function WalliaLogo({ size = 32, hideWordmark = false }) {
  return (
    <Stack direction="row" alignItems="center" spacing={1.25} sx={{ color: 'text.primary' }}>
      <WalliaMark size={size} title={hideWordmark ? 'Wall.ia' : undefined} />
      {!hideWordmark && (
        <Box
          component="span"
          sx={{
            fontFamily: (theme) => theme.typography.h6.fontFamily,
            fontWeight: 700,
            fontSize: size * 0.62,
            letterSpacing: '-0.03em',
            lineHeight: 1,
          }}
        >
          Wall
          <Box component="span" sx={{ color: 'brand.volt' }}>
            .
          </Box>
          ia
        </Box>
      )}
    </Stack>
  );
}
WalliaLogo.propTypes = { size: PropTypes.number, hideWordmark: PropTypes.bool };
