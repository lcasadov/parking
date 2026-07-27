import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { MyFixedAssignmentsPage } from './MyFixedAssignmentsPage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { employeeUser } from '../mocks/fixtures';
import { pageOfReleases, releaseFuture, releaseToday } from '../mocks/releaseFixtures';
import { renderWithProviders } from '../test/renderWithProviders';
import type { FixedAssignment } from '../types/fixedAssignment';

function ownAssignment(day: number): FixedAssignment {
  return {
    id: 200 + day,
    parkingSpaceId: 3,
    employeeId: employeeUser.employeeId,
    dayOfWeek: day,
    active: true,
    createdById: 1,
    createdAt: '2026-02-01T09:00:00Z',
    revokedById: null,
    revokedAt: null,
  };
}

function useOwnAssignments(): void {
  server.use(
    http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)),
    // Fijo TODOS los días (L-D): así cualquier día elegido en el calendario del
    // modal de ausencia aporta un recurso a liberar, sea cual sea la fecha real.
    http.get(`${MSW_BASE}/fixed-assignments/employee/:id`, () =>
      HttpResponse.json([1, 2, 3, 4, 5, 6, 7].map(ownAssignment)),
    ),
  );
}

// Abre el modal de ausencia y selecciona varios días del mes en el calendario
// (días del mes actual, no deshabilitados → incluyen laborables con fijo L-V),
// dejando el formulario listo para confirmar.
async function openAbsenceWithWeekRange(): Promise<ReturnType<typeof within>> {
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /vacaciones|holiday/i }));
  const dialog = within(await screen.findByRole('dialog'));
  const cells = [
    ...document.querySelectorAll('.rc-day:not(.is-out):not(.is-disabled)'),
  ] as HTMLElement[];
  // Los primeros días seleccionables desde hoy (robusto a fin de mes); con fijo
  // en todos los días de la semana, cualquiera aporta un recurso a liberar.
  cells.slice(0, 5).forEach((cell) => fireEvent.click(cell));
  return dialog;
}

