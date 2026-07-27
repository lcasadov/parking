import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { ROUTES } from './paths';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

describe('Parking spaces RBAC', () => {
  it('should_render_parking_spaces_view_when_admin_opens_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminParkingSpaces });

    expect(
      await screen.findByRole('heading', { name: /recursos|resources/i }),
    ).toBeInTheDocument();
  });

  it('should_redirect_to_login_when_employee_role_opens_parking_spaces_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminParkingSpaces });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
    expect(
      screen.queryByRole('heading', { name: /recursos|resources/i }),
    ).not.toBeInTheDocument();
  });
});
