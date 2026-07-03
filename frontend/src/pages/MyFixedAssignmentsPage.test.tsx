import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { MyFixedAssignmentsPage } from './MyFixedAssignmentsPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { employeeUser } from '../mocks/fixtures';
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

    // Solo lectura: sin controles de edicion/revocacion/alta.
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
});
