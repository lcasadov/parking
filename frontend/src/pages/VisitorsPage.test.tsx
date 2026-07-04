import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { VisitorsPage } from './VisitorsPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import {
  pageOfVisitors,
  reservationFuture,
  reservationPast,
  visitorCarla,
  visitorDiego,
} from '../mocks/visitorFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const VISITORS_URL = `${MSW_BASE}/visitors`;
const RESERVATIONS_URL = `${MSW_BASE}/visitor-reservations`;

function rowFor(text: string): HTMLElement {
  return screen.getByText(text).closest('tr') as HTMLElement;
}

async function openReservationsTab(user: ReturnType<typeof userEvent.setup>): Promise<void> {
  await user.click(screen.getByRole('tab', { name: /reservas futuras|upcoming reservations/i }));
}

describe('VisitorsPage (ADMIN)', () => {
  it('should_render_visitor_rows_when_list_loads', async () => {
    renderWithProviders(<VisitorsPage />);

    expect(await screen.findByText('Carla Cortes')).toBeInTheDocument();
    expect(screen.getByText('Diego Duarte')).toBeInTheDocument();
    expect(screen.getByText('12345678Z')).toBeInTheDocument();
  });

  it('should_request_with_q_when_searching_by_national_id_or_name', async () => {
    let receivedQ: string | null = null;
    server.use(
      http.get(VISITORS_URL, ({ request }) => {
        const q = new URL(request.url).searchParams.get('q');
        receivedQ = q;
        const all = [visitorCarla, visitorDiego];
        const filtered = q
          ? all.filter((v) =>
              `${v.firstName} ${v.lastName} ${v.nationalId}`
                .toLowerCase()
                .includes(q.toLowerCase()),
            )
          : all;
        return HttpResponse.json(pageOfVisitors(filtered));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<VisitorsPage />);
    await screen.findByText('Carla Cortes');

    await user.type(screen.getByRole('searchbox'), 'Diego');

    await waitFor(() => expect(receivedQ).toBe('Diego'));
    await waitFor(() => {
      expect(screen.queryByText('Carla Cortes')).not.toBeInTheDocument();
    });
    expect(screen.getByText('Diego Duarte')).toBeInTheDocument();
  });

  it('should_open_create_form_when_clicking_new_visitor', async () => {
    const user = userEvent.setup();
    renderWithProviders(<VisitorsPage />);
    await screen.findByText('Carla Cortes');

    await user.click(screen.getByRole('button', { name: /nuevo visitante|new visitor/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/nuevo visitante|new visitor/i)).toBeInTheDocument();
  });

  it('should_open_edit_form_prefilled_when_clicking_edit', async () => {
    const user = userEvent.setup();
    renderWithProviders(<VisitorsPage />);
    await screen.findByText('Carla Cortes');
    const carlaRow = rowFor('Carla Cortes');

    await user.click(within(carlaRow).getByRole('button', { name: /editar|edit/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/editar visitante|edit visitor/i)).toBeInTheDocument();
    expect(within(dialog).getByLabelText(/^nombre$|^first name$/i)).toHaveValue('Carla');
  });

  it('should_open_detail_when_clicking_view', async () => {
    const user = userEvent.setup();
    renderWithProviders(<VisitorsPage />);
    await screen.findByText('Carla Cortes');
    const carlaRow = rowFor('Carla Cortes');

    await user.click(within(carlaRow).getByRole('button', { name: /^ver$|^view$/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/ficha de visitante|visitor card/i)).toBeInTheDocument();
    expect(within(dialog).getByText('12345678Z')).toBeInTheDocument();
  });

  it('should_show_error_message_when_list_request_fails', async () => {
    server.use(
      http.get(VISITORS_URL, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: '2026-03-01T09:00:00Z' },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<VisitorsPage />);

    expect(
      await screen.findByText(/no se pudieron cargar los visitantes|could not load visitors/i),
    ).toBeInTheDocument();
  });

  it('should_render_reservations_when_switching_to_reservations_tab', async () => {
    const user = userEvent.setup();
    renderWithProviders(<VisitorsPage />);
    await screen.findByText('Carla Cortes');

    await openReservationsTab(user);

    expect(await screen.findByText(reservationFuture.reservationDate)).toBeInTheDocument();
    expect(screen.getByText(reservationPast.reservationDate)).toBeInTheDocument();
  });

  it('should_disable_cancel_when_reservation_is_past', async () => {
    const user = userEvent.setup();
    renderWithProviders(<VisitorsPage />);
    await screen.findByText('Carla Cortes');
    await openReservationsTab(user);
    await screen.findByText(reservationPast.reservationDate);

    const pastRow = rowFor(reservationPast.reservationDate);
    expect(within(pastRow).getByRole('button', { name: /anular|cancel/i })).toBeDisabled();
  });

  it('should_open_new_reservation_modal_when_clicking_new_reservation', async () => {
    const user = userEvent.setup();
    renderWithProviders(<VisitorsPage />);
    await screen.findByText('Carla Cortes');
    await openReservationsTab(user);
    await screen.findByText(reservationFuture.reservationDate);

    await user.click(screen.getByRole('button', { name: /nueva reserva|new reservation/i }));

    const dialog = await screen.findByRole('dialog');
    expect(
      within(dialog).getByText(/nueva reserva de visita|new visitor reservation/i),
    ).toBeInTheDocument();
  });

  it('should_change_page_when_navigating_visitors_pagination', async () => {
    let requestedPage: string | null = null;
    server.use(
      http.get(VISITORS_URL, ({ request }) => {
        requestedPage = new URL(request.url).searchParams.get('page');
        const isFirst = requestedPage === null || requestedPage === '0';
        return HttpResponse.json({
          content: [isFirst ? visitorCarla : visitorDiego],
          totalElements: 2,
          totalPages: 2,
          size: 1,
          number: isFirst ? 0 : 1,
          first: isFirst,
          last: !isFirst,
        });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<VisitorsPage />);
    await screen.findByText('Carla Cortes');

    await user.click(screen.getByRole('button', { name: /siguiente|next/i }));

    await waitFor(() => expect(requestedPage).toBe('1'));
    expect(await screen.findByText('Diego Duarte')).toBeInTheDocument();
  });

  it('should_change_page_when_navigating_reservations_pagination', async () => {
    server.use(
      http.get(RESERVATIONS_URL, ({ request }) => {
        const page = new URL(request.url).searchParams.get('page');
        const isFirst = page === null || page === '0';
        return HttpResponse.json({
          content: [isFirst ? reservationFuture : reservationPast],
          totalElements: 2,
          totalPages: 2,
          size: 1,
          number: isFirst ? 0 : 1,
          first: isFirst,
          last: !isFirst,
        });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<VisitorsPage />);
    await screen.findByText('Carla Cortes');
    await openReservationsTab(user);
    await screen.findByText(reservationFuture.reservationDate);

    await user.click(screen.getByRole('button', { name: /siguiente|next/i }));

    expect(await screen.findByText(reservationPast.reservationDate)).toBeInTheDocument();
  });

  it('should_cancel_future_reservation_when_confirming', async () => {
    const user = userEvent.setup();
    let cancelledId: number | null = null;
    server.use(
      http.delete(`${RESERVATIONS_URL}/:id`, ({ params }) => {
        cancelledId = Number(params.id);
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderWithProviders(<VisitorsPage />);
    await screen.findByText('Carla Cortes');
    await openReservationsTab(user);
    await screen.findByText(reservationFuture.reservationDate);

    const futureRow = rowFor(reservationFuture.reservationDate);
    await user.click(within(futureRow).getByRole('button', { name: /anular|cancel/i }));
    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /anular reserva|cancel reservation/i }));

    await waitFor(() => expect(cancelledId).toBe(reservationFuture.id));
  });
});
