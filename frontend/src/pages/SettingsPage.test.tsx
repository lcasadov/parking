import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { SettingsPage } from './SettingsPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';

// El mapa (mapbox-gl) no corre en jsdom y depende de VITE_MAPBOX_TOKEN; su cobertura la
// da ParkingLocationMap.test.tsx. Aquí se sustituye por un input simple para probar la
// tarjeta de dirección sin inicializar WebGL.
vi.mock('../components/ParkingLocationMap', () => ({
  ParkingLocationMap: ({
    address,
    onAddressChange,
  }: {
    address: string;
    onAddressChange: (value: string) => void;
  }) => (
    <input
      aria-label="Dirección"
      value={address}
      onChange={(event) => onAddressChange(event.target.value)}
    />
  ),
}));

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

  it('should_put_parking_address_when_admin_edits_and_saves', async () => {
    let body: Record<string, unknown> | null = null;
    server.use(
      http.get(SETTINGS_URL, () =>
        HttpResponse.json({
          approvalMode: 'MANUAL',
          parkingAddress: 'Old Av',
          parkingLat: null,
          parkingLng: null,
          weekendReservable: false,
          updatedById: null,
          updatedAt: null,
        }),
      ),
      http.put(`${MSW_BASE}/admin/settings/parking-address`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ approvalMode: 'MANUAL', parkingAddress: 'New Av' });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<SettingsPage />);

    // La tarjeta de dirección está activa (había dirección); el input lo renderiza el mapa.
    const input = await screen.findByLabelText(/dirección|address/i);
    await user.clear(input);
    await user.type(input, 'New Av');
    const card = within(input.closest('form') as HTMLElement);
    await user.click(card.getByRole('button', { name: SAVE_BUTTON }));

    await waitFor(() => expect(body).toMatchObject({ parkingAddress: 'New Av' }));
  });

  it('should_put_weekend_reservable_when_admin_toggles_and_saves', async () => {
    let body: Record<string, unknown> | null = null;
    server.use(
      http.get(SETTINGS_URL, () =>
        HttpResponse.json({
          approvalMode: 'MANUAL',
          parkingAddress: null,
          weekendReservable: false,
          updatedById: null,
          updatedAt: null,
        }),
      ),
      http.put(`${MSW_BASE}/admin/settings/weekend-reservable`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ approvalMode: 'MANUAL', weekendReservable: true });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<SettingsPage />);

    const heading = await screen.findByRole('heading', {
      name: /reservas en fin de semana|weekend bookings/i,
    });
    const card = within(heading.closest('.settings-card') as HTMLElement);
    await user.click(card.getByRole('switch'));
    await user.click(card.getByRole('button', { name: SAVE_BUTTON }));

    await waitFor(() => expect(body).toMatchObject({ weekendReservable: true }));
  });
});
