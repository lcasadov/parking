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

    // El control de tema es un segmentado de dos botones (sol = claro,
    // luna = oscuro) con aria-pressed marcando el tema activo.
    const lightButton = screen.getByRole('button', { name: /claro|light/i });
    const darkButton = screen.getByRole('button', { name: /oscuro|dark/i });
    expect(lightButton).toHaveAttribute('aria-pressed', 'true');
    expect(darkButton).toHaveAttribute('aria-pressed', 'false');

    await user.click(darkButton);

    await waitFor(() => {
      expect(document.body.classList.contains('theme-dark')).toBe(true);
    });
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(darkButton).toHaveAttribute('aria-pressed', 'true');
    expect(lightButton).toHaveAttribute('aria-pressed', 'false');
  });
});
