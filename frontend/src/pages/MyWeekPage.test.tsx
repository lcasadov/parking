import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { MyWeekPage } from './MyWeekPage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser } from '../mocks/fixtures';
import { defaultMyWeek } from '../mocks/calendarFixtures';
import { renderWithProviders } from '../test/renderWithProviders';
import { addDaysIso } from '../utils/calendar';
import { todayIso } from '../utils/requests';
import type { MyWeekResponse } from '../types/calendar';

const ME_URL = `${MSW_BASE}/auth/me`;
const MY_WEEK_URL = `${MSW_BASE}/calendar/my-week`;
const RELEASE_BTN = { name: /^liberar$|^release$/i } as const;

// Semana con recursos liberables futuros (fechas relativas a hoy para no depender del reloj).
const APPROVED_DATE = addDaysIso(todayIso(), 2);
const PENDING_DATE = addDaysIso(todayIso(), 3);
const FIXED_DATE = addDaysIso(todayIso(), 4);

const liberableWeek: MyWeekResponse = {
  weekStart: todayIso(),
  days: [
    // Día ocupado por solicitud APPROVED futura → liberar cancelándola.
    {
      date: APPROVED_DATE,
      state: 'ASSIGNED',
      parkingSpaceLabel: 'P-07',
      requestStatus: 'APPROVED',
      requestId: 55,
    },
    // Día con solicitud propia PENDING → liberar cancelándola.
    {
      date: PENDING_DATE,
      state: 'REQUEST_PENDING',
      parkingSpaceLabel: null,
      requestStatus: 'PENDING',
      requestId: 66,
    },
    // Día de asignación fija futura → liberar creando un Release.
    {
      date: FIXED_DATE,
      state: 'ASSIGNED',
      parkingSpaceLabel: 'P-12',
      requestStatus: null,
      requestId: null,
    },
    // Día libre → no liberable.
    {
      date: addDaysIso(todayIso(), 5),
      state: 'FREE',
      parkingSpaceLabel: null,
      requestStatus: null,
      requestId: null,
    },
  ],
};

// Selecciona la tarjeta de día que contiene `text` (filtra coincidencias de la leyenda,
// que no vive dentro de un button.week-card-body).
async function selectDayByText(user: ReturnType<typeof userEvent.setup>, text: RegExp): Promise<void> {
  // La tarjeta se vuelve seleccionable (button) cuando su recurso es liberable; para el
  // caso fijo, eso ocurre al resolverse la consulta de asignaciones fijas del empleado.
  const card = await waitFor(() => {
    const nodes = screen.getAllByText(text);
    const found = nodes.map((node) => node.closest('button.week-card-body')).find(Boolean);
    if (!found) {
      throw new Error(`No selectable day card for ${text}`);
    }
    return found;
  });
  await user.click(card);
}

