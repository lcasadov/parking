import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { MyRequestsPage } from './MyRequestsPage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import {
  pageOfRequests,
  requestApproved,
  requestApprovedDesk,
  requestPending1,
  requestPendingDesk,
  requestRejected,
  requestWaitlisted,
} from '../mocks/requestFixtures';
import { renderWithProviders } from '../test/renderWithProviders';
import { todayIso } from '../utils/requests';
import { addDaysIso } from '../utils/calendar';
import type { Request } from '../types/request';

// Fixtures con fechas DISTINTAS dentro de la ventana para poder localizar cada
// fila por su fecha en la tabla.

// PENDING con fecha propia (siempre cancelable → "Cancelar solicitud").
const pendingDistinct: Request = {
  ...requestPending1,
  id: 600,
  requestedDate: addDaysIso(todayIso(), 1),
  status: 'PENDING',
};

// APPROVED con fecha futura (>= hoy): liberable ("Liberar").
const approvedFuture: Request = {
  ...requestApproved,
  id: 601,
  requestedDate: addDaysIso(todayIso(), 5),
  status: 'APPROVED',
};

// APPROVED con fecha de hoy: liberable (hoy inclusive).
const approvedToday: Request = {
  ...requestApproved,
  id: 602,
  requestedDate: todayIso(),
  status: 'APPROVED',
};

// CANCELLED (terminal): sin acción.
const requestCancelled: Request = {
  ...requestApproved,
  id: 603,
  requestedDate: addDaysIso(todayIso(), 10),
  status: 'CANCELLED',
};

