import { alpha } from '@mui/material/styles';
import palette from './palette';

// Dark UI: depth comes from surface lightness and borders; shadows only separate floating layers.
const color = palette.common.black;

export default function customShadows() {
  const transparent = alpha(color, 0.4);

  return {
    z1: `0 1px 2px 0 ${transparent}`,
    z4: `0 4px 8px 0 ${transparent}`,
    z8: `0 8px 16px 0 ${transparent}`,
    z12: `0 12px 24px -4px ${transparent}`,
    z16: `0 16px 32px -4px ${transparent}`,
    z20: `0 20px 40px -4px ${transparent}`,
    z24: `0 24px 48px 0 ${transparent}`,
    //
    primary: `0 0 0 1px ${alpha(palette.primary.main, 0.4)}, 0 8px 24px -8px ${alpha(palette.primary.main, 0.45)}`,
    info: `0 8px 16px 0 ${alpha(palette.info.main, 0.24)}`,
    secondary: `0 8px 16px 0 ${alpha(palette.secondary.main, 0.24)}`,
    success: `0 8px 16px 0 ${alpha(palette.success.main, 0.24)}`,
    warning: `0 8px 16px 0 ${alpha(palette.warning.main, 0.24)}`,
    error: `0 8px 16px 0 ${alpha(palette.error.main, 0.24)}`,
    //
    card: 'none',
    dialog: `0 24px 64px -12px ${alpha(color, 0.7)}`,
    dropdown: `0 0 0 1px ${palette.divider}, 0 20px 40px -4px ${alpha(color, 0.6)}`,
  };
}
