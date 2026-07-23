import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { FloorPlanPage } from './FloorPlanPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, employeeUser } from '../mocks/fixtures';
import {
  defaultFloorPlan,
  floorDeskExecutive,
  floorDeskFree,
  floorDeskMine,
  floorDeskAssigned,
  floorDeskUnplaced,
  floorPlanOf,
} from '../mocks/floorPlanFixtures';
import { renderWithProviders } from '../test/renderWithProviders';
import { addDaysIso } from '../utils/calendar';
import { todayIso } from '../utils/requests';

const FLOOR_PLAN_URL = `${MSW_BASE}/floor-plan`;
const ME_URL = `${MSW_BASE}/auth/me`;

function useEmployee(): void {
  server.use(http.get(ME_URL, () => HttpResponse.json(employeeUser)));
}

function useAdmin(): void {
  server.use(http.get(ME_URL, () => HttpResponse.json(adminUser)));
}

// Los flujos de solicitud desde el plano piden confirmacion explicita antes de
// crear la solicitud (requests spec): clicar el marcador/boton solo abre el
// dialogo; hay que confirmar para que se dispare el POST.
async function confirmPendingRequest(): Promise<void> {
  const user = userEvent.setup();
  const dialog = await screen.findByRole('dialog');
  await user.click(within(dialog).getByRole('button', { name: /^solicitar$|^request$/i }));
}

