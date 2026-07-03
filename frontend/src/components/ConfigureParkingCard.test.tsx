import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { ConfigureParkingCard } from './ConfigureParkingCard';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { spaceP01 } from '../mocks/parkingSpaceFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const CONFIGURE_URL = `${MSW_BASE}/parking-spaces/configure`;

describe('ConfigureParkingCard', () => {
  it('should_send_total_when_admin_configures_spaces', async () => {
    let sentTotal: number | null = null;
    server.use(
      http.post(CONFIGURE_URL, async ({ request }) => {
        const body = (await request.json()) as { total: number };
        sentTotal = body.total;
        return HttpResponse.json([spaceP01]);
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<ConfigureParkingCard />);

    await user.type(screen.getByLabelText(/número total de plazas|total number of spaces/i), '25');
    await user.click(screen.getByRole('button', { name: /aplicar|apply/i }));

    await waitFor(() => {
      expect(sentTotal).toBe(25);
    });
    expect(
      await screen.findByText(/configuración aplicada|configuration applied/i),
    ).toBeInTheDocument();
  });

  it('should_show_error_when_configure_returns_400', async () => {
    server.use(
      http.post(CONFIGURE_URL, () =>
        HttpResponse.json(
          {
            error: 'validation',
            message: 'Invalid total',
            fields: { total: 'must be >= 0' },
            timestamp: new Date().toISOString(),
          },
          { status: 400 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(<ConfigureParkingCard />);

    // Un valor positivo pasa la validacion cliente y provoca el 400 del servidor.
    await user.type(screen.getByLabelText(/número total de plazas|total number of spaces/i), '5');
    await user.click(screen.getByRole('button', { name: /aplicar|apply/i }));

    expect(await screen.findByText(/must be >= 0/i)).toBeInTheDocument();
  });

  it('should_reject_negative_total_without_calling_server', async () => {
    let called = false;
    server.use(
      http.post(CONFIGURE_URL, () => {
        called = true;
        return HttpResponse.json([spaceP01]);
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<ConfigureParkingCard />);

    await user.type(screen.getByLabelText(/número total de plazas|total number of spaces/i), '-3');
    await user.click(screen.getByRole('button', { name: /aplicar|apply/i }));

    expect(
      await screen.findByText(/introduce un número entero|enter an integer/i),
    ).toBeInTheDocument();
    expect(called).toBe(false);
  });
});
