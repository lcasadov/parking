import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { EmployeeVehiclesPanel } from './EmployeeVehiclesPanel';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';
import type { EmployeeVehicle } from '../types/employeeVehicle';

const EMPLOYEE_ID = 15;
const VEHICLES_URL = `${MSW_BASE}/employees/${EMPLOYEE_ID}/vehicles`;

function vehicle(overrides: Partial<EmployeeVehicle> = {}): EmployeeVehicle {
  return {
    id: 3,
    licensePlate: '1234ABC',
    brand: 'Seat',
    model: 'Leon',
    color: 'Gris',
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('EmployeeVehiclesPanel', () => {
  it('should_show_save_first_hint_when_employee_has_no_id', () => {
    renderWithProviders(<EmployeeVehiclesPanel employeeId={null} />);

    expect(screen.getByText(/guarda primero|save the employee first/i)).toBeInTheDocument();
  });

  it('should_render_vehicles_table_when_employee_has_vehicles', async () => {
    server.use(http.get(VEHICLES_URL, () => HttpResponse.json([vehicle()])));

    renderWithProviders(<EmployeeVehiclesPanel employeeId={EMPLOYEE_ID} />);

    expect(await screen.findByText('1234ABC')).toBeInTheDocument();
    expect(screen.getByText('Seat')).toBeInTheDocument();
    expect(screen.getByText('Leon')).toBeInTheDocument();
  });

  it('should_reject_empty_plate_and_not_call_api', async () => {
    const user = userEvent.setup();
    let created = false;
    server.use(
      http.get(VEHICLES_URL, () => HttpResponse.json([])),
      http.post(VEHICLES_URL, () => {
        created = true;
        return HttpResponse.json(vehicle(), { status: 201 });
      }),
    );

    renderWithProviders(<EmployeeVehiclesPanel employeeId={EMPLOYEE_ID} />);

    await user.click(await screen.findByRole('button', { name: /añadir|add vehicle/i }));
    await user.click(screen.getByRole('button', { name: /guardar veh|save vehicle/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/matrícula|license plate/i);
    expect(created).toBe(false);
  });

  it('should_create_vehicle_and_refresh_list_after_success', async () => {
    const user = userEvent.setup();
    let listCalls = 0;
    const stored: EmployeeVehicle[] = [];
    server.use(
      http.get(VEHICLES_URL, () => {
        listCalls += 1;
        return HttpResponse.json([...stored]);
      }),
      http.post(VEHICLES_URL, async ({ request }) => {
        const body = (await request.json()) as { licensePlate: string };
        const created = vehicle({ id: 9, licensePlate: body.licensePlate });
        stored.push(created);
        return HttpResponse.json(created, { status: 201 });
      }),
    );

    renderWithProviders(<EmployeeVehiclesPanel employeeId={EMPLOYEE_ID} />);

    await user.click(await screen.findByRole('button', { name: /añadir|add vehicle/i }));
    const plateField = screen.getByLabelText(/matrícula|license plate/i);
    await user.type(plateField, '9999ZZZ');
    await user.click(screen.getByRole('button', { name: /guardar veh|save vehicle/i }));

    // La lista se invalida y refetcha, mostrando el vehículo recién creado.
    expect(await screen.findByText('9999ZZZ')).toBeInTheDocument();
    await waitFor(() => expect(listCalls).toBeGreaterThanOrEqual(2));
  });

  it('should_delete_vehicle_after_confirming_in_dialog', async () => {
    const user = userEvent.setup();
    let deleted = false;
    const stored = [vehicle()];
    server.use(
      http.get(VEHICLES_URL, () => HttpResponse.json(deleted ? [] : stored)),
      http.delete(`${VEHICLES_URL}/:vehicleId`, () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderWithProviders(<EmployeeVehiclesPanel employeeId={EMPLOYEE_ID} />);

    const row = (await screen.findByText('1234ABC')).closest('tr') as HTMLElement;
    await user.click(within(row).getByRole('button', { name: /^borrar$|^delete$/i }));

    // La confirmación vive en un diálogo aparte (portal), no dentro de la fila.
    const dialog = await screen.findByRole('alertdialog');
    await user.click(within(dialog).getByRole('button', { name: /sí, borrar|yes, delete/i }));

    await waitFor(() => expect(deleted).toBe(true));
  });

  it('should_show_status_column_and_open_history', async () => {
    const user = userEvent.setup();
    server.use(
      http.get(VEHICLES_URL, () => HttpResponse.json([vehicle({ status: 'APPROVED' })])),
      http.get(`${MSW_BASE}/employee-vehicles/3/history`, () =>
        HttpResponse.json([
          {
            id: 1,
            eventType: 'CREATED',
            actorRole: 'ADMIN',
            fromStatus: null,
            toStatus: 'APPROVED',
            note: null,
            previousData: null,
            createdAt: '2026-01-01T00:00:00Z',
          },
        ]),
      ),
    );

    renderWithProviders(<EmployeeVehiclesPanel employeeId={EMPLOYEE_ID} />);

    // Columna Estado (showStatus) visible en el tab admin.
    expect(await screen.findByText(/^aprobado$|^approved$/i)).toBeInTheDocument();

    const row = (await screen.findByText('1234ABC')).closest('tr') as HTMLElement;
    await user.click(within(row).getByRole('button', { name: /histórico|history/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/alta del veh|vehicle created/i)).toBeInTheDocument();
  });
});
