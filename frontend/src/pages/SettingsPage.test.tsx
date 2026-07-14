import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { SettingsPage } from './SettingsPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';

const SETTINGS_URL = `${MSW_BASE}/admin/settings`;
const MODE_LABEL = /modo de aprobación|approval mode/i;
const SAVE_BUTTON = /guardar|save/i;

function settings(approvalMode: string) {
  return HttpResponse.json({ approvalMode, updatedById: null, updatedAt: null });
}

describe('SettingsPage', () => {
  it('should_show_current_mode_from_server', async () => {
    server.use(http.get(SETTINGS_URL, () => settings('AUTOMATIC')));
    renderWithProviders(<SettingsPage />);

    const select = (await screen.findByLabelText(MODE_LABEL)) as HTMLSelectElement;
    await waitFor(() => expect(select.value).toBe('AUTOMATIC'));
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

    const select = (await screen.findByLabelText(MODE_LABEL)) as HTMLSelectElement;
    await waitFor(() => expect(select.value).toBe('MANUAL'));

    await user.selectOptions(select, 'AUTOMATIC');
    await user.click(screen.getByRole('button', { name: SAVE_BUTTON }));

    await waitFor(() => expect(receivedMode).toBe('AUTOMATIC'));
  });

  it('should_disable_save_when_mode_unchanged', async () => {
    server.use(http.get(SETTINGS_URL, () => settings('MANUAL')));
    renderWithProviders(<SettingsPage />);

    await screen.findByLabelText(MODE_LABEL);
    expect(screen.getByRole('button', { name: SAVE_BUTTON })).toBeDisabled();
  });
});
