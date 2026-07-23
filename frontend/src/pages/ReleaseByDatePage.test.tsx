import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { ReleaseByDatePage } from './ReleaseByDatePage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';
import { todayIso } from '../utils/releases';

// Recurso ocupado por asignación fija (sin requestId) → liberación administrativa (Release).
const parkingFixed = {
  resourceType: 'PARKING' as const,
  resourceId: 8,
  resourceNumber: 3005,
  floor: 3,
  employeeId: 15,
  employeeName: 'Ada Lovelace',
  origin: 'FIXED_ASSIGNMENT' as const,
  requestId: null,
};

// Puesto ocupado por asignación fija (sin requestId) → liberación administrativa (Release).
const deskFixed = {
  resourceType: 'DESK' as const,
  resourceId: 20,
  resourceNumber: 12,
  floor: null,
  employeeId: 99,
  employeeName: 'Grace Hopper',
  origin: 'FIXED_ASSIGNMENT' as const,
  requestId: null,
};

// Puesto ocupado por solicitud APPROVED (trae requestId) → liberar cancelando la solicitud.
const deskRequest = {
  resourceType: 'DESK' as const,
  resourceId: 20,
  resourceNumber: 12,
  floor: null,
  employeeId: 99,
  employeeName: 'Grace Hopper',
  origin: 'REQUEST_APPROVED' as const,
  requestId: 42,
};

function useOccupancyWith(items: unknown[]): void {
  server.use(
    http.get(`${MSW_BASE}/occupancy`, ({ request }) => {
      const date = new URL(request.url).searchParams.get('date');
      return HttpResponse.json({ date, occupiedResources: items });
    }),
  );
}

describe('ReleaseByDatePage (ADMIN)', () => {
  it('should_list_occupied_resources_for_the_default_date', async () => {
    useOccupancyWith([parkingFixed, deskRequest]);
    renderWithProviders(<ReleaseByDatePage />);

    expect(await screen.findByText('Plaza 3005 · Planta 3')).toBeInTheDocument();
    expect(screen.getByText('Ada Lovelace')).toBeInTheDocument();
    expect(screen.getByText('Puesto 12')).toBeInTheDocument();
    expect(screen.getByText('Grace Hopper')).toBeInTheDocument();
  });

  it('should_open_prefilled_admin_release_when_fixed_resource_released', async () => {
    const user = userEvent.setup();
    useOccupancyWith([parkingFixed]);
    renderWithProviders(<ReleaseByDatePage />);

    await screen.findByText('Plaza 3005 · Planta 3');
    await user.click(screen.getByRole('button', { name: /^liberar$|^release$/i }));

    const dialog = within(await screen.findByRole('dialog'));
    // Modal pre-rellenado: muestra empleado, recurso y fecha fijados (sin selectores).
    expect(dialog.getByText('Ada Lovelace')).toBeInTheDocument();
    expect(dialog.getByText('Plaza 3005 · Planta 3')).toBeInTheDocument();
    expect(dialog.getByText(todayIso())).toBeInTheDocument();
    expect(dialog.queryByLabelText(/empleado|employee/i)).not.toBeInTheDocument();
    // Etiqueta del recurso corregida: para una PLAZA muestra "Plaza" (no un genérico).
    expect(dialog.getByText('Plaza', { selector: 'dt' })).toBeInTheDocument();
  });

  it('should_label_resource_as_puesto_for_a_desk_fixed_release', async () => {
    const user = userEvent.setup();
    useOccupancyWith([deskFixed]);
    renderWithProviders(<ReleaseByDatePage />);

    await screen.findByText('Puesto 12');
    await user.click(screen.getByRole('button', { name: /^liberar$|^release$/i }));

    // Corrección "Plaza"→"Recurso": para un PUESTO la etiqueta del modal dice "Puesto".
    const dialog = within(await screen.findByRole('dialog'));
    expect(dialog.getByText('Puesto', { selector: 'dt' })).toBeInTheDocument();
    expect(dialog.queryByText('Plaza', { selector: 'dt' })).not.toBeInTheDocument();
  });

  it('should_post_administrative_release_with_resource_type_and_refresh', async () => {
    const user = userEvent.setup();
    let sentBody: Record<string, unknown> | null = null;
    let occupancyCalls = 0;
    server.use(
      http.get(`${MSW_BASE}/occupancy`, ({ request }) => {
        occupancyCalls += 1;
        const date = new URL(request.url).searchParams.get('date');
        // La primera carga muestra el puesto ocupado; tras liberar, ya no aparece.
        const items = occupancyCalls === 1 ? [deskFixed] : [];
        return HttpResponse.json({ date, occupiedResources: items });
      }),
      http.post(`${MSW_BASE}/releases/administrative`, async ({ request }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 500, type: 'ADMINISTRATIVE' }, { status: 201 });
      }),
    );
    renderWithProviders(
      <>
        <ReleaseByDatePage />
        <Toast />
      </>,
    );

    await screen.findByText('Puesto 12');
    await user.click(screen.getByRole('button', { name: /^liberar$|^release$/i }));

    const dialog = within(await screen.findByRole('dialog'));
    await user.type(dialog.getByLabelText(/motivo|reason/i), 'No acude');
    await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));

    await waitFor(() =>
      expect(sentBody).toEqual({
        employeeId: 99,
        parkingSpaceId: 20,
        releaseDate: todayIso(),
        reason: 'No acude',
        resourceType: 'DESK',
      }),
    );
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(
      await screen.findByText(/liberación administrativa creada|administrative release created/i),
    ).toBeInTheDocument();
  });

  it('should_admin_cancel_request_when_releasing_request_origin_resource', async () => {
    const user = userEvent.setup();
    let cancelId: string | null = null;
    let cancelBody: Record<string, unknown> | null = null;
    let administrativeCalled = false;
    let occupancyCalls = 0;
    server.use(
      http.get(`${MSW_BASE}/occupancy`, ({ request }) => {
        occupancyCalls += 1;
        const date = new URL(request.url).searchParams.get('date');
        const items = occupancyCalls === 1 ? [deskRequest] : [];
        return HttpResponse.json({ date, occupiedResources: items });
      }),
      http.post(`${MSW_BASE}/releases/administrative`, () => {
        administrativeCalled = true;
        return HttpResponse.json({ id: 1 }, { status: 201 });
      }),
      http.post(`${MSW_BASE}/requests/:id/admin-cancel`, async ({ request, params }) => {
        cancelId = String(params.id);
        cancelBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: Number(params.id), status: 'CANCELLED' });
      }),
    );
    renderWithProviders(
      <>
        <ReleaseByDatePage />
        <Toast />
      </>,
    );

    await screen.findByText('Puesto 12');
    await user.click(screen.getByRole('button', { name: /^liberar$|^release$/i }));

    // El recurso proviene de una solicitud → modal de cancelación con motivo obligatorio.
    const dialog = within(await screen.findByRole('dialog'));
    await user.type(dialog.getByLabelText(/motivo|reason/i), 'El empleado no acude');
    await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));

    await waitFor(() => expect(cancelId).toBe('42'));
    expect(cancelBody).toEqual({ reason: 'El empleado no acude' });
    expect(administrativeCalled).toBe(false);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
});
