import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AdminCalendarPage } from './AdminCalendarPage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';
import { addDaysIso, isoWeekday, mondayOfWeek } from '../utils/calendar';
import { todayIso } from '../utils/requests';
import type { AdminWeeklyCalendarResponse } from '../types/calendar';

const CALENDAR_URL = `${MSW_BASE}/calendar/admin`;
const FIXED_URL = `${MSW_BASE}/fixed-assignments/employee/:employeeId`;

// Fechas futuras (relativas a hoy) para que las celdas sean accionables: no se
// asignan ni liberan fechas pasadas.
const FREE_DATE = addDaysIso(todayIso(), 3);
const ASSIGNED_DATE = addDaysIso(todayIso(), 4);
const APPROVED_DATE = addDaysIso(todayIso(), 5);

const RESOURCE_ID = 5;

// Rejilla con una fila (P-05) y tres celdas: libre (asignar), asignada fija
// (liberación administrativa) y solicitud aprobada (admin-cancel).
const actionableCalendar: AdminWeeklyCalendarResponse = {
  weekStart: mondayOfWeek(),
  days: [FREE_DATE, ASSIGNED_DATE, APPROVED_DATE],
  rows: [
    {
      parkingSpaceId: RESOURCE_ID,
      label: 'P-05',
      cells: [
        { date: FREE_DATE, state: 'FREE', employeeId: null, employeeName: null, requestId: null },
        {
          date: ASSIGNED_DATE,
          state: 'ASSIGNED',
          employeeId: 10,
          employeeName: 'Alice Andersson',
          requestId: null,
        },
        {
          date: APPROVED_DATE,
          state: 'REQUEST_APPROVED',
          employeeId: 12,
          employeeName: 'Bob Beck',
          requestId: 701,
        },
      ],
    },
  ],
};

function useActionableCalendar(): void {
  server.use(http.get(CALENDAR_URL, () => HttpResponse.json(actionableCalendar)));
}

// Devuelve el botón de la celda cuyo texto de estado coincide con `state`.
function cellButton(grid: HTMLElement, state: RegExp): HTMLElement {
  const label = within(grid).getByText(state);
  const button = label.closest('button');
  if (!button) {
    throw new Error(`No actionable button for ${state}`);
  }
  return button;
}

describe('Ocupación accionable (ADMIN)', () => {
  it('should_assignFixed_preloadingAndResendingAllDays', async () => {
    // El empleado 10 tiene plaza fija PARKING los días 1,2,3 (handler por defecto).
    let putBody: { parkingSpaceId: number; daysOfWeek: number[]; resourceType?: string } | null =
      null;
    useActionableCalendar();
    server.use(
      http.put(FIXED_URL, async ({ request }) => {
        putBody = (await request.json()) as typeof putBody;
        return HttpResponse.json([]);
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminCalendarPage />);

    const grid = await screen.findByRole('table');
    await user.click(cellButton(grid, /^libre$|^free$/i));

    const dialog = within(await screen.findByRole('dialog'));
    await user.selectOptions(dialog.getByLabelText(/empleado|employee/i), '10');
    await user.click(dialog.getByRole('button', { name: /fija|fixed/i }));
    await user.click(dialog.getByRole('button', { name: /^asignar$|^assign$/i }));

    await waitFor(() => expect(putBody).not.toBeNull());
    // CRÍTICO (design §Risk D): el PUT reenvía el CONJUNTO COMPLETO — los días
    // precargados (1,2,3) MÁS el día de la celda —, nunca solo el nuevo día.
    const body = putBody as unknown as { parkingSpaceId: number; daysOfWeek: number[] };
    expect(body.parkingSpaceId).toBe(RESOURCE_ID);
    expect(body.daysOfWeek).toEqual(expect.arrayContaining([1, 2, 3]));
    expect(body.daysOfWeek).toContain(isoWeekday(FREE_DATE));
  });

  it('should_assignPunctual_when_punctualModeChosen', async () => {
    let assignBody: Record<string, unknown> | null = null;
    useActionableCalendar();
    server.use(
      http.post(`${MSW_BASE}/requests/admin`, async ({ request }) => {
        assignBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 990, status: 'APPROVED' }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminCalendarPage />);

    const grid = await screen.findByRole('table');
    await user.click(cellButton(grid, /^libre$|^free$/i));

    const dialog = within(await screen.findByRole('dialog'));
    await user.selectOptions(dialog.getByLabelText(/empleado|employee/i), '10');
    // El modo puntual es el predeterminado.
    await user.click(dialog.getByRole('button', { name: /^asignar$|^assign$/i }));

    await waitFor(() =>
      expect(assignBody).toEqual({
        employeeId: 10,
        requestedDate: FREE_DATE,
        resourceType: 'PARKING',
        resourceId: RESOURCE_ID,
      }),
    );
  });

  it('should_releaseFixed_fromAssignedCell', async () => {
    let releaseBody: Record<string, unknown> | null = null;
    useActionableCalendar();
    server.use(
      http.post(`${MSW_BASE}/releases/administrative`, async ({ request }) => {
        releaseBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 998 }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminCalendarPage />);

    const grid = await screen.findByRole('table');
    await user.click(cellButton(grid, /^asignada$|^assigned$/i));

    const dialog = within(await screen.findByRole('dialog'));
    await user.type(dialog.getByLabelText(/motivo|reason/i), 'No acude esta semana');
    await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));

    await waitFor(() =>
      expect(releaseBody).toEqual({
        employeeId: 10,
        parkingSpaceId: RESOURCE_ID,
        releaseDate: ASSIGNED_DATE,
        reason: 'No acude esta semana',
        resourceType: 'PARKING',
      }),
    );
  });

  it('should_adminCancel_fromApprovedCell', async () => {
    let cancelledId: string | null = null;
    useActionableCalendar();
    server.use(
      http.post(`${MSW_BASE}/requests/:id/admin-cancel`, ({ params }) => {
        cancelledId = String(params.id);
        return HttpResponse.json({ id: Number(params.id), status: 'CANCELLED' });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminCalendarPage />);

    const grid = await screen.findByRole('table');
    await user.click(cellButton(grid, /solicitud aprobada|request approved/i));

    const dialog = within(await screen.findByRole('dialog'));
    await user.type(dialog.getByLabelText(/motivo|reason/i), 'Reasignación');
    await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));

    await waitFor(() => expect(cancelledId).toBe('701'));
  });

  it('should_showConflictError_when_assignReturns409', async () => {
    useActionableCalendar();
    server.use(
      http.post(`${MSW_BASE}/requests/admin`, () =>
        HttpResponse.json(
          { error: 'CONFLICT', message: 'ocupado', timestamp: '2026-07-23T09:00:00Z' },
          { status: 409 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(
      <>
        <AdminCalendarPage />
        <Toast />
      </>,
    );

    const grid = await screen.findByRole('table');
    await user.click(cellButton(grid, /^libre$|^free$/i));

    const dialog = within(await screen.findByRole('dialog'));
    await user.selectOptions(dialog.getByLabelText(/empleado|employee/i), '10');
    await user.click(dialog.getByRole('button', { name: /^asignar$|^assign$/i }));

    // El conflicto se muestra en contexto (toast traducido) sin romper la rejilla.
    expect(
      await screen.findByText(/ya está ocupado esa fecha|already occupied on that date/i),
    ).toBeInTheDocument();
    expect(screen.getByRole('table')).toBeInTheDocument();
  });
});
