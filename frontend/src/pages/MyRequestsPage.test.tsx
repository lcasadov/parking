import { fireEvent, screen, waitFor, within } from '@testing-library/react';
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
} from '../mocks/requestFixtures';
import { renderWithProviders } from '../test/renderWithProviders';
import { todayIso } from '../utils/requests';
import { addDaysIso } from '../utils/calendar';
import type { Request } from '../types/request';

// Fixtures con fechas DISTINTAS dentro de la ventana para poder localizar cada
// fila por su fecha en la tabla (change cancel-approved-request §5.1).

// PENDING con fecha propia (siempre cancelable).
const pendingDistinct: Request = {
  ...requestPending1,
  id: 600,
  requestedDate: addDaysIso(todayIso(), 1),
  status: 'PENDING',
};

// APPROVED con fecha futura (>= hoy): cancelable, libera el recurso.
const approvedFuture: Request = {
  ...requestApproved,
  id: 601,
  requestedDate: addDaysIso(todayIso(), 5),
  status: 'APPROVED',
};

// APPROVED con fecha de hoy: cancelable (hoy inclusive, D1).
const approvedToday: Request = {
  ...requestApproved,
  id: 602,
  requestedDate: todayIso(),
  status: 'APPROVED',
};

// CANCELLED (terminal): sin boton Cancelar.
const requestCancelled: Request = {
  ...requestApproved,
  id: 603,
  requestedDate: addDaysIso(todayIso(), 10),
  status: 'CANCELLED',
};

const CONFLICT_BODY = {
  error: 'REQUEST_ALREADY_PENDING',
  message: 'already pending',
  timestamp: '2026-03-01T09:00:00Z',
};
const WINDOW_BODY = {
  error: 'REQUEST_OUTSIDE_WINDOW',
  message: 'outside window',
  timestamp: '2026-03-01T09:00:00Z',
};

async function openCreateForm(): Promise<void> {
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /nueva solicitud|new request/i }));
  await screen.findByRole('dialog');
}

function setValidDate(): void {
  const dialog = within(screen.getByRole('dialog'));
  const input = dialog.getByLabelText(/fecha de la solicitud|request date/i);
  fireEvent.change(input, { target: { value: todayIso() } });
}

async function submitCreate(): Promise<void> {
  const user = userEvent.setup();
  const dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('button', { name: /enviar solicitud|submit request/i }));
}

