import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  THEME_STORAGE_KEY,
  ThemeContext,
  type Theme,
  type ThemeContextValue,
} from './themeContext';

function readStoredTheme(): Theme {
  return localStorage.getItem(THEME_STORAGE_KEY) === 'dark' ? 'dark' : 'light';
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(readStoredTheme);

  useEffect(() => {
    document.body.classList.toggle('theme-dark', theme === 'dark');
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  const setTheme = useCallback((next: Theme) => setThemeState(next), []);
  const toggleTheme = useCallback(
    () => setThemeState((prev) => (prev === 'dark' ? 'light' : 'dark')),
    [],
  );

  const value = useMemo<ThemeContextValue>(
    () => ({ theme, toggleTheme, setTheme }),
    [theme, toggleTheme, setTheme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
