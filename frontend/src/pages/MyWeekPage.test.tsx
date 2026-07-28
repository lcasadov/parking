import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { MyWeekPage } from './MyWeekPage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';
import { addDaysIso, isoWeekday } from '../utils/calendar';
import { todayIso } from '../utils/requests';
import type { MyWeekResponse } from '../types/calendar';
import type { FixedAssignment } from '../types/fixedAssignment';

const ME_URL = `${MSW_BASE}/auth/me`;
const MY_WEEK_URL = `${MSW_BASE}/calendar/my-week`;
const FIXED_URL = `${MSW_BASE}/fixed-assignments/employee/:employeeId`;

// Fechas relativas a hoy (no dependen del reloj de la suite).
const PARKING_FIXED_DATE = addDaysIso(todayIso(), 2);
const DESK_APPROVED_DATE = addDaysIso(todayIso(), 3);
const PENDING_DATE = addDaysIso(todayIso(), 4);
const DESK_FIXED_DATE = addDaysIso(todayIso(), 5);

// Semana con AMBOS recursos por día en estados distintos (design §D4).
const multiResourceWeek: MyWeekResponse = {
  weekStart: todayIso(),
  days: [
    // Plaza fija futura (P-12) + puesto libre → la plaza ofrece "Liberar",
    // el puesto se muestra como "Libre" (no "sin plaza").
    {
      date: PARKING_FIXED_DATE,
      state: 'ASSIGNED',
      parkingSpaceLabel: 'P-12',
      requestStatus: null,
      requestId: null,
      deskState: 'FREE',
      deskLabel: null,
      deskRequestStatus: null,
      deskRequestId: null,
    },
    // Plaza libre + puesto ocupado por solicitud APPROVED (D-03, requestId 55) →
    // el puesto ofrece "Cancelar"; la plaza NO rotula "sin plaza".
    {
      date: DESK_APPROVED_DATE,
      state: 'FREE',
      parkingSpaceLabel: null,
      requestStatus: null,
      requestId: null,
      deskState: 'ASSIGNED',
      deskLabel: 'D-03',
      deskRequestStatus: 'APPROVED',
      deskRequestId: 55,
    },
    // Plaza con solicitud propia PENDING (requestId 66) → "Cancelar".
    {
      date: PENDING_DATE,
      state: 'REQUEST_PENDING',
      parkingSpaceLabel: null,
      requestStatus: 'PENDING',
      requestId: 66,
      deskState: 'FREE',
      deskLabel: null,
      deskRequestStatus: null,
      deskRequestId: null,
    },
  ],
};

// HOY con plaza y puesto aprobados (héroe shield con acciones "Liberar"); los días
// siguientes pueblan la lista semanal (nodos HOY/MAÑANA de la línea de tránsito).
const heroTodayWeek: MyWeekResponse = {
  weekStart: todayIso(),
  days: [
    {
      date: todayIso(),
      state: 'ASSIGNED',
      parkingSpaceLabel: 'P-99',
      requestStatus: 'APPROVED',
      requestId: 77,
      deskState: 'ASSIGNED',
      deskLabel: 'D-05',
      deskRequestStatus: 'APPROVED',
      deskRequestId: 88,
    },
    {
      date: addDaysIso(todayIso(), 1),
      state: 'FREE',
      parkingSpaceLabel: null,
      requestStatus: null,
      requestId: null,
      deskState: 'FREE',
      deskLabel: null,
      deskRequestStatus: null,
      deskRequestId: null,
    },
    {
      date: addDaysIso(todayIso(), 2),
      state: 'FREE',
      parkingSpaceLabel: null,
      requestStatus: null,
      requestId: null,
      deskState: 'FREE',
      deskLabel: null,
      deskRequestStatus: null,
      deskRequestId: null,
    },
  ],
};

// Puesto fijo de un empleado (resourceType DESK) para el día indicado.
function deskFixedAssignment(date: string, deskId: number): FixedAssignment {
  return {
    id: 200,
    parkingSpaceId: deskId,
    employeeId: 10,
    dayOfWeek: isoWeekday(date),
    resourceType: 'DESK',
    active: true,
    createdById: 1,
    createdAt: '2026-02-01T09:00:00Z',
    revokedById: null,
    revokedAt: null,
  };
}

