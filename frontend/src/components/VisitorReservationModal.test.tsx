import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { VisitorReservationModal } from './VisitorReservationModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { reservationFuture, visitorCarla } from '../mocks/visitorFixtures';
import { todayIso } from '../utils/visitors';
import { renderWithProviders } from '../test/renderWithProviders';

const RESERVATIONS_URL = `${MSW_BASE}/visitor-reservations`;

function noop(): void {
  // sin efecto.
}

describe('VisitorReservationModal', () => {
  it('should_create_reservation_when_space_is_available_for_date', async () => {
    const user = userEvent.setup();
    let sentBody: Record<string, unknown> | null = null;
    let created = false;
    server.use(
      http.post(RESERVATIONS_URL, async ({ request }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...reservationFuture, id: 999 }, { status: 201 });
      }),
    );
    renderWithProviders(
      <VisitorReservationModal onClose={noop} onCreated={() => (created = true)} />,
    );

    await screen.findByRole('option', { name: /Carla/i });
    await user.selectOptions(
      screen.getByLabelText(/visitante|visitor/i),
      String(visitorCarla.id),
    );
    await user.type(screen.getByLabelText(/fecha|date/i), todayIso());
    await user.selectOptions(screen.getByLabelText(/plaza|space/i), '1');
    await user.click(screen.getByRole('button', { name: /crear reserva|create reservation/i }));

    await waitFor(() => expect(created).toBe(true));
    expect(sentBody).toMatchObject({
      visitorId: visitorCarla.id,
      resourceType: 'PARKING',
      resourceId: 1,
      reservationDate: todayIso(),
    });
  });

  it('should_show_inline_error_when_space_is_already_occupied_409', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(RESERVATIONS_URL, () =>
        HttpResponse.json(
          { error: 'CONFLICT', message: 'occupied', timestamp: '2026-03-01T09:00:00Z' },
          { status: 409 },
        ),
      ),
    );
    renderWithProviders(<VisitorReservationModal onClose={noop} onCreated={noop} />);

    await screen.findByRole('option', { name: /Carla/i });
    await user.selectOptions(
      screen.getByLabelText(/visitante|visitor/i),
      String(visitorCarla.id),
    );
    await user.type(screen.getByLabelText(/fecha|date/i), todayIso());
    await user.selectOptions(screen.getByLabelText(/plaza|space/i), '1');
    await user.click(screen.getByRole('button', { name: /crear reserva|create reservation/i }));

    expect(
      await screen.findByText(
        /la plaza ya está ocupada|the space is already occupied/i,
      ),
    ).toBeInTheDocument();
  });

  it('should_show_required_error_when_submitting_without_fields', async () => {
    const user = userEvent.setup();
    let posted = false;
    server.use(
      http.post(RESERVATIONS_URL, () => {
        posted = true;
        return HttpResponse.json(reservationFuture, { status: 201 });
      }),
    );
    renderWithProviders(<VisitorReservationModal onClose={noop} onCreated={noop} />);

    await screen.findByRole('option', { name: /Carla/i });
    await user.click(screen.getByRole('button', { name: /crear reserva|create reservation/i }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/selecciona un visitante|select a visitor/i);
    expect(posted).toBe(false);
  });

  it('should_preselect_and_lock_visitor_when_opened_from_visitor_row', async () => {
    renderWithProviders(
      <VisitorReservationModal visitor={visitorCarla} onClose={noop} onCreated={noop} />,
    );

    await screen.findByRole('option', { name: /Carla/i });
    const select = screen.getByLabelText(/visitante|visitor/i) as HTMLSelectElement;
    expect(select).toBeDisabled();
    expect(select.value).toBe(String(visitorCarla.id));
  });
});
