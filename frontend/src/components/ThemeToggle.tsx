import { useTranslation } from 'react-i18next';
import { useTheme } from '../theme/themeContext';
import { Toggle } from './Toggle';

// Switch de modo oscuro (mockups 17/18). on = oscuro. Anade body.theme-dark y
// persiste via ThemeProvider (la logica no cambia; solo el control visual).
export function ThemeToggle() {
  const { t } = useTranslation();
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <Toggle
      checked={isDark}
      onChange={toggleTheme}
      label={isDark ? t('common.themeDark') : t('common.themeLight')}
    />
  );
}
