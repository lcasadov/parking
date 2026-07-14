import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { CreateRequestModal } from './CreateRequestModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { requestApproved, requestPending1 } from '../mocks/requestFixtures';
import { renderWithProviders } from '../test/renderWithProviders';
import { todayIso } from '../utils/requests';
import { API_ERROR_TOAST, type ApiErrorToastDetail } from '../api/events';
import { floorPlanOf } from '../mocks/floorPlanFixtures';
import type { FloorPlanDesk } from '../types/floorPlan';

const REQUESTS_URL = `${MSW_BASE}/requests`;
const FLOOR_PLAN_URL = `${MSW_BASE}/floor-plan`;

// Puesto con id (55) distinto del número (12): permite verificar que el modal
// muestra el NÚMERO y nunca el identificador interno (tasks §4.1).
const pickableDesk: FloorPlanDesk = {
  deskId: 55,
  deskNumber: 12,
  category: 'STANDARD',
  coordX: 20,
  coordY: 30,
  state: 'FREE',
};

// Captura los mensajes (claves i18n) de los toasts emitidos por el modal.
function captureToasts(): { messages: string[] } {
  const captured = { messages: [] as string[] };
  window.addEventListener(API_ERROR_TOAST, (event) => {
    captured.messages.push((event as CustomEvent<ApiErrorToastDetail>).detail.message);
  });
  return captured;
}

// Captura los cuerpos de cada POST /requests para comprobar resourceType/resourceId.
function captureRequestBodies(): {
  bodies: Array<{ resourceType?: string; resourceId?: number }>;
} {
  const captured = { bodies: [] as Array<{ resourceType?: string; resourceId?: number }> };
  server.use(
    http.post(REQUESTS_URL, async ({ request }) => {
      const body = (await request.json()) as {
        requestedDate: string;
        resourceType?: string;
        resourceId?: number;
      };
      captured.bodies.push(body);
      return HttpResponse.json(
        { ...requestPending1, id: 999, requestedDate: body.requestedDate },
        { status: 201 },
      );
    }),
  );
  return captured;
}

async function fillDate(): Promise<void> {
  const user = userEvent.setup();
  await user.clear(screen.getByLabelText(/fecha de la solicitud|request date/i));
  await user.type(screen.getByLabelText(/fecha de la solicitud|request date/i), todayIso());
}

async function submit(): Promise<void> {
  const user = userEvent.setup();
  const dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('button', { name: /enviar solicitud|submit request/i }));
}

describe('CreateRequestModal (unified request)', () => {
  it('should_post_only_parking_request_when_only_parking_selected', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(1);
    expect(captured.bodies[0].resourceType).toBe('PARKING');
  });

  it('should_post_desk_request_when_only_desk_selected', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await user.click(screen.getByLabelText(/plaza de parking|parking space/i));
    await user.click(screen.getByLabelText(/puesto de oficina|office desk/i));
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(1);
    expect(captured.bodies[0].resourceType).toBe('DESK');
  });

  it('should_post_both_requests_when_parking_and_desk_selected', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await user.click(screen.getByLabelText(/puesto de oficina|office desk/i));
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(2);
    const types = captured.bodies.map((b) => b.resourceType).sort();
    expect(types).toEqual(['DESK', 'PARKING']);
  });

  it('should_show_error_when_no_resource_selected', async () => {
    const captured = captureRequestBodies();
    const user = userEvent.setup();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={vi.fn()} />);

    await fillDate();
    await user.click(screen.getByLabelText(/plaza de parking|parking space/i));
    await submit();

    expect(
      await screen.findByText(/al menos un recurso|at least one resource/i),
    ).toBeInTheDocument();
    expect(captured.bodies).toHaveLength(0);
  });

  it('should_show_instant_approval_feedback_when_request_is_born_approved', async () => {
    const toasts = captureToasts();
    server.use(
      http.post(REQUESTS_URL, () =>
        HttpResponse.json({ ...requestApproved, id: 777 }, { status: 201 }),
      ),
    );
    const onCreated = vi.fn();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(toasts.messages).toContain('requests.mine.createdApproved');
    expect(toasts.messages).not.toContain('requests.mine.created');
  });

  it('should_show_no_availability_message_when_automatic_has_no_free_space', async () => {
    const toasts = captureToasts();
    server.use(
      http.post(REQUESTS_URL, () =>
        HttpResponse.json(
          { error: 'NO_AVAILABILITY', message: 'no free space', timestamp: '2026-03-02T10:00:00Z' },
          { status: 409 },
        ),
      ),
    );
    const onCreated = vi.fn();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await submit();

    await waitFor(() =>
      expect(toasts.messages).toContain('requests.errors.noAvailability'),
    );
    expect(onCreated).not.toHaveBeenCalled();
  });

  it('should_show_choose_desk_button_only_when_desk_is_selected', async () => {
    const user = userEvent.setup();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={vi.fn()} />);

    // Sin PUESTO seleccionado no hay botón de selección de puesto.
    expect(
      screen.queryByRole('button', { name: /seleccionar puesto|select desk/i }),
    ).not.toBeInTheDocument();

    await fillDate();
    await user.click(screen.getByLabelText(/puesto de oficina|office desk/i));

    expect(
      screen.getByRole('button', { name: /seleccionar puesto|select desk/i }),
    ).toBeInTheDocument();
  });

  it('should_show_chosen_desk_number_never_the_id_after_picking', async () => {
    server.use(http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([pickableDesk]))));
    const user = userEvent.setup();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={vi.fn()} />);

    await fillDate();
    await user.click(screen.getByLabelText(/puesto de oficina|office desk/i));
    await user.click(screen.getByRole('button', { name: /seleccionar puesto|select desk/i }));

    // El plano se abre como selector: pinchar el puesto libre (número 12).
    const marker = await screen.findByRole('button', { name: /puesto 12|desk 12/i });
    await user.click(marker);

    // El modal muestra el NÚMERO (12), nunca el identificador interno (55).
    expect(
      await screen.findByText(/puesto elegido: 12|chosen desk: 12/i),
    ).toBeInTheDocument();
    expect(screen.queryByText(/55/)).not.toBeInTheDocument();
  });

  it('should_post_resourceId_of_chosen_desk_when_submitting_desk_request', async () => {
    const captured = captureRequestBodies();
    server.use(http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf([pickableDesk]))));
    const onCreated = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await user.click(screen.getByLabelText(/plaza de parking|parking space/i)); // desmarca PLAZA
    await user.click(screen.getByLabelText(/puesto de oficina|office desk/i));
    await user.click(screen.getByRole('button', { name: /seleccionar puesto|select desk/i }));
    await user.click(await screen.findByRole('button', { name: /puesto 12|desk 12/i }));
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(1);
    expect(captured.bodies[0].resourceType).toBe('DESK');
    expect(captured.bodies[0].resourceId).toBe(55);
  });
});
