import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { ROUTES } from './paths';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const ME_URL = `${MSW_BASE}/auth/me`;
const HEADING = /plano|floor plan/i;

describe('Floor plan RBAC', () => {
  it('should_render_floor_plan_when_admin_opens_admin_route', async () => {
    server.use(http.get(ME_URL, () => HttpResponse.json(adminUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminFloorPlan });

    expect(await screen.findByRole('heading', { name: HEADING })).toBeInTheDocument();
  });

  it('should_render_floor_plan_when_employee_opens_employee_route', async () => {
    server.use(http.get(ME_URL, () => HttpResponse.json(employeeUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.employeeFloorPlan });

    expect(await screen.findByRole('heading', { name: HEADING })).toBeInTheDocument();
  });

  it('should_redirect_to_login_when_employee_opens_admin_floor_plan_route', async () => {
    server.use(http.get(ME_URL, () => HttpResponse.json(employeeUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminFloorPlan });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
    expect(screen.queryByRole('heading', { name: HEADING })).not.toBeInTheDocument();
  });
});
