import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ParkingDirectionsButton } from './ParkingDirectionsButton';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';

const ADDRESS_URL = `${MSW_BASE}/settings/parking-address`;

describe('ParkingDirectionsButton', () => {
  afterEach(() => vi.restoreAllMocks());

  it('renders nothing when no location is configured', async () => {
    server.use(
      http.get(ADDRESS_URL, () =>
        HttpResponse.json({ parkingAddress: null, parkingLat: null, parkingLng: null }),
      ),
    );
    renderWithProviders(<ParkingDirectionsButton />);

    await waitFor(() => expect(screen.queryByRole('button')).not.toBeInTheDocument());
  });

  it('navigates to the exact coordinates when they are configured', async () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    server.use(
      http.get(ADDRESS_URL, () =>
        HttpResponse.json({ parkingAddress: 'Av', parkingLat: 40.5, parkingLng: -3.6 }),
      ),
    );
    renderWithProviders(<ParkingDirectionsButton />);

    await userEvent.click(await screen.findByRole('button'));

    expect(openSpy).toHaveBeenCalledTimes(1);
    expect(openSpy.mock.calls[0][0]).toContain('google.com/maps');
    // Las coordenadas van codificadas (la coma → %2C).
    expect(openSpy.mock.calls[0][0]).toContain('40.5%2C-3.6');
  });

  it('falls back to the postal address when there are no coordinates', async () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    server.use(
      http.get(ADDRESS_URL, () =>
        HttpResponse.json({ parkingAddress: 'Av. de Europa 18', parkingLat: null, parkingLng: null }),
      ),
    );
    renderWithProviders(<ParkingDirectionsButton />);

    await userEvent.click(await screen.findByRole('button'));

    expect(openSpy.mock.calls[0][0]).toContain(encodeURIComponent('Av. de Europa 18'));
  });
});
