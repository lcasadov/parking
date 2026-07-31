import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { VehicleReviewPage } from './VehicleReviewPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';
import type { VehicleReviewRow } from '../types/employeeVehicleReview';
import type { VehicleStatus } from '../types/vehicle';

const URL = `${MSW_BASE}/employee-vehicles`;

function row(overrides: Partial<VehicleReviewRow> = {}): VehicleReviewRow {
  return {
    vehicleId: 7,
    employee: { id: 15, fullName: 'Ana García', department: 'IT' },
    licensePlate: '1234ABC',
    brand: 'Seat',
    model: 'Leon',
    color: 'Gris',
    status: 'PENDING' as VehicleStatus,
    rejectionReason: null,
    submittedAt: '2026-07-31T09:00:00Z',
    ...overrides,
  };
}

function page(rows: VehicleReviewRow[]) {
  return {
    content: rows,
    totalElements: rows.length,
    totalPages: 1,
    size: 20,
    number: 0,
    first: true,
    last: true,
  };
}

// Handlers comunes: listado + contadores (siempre presentes al montar la página).
function listOf(rows: VehicleReviewRow[]) {
  return [
    http.get(`${URL}/counts`, () => HttpResponse.json({ PENDING: rows.length })),
    http.get(URL, () => HttpResponse.json(page(rows))),
  ];
}

async function openStatusModal(user: ReturnType<typeof userEvent.setup>) {
  const tr = (await screen.findByText('1234ABC')).closest('tr') as HTMLElement;
  await user.click(within(tr).getByRole('button', { name: /cambiar estado|change status/i }));
  return screen.findByRole('dialog');
}

describe('VehicleReviewPage', () => {
  it('should_render_rows_with_employee_and_status', async () => {
    server.use(...listOf([row()]));

    renderWithProviders(<VehicleReviewPage />);

    expect(await screen.findByText('Ana García')).toBeInTheDocument();
    expect(screen.getByText('1234ABC')).toBeInTheDocument();
    expect(screen.getByText(/^pendiente$|^pending$/i)).toBeInTheDocument();
  });

  it('should_approve_via_change_status_with_mutua_note', async () => {
    const user = userEvent.setup();
    let sentStatus: string | null = null;
    server.use(
      ...listOf([row()]),
      http.post(`${URL}/7/status`, async ({ request }) => {
        const body = (await request.json()) as { status: string };
        sentStatus = body.status;
        return HttpResponse.json(row({ status: body.status as never }));
      }),
    );

    renderWithProviders(<VehicleReviewPage />);
    const dialog = await openStatusModal(user);

    await user.selectOptions(within(dialog).getByLabelText(/nuevo estado|new status/i), 'APPROVED');
    // Aviso de la mutua al elegir Aprobado.
    expect(within(dialog).getByText(/mutua|insurer/i)).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: /^guardar$|^save$/i }));

    await waitFor(() => expect(sentStatus).toBe('APPROVED'));
  });

  it('should_require_reason_when_changing_to_rejected', async () => {
    const user = userEvent.setup();
    let sentReason: string | null = null;
    server.use(
      ...listOf([row()]),
      http.post(`${URL}/7/status`, async ({ request }) => {
        const body = (await request.json()) as { status: string; reason?: string };
        sentReason = body.reason ?? null;
        return HttpResponse.json(row({ status: 'REJECTED' }));
      }),
    );

    renderWithProviders(<VehicleReviewPage />);
    const dialog = await openStatusModal(user);

    await user.selectOptions(within(dialog).getByLabelText(/nuevo estado|new status/i), 'REJECTED');
    // Sin motivo -> error, no envía.
    await user.click(within(dialog).getByRole('button', { name: /^guardar$|^save$/i }));
    expect(await within(dialog).findByRole('alert')).toBeInTheDocument();
    expect(sentReason).toBeNull();

    await user.type(within(dialog).getByLabelText(/motivo|reason/i), 'Aprobado por error');
    await user.click(within(dialog).getByRole('button', { name: /^guardar$|^save$/i }));

    await waitFor(() => expect(sentReason).toBe('Aprobado por error'));
  });

  it('should_show_confirm_and_restore_actions_for_pending_deletion', async () => {
    server.use(...listOf([row({ status: 'PENDING_DELETION' })]));

    renderWithProviders(<VehicleReviewPage />);

    const tr = (await screen.findByText('1234ABC')).closest('tr') as HTMLElement;
    expect(within(tr).getByRole('button', { name: /restaurar|restore/i })).toBeInTheDocument();
    expect(within(tr).getByRole('button', { name: /confirmar borrado|confirm deletion/i })).toBeInTheDocument();
    // Los pendientes de borrado no ofrecen "Cambiar estado".
    expect(within(tr).queryByRole('button', { name: /cambiar estado|change status/i })).toBeNull();
  });

  it('should_show_status_counts_on_filter_chips', async () => {
    server.use(
      http.get(`${URL}/counts`, () =>
        HttpResponse.json({ PENDING: 3, IN_PROGRESS: 2, PENDING_DELETION: 1, APPROVED: 5 }),
      ),
      http.get(URL, () => HttpResponse.json(page([row()]))),
    );

    renderWithProviders(<VehicleReviewPage />);

    // "Abiertos" = 3 + 2 + 1 = 6 (el contador llega de forma asíncrona).
    const abiertos = await screen.findByRole('tab', { name: /abiertos|open/i });
    await waitFor(() => expect(abiertos).toHaveTextContent('6'));
    expect(screen.getByRole('tab', { name: /aprobados|approved/i })).toHaveTextContent('5');
  });
});
