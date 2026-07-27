import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { SettingsPage } from './SettingsPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';

const SETTINGS_URL = `${MSW_BASE}/admin/settings`;
const SAVE_BUTTON = /guardar|save/i;

function settings(approvalMode: string) {
  return HttpResponse.json({ approvalMode, updatedById: null, updatedAt: null });
}

describe('SettingsPage', () => {
  it('should_show_current_mode_from_server', async () => {
    server.use(http.get(SETTINGS_URL, () => settings('AUTOMATIC')));
    renderWithProviders(<SettingsPage />);

    // El modo se elige con teselas seleccionables (aria-pressed); ya no hay <select>.
    const autoTile = await screen.findByRole('button', { name: /automático|automatic/i });
    await waitFor(() => expect(autoTile).toHaveAttribute('aria-pressed', 'true'));
  });

  it('should_put_new_mode_when_admin_switches_and_saves', async () => {
    let receivedMode: string | null = null;
    server.use(
      http.get(SETTINGS_URL, () => settings('MANUAL')),
      http.put(SETTINGS_URL, async ({ request }) => {
        const body = (await request.json()) as { approvalMode: string };
        receivedMode = body.approvalMode;
        return settings(body.approvalMode);
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<SettingsPage />);

    // Elige la tesela "Automático" y guarda (dentro del form del modo).
    const autoTile = await screen.findByRole('button', { name: /automático|automatic/i });
    await user.click(autoTile);
    const approvalForm = within(autoTile.closest('form') as HTMLElement);
    await user.click(approvalForm.getByRole('button', { name: SAVE_BUTTON }));

    await waitFor(() => expect(receivedMode).toBe('AUTOMATIC'));
  });

  it('should_disable_save_when_mode_unchanged', async () => {
    server.use(http.get(SETTINGS_URL, () => settings('MANUAL')));
    renderWithProviders(<SettingsPage />);

    const manualTile = await screen.findByRole('button', { name: /^manual/i });
    const approvalForm = within(manualTile.closest('form') as HTMLElement);
    expect(approvalForm.getByRole('button', { name: SAVE_BUTTON })).toBeDisabled();
  });
});
