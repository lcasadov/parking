import { useTranslation } from 'react-i18next';
import { useTheme } from '../theme/themeContext';

// Selector de tema segmentado (sol = claro / luna = oscuro), a juego con
// LanguageToggle. La lógica no cambia: setTheme persiste vía ThemeProvider.
export function ThemeToggle() {
  const { t } = useTranslation();
  const { theme, setTheme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <div className="segmented segmented-icon" role="group" aria-label={t('common.theme')}>
      <button
        type="button"
        className={isDark ? '' : 'active'}
        aria-pressed={!isDark}
        aria-label={t('common.themeLight')}
        title={t('common.themeLight')}
        onClick={() => setTheme('light')}
      >
        <i className="ti ti-sun" aria-hidden="true" />
      </button>
      <button
        type="button"
        className={isDark ? 'active' : ''}
        aria-pressed={isDark}
        aria-label={t('common.themeDark')}
        title={t('common.themeDark')}
        onClick={() => setTheme('dark')}
      >
        <i className="ti ti-moon" aria-hidden="true" />
      </button>
    </div>
  );
}
