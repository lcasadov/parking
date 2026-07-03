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

    await user.click(screen.getByRole('button', { name: /tema|theme|oscuro|dark/i }));

    await waitFor(() => {
      expect(document.body.classList.contains('theme-dark')).toBe(true);
    });
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
  });
});