describe('MyFixedAssignmentsPage (EMPLOYEE)', () => {
  // Por defecto sin liberaciones propias: el panel "Días que has liberado" no
  // aparece y no interfiere con los tests que no van de liberaciones. Los tests
  // del panel fijan sus propias liberaciones.
  beforeEach(() => {
    server.use(
      http.get(`${MSW_BASE}/releases/mine`, () => HttpResponse.json(pageOfReleases([]))),
    );
  });

  it('should_show_own_assignments_read_only_when_employee_opens_view', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)),
      http.get(`${MSW_BASE}/fixed-assignments/employee/:id`, ({ params }) => {
        expect(Number(params.id)).toBe(employeeUser.employeeId);
        return HttpResponse.json([ownAssignment(2), ownAssignment(4)]);
      }),
    );

    renderWithProviders(<MyFixedAssignmentsPage />);

    expect(
      await screen.findByRole('heading', { name: /mis asignaciones fijas|my fixed assignments/i }),
    ).toBeInTheDocument();
    expect(await screen.findByText(/martes|tuesday/i)).toBeInTheDocument();
    expect(screen.getByText(/jueves|thursday/i)).toBeInTheDocument();

    // Read-only: sin editar/revocar/alta NI "Liberar" por fila (la liberación puntual
    // vive en Mi Semana; aquí solo la acción de ausencia).
    expect(screen.queryByRole('button', { name: /editar|edit/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /revocar|revoke/i })).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /nueva asignación|new assignment/i }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /^liberar$|^release$/i }),
    ).not.toBeInTheDocument();
    // El CTA de ausencia sí está presente.
    expect(screen.getByRole('button', { name: /vacaciones|holiday/i })).toBeInTheDocument();
  });

  it('should_show_real_space_number_instead_of_generic_label_when_employee_has_parking_assignment', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)),
      http.get(`${MSW_BASE}/fixed-assignments/employee/:id`, () =>
        HttpResponse.json([ownAssignment(2)]),
      ),
      http.get(`${MSW_BASE}/parking-spaces/:id`, ({ params }) =>
        HttpResponse.json({
          id: Number(params.id),
          number: 1001,
          label: 'P-01',
          floor: 1,
          active: true,
          createdAt: '2026-01-10T09:00:00Z',
        }),
      ),
    );

    renderWithProviders(<MyFixedAssignmentsPage />);

    expect(await screen.findByText(/plaza 1001|space 1001/i)).toBeInTheDocument();
    expect(screen.queryByText(/^plaza fija$|^fixed space$/i)).not.toBeInTheDocument();
  });

  it('should_show_empty_message_when_employee_has_no_assignments', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)),
      http.get(`${MSW_BASE}/fixed-assignments/employee/:id`, () => HttpResponse.json([])),
    );

    renderWithProviders(<MyFixedAssignmentsPage />);

    expect(
      await screen.findByText(/no tienes asignaciones fijas|you have no fixed assignments/i),
    ).toBeInTheDocument();
    // Sin recursos fijos no se ofrece la acción de ausencia.
    expect(
      screen.queryByRole('button', { name: /vacaciones|holiday/i }),
    ).not.toBeInTheDocument();
  });

  it('should_release_fixed_resources_in_batch_via_absence_modal', async () => {
    let releaseCalls = 0;
    useOwnAssignments();
    server.use(
      http.post(`${MSW_BASE}/releases`, async ({ request }) => {
        const body = (await request.json()) as { releaseDate: string };
        releaseCalls += 1;
        return HttpResponse.json({ id: 900 + releaseCalls, releaseDate: body.releaseDate }, { status: 201 });
      }),
    );
    renderWithProviders(
      <>
        <MyFixedAssignmentsPage />
        <Toast />
      </>,
    );

    const dialog = await openAbsenceWithWeekRange();
    await userEvent.setup().click(dialog.getByRole('button', { name: /liberar|release/i }));

    await waitFor(() => expect(releaseCalls).toBeGreaterThan(0));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await screen.findByText(/recursos liberados|resources released/i)).toBeInTheDocument();
  });

  it('should_list_released_days_and_undo_one', async () => {
    useOwnAssignments();
    let deleted = 0;
    server.use(
      http.get(`${MSW_BASE}/releases/mine`, () => HttpResponse.json(pageOfReleases([releaseFuture]))),
      http.delete(`${MSW_BASE}/releases/:id`, () => {
        deleted += 1;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderWithProviders(
      <>
        <MyFixedAssignmentsPage />
        <Toast />
      </>,
    );
    const user = userEvent.setup();

    expect(
      await screen.findByText(/días que has liberado|days you have released/i),
    ).toBeInTheDocument();
    const undo = (await screen.findAllByRole('button', { name: /^deshacer$|^undo$/i }))[0];
    await user.click(undo);
    await waitFor(() => expect(deleted).toBe(1));
  });

  it('should_undo_all_releases_after_confirming', async () => {
    useOwnAssignments();
    let deleted = 0;
    server.use(
      http.get(`${MSW_BASE}/releases/mine`, () =>
        HttpResponse.json(pageOfReleases([releaseFuture, releaseToday])),
      ),
      http.delete(`${MSW_BASE}/releases/:id`, () => {
        deleted += 1;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderWithProviders(
      <>
        <MyFixedAssignmentsPage />
        <Toast />
      </>,
    );
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /deshacer todos|undo all/i }));
    // Pide confirmación antes de deshacerlas todas.
    const dialog = within(await screen.findByRole('alertdialog'));
    await user.click(dialog.getByRole('button', { name: /deshacer todos|undo all/i }));

    await waitFor(() => expect(deleted).toBe(2));
  });

  it('should_select_a_full_range_with_two_clicks_in_range_mode', async () => {
    useOwnAssignments();
    renderWithProviders(<MyFixedAssignmentsPage />);
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /vacaciones|holiday/i }));
    const dialog = within(await screen.findByRole('dialog'));
    // Cambia a modo "Rango".
    await user.click(dialog.getByRole('radio', { name: /rango|range/i }));

    const selectable = () =>
      [...document.querySelectorAll('.rc-day:not(.is-out):not(.is-disabled)')] as HTMLElement[];
    const cells = selectable();
    // Dos clics (inicio y fin) rellenan todo el intervalo, ambos inclusive.
    fireEvent.click(cells[0]);
    fireEvent.click(cells[4]);

    const selected = selectable().filter((c) => c.getAttribute('aria-pressed') === 'true');
    expect(selected).toHaveLength(5);
  });

  it('should_show_error_toast_when_absence_release_fails', async () => {
    useOwnAssignments();
    server.use(
      http.post(`${MSW_BASE}/releases`, () =>
        HttpResponse.json(
          { error: 'RELEASE_PAST_DATE', message: 'past', timestamp: '2026-03-01T09:00:00Z' },
          { status: 400 },
        ),
      ),
    );
    renderWithProviders(
      <>
        <MyFixedAssignmentsPage />
        <Toast />
      </>,
    );

    const dialog = await openAbsenceWithWeekRange();
    await userEvent.setup().click(dialog.getByRole('button', { name: /liberar|release/i }));

    expect(
      await screen.findByText(/no se pudo liberar|could not release/i),
    ).toBeInTheDocument();
  });
});
