import { Box, Button, Tooltip } from '@mui/material';
import { useTranslation } from 'react-i18next';

export default function LanguageSelector() {
  const { t, i18n } = useTranslation();

  const currentLanguage = i18n.resolvedLanguage?.startsWith('en') ? 'en' : 'pt';

  const changeLanguage = (language) => {
    i18n.changeLanguage(language);
  };

  const renderButton = (language, label, tooltip) => {
    const selected = currentLanguage === language;

    return (
      <Tooltip title={tooltip}>
        <Button
          type="button"
          aria-pressed={selected}
          onClick={() => changeLanguage(language)}
          sx={{
            minWidth: 42,
            height: 32,
            px: 1.25,
            borderRadius: '999px',
            color: selected ? 'common.white' : 'text.secondary',
            bgcolor: selected ? 'rgba(29,29,31,0.92)' : 'transparent',
            boxShadow: selected ? '0 4px 12px rgba(0,0,0,0.16)' : 'none',

            '&:hover': {
              color: selected ? 'common.white' : 'text.primary',
              bgcolor: selected ? 'rgba(29,29,31,1)' : 'rgba(255,255,255,0.58)',
            },
          }}
        >
          {label}
        </Button>
      </Tooltip>
    );
  };

  return (
    <Box
      role="group"
      aria-label={t('language.selector')}
      sx={{
        p: '3px',
        display: 'flex',
        alignItems: 'center',
        borderRadius: '999px',
        bgcolor: 'rgba(255,255,255,0.42)',
        border: '1px solid rgba(255,255,255,0.7)',
        boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.7)',
        backdropFilter: 'blur(18px) saturate(160%)',
        WebkitBackdropFilter: 'blur(18px) saturate(160%)',
      }}
    >
      {renderButton('pt', 'PT', t('language.portuguese'))}

      {renderButton('en', 'EN', t('language.english'))}
    </Box>
  );
}
