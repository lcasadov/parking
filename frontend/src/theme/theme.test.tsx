import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { ThemeToggle } from '../components/ThemeToggle';
import { THEME_STORAGE_KEY } from './themeContext';
import { renderWithProviders } from '../test/renderWithProviders';

describe('ThemeToggle', () => {
  it('should_add_dark_class_and_persist_when_theme_toggled', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ThemeToggle />);

    expect(document.body.classList.contains('theme-dark')).toBe(false);

    // WP2: el control de tema es ahora un switch (role=switch), on = oscuro.
    const themeSwitch = screen.getByRole('switch', { name: /claro|light|oscuro|dark/i });
    expect(themeSwitch).toHaveAttribute('aria-checked', 'false');

    await user.click(themeSwitch);

    await waitFor(() => {
      expect(document.body.classList.contains('theme-dark')).toBe(true);
    });
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'true');
  });
});
