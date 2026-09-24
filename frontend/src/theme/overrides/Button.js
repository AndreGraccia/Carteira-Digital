import { alpha } from '@mui/material/styles';

export default function Button(theme) {
  // One visible focus for everything clickable, not only Button: the default MUI focus is a 24% tint
  // that disappears on the dark surfaces. Touch targets of 44px (WCAG 2.5.5).
  const focus = { outline: `2px solid ${theme.palette.primary.main}`, outlineOffset: 2 };
  return {
    MuiButtonBase: {
      styleOverrides: { root: { '&.Mui-focusVisible': focus } },
    },
    MuiLink: {
      styleOverrides: { root: { '&:focus-visible': { ...focus, borderRadius: 4 } } },
    },
    MuiSwitch: {
      styleOverrides: {
        root: { '& .MuiSwitch-switchBase.Mui-focusVisible + .MuiSwitch-track': focus },
      },
    },
    MuiIconButton: {
      styleOverrides: { root: { minWidth: 44, minHeight: 44 } },
    },
    MuiToggleButton: {
      styleOverrides: { root: { minHeight: 44, minWidth: 44 } },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          borderRadius: 12,
          padding: theme.spacing(1.25, 2.5),
          minHeight: 44,
          '&.Mui-focusVisible': {
            outline: `2px solid ${theme.palette.primary.main}`,
            outlineOffset: 3,
          },
          '&:hover': {
            boxShadow: 'none',
          },
        },
        sizeLarge: {
          height: 48,
        },
        containedInherit: {
          color: theme.palette.text.primary,
          backgroundColor: theme.palette.background.raised,
          '&:hover': {
            backgroundColor: theme.palette.grey[600],
          },
        },
        containedPrimary: {
          boxShadow: 'none',
          '&:hover': {
            backgroundColor: theme.palette.primary.light,
            boxShadow: theme.customShadows.primary,
          },
        },
        outlinedPrimary: {
          borderColor: alpha(theme.palette.primary.main, 0.5),
        },
        containedSecondary: {
          boxShadow: 'none',
        },
        outlinedInherit: {
          border: `1px solid ${alpha(theme.palette.grey[500], 0.32)}`,
          '&:hover': {
            backgroundColor: theme.palette.action.hover,
          },
        },
        textInherit: {
          '&:hover': {
            backgroundColor: theme.palette.action.hover,
          },
        },
      },
    },
  };
}
