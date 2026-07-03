import { useTranslation } from 'react-i18next';
import { useTheme } from '../theme/themeContext';

// Toggle de modo oscuro. Anade body.theme-dark y persiste (via ThemeProvider).
export function ThemeToggle() {
  const { t } = useTranslation();
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <button
      type="button"
      className="btn btn-white btn-icon-only"
      aria-pressed={isDark}
      aria-label={isDark ? t('common.themeLight') : t('common.themeDark')}
      onClick={toggleTheme}
    >
      <i className={`ti ti-${isDark ? 'sun' : 'moon'}`} aria-hidden="true" />
    </button>
  );
}
