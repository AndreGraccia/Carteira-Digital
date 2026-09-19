import {
  GlobalStyles as MUIGlobalStyles,
} from '@mui/material';

export default function GlobalStyles() {
  return (
    <MUIGlobalStyles
      styles={(theme) => ({
        '*': {
          boxSizing: 'border-box',
        },

        '*::selection': {
          color: theme.palette.common.white,
          backgroundColor: theme.palette.primary.main,
        },

        html: {
          margin: 0,
          padding: 0,
          width: '100%',
          minHeight: '100%',
          WebkitOverflowScrolling: 'touch',
        },

        body: {
          margin: 0,
          padding: 0,
          width: '100%',
          minHeight: '100%',
          color: theme.palette.text.primary,
          backgroundColor:
            theme.palette.background.default,

          fontFamily: theme.typography.fontFamily,
          WebkitFontSmoothing: 'antialiased',
          MozOsxFontSmoothing: 'grayscale',
        },

        '#root': {
          width: '100%',
          minHeight: '100vh',
        },

        button: {
          fontFamily: 'inherit',
        },

        input: {
          fontFamily: 'inherit',

          '&[type=number]': {
            MozAppearance: 'textfield',

            '&::-webkit-outer-spin-button': {
              margin: 0,
              WebkitAppearance: 'none',
            },

            '&::-webkit-inner-spin-button': {
              margin: 0,
              WebkitAppearance: 'none',
            },
          },
        },

        img: {
          display: 'block',
          maxWidth: '100%',
        },

        ul: {
          margin: 0,
          padding: 0,
        },

        '::-webkit-scrollbar': {
          width: 10,
          height: 10,
        },

        '::-webkit-scrollbar-track': {
          background: 'transparent',
        },

        '::-webkit-scrollbar-thumb': {
          border: '3px solid transparent',
          borderRadius: 999,
          backgroundClip: 'padding-box',
          backgroundColor:
            'rgba(99,99,102,0.34)',
        },

        '::-webkit-scrollbar-thumb:hover': {
          backgroundColor:
            'rgba(72,72,74,0.5)',
        },

        '@media (prefers-reduced-motion: reduce)': {
          '*, *::before, *::after': {
            scrollBehavior: 'auto !important',
            animationDuration: '0.01ms !important',
            animationIterationCount: '1 !important',
            transitionDuration: '0.01ms !important',
          },
        },
      })}
    />
  );
}