describe('MyWeekPage (EMPLOYEE)', () => {
  it('should_renderOwnWeek_when_myWeekLoaded', async () => {
    server.use(http.get(MY_WEEK_URL, () => HttpResponse.json(defaultMyWeek)));
    renderWithProviders(<MyWeekPage />);

    // El estado aparece en la celda del día y (parcialmente) en la leyenda inferior.
    expect((await screen.findAllByText(/plaza asignada|space assigned/i)).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/plaza liberada|space released/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/solicitud pendiente|request pending/i).length).toBeGreaterThan(0);
    // Muestra la etiqueta de la plaza propia (asignada y liberada la misma semana).
    expect(screen.getAllByText(/plaza P-12|space P-12/i).length).toBeGreaterThan(0);
  });

  it('should_notRenderThirdPartyNames_when_showingMyWeek', async () => {
    // El contrato de my-week omite nombres; incluso si el backend enviara un
    // campo extra, la vista nunca lo renderiza.
    server.use(
      http.get(MY_WEEK_URL, () =>
        HttpResponse.json({
          weekStart: '2026-05-11',
          days: [
            {
              date: '2026-05-11',
              state: 'ASSIGNED',
              parkingSpaceLabel: 'P-12',
              requestStatus: null,
              requestId: null,
              employeeName: 'Alice Andersson',
            },
          ],
        }),
      ),
    );
    renderWithProviders(<MyWeekPage />);

    await screen.findAllByText(/plaza asignada|space assigned/i);
    expect(screen.queryByText('Alice Andersson')).not.toBeInTheDocument();
  });

  it('should_showError_when_myWeekRequestFails', async () => {
    server.use(
      http.get(MY_WEEK_URL, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: '2026-05-11T09:00:00Z' },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<MyWeekPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/no se pudo cargar tu semana|your week could not be loaded/i),
      ).toBeInTheDocument();
    });
  });

  it('should_openRequestModal_when_requestActionClicked', async () => {
    server.use(http.get(MY_WEEK_URL, () => HttpResponse.json(defaultMyWeek)));
    const user = userEvent.setup();
    renderWithProviders(<MyWeekPage />);
    await screen.findAllByText(/plaza asignada|space assigned/i);

    await user.click(screen.getByRole('button', { name: /^solicitar$|^request$/i }));

    // El modal de solicitud unificada aparece (diálogo + botón de envío).
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /enviar solicitud|submit request/i }),
    ).toBeInTheDocument();
  });

  it('should_disableRelease_untilLiberableDaySelected', async () => {
    server.use(http.get(MY_WEEK_URL, () => HttpResponse.json(liberableWeek)));
    const user = userEvent.setup();
    renderWithProviders(<MyWeekPage />);
    await screen.findByText(/plaza P-07|space P-07/i);

    // Sin selección, "Liberar" está deshabilitado (la acción es por-día).
    expect(screen.getByRole('button', RELEASE_BTN)).toBeDisabled();

    await selectDayByText(user, /plaza P-07|space P-07/i);
    await waitFor(() => expect(screen.getByRole('button', RELEASE_BTN)).toBeEnabled());
  });

  it('should_cancelOwnRequest_when_releasingApprovedDay', async () => {
    let cancelledId: string | null = null;
    server.use(
      http.get(MY_WEEK_URL, () => HttpResponse.json(liberableWeek)),
      http.post(`${MSW_BASE}/requests/:id/cancel`, ({ params }) => {
        cancelledId = String(params.id);
        return HttpResponse.json({ id: Number(params.id), status: 'CANCELLED' });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(
      <>
        <MyWeekPage />
        <Toast />
      </>,
    );

    await selectDayByText(user, /plaza P-07|space P-07/i);
    await user.click(screen.getByRole('button', RELEASE_BTN));

    // Se abre el modal de cancelar solicitud (no el de liberar plaza fija).
    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /cancelar solicitud|cancel request/i }));

    await waitFor(() => expect(cancelledId).toBe('55'));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('should_cancelOwnRequest_when_releasingPendingDay', async () => {
    // La ruta de cancelación es agnóstica al tipo de recurso (soporta plaza y puesto):
    // libera cualquier recurso ocupado por la solicitud propia del día.
    let cancelledId: string | null = null;
    server.use(
      http.get(MY_WEEK_URL, () => HttpResponse.json(liberableWeek)),
      http.post(`${MSW_BASE}/requests/:id/cancel`, ({ params }) => {
        cancelledId = String(params.id);
        return HttpResponse.json({ id: Number(params.id), status: 'CANCELLED' });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<MyWeekPage />);

    await selectDayByText(user, /solicitud pendiente|request pending/i);
    await user.click(screen.getByRole('button', RELEASE_BTN));
    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /cancelar solicitud|cancel request/i }));

    await waitFor(() => expect(cancelledId).toBe('66'));
  });

  it('should_createRelease_when_releasingFixedDay', async () => {
    // El empleado 10 (Alice) tiene plaza fija PARKING en los handlers por defecto.
    let releaseBody: Record<string, unknown> | null = null;
    server.use(
      http.get(ME_URL, () => HttpResponse.json({ ...adminUser, employeeId: 10, role: 'EMPLOYEE' })),
      http.get(MY_WEEK_URL, () => HttpResponse.json(liberableWeek)),
      http.post(`${MSW_BASE}/releases`, async ({ request }) => {
        releaseBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 999, releaseDate: releaseBody.releaseDate }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<MyWeekPage />);

    await selectDayByText(user, /plaza P-12|space P-12/i);
    await user.click(screen.getByRole('button', RELEASE_BTN));

    // Se abre el modal de liberar plaza fija con la fecha del día ya fijada (sin selector de fecha).
    const dialog = within(await screen.findByRole('dialog'));
    expect(dialog.queryByLabelText(/fecha a liberar|date to release/i)).not.toBeInTheDocument();
    await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));

    await waitFor(() =>
      expect(releaseBody).toEqual({ releaseDate: FIXED_DATE, parkingSpaceId: 1 }),
    );
  });

  it('should_showEmptyState_when_noDaysReturned', async () => {
    server.use(
      http.get(MY_WEEK_URL, () => HttpResponse.json({ weekStart: '2026-05-11', days: [] })),
    );
    renderWithProviders(<MyWeekPage />);

    expect(
      await screen.findByText(/no hay días que mostrar|no days to show/i),
    ).toBeInTheDocument();
  });
});
