import { alpha } from '@mui/material/styles';

const GREY = {
  0: '#FFFFFF',
  100: '#FAFAFC',
  200: '#F2F2F7',
  300: '#E5E5EA',
  400: '#D1D1D6',
  500: '#8E8E93',
  600: '#636366',
  700: '#48484A',
  800: '#1D1D1F',
  900: '#0A0A0B',
};

const PRIMARY = {
  lighter: '#EAF4FF',
  light: '#64B5FF',
  main: '#0071E3',
  dark: '#0062CC',
  darker: '#003E80',
  contrastText: '#FFFFFF',
};

const SECONDARY = {
  lighter: '#F3EAFF',
  light: '#BF8CFF',
  main: '#AF52DE',
  dark: '#8944AB',
  darker: '#572A70',
  contrastText: '#FFFFFF',
};

const INFO = {
  lighter: '#E8F8FF',
  light: '#70D7FF',
  main: '#32ADE6',
  dark: '#1683B4',
  darker: '#0B4F6C',
  contrastText: '#FFFFFF',
};

const SUCCESS = {
  lighter: '#EAF9EE',
  light: '#67D982',
  main: '#34C759',
  dark: '#248A3D',
  darker: '#145523',
  contrastText: '#FFFFFF',
};

const WARNING = {
  lighter: '#FFF5E6',
  light: '#FFBD66',
  main: '#FF9F0A',
  dark: '#C77800',
  darker: '#754700',
  contrastText: GREY[900],
};

const ERROR = {
  lighter: '#FFF0EF',
  light: '#FF7B73',
  main: '#FF3B30',
  dark: '#D70015',
  darker: '#8A0010',
  contrastText: '#FFFFFF',
};

const palette = {
  common: {
    black: '#000000',
    white: '#FFFFFF',
  },

  primary: PRIMARY,
  secondary: SECONDARY,
  info: INFO,
  success: SUCCESS,
  warning: WARNING,
  error: ERROR,
  grey: GREY,

  divider: alpha(GREY[500], 0.18),

  text: {
    primary: GREY[800],
    secondary: GREY[600],
    disabled: alpha(GREY[600], 0.5),
  },

  background: {
    paper: GREY[0],
    default: '#F5F5F7',
    neutral: GREY[200],
  },

  action: {
    active: GREY[700],
    hover: alpha(GREY[800], 0.05),
    selected: alpha(PRIMARY.main, 0.11),
    disabled: alpha(GREY[600], 0.42),
    disabledBackground: alpha(GREY[500], 0.14),
    focus: alpha(PRIMARY.main, 0.16),
    hoverOpacity: 0.05,
    disabledOpacity: 0.48,
  },
};

export default palette;
