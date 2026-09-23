import { alpha } from '@mui/material/styles';

export default function Button(theme) {
  return {
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
