import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { MyVehiclesPage } from './MyVehiclesPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';
import type { Vehicle } from '../types/vehicle';

const VEHICLES_URL = `${MSW_BASE}/me/vehicles`;

function vehicle(overrides: Partial<Vehicle> = {}): Vehicle {
  return {
    id: 1,
    licensePlate: '1234ABC',
    brand: 'Seat',
    model: 'Leon',
    color: 'Gris',
    status: 'PENDING',
    rejectionReason: null,
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('MyVehiclesPage', () => {
  it('should_render_vehicles_with_status_and_rejection_reason', async () => {
    server.use(
      http.get(VEHICLES_URL, () =>
        HttpResponse.json([
          vehicle({ id: 1, licensePlate: 'PEND111', status: 'PENDING' }),
          vehicle({ id: 2, licensePlate: 'APRV222', status: 'APPROVED' }),
          vehicle({
            id: 3,
            licensePlate: 'REJK333',
            status: 'REJECTED',
            rejectionReason: 'Matrícula ilegible',
          }),
        ]),
      ),
    );

    renderWithProviders(<MyVehiclesPage />);

    expect(await screen.findByText('PEND111')).toBeInTheDocument();
    expect(screen.getByText(/^pendiente$|^pending$/i)).toBeInTheDocument();
    expect(screen.getByText(/^aprobado$|^approved$/i)).toBeInTheDocument();
    expect(screen.getByText(/^rechazado$|^rejected$/i)).toBeInTheDocument();
    expect(screen.getByText(/matrícula ilegible/i)).toBeInTheDocument();
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

    renderWithProviders(<MyVehiclesPage />);

    await user.click(await screen.findByRole('button', { name: /añadir|add vehicle/i }));
    // El aviso de validación del modal (frase única del formNotice).
    expect(
      screen.getByText(/podrás usarlo cuando lo aprueben|you can use it once approved/i),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /guardar veh|save vehicle/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/matrícula|license plate/i);
    expect(created).toBe(false);
  });

  it('should_create_vehicle_pending_and_refresh_list', async () => {
    const user = userEvent.setup();
    const stored: Vehicle[] = [];
    let listCalls = 0;
    server.use(
      http.get(VEHICLES_URL, () => {
        listCalls += 1;
        return HttpResponse.json([...stored]);
      }),
      http.post(VEHICLES_URL, async ({ request }) => {
        const body = (await request.json()) as { licensePlate: string };
        const created = vehicle({ id: 9, licensePlate: body.licensePlate, status: 'PENDING' });
        stored.push(created);
        return HttpResponse.json(created, { status: 201 });
      }),
    );

    renderWithProviders(<MyVehiclesPage />);

    await user.click(await screen.findByRole('button', { name: /añadir|add vehicle/i }));
    await user.type(screen.getByLabelText(/matrícula|license plate/i), '9999ZZZ');
    await user.click(screen.getByRole('button', { name: /guardar veh|save vehicle/i }));

    expect(await screen.findByText('9999ZZZ')).toBeInTheDocument();
    await waitFor(() => expect(listCalls).toBeGreaterThanOrEqual(2));
  });

  it('should_delete_vehicle_after_confirming_in_dialog', async () => {
    const user = userEvent.setup();
    let deleted = false;
    server.use(
      http.get(VEHICLES_URL, () => HttpResponse.json(deleted ? [] : [vehicle()])),
      http.delete(`${VEHICLES_URL}/:vehicleId`, () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderWithProviders(<MyVehiclesPage />);

    const card = (await screen.findByText('1234ABC')).closest('li') as HTMLElement;
    await user.click(within(card).getByRole('button', { name: /^borrar$|^delete$/i }));

    const dialog = await screen.findByRole('alertdialog');
    await user.click(within(dialog).getByRole('button', { name: /sí, borrar|yes, delete/i }));

    await waitFor(() => expect(deleted).toBe(true));
  });
});