describe('MyRequestsPage (EMPLOYEE)', () => {
  it('should_list_only_own_requests_when_employee_lists_mine', async () => {
    renderWithProviders(<MyRequestsPage />);

    expect(await screen.findByText(requestPending1.requestedDate)).toBeInTheDocument();
    const pendingRow = screen.getByText(requestPending1.requestedDate).closest('tr') as HTMLElement;
    expect(within(pendingRow).getByText(/pendiente|pending/i)).toBeInTheDocument();
    // PENDING ofrece "Cancelar solicitud".
    expect(
      within(pendingRow).getByRole('button', { name: /cancelar solicitud|cancel request/i }),
    ).toBeInTheDocument();
    // requestApproved tiene fecha pasada → sin acción.
    const approvedRow = screen
      .getByText(requestApproved.requestedDate)
      .closest('tr') as HTMLElement;
    expect(
      within(approvedRow).queryByRole('button', { name: /cancelar|liberar|cancel|release/i }),
    ).not.toBeInTheDocument();
  });

  it('should_show_release_for_approved_and_cancel_for_pending_and_hide_otherwise', async () => {
    // PENDING → "Cancelar solicitud"; APPROVED futura/hoy → "Liberar"; APPROVED
    // pasada, REJECTED y CANCELLED → sin acción.
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () =>
        HttpResponse.json(
          pageOfRequests([
            pendingDistinct,
            approvedFuture,
            approvedToday,
            requestApproved, // APPROVED con fecha pasada
            requestRejected,
            requestCancelled,
          ]),
        ),
      ),
    );
    renderWithProviders(<MyRequestsPage />);

    function rowFor(request: Request): HTMLElement {
      return screen.getByText(request.requestedDate).closest('tr') as HTMLElement;
    }

    await screen.findByText(pendingDistinct.requestedDate);

    // PENDING → "Cancelar solicitud".
    expect(
      within(rowFor(pendingDistinct)).getByRole('button', {
        name: /cancelar solicitud|cancel request/i,
      }),
    ).toBeInTheDocument();

    // APPROVED futura/hoy → "Liberar".
    for (const shown of [approvedFuture, approvedToday]) {
      expect(
        within(rowFor(shown)).getByRole('button', { name: /liberar|release/i }),
      ).toBeInTheDocument();
    }

    // Sin acción en pasada/rechazada/cancelada.
    for (const hidden of [requestApproved, requestRejected, requestCancelled]) {
      expect(
        within(rowFor(hidden)).queryByRole('button', {
          name: /cancelar|liberar|cancel|release/i,
        }),
      ).not.toBeInTheDocument();
    }
  });

  it('should_label_released_vs_cancelled_by_prior_resolution', async () => {
    // CANCELLED que venía de APPROVED (resolvedAt no nulo) → "Liberada".
    const released: Request = {
      ...requestApproved,
      id: 701,
      requestedDate: addDaysIso(todayIso(), 4),
      status: 'CANCELLED',
      resolvedAt: '2026-03-02T10:00:00Z',
    };
    // CANCELLED que venía de PENDING (nunca resuelta) → "Cancelada".
    const cancelled: Request = {
      ...requestApproved,
      id: 702,
      requestedDate: addDaysIso(todayIso(), 5),
      status: 'CANCELLED',
      resolvedAt: null,
    };
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () =>
        HttpResponse.json(pageOfRequests([released, cancelled])),
      ),
    );
    renderWithProviders(<MyRequestsPage />);

    const releasedRow = (await screen.findByText(released.requestedDate)).closest('tr') as HTMLElement;
    expect(within(releasedRow).getByText(/liberada|released/i)).toBeInTheDocument();

    const cancelledRow = screen.getByText(cancelled.requestedDate).closest('tr') as HTMLElement;
    expect(within(cancelledRow).getByText(/cancelada|cancelled/i)).toBeInTheDocument();
  });

  it('should_release_approved_future_via_modal_and_invalidate_query', async () => {
    const user = userEvent.setup();
    let cancelledId = 0;
    let mineCalls = 0;
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () => {
        mineCalls += 1;
        const row = cancelledId === approvedFuture.id
          ? { ...approvedFuture, status: 'CANCELLED' as const }
          : approvedFuture;
        return HttpResponse.json(pageOfRequests([row]));
      }),
      http.post(`${MSW_BASE}/requests/:id/cancel`, ({ params }) => {
        cancelledId = Number(params.id);
        return HttpResponse.json({ ...approvedFuture, status: 'CANCELLED' });
      }),
    );
    renderWithProviders(
      <>
        <MyRequestsPage />
        <Toast />
      </>,
    );

    const approvedRow = (await screen.findByText(approvedFuture.requestedDate)).closest(
      'tr',
    ) as HTMLElement;
    // APPROVED → botón "Liberar".
    await user.click(within(approvedRow).getByRole('button', { name: /liberar|release/i }));

    // El modal de confirmación habla de liberar (confirmar = "Liberar" exacto; el
    // botón de cancelar es "No liberar").
    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));

    // Se reutiliza el endpoint de cancelación de la solicitud (libera el recurso).
    await waitFor(() => expect(cancelledId).toBe(approvedFuture.id));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await waitFor(() => expect(mineCalls).toBeGreaterThan(1));
  });

  it('should_cancel_request_when_owner_cancels_pending', async () => {
    const user = userEvent.setup();
    let cancelled = false;
    server.use(
      http.post(`${MSW_BASE}/requests/:id/cancel`, ({ params }) => {
        cancelled = true;
        return HttpResponse.json({ ...requestPending1, id: Number(params.id), status: 'CANCELLED' });
      }),
    );
    renderWithProviders(<MyRequestsPage />);

    const pendingRow = (await screen.findByText(requestPending1.requestedDate)).closest(
      'tr',
    ) as HTMLElement;
    await user.click(
      within(pendingRow).getByRole('button', { name: /cancelar solicitud|cancel request/i }),
    );

    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /cancelar solicitud|cancel request/i }));

    await waitFor(() => expect(cancelled).toBe(true));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('should_return_409_when_cancel_already_resolved_request', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${MSW_BASE}/requests/:id/cancel`, () =>
        HttpResponse.json(
          { error: 'REQUEST_NOT_PENDING', message: 'resolved', timestamp: '2026-03-01T09:00:00Z' },
          { status: 409 },
        ),
      ),
    );
    renderWithProviders(
      <>
        <MyRequestsPage />
        <Toast />
      </>,
    );

    const pendingRow = (await screen.findByText(requestPending1.requestedDate)).closest(
      'tr',
    ) as HTMLElement;
    await user.click(
      within(pendingRow).getByRole('button', { name: /cancelar solicitud|cancel request/i }),
    );
    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /cancelar solicitud|cancel request/i }));

    expect(
      await screen.findByText(/ya no está pendiente|no longer pending/i),
    ).toBeInTheDocument();
  });

  it('should_render_pagination_when_more_than_one_page', async () => {
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () =>
        HttpResponse.json(
          pageOfRequests([requestPending1], { totalPages: 2, first: true, last: false }),
        ),
      ),
    );
    renderWithProviders(<MyRequestsPage />);

    // Acotado a la <nav> de paginación (la barra de mes también tiene ‹ › con
    // "Mes anterior/siguiente" que casarían con el mismo matcher).
    await screen.findByText(requestPending1.requestedDate);
    const pagination = within(screen.getByRole('navigation'));
    expect(pagination.getByRole('button', { name: /siguiente|next/i })).toBeEnabled();
    expect(pagination.getByRole('button', { name: /anterior|previous/i })).toBeDisabled();
  });

  it('should_show_real_resource_number_not_internal_id_when_approved', async () => {
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () =>
        HttpResponse.json(
          pageOfRequests([requestApproved, requestApprovedDesk, requestPending1]),
        ),
      ),
    );
    renderWithProviders(<MyRequestsPage />);

    expect(await screen.findByText(/^plaza 3005|^space 3005/i)).toBeInTheDocument();
    expect(screen.getByText(/^puesto 12$|^desk 12$/i)).toBeInTheDocument();
    expect(screen.queryByText('#1')).not.toBeInTheDocument();
    expect(screen.queryByText('#8')).not.toBeInTheDocument();
    const pendingRow = screen.getByText(requestPending1.requestedDate).closest('tr') as HTMLElement;
    expect(within(pendingRow).getByText('—')).toBeInTheDocument();
  });

  it('should_show_resource_type_pill_when_listing_my_requests', async () => {
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () =>
        HttpResponse.json(pageOfRequests([requestPending1, requestPendingDesk])),
      ),
    );
    renderWithProviders(<MyRequestsPage />);

    // selector '.pill' evita las <option> "Plaza"/"Puesto" del select de recurso
    // y espera a que la fila (asíncrona) renderice el pill.
    const deskPill = await screen.findByText(/^puesto$|^desk$/i, { selector: '.pill' });
    expect(deskPill).toHaveClass('pill');
    const parkingPills = screen.getAllByText(/^plaza$|^space$/i, { selector: '.pill' });
    expect(parkingPills).toHaveLength(1);
  });

  it('should_filter_rows_by_resource_when_resource_filter_changes', async () => {
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () =>
        HttpResponse.json(pageOfRequests([requestPending1, requestPendingDesk])),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(<MyRequestsPage />);

    // Con ambos recursos: hay pill de plaza y de puesto.
    await screen.findByText(/^puesto$|^desk$/i, { selector: '.pill' });
    expect(screen.getAllByText(/^plaza$|^space$/i, { selector: '.pill' })).toHaveLength(1);

    // Filtra por Puesto → desaparecen los pills de plaza.
    await user.selectOptions(
      screen.getByLabelText(/filtrar por recurso|filter by resource/i),
      'DESK',
    );
    expect(screen.queryByText(/^plaza$|^space$/i, { selector: '.pill' })).not.toBeInTheDocument();
    expect(screen.getAllByText(/^puesto$|^desk$/i, { selector: '.pill' })).toHaveLength(1);
  });

  it('should_show_waitlist_badge_when_pending_request_is_waitlisted', async () => {
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () =>
        HttpResponse.json(pageOfRequests([requestWaitlisted, requestPending1])),
      ),
    );
    renderWithProviders(<MyRequestsPage />);

    const waitlistedRow = (await screen.findByText(requestWaitlisted.requestedDate)).closest(
      'tr',
    ) as HTMLElement;
    expect(
      within(waitlistedRow).getByText(/en lista de espera|on the waitlist/i),
    ).toBeInTheDocument();

    const pendingRow = screen.getByText(requestPending1.requestedDate).closest('tr') as HTMLElement;
    expect(
      within(pendingRow).queryByText(/en lista de espera|on the waitlist/i),
    ).not.toBeInTheDocument();

    expect(screen.queryByText(/posición|position/i)).not.toBeInTheDocument();
  });
});