describe('FloorPlanPage', () => {
  it('should_render_a_marker_per_placed_desk_when_plan_loads', async () => {
    useEmployee();
    renderWithProviders(<FloorPlanPage />);

    // 6 placed desks (deskUnplaced has null coords → not a marker).
    const markers = await screen.findAllByTestId('floor-marker');
    expect(markers).toHaveLength(6);
  });

  it('should_position_marker_by_percentage_coordinates_when_rendering', async () => {
    useEmployee();
    server.use(http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([floorDeskFree]))));
    renderWithProviders(<FloorPlanPage />);

    const marker = await screen.findByTestId('floor-marker');
    expect(marker).toHaveStyle({ left: '20%', top: '30%' });
  });

  it('should_color_marker_by_state_when_rendering', async () => {
    useEmployee();
    server.use(
      http.get(FLOOR_PLAN_URL, () =>
        HttpResponse.json(floorPlanOf([floorDeskFree, floorDeskMine, floorDeskAssigned])),
      ),
    );
    renderWithProviders(<FloorPlanPage />);

    const free = await screen.findByRole('button', { name: /puesto 1/i });
    const mine = screen.getByRole('button', { name: /puesto 3/i });
    const assigned = screen.getByRole('button', { name: /puesto 2/i });
    expect(free).toHaveClass('floor-marker-free');
    expect(mine).toHaveClass('floor-marker-mine');
    expect(assigned).toHaveClass('floor-marker-assigned');
  });

  it('should_distinguish_executive_desks_visually_when_rendering', async () => {
    useEmployee();
    server.use(
      http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([floorDeskExecutive]))),
    );
    renderWithProviders(<FloorPlanPage />);

    const marker = await screen.findByRole('button', { name: /puesto 6/i });
    expect(marker).toHaveClass('floor-marker-executive');
  });

  it('should_list_unplaced_desks_apart_when_desk_has_no_coordinates', async () => {
    useEmployee();
    server.use(
      http.get(FLOOR_PLAN_URL, () =>
        HttpResponse.json(floorPlanOf([floorDeskFree, floorDeskUnplaced])),
      ),
    );
    renderWithProviders(<FloorPlanPage />);

    // Only 1 marker (the placed desk); the unplaced desk shows in the aside list.
    const markers = await screen.findAllByTestId('floor-marker');
    expect(markers).toHaveLength(1);
    const unplaced = screen.getByTestId('floor-unplaced');
    expect(within(unplaced).getByText(/7/)).toBeInTheDocument();
  });

  it('should_reload_plan_with_new_date_when_datebar_steps_forward', async () => {
    useEmployee();
    const dates: string[] = [];
    server.use(
      http.get(FLOOR_PLAN_URL, ({ request }) => {
        dates.push(new URL(request.url).searchParams.get('date') ?? '');
        return HttpResponse.json(defaultFloorPlan);
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);
    await screen.findAllByTestId('floor-marker');

    // El stepper de la datebar es el único control de fecha (se eliminó el
    // input[type=date] redundante). Avanzar un día debe recargar el plano.
    await user.click(screen.getByRole('button', { name: /día siguiente|next day/i }));

    await waitFor(() => expect(dates).toContain(addDaysIso(todayIso(), 1)));
  });

  it('should_show_success_feedback_when_requesting_a_free_desk', async () => {
    useEmployee();
    let requestedDeskId: string | undefined;
    server.use(
      http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([floorDeskFree]))),
      http.post(`${FLOOR_PLAN_URL}/desks/:deskId/request`, ({ params }) => {
        requestedDeskId = params.deskId as string;
        return HttpResponse.json({ requestId: 1, state: 'REQUESTED' }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);

    const marker = await screen.findByRole('button', { name: /puesto 1/i });
    await user.click(marker);
    await confirmPendingRequest();

    expect(await screen.findByText(/solicitud creada|request created/i)).toBeInTheDocument();
    expect(requestedDeskId).toBe('1');
  });

  it('should_ask_for_confirmation_before_requesting_a_free_desk', async () => {
    useEmployee();
    server.use(http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([floorDeskFree]))));
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);

    const marker = await screen.findByRole('button', { name: /puesto 1/i });
    await user.click(marker);

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/1/)).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: /^cancelar$|^cancel$/i }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('should_show_conflict_feedback_when_requesting_a_non_free_desk_returns_409', async () => {
    useEmployee();
    server.use(
      http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([floorDeskFree]))),
      http.post(`${FLOOR_PLAN_URL}/desks/:deskId/request`, () =>
        HttpResponse.json(
          { error: 'NOT_AVAILABLE', message: 'desk not free', timestamp: new Date().toISOString() },
          { status: 409 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);

    const marker = await screen.findByRole('button', { name: /puesto 1/i });
    await user.click(marker);
    await confirmPendingRequest();

    expect(
      await screen.findByText(/no está disponible|no longer available|not available/i),
    ).toBeInTheDocument();
  });

  it('should_show_spinner_while_plan_is_loading', async () => {
    useEmployee();
    server.use(
      http.get(FLOOR_PLAN_URL, async () => {
        await new Promise((resolve) => setTimeout(resolve, 40));
        return HttpResponse.json(defaultFloorPlan);
      }),
    );
    renderWithProviders(<FloorPlanPage />);

    expect(await screen.findByText(/cargando|loading/i)).toBeInTheDocument();
    await screen.findAllByTestId('floor-marker');
  });

  it('should_show_error_message_when_date_is_outside_request_window', async () => {
    useEmployee();
    server.use(
      http.get(FLOOR_PLAN_URL, () =>
        HttpResponse.json(
          {
            error: 'OUTSIDE_REQUEST_WINDOW',
            message: 'date out of window',
            fields: { date: 'out of range' },
            timestamp: new Date().toISOString(),
          },
          { status: 400 },
        ),
      ),
    );
    renderWithProviders(<FloorPlanPage />);

    expect(
      await screen.findByText(/no puede ser anterior a hoy|cannot be earlier than today/i),
    ).toBeInTheDocument();
  });

  it('should_show_the_save_positions_button_and_confirm_on_click', async () => {
    useAdmin();
    server.use(http.get(FLOOR_PLAN_URL, () => HttpResponse.json(defaultFloorPlan)));
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);
    await screen.findAllByTestId('floor-marker');

    await user.click(screen.getByRole('button', { name: /editar posiciones|edit positions/i }));
    // El editor expone un guardado explícito además del auto-save por arrastre.
    await user.click(screen.getByRole('button', { name: /guardar posiciones|save positions/i }));

    expect(
      await screen.findByText(/posiciones guardadas|positions saved/i),
    ).toBeInTheDocument();
  });

  it('should_render_markers_neutral_in_edit_mode', async () => {
    useAdmin();
    server.use(
      http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([floorDeskAssigned]))),
    );
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);
    await screen.findByTestId('floor-marker');

    await user.click(screen.getByRole('button', { name: /editar posiciones|edit positions/i }));

    const marker = screen.getByRole('button', { name: /puesto 2/i });
    expect(marker).toHaveClass('floor-marker-neutral');
    expect(marker).not.toHaveClass('floor-marker-assigned');
  });

  it('should_not_show_edit_toggle_when_user_is_employee', async () => {
    useEmployee();
    renderWithProviders(<FloorPlanPage />);
    await screen.findAllByTestId('floor-marker');

    expect(
      screen.queryByRole('button', { name: /editar posiciones|edit positions/i }),
    ).not.toBeInTheDocument();
  });

  it('should_show_edit_toggle_when_user_is_admin', async () => {
    useAdmin();
    renderWithProviders(<FloorPlanPage />);
    await screen.findAllByTestId('floor-marker');

    expect(
      screen.getByRole('button', { name: /editar posiciones|edit positions/i }),
    ).toBeInTheDocument();
  });

  it('should_persist_new_position_when_admin_drags_a_marker', async () => {
    useAdmin();
    let putBody: Record<string, unknown> | null = null;
    let putDeskId: string | undefined;
    server.use(
      http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([floorDeskFree]))),
      http.put(`${FLOOR_PLAN_URL}/desks/:deskId/position`, async ({ request, params }) => {
        putBody = (await request.json()) as Record<string, unknown>;
        putDeskId = params.deskId as string;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);

    await screen.findByTestId('floor-marker');
    await user.click(screen.getByRole('button', { name: /editar posiciones|edit positions/i }));

    const surface = screen.getByTestId('floor-plan-surface');
    surface.getBoundingClientRect = vi.fn().mockReturnValue({
      left: 0,
      top: 0,
      width: 1000,
      height: 1000,
      right: 1000,
      bottom: 1000,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    });

    const marker = screen.getByRole('button', { name: /puesto 1/i });
    // Pointer Events (unifican ratón + táctil): arrastre del marcador.
    fireEvent.pointerDown(marker, { clientX: 200, clientY: 300, pointerId: 1 });
    fireEvent.pointerMove(surface, { clientX: 250, clientY: 500, pointerId: 1 });
    fireEvent.pointerUp(surface, { clientX: 250, clientY: 500, pointerId: 1 });

    await waitFor(() => expect(putBody).not.toBeNull());
    expect(putDeskId).toBe('1');
    expect(putBody).toEqual({ coordX: 25, coordY: 50 });
  });

  it('should_request_a_free_desk_from_the_mobile_list', async () => {
    useEmployee();
    let requestedDeskId: string | undefined;
    server.use(
      http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([floorDeskFree]))),
      http.post(`${FLOOR_PLAN_URL}/desks/:deskId/request`, ({ params }) => {
        requestedDeskId = params.deskId as string;
        return HttpResponse.json({ requestId: 2, state: 'REQUESTED' }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);
    await screen.findByTestId('floor-marker');

    // The mobile "available to request" list exposes a Solicitar button per row.
    await user.click(screen.getByRole('button', { name: /^solicitar$|^request$/i }));
    await confirmPendingRequest();

    expect(await screen.findByText(/solicitud creada|request created/i)).toBeInTheDocument();
    expect(requestedDeskId).toBe('1');
  });

  it('should_show_conflict_feedback_from_the_mobile_list_on_409', async () => {
    useEmployee();
    server.use(
      http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([floorDeskFree]))),
      http.post(`${FLOOR_PLAN_URL}/desks/:deskId/request`, () =>
        HttpResponse.json(
          { error: 'NOT_AVAILABLE', message: 'taken', timestamp: new Date().toISOString() },
          { status: 409 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);
    await screen.findByTestId('floor-marker');

    await user.click(screen.getByRole('button', { name: /^solicitar$|^request$/i }));
    await confirmPendingRequest();

    expect(
      await screen.findByText(/no está disponible|no longer available/i),
    ).toBeInTheDocument();
  });

  it('should_dim_non_matching_markers_when_a_state_filter_is_active', async () => {
    useEmployee();
    server.use(
      http.get(FLOOR_PLAN_URL, () =>
        HttpResponse.json(floorPlanOf([floorDeskFree, floorDeskAssigned])),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);
    await screen.findByRole('button', { name: /puesto 1/i });

    // Activate the "Occupied" (ASSIGNED) filter chip (scoped to the filters group
    // so the marker whose label also contains "Ocupado" is not matched).
    const filters = within(screen.getByRole('group', { name: /filtros por estado|filters by state/i }));
    await user.click(filters.getByRole('button', { name: /ocupado|occupied/i }));

    expect(screen.getByRole('button', { name: /puesto 1/i })).toHaveClass('floor-marker-dimmed');
    expect(screen.getByRole('button', { name: /puesto 2/i })).not.toHaveClass(
      'floor-marker-dimmed',
    );
  });

  it('should_navigate_dates_with_the_datebar', async () => {
    useEmployee();
    const dates: string[] = [];
    server.use(
      http.get(FLOOR_PLAN_URL, ({ request }) => {
        dates.push(new URL(request.url).searchParams.get('date') ?? '');
        return HttpResponse.json(defaultFloorPlan);
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);
    await screen.findAllByTestId('floor-marker');

    await user.click(screen.getByRole('button', { name: /día siguiente|next day/i }));

    await waitFor(() => expect(dates.length).toBeGreaterThan(1));
  });

  it('should_not_request_desk_when_admin_is_in_edit_mode', async () => {
    useAdmin();
    let requestCalled = false;
    server.use(
      http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([floorDeskFree]))),
      http.post(`${FLOOR_PLAN_URL}/desks/:deskId/request`, () => {
        requestCalled = true;
        return HttpResponse.json({ requestId: 1, state: 'REQUESTED' }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanPage />);

    await screen.findByTestId('floor-marker');
    await user.click(screen.getByRole('button', { name: /editar posiciones|edit positions/i }));
    await user.click(screen.getByRole('button', { name: /puesto 1/i }));

    // A pure click (no drag) in edit mode must not create a request.
    expect(requestCalled).toBe(false);
  });
});
