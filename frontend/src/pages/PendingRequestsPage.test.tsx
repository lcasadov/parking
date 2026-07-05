import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { PendingRequestsPage } from './PendingRequestsPage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import {
  pageOfRequests,
  requestApproved,
  requestPending1,
  requestPending2,
  requestPendingDesk,
} from '../mocks/requestFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

async function openApproveModal(): Promise<void> {
  const user = userEvent.setup();
  const buttons = await screen.findAllByRole('button', { name: /aprobar|approve/i });
  await user.click(buttons[0]);
  await screen.findByRole('dialog');
}

async function openRejectModal(): Promise<void> {
  const user = userEvent.setup();
  const buttons = await screen.findAllByRole('button', { name: /rechazar|reject/i });
  await user.click(buttons[0]);
  await screen.findByRole('dialog');
}

describe('PendingRequestsPage (ADMIN)', () => {
  it('should_list_pending_in_fifo_order_when_admin_lists_pending', async () => {
    renderWithProviders(<PendingRequestsPage />);

    await screen.findByText(requestPending1.requestedDate);
    const dataRows = screen.getAllByRole('row').filter((row) => within(row).queryAllByRole('cell').length > 0);
    // FIFO: la primera fila es la mas antigua (createdAt 08:00 < 09:30).
    expect(within(dataRows[0]).getByText(requestPending1.requestedDate)).toBeInTheDocument();
    expect(within(dataRows[1]).getByText(requestPending2.requestedDate)).toBeInTheDocument();
  });

  it('should_approve_request_when_space_available', async () => {
    const user = userEvent.setup();
    let approvedBody: unknown = null;
    server.use(
      http.post(`${MSW_BASE}/requests/:id/approve`, async ({ request, params }) => {
        approvedBody = await request.json();
        return HttpResponse.json({ ...requestApproved, id: Number(params.id) });
      }),
    );
    renderWithProviders(<PendingRequestsPage />);
    await openApproveModal();

    const dialog = within(screen.getByRole('dialog'));
    // La lista de plazas disponibles se carga via GET /availability (async).
    await dialog.findByRole('option', { name: 'P-01' });
    await user.selectOptions(dialog.getByLabelText(/plaza disponible|available space/i), '1');
    await user.type(dialog.getByLabelText(/nota de aprobación|approval note/i), 'ok');
    await user.click(dialog.getByRole('button', { name: /^aprobar$|^approve$/i }));

    await waitFor(() => expect(approvedBody).toEqual({ parkingSpaceId: 1, approvalNote: 'ok' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('should_require_space_when_approve_submitted_without_selection', async () => {
    const user = userEvent.setup();
    renderWithProviders(<PendingRequestsPage />);
    await openApproveModal();

    const dialog = within(screen.getByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /^aprobar$|^approve$/i }));

    expect(dialog.getByRole('alert')).toHaveTextContent(/selecciona una plaza|select a space/i);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('should_return_409_when_approve_space_not_available', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${MSW_BASE}/requests/:id/approve`, () =>
        HttpResponse.json(
          { error: 'SPACE_NOT_AVAILABLE', message: 'taken', timestamp: '2026-03-01T09:00:00Z' },
          { status: 409 },
        ),
      ),
    );
    renderWithProviders(
      <>
        <PendingRequestsPage />
        <Toast />
      </>,
    );
    await openApproveModal();

    const dialog = within(screen.getByRole('dialog'));
    await dialog.findByRole('option', { name: 'P-01' });
    await user.selectOptions(dialog.getByLabelText(/plaza disponible|available space/i), '1');
    await user.click(dialog.getByRole('button', { name: /^aprobar$|^approve$/i }));

    expect(
      await screen.findByText(/la plaza no está disponible|space is not available/i),
    ).toBeInTheDocument();
  });

  it('should_reject_request_when_reason_code_from_catalog', async () => {
    const user = userEvent.setup();
    let rejectBody: unknown = null;
    server.use(
      http.post(`${MSW_BASE}/requests/:id/reject`, async ({ request, params }) => {
        rejectBody = await request.json();
        return HttpResponse.json({ ...requestPending1, id: Number(params.id), status: 'REJECTED' });
      }),
    );
    renderWithProviders(<PendingRequestsPage />);
    await openRejectModal();

    const dialog = within(screen.getByRole('dialog'));
    await user.selectOptions(
      dialog.getByLabelText(/motivo del rechazo|rejection reason/i),
      'NO_AVAILABILITY',
    );
    await user.click(dialog.getByRole('button', { name: /confirmar rechazo|confirm rejection/i }));

    await waitFor(() => expect(rejectBody).toEqual({ reasonCode: 'NO_AVAILABILITY' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('should_return_400_when_reject_other_without_free_text', async () => {
    const user = userEvent.setup();
    let serverCalled = false;
    server.use(
      http.post(`${MSW_BASE}/requests/:id/reject`, () => {
        serverCalled = true;
        return HttpResponse.json(
          { error: 'validation', message: 'reason required', timestamp: '2026-03-01T09:00:00Z' },
          { status: 400 },
        );
      }),
    );
    renderWithProviders(<PendingRequestsPage />);
    await openRejectModal();

    const dialog = within(screen.getByRole('dialog'));
    await user.selectOptions(dialog.getByLabelText(/motivo del rechazo|rejection reason/i), 'OTHER');
    await user.click(dialog.getByRole('button', { name: /confirmar rechazo|confirm rejection/i }));

    // La validacion de cliente bloquea el envio (OTHER exige texto libre >=5).
    expect(dialog.getByRole('alert')).toHaveTextContent(
      /indica el detalle del motivo|provide the reason detail/i,
    );
    expect(serverCalled).toBe(false);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('should_reject_with_free_text_when_reason_is_other', async () => {
    const user = userEvent.setup();
    let rejectBody: unknown = null;
    server.use(
      http.post(`${MSW_BASE}/requests/:id/reject`, async ({ request, params }) => {
        rejectBody = await request.json();
        return HttpResponse.json({ ...requestPending1, id: Number(params.id), status: 'REJECTED' });
      }),
    );
    renderWithProviders(<PendingRequestsPage />);
    await openRejectModal();

    const dialog = within(screen.getByRole('dialog'));
    await user.selectOptions(dialog.getByLabelText(/motivo del rechazo|rejection reason/i), 'OTHER');
    await user.type(dialog.getByLabelText(/detalle del motivo|reason detail/i), 'obras en el garaje');
    await user.click(dialog.getByRole('button', { name: /confirmar rechazo|confirm rejection/i }));

    await waitFor(() =>
      expect(rejectBody).toEqual({ reasonCode: 'OTHER', rejectionReason: 'obras en el garaje' }),
    );
  });

  it('should_show_reject_invalid_toast_when_reject_returns_400', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${MSW_BASE}/requests/:id/reject`, () =>
        HttpResponse.json(
          { error: 'validation', message: 'bad reason', timestamp: '2026-03-01T09:00:00Z' },
          { status: 400 },
        ),
      ),
    );
    renderWithProviders(
      <>
        <PendingRequestsPage />
        <Toast />
      </>,
    );
    await openRejectModal();

    const dialog = within(screen.getByRole('dialog'));
    await user.selectOptions(
      dialog.getByLabelText(/motivo del rechazo|rejection reason/i),
      'OUTSIDE_POLICY',
    );
    await user.click(dialog.getByRole('button', { name: /confirmar rechazo|confirm rejection/i }));

    expect(
      await screen.findByText(/revisa el motivo del rechazo|check the rejection reason/i),
    ).toBeInTheDocument();
  });

  it('should_render_pagination_when_more_than_one_page', async () => {
    server.use(
      http.get(`${MSW_BASE}/requests/pending`, () =>
        HttpResponse.json(
          pageOfRequests([requestPending1], { totalPages: 2, first: true, last: false }),
        ),
      ),
    );
    renderWithProviders(<PendingRequestsPage />);

    const next = await screen.findByRole('button', { name: /siguiente|next/i });
    expect(next).toBeEnabled();
    expect(screen.getByRole('button', { name: /anterior|previous/i })).toBeDisabled();
  });

  it('should_show_resource_type_pill_per_row_when_listing_pending', async () => {
    server.use(
      http.get(`${MSW_BASE}/requests/pending`, () =>
        HttpResponse.json(pageOfRequests([requestPending1, requestPendingDesk])),
      ),
    );
    renderWithProviders(<PendingRequestsPage />);

    // Una fila de plaza y una de puesto: cada tipo visible como pill.
    expect(await screen.findByText(/^puesto$|^desk$/i)).toBeInTheDocument();
    expect(screen.getByText(/^plaza$|^space$/i)).toBeInTheDocument();
  });

  it('should_offer_desks_and_approve_desk_request_when_request_is_desk', async () => {
    const user = userEvent.setup();
    let approvedBody: unknown = null;
    server.use(
      http.get(`${MSW_BASE}/requests/pending`, () =>
        HttpResponse.json(pageOfRequests([requestPendingDesk])),
      ),
      http.post(`${MSW_BASE}/requests/:id/approve`, async ({ request, params }) => {
        approvedBody = await request.json();
        return HttpResponse.json({ ...requestApproved, id: Number(params.id) });
      }),
    );
    renderWithProviders(<PendingRequestsPage />);
    await openApproveModal();

    const dialog = within(screen.getByRole('dialog'));
    // Solicitud de puesto: el modal ofrece puestos (D-xx), no plazas.
    expect(dialog.getByText(/solicitud de puesto|desk request/i)).toBeInTheDocument();
    await dialog.findByRole('option', { name: 'D-01' });
    expect(dialog.getByRole('option', { name: 'D-02' })).toBeInTheDocument();
    expect(dialog.queryByRole('option', { name: 'P-01' })).not.toBeInTheDocument();

    await user.selectOptions(dialog.getByLabelText(/puesto disponible|available desk/i), '2');
    await user.click(dialog.getByRole('button', { name: /^aprobar$|^approve$/i }));

    // El id del puesto viaja en parkingSpaceId (resource_id generico).
    await waitFor(() => expect(approvedBody).toEqual({ parkingSpaceId: 2 }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
});
