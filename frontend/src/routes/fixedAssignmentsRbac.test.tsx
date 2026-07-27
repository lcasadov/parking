import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { ROUTES } from './paths';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

// La gestion de asignaciones fijas del ADMIN se movio al modal de empleado; solo
// queda la vista propia del empleado (MyFixedAssignmentsPage).
describe('Fixed assignments RBAC', () => {
  it('should_render_own_view_when_employee_opens_my_fixed_assignments_route', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)),
      http.get(`${MSW_BASE}/fixed-assignments/employee/:id`, () => HttpResponse.json([])),
    );
    renderWithProviders(<AppRoutes />, { route: ROUTES.employeeFixedAssignments });

    expect(
      await screen.findByRole('heading', { name: /mis sitios fijos|my fixed spots/i }),
    ).toBeInTheDocument();
  });

  it('should_redirect_to_login_when_admin_opens_employee_fixed_assignments_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.employeeFixedAssignments });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
  });
});
