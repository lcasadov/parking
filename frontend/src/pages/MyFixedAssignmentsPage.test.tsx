import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { MyFixedAssignmentsPage } from './MyFixedAssignmentsPage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';
import { todayIso } from '../utils/releases';
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
    http.get(`${MSW_BASE}/fixed-assignments/employee/:id`, () =>
      HttpResponse.json([ownAssignment(2), ownAssignment(4)]),
    ),
  );
}

async function openReleaseForm(): Promise<void> {
  const user = userEvent.setup();
  const buttons = await screen.findAllByRole('button', { name: /liberar|release/i });
  await user.click(buttons[0]);
  await screen.findByRole('dialog');
}

function setReleaseDate(): void {
  const dialog = within(screen.getByRole('dialog'));
  fireEvent.change(dialog.getByLabelText(/fecha a liberar|date to release/i), {
    target: { value: todayIso() },
  });
}

async function submitRelease(): Promise<void> {
  const user = userEvent.setup();
  const dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));
}

describe('MyFixedAssignmentsPage (EMPLOYEE)', () => {
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

    // Sin controles de edicion/revocacion/alta (esos son solo del panel admin).
    expect(screen.queryByRole('button', { name: /editar|edit/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /revocar|revoke/i })).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /nueva asignación|new assignment/i }),
    ).not.toBeInTheDocument();
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
  });

  it('should_create_voluntary_release_when_owner_and_future_date', async () => {
    let sentType: string | null = null;
    useOwnAssignments();
    server.use(
      http.post(`${MSW_BASE}/releases`, async ({ request }) => {
        const body = (await request.json()) as { releaseDate: string };
        sentType = 'VOLUNTARY';
        return HttpResponse.json(
          { id: 999, releaseDate: body.releaseDate, type: 'VOLUNTARY' },
          { status: 201 },
        );
      }),
    );
    renderWithProviders(
      <>
        <MyFixedAssignmentsPage />
        <Toast />
      </>,
    );

    await openReleaseForm();
    setReleaseDate();
    await submitRelease();

    await waitFor(() => expect(sentType).toBe('VOLUNTARY'));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await screen.findByText(/plaza liberada|space released/i)).toBeInTheDocument();
  });

  it('should_reject_with_400_when_release_date_in_past', async () => {
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

    await openReleaseForm();
    setReleaseDate();
    await submitRelease();

    expect(
      await screen.findByText(/la fecha debe ser hoy o futura|date must be today or later/i),
    ).toBeInTheDocument();
  });

  it('should_reject_with_409_when_no_fixed_assignment_for_that_day', async () => {
    useOwnAssignments();
    server.use(
      http.post(`${MSW_BASE}/releases`, () =>
        HttpResponse.json(
          { error: 'RELEASE_NO_ASSIGNMENT', message: 'none', timestamp: '2026-03-01T09:00:00Z' },
          { status: 409 },
        ),
      ),
    );
    renderWithProviders(
      <>
        <MyFixedAssignmentsPage />
        <Toast />
      </>,
    );

    await openReleaseForm();
    setReleaseDate();
    await submitRelease();

    expect(
      await screen.findByText(/no tiene asignación fija ese día|has no fixed assignment that day/i),
    ).toBeInTheDocument();
  });
});
