import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { VisitorVehiclesPanel } from './VisitorVehiclesPanel';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';
import type { VisitorVehicle } from '../types/visitorVehicle';

const VISITOR_ID = 15;
const VEHICLES_URL = `${MSW_BASE}/visitors/${VISITOR_ID}/vehicles`;

function vehicle(overrides: Partial<VisitorVehicle> = {}): VisitorVehicle {
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

describe('VisitorVehiclesPanel', () => {
  it('should_show_save_first_hint_when_visitor_has_no_id', () => {
    renderWithProviders(<VisitorVehiclesPanel visitorId={null} />);

    expect(screen.getByText(/guarda primero el visitante|save the visitor first/i)).toBeInTheDocument();
  });

  it('should_render_vehicles_table_when_visitor_has_vehicles', async () => {
    server.use(http.get(VEHICLES_URL, () => HttpResponse.json([vehicle()])));

    renderWithProviders(<VisitorVehiclesPanel visitorId={VISITOR_ID} />);

    expect(await screen.findByText('1234ABC')).toBeInTheDocument();
    expect(screen.getByText('Seat')).toBeInTheDocument();
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

    renderWithProviders(<VisitorVehiclesPanel visitorId={VISITOR_ID} />);

    await user.click(await screen.findByRole('button', { name: /añadir|add vehicle/i }));
    await user.click(screen.getByRole('button', { name: /guardar veh|save vehicle/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/matrícula|license plate/i);
    expect(created).toBe(false);
  });

  it('should_create_vehicle_and_refresh_list_after_success', async () => {
    const user = userEvent.setup();
    let listCalls = 0;
    const stored: VisitorVehicle[] = [];
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

    renderWithProviders(<VisitorVehiclesPanel visitorId={VISITOR_ID} />);

    await user.click(await screen.findByRole('button', { name: /añadir|add vehicle/i }));
    await user.type(screen.getByLabelText(/matrícula|license plate/i), '9999ZZZ');
    await user.click(screen.getByRole('button', { name: /guardar veh|save vehicle/i }));

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

    renderWithProviders(<VisitorVehiclesPanel visitorId={VISITOR_ID} />);

    const row = (await screen.findByText('1234ABC')).closest('tr') as HTMLElement;
    await user.click(within(row).getByRole('button', { name: /^borrar$|^delete$/i }));

    const dialog = await screen.findByRole('alertdialog');
    await user.click(within(dialog).getByRole('button', { name: /sí, borrar|yes, delete/i }));

    await waitFor(() => expect(deleted).toBe(true));
  });
});
