export function remToPx(value) {
  return Math.round(parseFloat(value) * 16);
}

export function pxToRem(value) {
  return `${value / 16}rem`;
}

export function responsiveFontSizes({ sm, md, lg }) {
  return {
    '@media (min-width:600px)': {
      fontSize: pxToRem(sm),
    },
    '@media (min-width:900px)': {
      fontSize: pxToRem(md),
    },
    '@media (min-width:1200px)': {
      fontSize: pxToRem(lg),
    },
  };
}

// Inter for reading, Space Grotesk for headings and big numbers, JetBrains Mono for codes.
// Loaded from @fontsource (self-hosted, so the nginx CSP can stay 'self').
const SYSTEM = ['-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', 'Helvetica', 'Arial', 'sans-serif'];
export const FONT_PRIMARY = ['"Inter Variable"', 'Inter', ...SYSTEM].join(',');
export const FONT_DISPLAY = ['"Space Grotesk Variable"', '"Space Grotesk"', ...SYSTEM].join(',');
export const FONT_MONO = [
  '"JetBrains Mono Variable"',
  '"JetBrains Mono"',
  'ui-monospace',
  'Consolas',
  'monospace',
].join(',');

const typography = {
  fontFamily: FONT_PRIMARY,

  fontWeightRegular: 400,
  fontWeightMedium: 500,
  fontWeightBold: 700,

  h1: {
    fontFamily: FONT_DISPLAY,
    fontWeight: 700,
    lineHeight: 1.08,
    letterSpacing: '-0.045em',
    fontSize: pxToRem(40),
    ...responsiveFontSizes({
      sm: 48,
      md: 56,
      lg: 64,
    }),
  },

  h2: {
    fontFamily: FONT_DISPLAY,
    fontWeight: 700,
    lineHeight: 1.12,
    letterSpacing: '-0.04em',
    fontSize: pxToRem(34),
    ...responsiveFontSizes({
      sm: 38,
      md: 44,
      lg: 48,
    }),
  },

  h3: {
    fontFamily: FONT_DISPLAY,
    fontWeight: 700,
    lineHeight: 1.18,
    letterSpacing: '-0.035em',
    fontSize: pxToRem(28),
    ...responsiveFontSizes({
      sm: 30,
      md: 32,
      lg: 34,
    }),
  },

  h4: {
    fontFamily: FONT_DISPLAY,
    fontWeight: 700,
    lineHeight: 1.22,
    letterSpacing: '-0.03em',
    fontSize: pxToRem(23),
    ...responsiveFontSizes({
      sm: 25,
      md: 27,
      lg: 28,
    }),
  },

  h5: {
    fontFamily: FONT_DISPLAY,
    fontWeight: 600,
    lineHeight: 1.3,
    letterSpacing: '-0.02em',
    fontSize: pxToRem(20),
  },

  h6: {
    fontFamily: FONT_DISPLAY,
    fontWeight: 600,
    lineHeight: 1.35,
    letterSpacing: '-0.015em',
    fontSize: pxToRem(18),
  },

  subtitle1: {
    fontWeight: 600,
    lineHeight: 1.45,
    fontSize: pxToRem(16),
  },

  subtitle2: {
    fontWeight: 600,
    lineHeight: 1.45,
    fontSize: pxToRem(14),
  },

  body1: {
    fontWeight: 400,
    lineHeight: 1.55,
    fontSize: pxToRem(16),
  },

  body2: {
    fontWeight: 400,
    lineHeight: 1.5,
    fontSize: pxToRem(14),
  },

  caption: {
    fontWeight: 400,
    lineHeight: 1.45,
    fontSize: pxToRem(12),
  },

  overline: {
    fontWeight: 600,
    lineHeight: 1.5,
    letterSpacing: '0.1em',
    fontSize: pxToRem(11),
    textTransform: 'uppercase',
  },

  button: {
    fontFamily: FONT_DISPLAY,
    fontWeight: 600,
    lineHeight: 1.4,
    fontSize: pxToRem(14),
    letterSpacing: '-0.01em',
    textTransform: 'none',
  },
};

export default typography;
