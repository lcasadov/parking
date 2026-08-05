import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CreateRequestModal } from './CreateRequestModal';
import { server } from '../mocks/server';
import { MSW_BASE, waitlistConflictThenSuccessHandler } from '../mocks/handlers';
import { pageOfRequests, requestApproved, requestPending1 } from '../mocks/requestFixtures';
import { emptyAvailability } from '../mocks/calendarFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

// Congela el reloj a un LUNES fijo: los tests navegan el calendario por fechas
// relativas a HOY (addDaysIso(todayIso(), n)); sin congelar, según el día en que
// se ejecuten, la celda destino puede caer en otro mes o en un día no clicable.
// Se falsea solo `Date` para no interferir con userEvent/react-query.
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date('2026-08-03T10:00:00Z'));
});

afterEach(() => {
  vi.useRealTimers();
});
import { addDaysIso } from '../utils/calendar';
import { todayIso } from '../utils/requests';
import { API_ERROR_TOAST, type ApiErrorToastDetail } from '../api/events';

const REQUESTS_URL = `${MSW_BASE}/requests`;
const AVAILABILITY_URL = `${MSW_BASE}/availability`;
const APPROVAL_MODE_URL = `${MSW_BASE}/settings/approval-mode`;

// Fuerza disponibilidad 0 para PARKING (deja DESK con la disponibilidad por
// defecto), para probar el aviso honesto de lista de espera.
function zeroParkingAvailability(): void {
  server.use(
    http.get(AVAILABILITY_URL, ({ request }) => {
      const url = new URL(request.url);
      const date = url.searchParams.get('date') ?? todayIso();
      if (url.searchParams.get('resourceType') === 'PARKING') {
        return HttpResponse.json({ ...emptyAvailability, date });
      }
      return HttpResponse.json({ ...emptyAvailability, date, availableResources: [{ parkingSpaceId: 1, label: 'D-01' }] });
    }),
  );
}

function useAutomaticApprovalMode(): void {
  server.use(http.get(APPROVAL_MODE_URL, () => HttpResponse.json({ approvalMode: 'AUTOMATIC' })));
}

// Captura los mensajes (claves i18n) de los toasts emitidos por el modal.
function captureToasts(): { messages: string[] } {
  const captured = { messages: [] as string[] };
  window.addEventListener(API_ERROR_TOAST, (event) => {
    captured.messages.push((event as CustomEvent<ApiErrorToastDetail>).detail.message);
  });
  return captured;
}

// Captura los cuerpos de cada POST /requests para comprobar resourceType/resourceId/waitlist.
function captureRequestBodies(): {
  bodies: Array<{
    requestedDate?: string;
    resourceType?: string;
    resourceId?: number;
    waitlist?: boolean;
  }>;
} {
  const captured = {
    bodies: [] as Array<{
      requestedDate?: string;
      resourceType?: string;
      resourceId?: number;
      waitlist?: boolean;
    }>,
  };
  server.use(
    http.post(REQUESTS_URL, async ({ request }) => {
      const body = (await request.json()) as {
        requestedDate: string;
        resourceType?: string;
        resourceId?: number;
        waitlist?: boolean;
      };
      captured.bodies.push(body);
      return HttpResponse.json(
        {
          ...requestPending1,
          id: 999,
          requestedDate: body.requestedDate,
          waitlisted: body.waitlist === true,
        },
        { status: 201 },
      );
    }),
  );
  return captured;
}

