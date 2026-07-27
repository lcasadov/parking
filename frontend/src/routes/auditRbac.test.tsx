import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { ROUTES } from './paths';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

// Las rutas antiguas de Auditoría y Accesos redirigen a la página unificada de
// Registros (RecordsPage), cuyo título de página es "Registros" / "Records".
const RECORDS_HEADING = /^registros$|^records$/i;

describe('Audit and LoginLogs RBAC', () => {
  it('should_render_audit_view_when_admin_opens_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminAudit });

    expect(
      await screen.findByRole('heading', { name: RECORDS_HEADING }),
    ).toBeInTheDocument();
  });

  it('should_render_login_logs_view_when_admin_opens_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminLoginLogs });

    expect(
      await screen.findByRole('heading', { name: RECORDS_HEADING }),
    ).toBeInTheDocument();
  });

  it('should_redirect_to_login_when_employee_opens_audit_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminAudit });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
    expect(
      screen.queryByRole('heading', { name: RECORDS_HEADING }),
    ).not.toBeInTheDocument();
  });

  it('should_redirect_to_login_when_employee_opens_login_logs_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminLoginLogs });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
    expect(
      screen.queryByRole('heading', { name: RECORDS_HEADING }),
    ).not.toBeInTheDocument();
  });
});
