import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { ROUTES } from './paths';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

describe('Requests RBAC', () => {
  it('should_render_pending_inbox_when_admin_opens_admin_requests_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminRequests });

    expect(
      await screen.findByRole('heading', { name: /solicitudes pendientes|pending requests/i }),
    ).toBeInTheDocument();
  });

  it('should_redirect_to_login_when_employee_opens_admin_requests_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminRequests });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
    expect(
      screen.queryByRole('heading', { name: /solicitudes pendientes|pending requests/i }),
    ).not.toBeInTheDocument();
  });

  it('should_render_my_requests_when_employee_opens_employee_requests_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.employeeRequests });

    expect(
      await screen.findByRole('heading', { name: /mis solicitudes|my requests/i }),
    ).toBeInTheDocument();
  });

  it('should_redirect_to_login_when_admin_opens_employee_requests_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.employeeRequests });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
    expect(
      screen.queryByRole('heading', { name: /mis solicitudes|my requests/i }),
    ).not.toBeInTheDocument();
  });
});