// Selecciona una fecha en el CALENDARIO del modal (rediseño: ya no hay <input date>).
// Por defecto HOY (celda con clase .is-today). Para fechas futuras navega meses
// hacia delante y clica el día correspondiente en el mes destino.
async function fillDate(value: string = todayIso()): Promise<void> {
  const user = userEvent.setup();
  if (value === todayIso()) {
    fireEvent.click(document.querySelector('.rc-day.is-today') as HTMLElement);
    return;
  }
  const target = new Date(`${value}T00:00:00`);
  const now = new Date(`${todayIso()}T00:00:00`);
  const months =
    (target.getFullYear() - now.getFullYear()) * 12 + (target.getMonth() - now.getMonth());
  const next = screen.getByRole('button', { name: /mes siguiente|next month/i });
  for (let i = 0; i < months; i += 1) {
    await user.click(next);
  }
  const day = String(target.getDate());
  const cell = [...document.querySelectorAll('.rc-day')].find(
    (el) =>
      el.textContent === day &&
      !el.classList.contains('is-out') &&
      !(el as HTMLButtonElement).disabled,
  ) as HTMLElement;
  fireEvent.click(cell);
}

async function submit(): Promise<void> {
  const user = userEvent.setup();
  const dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('button', { name: /enviar solicitud|submit request/i }));
}

// Ningún recurso viene marcado por defecto: el usuario elige qué reservar. Estos
// helpers marcan cada recurso desde su tarjeta.
async function selectParking(): Promise<void> {
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: /plaza de parking|parking space/i }));
}

async function selectDesk(): Promise<void> {
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: /puesto de trabajo|work desk/i }));
}