describe('MyRequestsPage (EMPLOYEE)', () => {
  it('should_list_only_own_requests_when_employee_lists_mine', async () => {
    renderWithProviders(<MyRequestsPage />);

    expect(await screen.findByText(requestPending1.requestedDate)).toBeInTheDocument();
    const pendingRow = screen.getByText(requestPending1.requestedDate).closest('tr') as HTMLElement;
    expect(within(pendingRow).getByText(/pendiente|pending/i)).toBeInTheDocument();
    // La cancelacion solo esta disponible en PENDING (BOLA/estado); no en APPROVED.
    expect(within(pendingRow).getByRole('button', { name: /cancelar|cancel/i })).toBeInTheDocument();
    const approvedRow = screen
      .getByText(requestApproved.requestedDate)
      .closest('tr') as HTMLElement;
    expect(
      within(approvedRow).queryByRole('button', { name: /cancelar|cancel/i }),
    ).not.toBeInTheDocument();
  });

  it('should_show_cancel_when_approved_future_or_today_and_hide_otherwise', async () => {
    // Un caso por estado/fecha: PENDING y APPROVED futura/hoy muestran Cancelar;
    // APPROVED pasada, REJECTED y CANCELLED no (change cancel-approved-request §5.1).
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () =>
        HttpResponse.json(
          pageOfRequests([
            pendingDistinct,
            approvedFuture,
            approvedToday,
            requestApproved, // APPROVED con fecha pasada (2026-03-05)
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

    // Espera al render de la tabla antes de inspeccionar filas.
    await screen.findByText(pendingDistinct.requestedDate);

    // Filas donde el boton Cancelar DEBE aparecer.
    for (const shown of [pendingDistinct, approvedFuture, approvedToday]) {
      expect(
        within(rowFor(shown)).getByRole('button', { name: /cancelar|cancel/i }),
      ).toBeInTheDocument();
    }

    // Filas donde el boton Cancelar NO debe aparecer.
    for (const hidden of [requestApproved, requestRejected, requestCancelled]) {
      expect(
        within(rowFor(hidden)).queryByRole('button', { name: /cancelar|cancel/i }),
      ).not.toBeInTheDocument();
    }
  });

  it('should_cancel_approved_future_via_same_modal_and_invalidate_query', async () => {
    const user = userEvent.setup();
    let cancelledId = 0;
    let mineCalls = 0;
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () => {
        mineCalls += 1;
        // Tras cancelar, la solicitud pasa a CANCELLED (recurso liberado).
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
    renderWithProviders(<MyRequestsPage />);

    const approvedRow = (await screen.findByText(approvedFuture.requestedDate)).closest(
      'tr',
    ) as HTMLElement;
    await user.click(within(approvedRow).getByRole('button', { name: /cancelar|cancel/i }));

    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /cancelar solicitud|cancel request/i }));

    // Se invoca la misma mutacion/endpoint que en PENDING sobre la APPROVED.
    await waitFor(() => expect(cancelledId).toBe(approvedFuture.id));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    // La invalidacion de "Mis solicitudes" refetchea (>1 llamada al listado).
    await waitFor(() => expect(mineCalls).toBeGreaterThan(1));
  });

  it('should_create_pending_request_when_date_within_window', async () => {
    renderWithProviders(
      <>
        <MyRequestsPage />
        <Toast />
      </>,
    );
    await openCreateForm();
    setValidDate();
    await submitCreate();

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await screen.findByText(/solicitud creada|request created/i)).toBeInTheDocument();
  });

  it('should_return_400_outside_window_when_create_rejected_by_server', async () => {
    server.use(
      http.post(`${MSW_BASE}/requests`, () => HttpResponse.json(WINDOW_BODY, { status: 400 })),
    );
    renderWithProviders(
      <>
        <MyRequestsPage />
        <Toast />
      </>,
    );
    await openCreateForm();
    setValidDate();
    await submitCreate();

    expect(
      await screen.findByText(/fuera de la ventana permitida|outside the allowed window/i),
    ).toBeInTheDocument();
  });

  it('should_return_409_already_pending_when_duplicate_request_for_same_date', async () => {
    server.use(
      http.post(`${MSW_BASE}/requests`, () => HttpResponse.json(CONFLICT_BODY, { status: 409 })),
    );
    renderWithProviders(
      <>
        <MyRequestsPage />
        <Toast />
      </>,
    );
    await openCreateForm();
    setValidDate();
    await submitCreate();

    expect(
      await screen.findByText(/ya tienes una solicitud pendiente|already have a pending request/i),
    ).toBeInTheDocument();
  });

  it('should_block_create_and_show_error_when_no_date_selected', async () => {
    renderWithProviders(<MyRequestsPage />);
    await openCreateForm();
    await submitCreate();

    const dialog = within(screen.getByRole('dialog'));
    expect(dialog.getByRole('alert')).toHaveTextContent(/selecciona una fecha|select a date/i);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
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
    await user.click(within(pendingRow).getByRole('button', { name: /cancelar|cancel/i }));

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
    await user.click(within(pendingRow).getByRole('button', { name: /cancelar|cancel/i }));
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

    const next = await screen.findByRole('button', { name: /siguiente|next/i });
    expect(next).toBeEnabled();
    expect(screen.getByRole('button', { name: /anterior|previous/i })).toBeDisabled();
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

    // La plaza aprobada muestra su NUMERO real (3005), no el parkingSpaceId interno (#1).
    expect(await screen.findByText(/^plaza 3005|^space 3005/i)).toBeInTheDocument();
    // El puesto aprobado muestra "Puesto 12" / "Desk 12" (id interno 8 oculto).
    expect(screen.getByText(/^puesto 12$|^desk 12$/i)).toBeInTheDocument();
    // Nunca se pinta el id interno de BD con el viejo formato "#<id>".
    expect(screen.queryByText('#1')).not.toBeInTheDocument();
    expect(screen.queryByText('#8')).not.toBeInTheDocument();
    // La solicitud PENDING sin recurso degrada a "—".
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

    // El empleado ve el tipo de recurso de cada solicitud (pill plaza / puesto).
    // Se filtra por la clase `pill` para no chocar con la cabecera «Plaza».
    const deskPill = await screen.findByText(/^puesto$|^desk$/i);
    expect(deskPill).toHaveClass('pill');
    const parkingPills = screen
      .getAllByText(/^plaza$|^space$/i)
      .filter((node) => node.classList.contains('pill'));
    expect(parkingPills).toHaveLength(1);
  });
});
