import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { LanguageToggle } from '../components/LanguageToggle';
import { LANGUAGE_STORAGE_KEY } from './index';
import { renderWithProviders } from '../test/renderWithProviders';

describe('i18n LanguageToggle', () => {
  it('should_switch_texts_when_language_toggled', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LanguageToggle />);

    // Por defecto ES: el boton EN esta visible.
    const enButton = screen.getByRole('button', { name: /^EN$/i });
    await user.click(enButton);

    await waitFor(() => {
      expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('en');
    });
  });

  it('should_persist_language_in_localStorage', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LanguageToggle />);

    await user.click(screen.getByRole('button', { name: /^EN$/i }));
    await waitFor(() => expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('en'));

    await user.click(screen.getByRole('button', { name: /^ES$/i }));
    await waitFor(() => expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('es'));
  });
});
