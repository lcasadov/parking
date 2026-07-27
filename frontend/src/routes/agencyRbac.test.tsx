import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { ROUTES, homePathForRole } from './paths';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, agencyUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

// La vista de AGENCIA es ahora el hub de liberaciones (ReleaseHubPage), cuyo
// título de página es "Liberar" / "Release".
const RELEASE_HEADING = /^liberar$|^release$/i;
const SIGN_IN = /entrar|sign in/i;

describe('Agency RBAC', () => {
  it('should_route_agency_home_to_agency_shell', () => {
    // El rol AGENCIA aterriza en el shell de agencia, no en /admin ni /employee.
    expect(homePathForRole('AGENCIA')).toBe(ROUTES.agency);
    expect(homePathForRole('AGENCIA')).not.toBe(ROUTES.admin);
    expect(homePathForRole('AGENCIA')).not.toBe(ROUTES.employee);
  });

  it('should_render_administrative_release_when_agency_opens_agency_shell', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(agencyUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.agency });

    expect(await screen.findByRole('heading', { name: RELEASE_HEADING })).toBeInTheDocument();
  });

  it('should_redirect_to_login_when_agency_opens_admin_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(agencyUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminEmployees });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: SIGN_IN })).toBeInTheDocument();
    });
  });

  it('should_redirect_to_login_when_agency_opens_employee_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(agencyUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.employeeReleases });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: SIGN_IN })).toBeInTheDocument();
    });
  });

  it('should_redirect_to_login_when_admin_opens_agency_shell', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.agencyReleases });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: SIGN_IN })).toBeInTheDocument();
    });
    expect(screen.queryByRole('heading', { name: RELEASE_HEADING })).not.toBeInTheDocument();
  });

  it('should_redirect_to_login_when_employee_opens_agency_shell', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.agencyReleases });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: SIGN_IN })).toBeInTheDocument();
    });
    expect(screen.queryByRole('heading', { name: RELEASE_HEADING })).not.toBeInTheDocument();
  });
});
