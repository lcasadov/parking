import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AdministrativeReleasesPage } from './AdministrativeReleasesPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';
import { addDaysIso, mondayOfWeek } from '../utils/calendar';

const PARKING_FIXED = {
  resourceType: 'PARKING',
  resourceId: 5,
  resourceNumber: 3005,
  floor: 3,
  employeeId: 10,
  employeeName: 'Alice Employee',
  origin: 'FIXED_ASSIGNMENT',
  requestId: null,
};

const DESK_APPROVED = {
  resourceType: 'DESK',
  resourceId: 12,
  resourceNumber: 12,
  floor: null,
  employeeId: 10,
  employeeName: 'Alice Employee',
  origin: 'REQUEST_APPROVED',
  requestId: 42,
};

// Devuelve una semana con las reservas indicadas en el dia 0 (lunes) y dia 1.
function weekOccupancy(
  weekStart: string,
  day0: Record<string, unknown>[],
  day1: Record<string, unknown>[] = [],
): Record<string, unknown> {
  const days = Array.from({ length: 7 }, (_, index) => ({
    date: addDaysIso(weekStart, index),
    reservations: index === 0 ? day0 : index === 1 ? day1 : [],
  }));
  return { employeeId: 10, employeeName: 'Alice Employee', weekStart, days };
}

// Registra empleados seleccionables + un handler de ocupacion que responde con
// las reservas de `day0`/`day1` para la semana solicitada. Devuelve el ultimo
// weekStart consultado para poder verificar la navegacion.
function useOccupancyHandler(
  day0: Record<string, unknown>[],
  day1: Record<string, unknown>[] = [],
) {
  const seen = { weekStart: '' };
  server.use(
    http.get(`${MSW_BASE}/releases/employees`, () =>
      HttpResponse.json([{ id: 10, fullName: 'Alice Employee' }]),
    ),
    http.get(`${MSW_BASE}/releases/employees/:id/occupancy`, ({ request }) => {
      const weekStart = new URL(request.url).searchParams.get('weekStart') ?? '';
      seen.weekStart = weekStart;
      return HttpResponse.json(weekOccupancy(weekStart, day0, day1));
    }),
  );
  return seen;
}

async function selectEmployee(): Promise<void> {
  const user = userEvent.setup();
  // Las opciones llegan de forma asincrona (GET /releases/employees); esperamos a
  // que la del empleado exista antes de seleccionarla.
  await screen.findByRole('option', { name: 'Alice Employee' });
  await user.selectOptions(screen.getByLabelText(/empleado|employee/i), '10');
}

