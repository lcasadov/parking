import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { VisitorFormModal } from './VisitorFormModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { visitorCarla } from '../mocks/visitorFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const VISITORS_URL = `${MSW_BASE}/visitors`;

function noop(): void {
  // sin efecto: callback de cierre en tests que no lo verifican.
}

describe('VisitorFormModal', () => {
  it('should_create_visitor_when_nationalId_is_unique', async () => {
    const user = userEvent.setup();
    let sentBody: Record<string, unknown> | null = null;
    let saved = false;
    server.use(
      http.post(VISITORS_URL, async ({ request }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...visitorCarla, ...sentBody, id: 99 }, { status: 201 });
      }),
    );
    renderWithProviders(<VisitorFormModal onClose={noop} onSaved={() => (saved = true)} />);

    await user.type(screen.getByLabelText(/^nombre$|^first name$/i), 'Nora');
    await user.type(screen.getByLabelText(/^apellidos$|^last name$/i), 'Nieto');
    await user.type(screen.getByLabelText(/dni|document/i), '99999999R');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => expect(saved).toBe(true));
    expect(sentBody).toMatchObject({
      firstName: 'Nora',
      lastName: 'Nieto',
      nationalId: '99999999R',
    });
  });

  it('should_show_inline_error_when_nationalId_is_duplicate_409', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(VISITORS_URL, () =>
        HttpResponse.json(
          {
            error: 'CONFLICT',
            message: 'duplicate',
            fields: { nationalId: 'exists' },
            timestamp: '2026-03-01T09:00:00Z',
          },
          { status: 409 },
        ),
      ),
    );
    renderWithProviders(<VisitorFormModal onClose={noop} onSaved={noop} />);

    await user.type(screen.getByLabelText(/^nombre$|^first name$/i), 'Nora');
    await user.type(screen.getByLabelText(/^apellidos$|^last name$/i), 'Nieto');
    await user.type(screen.getByLabelText(/dni|document/i), '12345678Z');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findByText(
        /ya existe un visitante con ese documento|a visitor with that document already exists/i,
      ),
    ).toBeInTheDocument();
  });

  it('should_show_required_errors_when_submitting_without_fields', async () => {
    const user = userEvent.setup();
    let posted = false;
    server.use(
      http.post(VISITORS_URL, () => {
        posted = true;
        return HttpResponse.json(visitorCarla, { status: 201 });
      }),
    );
    renderWithProviders(<VisitorFormModal onClose={noop} onSaved={noop} />);

    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findAllByText(/este campo es obligatorio|this field is required/i),
    ).toHaveLength(3);
    expect(posted).toBe(false);
  });

  it('should_let_add_vehicles_in_draft_mode_while_creating_without_saving_first', async () => {
    const user = userEvent.setup();
    renderWithProviders(<VisitorFormModal onClose={noop} onSaved={noop} />);

    await user.click(screen.getByRole('tab', { name: /veh[íi]culos|vehicles/i }));

    // Sin guardar antes: hay botón de añadir (modo borrador), no el aviso "guarda primero".
    expect(
      screen.queryByText(/guarda primero el visitante|save the visitor first/i),
    ).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /añadir|add vehicle/i })).toBeInTheDocument();
  });

  it('should_create_visitor_then_persist_draft_vehicles', async () => {
    const user = userEvent.setup();
    let visitorBody: Record<string, unknown> | null = null;
    const createdVehicles: Record<string, unknown>[] = [];
    let saved = false;
    server.use(
      http.post(VISITORS_URL, async ({ request }) => {
        visitorBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...visitorCarla, ...visitorBody, id: 77 }, { status: 201 });
      }),
      http.post(`${VISITORS_URL}/77/vehicles`, async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        createdVehicles.push(body);
        return HttpResponse.json({ id: 5, visitorId: 77, ...body }, { status: 201 });
      }),
    );
    renderWithProviders(<VisitorFormModal onClose={noop} onSaved={() => (saved = true)} />);

    // Datos mínimos del visitante.
    await user.type(screen.getByLabelText(/^nombre$|^first name$/i), 'Nora');
    await user.type(screen.getByLabelText(/^apellidos$|^last name$/i), 'Nieto');
    await user.type(screen.getByLabelText(/dni|document/i), '55555555K');

    // Añade un vehículo en borrador (sin guardar antes el visitante).
    await user.click(screen.getByRole('tab', { name: /veh[íi]culos|vehicles/i }));
    await user.click(screen.getByRole('button', { name: /añadir|add vehicle/i }));
    await user.type(screen.getByLabelText(/matrícula|license plate/i), '4321DCB');
    await user.click(screen.getByRole('button', { name: /guardar veh|save vehicle/i }));
    expect(await screen.findByText('4321DCB')).toBeInTheDocument();

    // Guarda el visitante desde la pestaña de detalles.
    await user.click(screen.getByRole('tab', { name: /detalles|details/i }));
    await user.click(screen.getByRole('button', { name: /^guardar$|^save$/i }));

    await waitFor(() => expect(saved).toBe(true));
    expect(visitorBody).toMatchObject({ firstName: 'Nora', nationalId: '55555555K' });
    expect(createdVehicles).toEqual([
      expect.objectContaining({ licensePlate: '4321DCB' }),
    ]);
  });

  it('should_update_visitor_when_editing_existing_card', async () => {
    const user = userEvent.setup();
    let putId: string | undefined;
    let saved = false;
    server.use(
      http.put(`${VISITORS_URL}/:id`, async ({ request, params }) => {
        putId = params.id as string;
        const body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...visitorCarla, ...body, id: Number(params.id) });
      }),
    );
    renderWithProviders(
      <VisitorFormModal visitor={visitorCarla} onClose={noop} onSaved={() => (saved = true)} />,
    );

    const dialog = within(screen.getByRole('dialog'));
    expect(dialog.getByLabelText(/^nombre$|^first name$/i)).toHaveValue('Carla');
    await user.clear(dialog.getByLabelText(/empresa|company/i));
    await user.type(dialog.getByLabelText(/empresa|company/i), 'Nueva Empresa');
    await user.click(dialog.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => expect(saved).toBe(true));
    expect(putId).toBe(String(visitorCarla.id));
  });
});