describe('CreateRequestModal (unified request)', () => {
  // El marcador del calendario (GET /requests/mine) determina qué días ya tienes
  // reservados (se omiten al enviar). Por defecto lo dejamos vacío para que solo
  // los tests de duplicados lo controlen; el fixture global incluye una reserva HOY.
  beforeEach(() => {
    server.use(http.get(`${MSW_BASE}/requests/mine`, () => HttpResponse.json(pageOfRequests([]))));
  });

  it('should_post_only_parking_request_when_only_parking_selected', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await selectParking();
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(1);
    expect(captured.bodies[0].resourceType).toBe('PARKING');
  });

  it('should_allow_submitting_a_far_future_date_without_upper_bound', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    const farFuture = addDaysIso(todayIso(), 120);
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate(farFuture);
    await selectParking();
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(1);
    expect(captured.bodies[0].requestedDate).toBe(farFuture);
  });

  it('should_post_desk_request_when_only_desk_selected', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await selectDesk();
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(1);
    expect(captured.bodies[0].resourceType).toBe('DESK');
  });

  it('should_post_both_requests_when_parking_and_desk_selected', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await selectParking();
    await selectDesk();
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(2);
    const types = captured.bodies.map((b) => b.resourceType).sort();
    expect(types).toEqual(['DESK', 'PARKING']);
  });

  it('should_disable_submit_when_no_resource_selected', async () => {
    const captured = captureRequestBodies();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={vi.fn()} />);

    await fillDate();
    // Ningún recurso viene marcado por defecto → no hay nada que reservar.

    // El envío se BLOQUEA deshabilitando el botón (rediseño), sin POST.
    const dialog = within(screen.getByRole('dialog'));
    expect(dialog.getByRole('button', { name: /enviar solicitud|submit request/i })).toBeDisabled();
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
    await selectParking();
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
    await selectParking();
    await submit();

    await waitFor(() =>
      expect(toasts.messages).toContain('requests.errors.noAvailability'),
    );
    expect(onCreated).not.toHaveBeenCalled();
  });

  it('should_not_offer_desk_picker_in_quick_reserve', async () => {
    const user = userEvent.setup();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={vi.fn()} />);

    await fillDate();
    await user.click(screen.getByRole('button', { name: /puesto de trabajo|work desk/i }));

    // Reserva rápida: el puesto se auto-asigna por categoría (capability
    // desk-auto-assignment), no se elige en el plano.
    expect(
      screen.queryByRole('button', { name: /seleccionar puesto|select desk/i }),
    ).not.toBeInTheDocument();
  });

  it('should_post_desk_request_without_resourceId_when_desk_selected', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await selectDesk();
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(1);
    expect(captured.bodies[0].resourceType).toBe('DESK');
    // Sin resourceId: el backend auto-asigna el puesto por categoría.
    expect(captured.bodies[0].resourceId).toBeUndefined();
  });

  // Marca HOY como plaza ya reservada (reserva viva) en el calendario del modal.
  function reservedParkingToday(): void {
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, () =>
        HttpResponse.json(
          pageOfRequests([{ ...requestPending1, resourceType: 'PARKING', requestedDate: todayIso() }]),
        ),
      ),
    );
  }

  it('should_lock_resource_card_when_single_day_already_reserved', async () => {
    reservedParkingToday();
    const captured = captureRequestBodies();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={vi.fn()} />);

    await fillDate();

    // La tarjeta de plaza se bloquea (ya la tienes ese día) y no es seleccionable.
    // Espera a que resuelva el marcador (GET /requests/mine) que marca el día.
    await screen.findByText(/ya tienes plaza este día|you already have a space this day/i);
    const parkingCard = screen.getByRole('button', {
      name: /plaza de parking|parking space/i,
    });
    expect(parkingCard).toBeDisabled();
    // No hay nada nuevo que crear → botón enviar deshabilitado, sin POST.
    const dialog = within(screen.getByRole('dialog'));
    expect(dialog.getByRole('button', { name: /enviar solicitud|submit request/i })).toBeDisabled();
    expect(captured.bodies).toHaveLength(0);
  });

  it('should_skip_reserved_day_and_only_post_the_free_one_on_multi_day', async () => {
    reservedParkingToday();
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    const free = addDaysIso(todayIso(), 3);
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate(); // HOY: ya reservada → se omite
    await fillDate(free); // día libre → se envía
    await selectParking();
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    // Solo se envía el día libre (el ya reservado se omite, sin 409 de ruido).
    expect(captured.bodies).toHaveLength(1);
    expect(captured.bodies[0].requestedDate).toBe(free);
  });

  describe('waitlist (capability request-waitlist)', () => {
    it('should_offer_join_waitlist_when_availability_is_zero_without_blocking_submit', async () => {
      zeroParkingAvailability();
      renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={vi.fn()} />);

      await fillDate();
      await selectParking();

      expect(
        await screen.findByText(/sin disponibilidad|no availability/i),
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: /apuntarme a la lista de espera|join the waitlist/i }),
      ).toBeInTheDocument();
      // El aviso no bloquea el envío: el botón de enviar solicitud sigue habilitado.
      const dialog = within(screen.getByRole('dialog'));
      expect(dialog.getByRole('button', { name: /enviar solicitud|submit request/i })).toBeEnabled();
    });

    it('should_send_waitlist_true_when_employee_joins_before_submitting', async () => {
      zeroParkingAvailability();
      const captured = captureRequestBodies();
      const onCreated = vi.fn();
      const user = userEvent.setup();
      renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

      await fillDate();
      await selectParking();
      await user.click(
        await screen.findByRole('button', { name: /apuntarme a la lista de espera|join the waitlist/i }),
      );
      // Tras apuntarse, el banner confirma sin mostrar posición numérica.
      expect(
        await screen.findByText(/apuntado: te avisaremos|joined: we will notify you/i),
      ).toBeInTheDocument();
      expect(screen.queryByText(/posición|position/i)).not.toBeInTheDocument();

      await submit();

      await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
      expect(captured.bodies).toHaveLength(1);
      expect(captured.bodies[0].waitlist).toBe(true);
    });

    it('should_offer_waitlist_retry_after_409_no_availability_in_automatic_mode', async () => {
      useAutomaticApprovalMode();
      server.use(waitlistConflictThenSuccessHandler());
      const onCreated = vi.fn();
      const user = userEvent.setup();
      renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

      await fillDate();
      await selectParking();
      await submit();

      const retryButton = await screen.findByRole('button', {
        name: /apuntarme a la lista de espera|join the waitlist/i,
      });
      expect(onCreated).not.toHaveBeenCalled();

      await user.click(retryButton);

      await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    });
  });
});