describe('AdministrativeReleasesPage (ADMIN/AGENCIA) — liberación por empleado y semana', () => {
  it('should_prompt_to_pick_employee_before_any_is_selected', async () => {
    renderWithProviders(<AdministrativeReleasesPage />);
    expect(
      await screen.findByText(/selecciona un empleado para ver|select an employee to see/i),
    ).toBeInTheDocument();
  });

  it('should_render_parking_and_desk_reservations_for_selected_employee_week', async () => {
    useOccupancyHandler([PARKING_FIXED], [DESK_APPROVED]);
    renderWithProviders(<AdministrativeReleasesPage />);

    await selectEmployee();

    expect(await screen.findByText(/Plaza 3005 · Planta 3|Space 3005 · Floor 3/)).toBeInTheDocument();
    expect(await screen.findByText(/Puesto 12|Desk 12/)).toBeInTheDocument();
    expect(screen.getAllByRole('checkbox')).toHaveLength(2);
  });

  it('should_show_empty_state_when_employee_has_no_reservations_that_week', async () => {
    useOccupancyHandler([], []);
    renderWithProviders(<AdministrativeReleasesPage />);

    await selectEmployee();

    expect(
      await screen.findByText(/no tiene reservas esta semana|has no reservations this week/i),
    ).toBeInTheDocument();
  });

  it('should_refetch_next_week_when_navigating_forward', async () => {
    const seen = useOccupancyHandler([PARKING_FIXED]);
    const user = userEvent.setup();
    renderWithProviders(<AdministrativeReleasesPage />);

    await selectEmployee();
    await screen.findByText(/Plaza 3005|Space 3005/);
    const thisWeek = mondayOfWeek();
    await waitFor(() => expect(seen.weekStart).toBe(thisWeek));

    await user.click(screen.getByRole('button', { name: /siguiente|next/i }));

    await waitFor(() => expect(seen.weekStart).toBe(addDaysIso(thisWeek, 7)));
  });

  it('should_reject_release_when_reason_missing', async () => {
    useOccupancyHandler([PARKING_FIXED]);
    let requested = false;
    server.use(
      http.post(`${MSW_BASE}/releases/administrative`, () => {
        requested = true;
        return HttpResponse.json({ id: 1 }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdministrativeReleasesPage />);

    await selectEmployee();
    await user.click(await screen.findByRole('checkbox'));
    await user.click(screen.getByRole('button', { name: /liberar reservas|release reservations/i }));

    expect(screen.getByRole('alert')).toHaveTextContent(/mínimo 5 caracteres|minimum 5 characters/i);
    expect(requested).toBe(false);
  });

  it('should_reject_release_when_no_reservation_selected', async () => {
    useOccupancyHandler([PARKING_FIXED]);
    const user = userEvent.setup();
    renderWithProviders(<AdministrativeReleasesPage />);

    await selectEmployee();
    await screen.findByText(/Plaza 3005|Space 3005/);
    await user.type(screen.getByLabelText(/motivo|reason/i), 'No acude esta semana');
    await user.click(screen.getByRole('button', { name: /liberar reservas|release reservations/i }));

    expect(screen.getByRole('alert')).toHaveTextContent(
      /al menos una reserva|at least one reservation/i,
    );
  });

  it('should_release_batch_routing_each_reservation_to_its_endpoint', async () => {
    useOccupancyHandler([PARKING_FIXED], [DESK_APPROVED]);
    let adminBody: Record<string, unknown> | null = null;
    let cancelId: string | null = null;
    let cancelReason: string | null = null;
    server.use(
      http.post(`${MSW_BASE}/releases/administrative`, async ({ request }) => {
        adminBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 900, type: 'ADMINISTRATIVE' }, { status: 201 });
      }),
      http.post(`${MSW_BASE}/requests/:id/admin-cancel`, async ({ request, params }) => {
        cancelId = String(params.id);
        cancelReason = ((await request.json()) as { reason: string }).reason;
        return HttpResponse.json({ id: 42, status: 'CANCELLED' });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdministrativeReleasesPage />);

    await selectEmployee();
    await screen.findByText(/Puesto 12|Desk 12/);
    const checkboxes = screen.getAllByRole('checkbox');
    await user.click(checkboxes[0]);
    await user.click(checkboxes[1]);
    await user.type(screen.getByLabelText(/motivo|reason/i), 'No acude esta semana');
    await user.click(screen.getByRole('button', { name: /liberar reservas|release reservations/i }));

    await waitFor(() => expect(cancelId).toBe('42'));
    expect(cancelReason).toBe('No acude esta semana');
    expect(adminBody).toMatchObject({
      employeeId: 10,
      parkingSpaceId: 5,
      resourceType: 'PARKING',
      reason: 'No acude esta semana',
    });
    expect(
      await screen.findByText(/2 reserva\(s\) liberada\(s\)|2 reservation\(s\) released/i),
    ).toBeInTheDocument();
  });

  it('should_report_partial_failure_when_one_reservation_fails', async () => {
    useOccupancyHandler([PARKING_FIXED], [DESK_APPROVED]);
    server.use(
      http.post(`${MSW_BASE}/releases/administrative`, () =>
        HttpResponse.json({ id: 900, type: 'ADMINISTRATIVE' }, { status: 201 }),
      ),
      http.post(`${MSW_BASE}/requests/:id/admin-cancel`, () =>
        HttpResponse.json(
          { error: 'CONFLICT', message: 'not approved', timestamp: '2026-03-01T09:00:00Z' },
          { status: 409 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdministrativeReleasesPage />);

    await selectEmployee();
    await screen.findByText(/Puesto 12|Desk 12/);
    const checkboxes = screen.getAllByRole('checkbox');
    await user.click(checkboxes[0]);
    await user.click(checkboxes[1]);
    await user.type(screen.getByLabelText(/motivo|reason/i), 'No acude esta semana');
    await user.click(screen.getByRole('button', { name: /liberar reservas|release reservations/i }));

    expect(
      await screen.findByText(/1 reserva\(s\) liberada\(s\), 1 con error|1 reservation\(s\) released, 1 failed/i),
    ).toBeInTheDocument();
  });
});
