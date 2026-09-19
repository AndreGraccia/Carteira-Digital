import { useTranslation } from 'react-i18next';
import { enUS, ptBR } from '@mui/material/locale';
import PropTypes from 'prop-types';
import { useMemo } from 'react';
import { CssBaseline } from '@mui/material';
import { ThemeProvider as MUIThemeProvider, createTheme, StyledEngineProvider } from '@mui/material/styles';
import palette from './palette';
import shadows from './shadows';
import typography from './typography';
import GlobalStyles from './globalStyles';
import customShadows from './customShadows';
import componentsOverride from './overrides';

ThemeProvider.propTypes = {
  children: PropTypes.node,
};

export default function ThemeProvider({ children }) {
  const { i18n } = useTranslation();
  const themeOptions = useMemo(
    () => ({
      palette,
      shape: { borderRadius: 14 },
      typography,
      shadows: shadows(),
      customShadows: customShadows(),
    }),
    []
  );

  const baseTheme = createTheme(themeOptions);
  const theme = createTheme(
    baseTheme,
    {
      components: {
        ...componentsOverride(baseTheme),
        MuiDialog: { styleOverrides: { paper: { borderRadius: 24 } } },
        MuiTabs: { styleOverrides: { indicator: { height: 3, borderRadius: 3 } } },
        MuiTab: { styleOverrides: { root: { textTransform: 'none', minHeight: 60 } } },
      },
    },
    i18n.resolvedLanguage?.startsWith('en') ? enUS : ptBR
  );

  return (
    <StyledEngineProvider injectFirst>
      <MUIThemeProvider theme={theme}>
        <CssBaseline />
        <GlobalStyles />
        {children}
      </MUIThemeProvider>
    </StyledEngineProvider>
  );
}
