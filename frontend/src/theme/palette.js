import { alpha } from '@mui/material/styles';

// Wall.ia — primitives. Components never use these directly; they use the semantic palette below
// (or `brand.*` for the few brand-only moments). Contrast pairs are listed in docs/brand/wallia-brand.md.
export const primitives = {
  void: '#07090D',
  ink: '#0A0D13',
  surface1: '#10141C',
  surface2: '#161B25',
  surface3: '#1D2330',
  line: '#232A37',
  lineStrong: '#56607A',
  text: '#E9EDF3',
  text2: '#A3ACBA',
  muted: '#7D8797',
  volt: '#D4FF3A',
  voltDeep: '#B8E01F',
  horizon: '#5CE1E6',
  mint: '#3DDC97',
  coral: '#FF7A7A',
  amber: '#FFB547',
};

const GREY = {
  0: '#FFFFFF',
  100: primitives.text,
  200: '#C9D0DA',
  300: primitives.text2,
  400: primitives.muted,
  500: '#5B6474',
  600: primitives.lineStrong,
  700: primitives.surface3,
  800: primitives.surface2,
  900: primitives.surface1,
};

const palette = {
  mode: 'dark',
  common: { black: primitives.void, white: '#FFFFFF' },

  // Action: the only colour that means "you can press this".
  primary: {
    lighter: alpha(primitives.volt, 0.14),
    light: '#E4FF7A',
    main: primitives.volt,
    dark: primitives.voltDeep,
    darker: '#6F8A00',
    contrastText: primitives.void,
  },
  // Projection / estimate: anything the engine expects but has not happened yet.
  secondary: {
    lighter: alpha(primitives.horizon, 0.14),
    light: '#9AEEF1',
    main: primitives.horizon,
    dark: '#2FB8BE',
    darker: '#146E72',
    contrastText: primitives.void,
  },
  info: {
    lighter: alpha(primitives.horizon, 0.14),
    light: '#9AEEF1',
    main: primitives.horizon,
    dark: '#2FB8BE',
    darker: '#146E72',
    contrastText: primitives.void,
  },
  success: {
    lighter: alpha(primitives.mint, 0.14),
    light: '#7EEBBC',
    main: primitives.mint,
    dark: '#23B176',
    darker: '#0F6B45',
    contrastText: primitives.void,
  },
  warning: {
    lighter: alpha(primitives.amber, 0.14),
    light: '#FFD08A',
    main: primitives.amber,
    dark: '#E0931F',
    darker: '#7A4A00',
    contrastText: primitives.void,
  },
  error: {
    lighter: alpha(primitives.coral, 0.14),
    light: '#FFA8A8',
    main: primitives.coral,
    dark: '#E65555',
    darker: '#7A1F1F',
    contrastText: primitives.void,
  },
  grey: GREY,

  // Brand-only tokens (logo, certainty markers). Never used as a button colour.
  brand: {
    volt: primitives.volt,
    horizon: primitives.horizon,
    ink: primitives.ink,
    surface3: primitives.surface3,
    lineStrong: primitives.lineStrong,
  },
  // Chart marks: darker steps of horizon/volt that sit in the dark-mode lightness band
  // (validated with the dataviz palette checker: CVD ΔE 16.0, all checks pass on #10141C).
  chart: {
    projection: '#1E9CA3',
    scenario: '#7F9E0A',
    negative: alpha(primitives.coral, 0.12),
    grid: primitives.line,
  },
  // Wallet card finishes (the physical-card metaphor). White text passes AA on every finish.
  cardFinishes: [
    'linear-gradient(135deg, #1D2330 0%, #0A0D13 100%)',
    `radial-gradient(120% 90% at 100% 0%, ${alpha(
      primitives.volt,
      0.28
    )} 0%, transparent 55%), linear-gradient(135deg, #1A2012 0%, #0A0D13 100%)`,
    `radial-gradient(120% 90% at 100% 0%, ${alpha(
      primitives.horizon,
      0.3
    )} 0%, transparent 55%), linear-gradient(135deg, #0F2A30 0%, #0A0D13 100%)`,
    'linear-gradient(135deg, #2B2240 0%, #0D0B14 100%)',
    'linear-gradient(135deg, #332414 0%, #0F0B07 100%)',
  ],
  // How sure a number is. Each state also has a text label and a line style, never colour alone.
  certainty: {
    confirmed: primitives.text,
    estimated: primitives.horizon,
    hypothetical: primitives.volt,
  },

  divider: primitives.line,

  text: {
    primary: primitives.text,
    secondary: primitives.text2,
    disabled: primitives.muted,
  },

  // Elevation by lightness: the higher the surface, the lighter it is.
  background: {
    default: primitives.ink,
    paper: primitives.surface1,
    neutral: primitives.surface2,
    raised: primitives.surface3,
  },

  action: {
    active: primitives.text2,
    hover: alpha(primitives.text, 0.06),
    selected: alpha(primitives.volt, 0.12),
    disabled: alpha(primitives.text2, 0.4),
    disabledBackground: alpha(primitives.text2, 0.12),
    focus: alpha(primitives.volt, 0.24),
    hoverOpacity: 0.06,
    disabledOpacity: 0.48,
  },
};

export default palette;