// Rediseño Wayfinding: el slot de un recurso ES el botón accionable; su nombre
// accesible es "Acción · Tipo · Etiqueta" (p. ej. "Liberar · Plaza de parking ·
// Plaza P-12"). Clicarlo abre directamente la reserva/liberación (ya no hay un botón
// "Liberar" anidado dentro de una fila). findByRole espera al montaje asíncrono.
function slotButton(name: RegExp): Promise<HTMLElement> {
  return screen.findByRole('button', { name });
}

function asEmployee(): void {
  server.use(
    http.get(ME_URL, () => HttpResponse.json({ ...adminUser, employeeId: 10, role: 'EMPLOYEE' })),
    // Fines de semana reservables en los tests → no se filtran días de finde y las
    // fechas relativas de los fixtures se muestran sea cual sea el día de la semana.
    http.get(`${MSW_BASE}/settings/weekend-reservable`, () =>
      HttpResponse.json({ weekendReservable: true }),
    ),
  );
}

describe('MyWeekPage (EMPLOYEE) — multi-recurso', () => {
  it('should_showParkingAndDeskStatesIndependently_perDay', async () => {
    asEmployee();
    server.use(http.get(MY_WEEK_URL, () => HttpResponse.json(multiResourceWeek)));
    renderWithProviders(<MyWeekPage />);

    // La plaza fija (P-12) y el puesto por solicitud (D-03) se muestran en paralelo.
    // El nombre accesible del slot lleva la etiqueta completa ("… · Plaza P-12").
    expect(await screen.findByRole('button', { name: /plaza P-12|space P-12/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /puesto D-03|desk D-03/i })).toBeInTheDocument();
    // Ambos tipos de recurso aparecen rotulados con su nombre completo en cada día.
    expect(screen.getAllByRole('button', { name: /plaza de parking|parking space/i }).length)
      .toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: /puesto de trabajo|work desk/i }).length)
      .toBeGreaterThan(0);
  });

  it('should_label_free_resources_with_resource_specific_text', async () => {
    asEmployee();
    server.use(http.get(MY_WEEK_URL, () => HttpResponse.json(multiResourceWeek)));
    renderWithProviders(<MyWeekPage />);

    // Rediseño: un recurso libre se rotula con copy específico por tipo
    // ("Sin plaza este día" / "Sin puesto este día"), no un genérico "Libre".
    await screen.findByRole('button', { name: /puesto D-03|desk D-03/i });
    expect(screen.getAllByRole('button', { name: /sin plaza este día|no space this day/i }).length)
      .toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: /sin puesto este día|no desk this day/i }).length)
      .toBeGreaterThan(0);
  });

  it('should_notRenderThirdPartyNames_when_showingMyWeek', async () => {
    asEmployee();
    server.use(
      http.get(MY_WEEK_URL, () =>
        HttpResponse.json({
          weekStart: todayIso(),
          days: [
            {
              date: PARKING_FIXED_DATE,
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

    // El marcador mono (P·12) siempre se renderiza, sea el slot accionable o estático.
    await screen.findByText('P·12');
    expect(screen.queryByText('Alice Andersson')).not.toBeInTheDocument();
  });

  it('should_showError_when_myWeekRequestFails', async () => {
    asEmployee();
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
    asEmployee();
    server.use(http.get(MY_WEEK_URL, () => HttpResponse.json(multiResourceWeek)));
    const user = userEvent.setup();
    renderWithProviders(<MyWeekPage />);
    await screen.findByText('P·12');

    await user.click(screen.getByRole('button', { name: /nueva reserva|new booking/i }));

    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /enviar solicitud|submit request/i }),
    ).toBeInTheDocument();
  });

  it('should_cancelOwnRequest_when_releasingApprovedDesk', async () => {
    asEmployee();
    let cancelledId: string | null = null;
    server.use(
      http.get(MY_WEEK_URL, () => HttpResponse.json(multiResourceWeek)),
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

    // El puesto APPROVED ya lo tienes: la fila ofrece "Liberar" (por debajo cancela
    // la solicitud propia para liberar el recurso). "Cancelar" queda solo para PENDING.
    const deskBtn = await slotButton(/puesto D-03|desk D-03/i);
    await user.click(deskBtn);

    const dialog = within(await screen.findByRole('dialog'));
    // Reserva aprobada: el modal está en modo "liberar" (confirma con "Liberar",
    // no "Cancelar solicitud"), aunque por debajo cancela la solicitud 55.
    await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));

    await waitFor(() => expect(cancelledId).toBe('55'));
  });

  it('should_createParkingRelease_when_releasingFixedParkingDay', async () => {
    // El empleado 10 tiene plaza fija PARKING (space 1) en los handlers por defecto.
    asEmployee();
    let releaseBody: Record<string, unknown> | null = null;
    server.use(
      http.get(MY_WEEK_URL, () => HttpResponse.json(multiResourceWeek)),
      http.post(`${MSW_BASE}/releases`, async ({ request }) => {
        releaseBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 999, releaseDate: releaseBody.releaseDate }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<MyWeekPage />);

    const parkingBtn = await slotButton(/plaza P-12|space P-12/i);
    await user.click(parkingBtn);

    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));

    // La plaza omite resourceType (default PARKING en backend).
    await waitFor(() =>
      expect(releaseBody).toEqual({ releaseDate: PARKING_FIXED_DATE, parkingSpaceId: 1 }),
    );
  });

  it('should_createDeskRelease_withResourceTypeDesk_when_releasingFixedDeskDay', async () => {
    asEmployee();
    let releaseBody: Record<string, unknown> | null = null;
    // La semana lleva un puesto fijo (D-05) y el empleado tiene ese puesto como fijo (id 20).
    const deskWeek: MyWeekResponse = {
      weekStart: todayIso(),
      days: [
        {
          date: DESK_FIXED_DATE,
          state: 'FREE',
          parkingSpaceLabel: null,
          requestStatus: null,
          requestId: null,
          deskState: 'ASSIGNED',
          deskLabel: 'D-05',
          deskRequestStatus: null,
          deskRequestId: null,
        },
      ],
    };
    server.use(
      http.get(MY_WEEK_URL, () => HttpResponse.json(deskWeek)),
      http.get(FIXED_URL, () => HttpResponse.json([deskFixedAssignment(DESK_FIXED_DATE, 20)])),
      http.post(`${MSW_BASE}/releases`, async ({ request }) => {
        releaseBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 999, releaseDate: releaseBody.releaseDate }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<MyWeekPage />);

    const deskBtn = await slotButton(/puesto D-05|desk D-05/i);
    await user.click(deskBtn);

    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));

    // El puesto DEBE enviar resourceType DESK y su resource id fijo (bug del "sin plaza"
    // / liberación de puesto como PARKING): 20, no una plaza.
    await waitFor(() =>
      expect(releaseBody).toEqual({
        releaseDate: DESK_FIXED_DATE,
        parkingSpaceId: 20,
        resourceType: 'DESK',
      }),
    );
  });

  it('should_render_today_hero_shield_with_actions_and_navigate_weeks', async () => {
    asEmployee();
    server.use(http.get(MY_WEEK_URL, () => HttpResponse.json(heroTodayWeek)));
    const user = userEvent.setup();
    renderWithProviders(<MyWeekPage />);

    // El héroe HOY muestra el shield con el marcador mono de la plaza (P·99).
    expect((await screen.findAllByText('P·99')).length).toBeGreaterThan(0);
    // Y ofrece la acción de liberar la plaza aprobada de hoy (héroe + slot semanal).
    expect(
      (await screen.findAllByRole('button', { name: /liberar.*plaza|release.*parking/i })).length,
    ).toBeGreaterThan(0);

    // Navegar a la semana siguiente ejercita el control de semanas sin romper (el
    // héroe de hoy permanece).
    await user.click(screen.getByRole('button', { name: /semana siguiente|next week/i }));
    expect((await screen.findAllByText('P·99')).length).toBeGreaterThan(0);
  });

  it('should_release_todays_approved_parking_from_the_hero', async () => {
    asEmployee();
    let cancelledId: string | null = null;
    server.use(
      http.get(MY_WEEK_URL, () => HttpResponse.json(heroTodayWeek)),
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

    // La acción "Liberar plaza" del héroe abre el modal de liberación de la reserva
    // aprobada de hoy (requestId 77) y, al confirmar, la cancela por debajo.
    const releaseButtons = await screen.findAllByRole('button', {
      name: /liberar.*plaza|release.*parking/i,
    });
    await user.click(releaseButtons[0]);
    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));

    await waitFor(() => expect(cancelledId).toBe('77'));
  });

  it('should_showEmptyState_when_noDaysReturned', async () => {
    asEmployee();
    server.use(
      http.get(MY_WEEK_URL, () => HttpResponse.json({ weekStart: todayIso(), days: [] })),
    );
    renderWithProviders(<MyWeekPage />);

    expect(
      await screen.findByText(/no quedan más días esta semana|no more days this week/i),
    ).toBeInTheDocument();
  });
});
