import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { CancelVisitorReservationModal } from './CancelVisitorReservationModal';
import { Toast } from './Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';

const RESERVATIONS_URL = `${MSW_BASE}/visitor-reservations`;

function noop(): void {
  // sin efecto.
}

describe('CancelVisitorReservationModal', () => {
  it('should_cancel_reservation_when_confirming_future_reservation', async () => {
    const user = userEvent.setup();
    let cancelledId: number | null = null;
    let cancelled = false;
    server.use(
      http.delete(`${RESERVATIONS_URL}/:id`, ({ params }) => {
        cancelledId = Number(params.id);
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderWithProviders(
      <CancelVisitorReservationModal
        reservationId={40}
        reservationDate="2026-07-05"
        onClose={noop}
        onCancelled={() => (cancelled = true)}
      />,
    );

    await user.click(screen.getByRole('button', { name: /anular reserva|cancel reservation/i }));

    await waitFor(() => expect(cancelled).toBe(true));
    expect(cancelledId).toBe(40);
  });

  it('should_show_toast_when_cancelling_past_reservation_returns_400', async () => {
    const user = userEvent.setup();
    server.use(
      http.delete(`${RESERVATIONS_URL}/:id`, () =>
        HttpResponse.json(
          { error: 'BAD_REQUEST', message: 'past', timestamp: '2026-03-01T09:00:00Z' },
          { status: 400 },
        ),
      ),
    );
    renderWithProviders(
      <>
        <CancelVisitorReservationModal
          reservationId={41}
          reservationDate="2020-01-01"
          onClose={noop}
          onCancelled={noop}
        />
        <Toast />
      </>,
    );

    await user.click(screen.getByRole('button', { name: /anular reserva|cancel reservation/i }));

    expect(
      await screen.findByText(
        /no se puede anular una reserva pasada|a past reservation cannot be cancelled/i,
      ),
    ).toBeInTheDocument();
  });
});
