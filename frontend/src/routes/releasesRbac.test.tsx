import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { ROUTES } from './paths';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

// La ruta antigua de liberaciones ADMIN redirige al hub de liberaciones
// (ReleaseHubPage), cuyo título de página es "Liberar" / "Release". La de
// empleado redirige a "Mis sitios fijos" (MyResourcesPage, pestaña de liberaciones).
const RELEASE_HUB_HEADING = /^liberar$|^release$/i;
const MY_RESOURCES_HEADING = /mis sitios fijos|my fixed spots/i;

describe('Releases RBAC', () => {
  it('should_render_admin_release_panel_when_admin_opens_admin_releases_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminReleases });

    expect(
      await screen.findByRole('heading', { name: RELEASE_HUB_HEADING }),
    ).toBeInTheDocument();
  });

  it('should_redirect_to_login_when_employee_opens_admin_releases_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminReleases });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
    expect(
      screen.queryByRole('heading', { name: RELEASE_HUB_HEADING }),
    ).not.toBeInTheDocument();
  });

  it('should_render_my_releases_when_employee_opens_employee_releases_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.employeeReleases });

    expect(
      await screen.findByRole('heading', { name: MY_RESOURCES_HEADING }),
    ).toBeInTheDocument();
  });

  it('should_redirect_to_login_when_admin_opens_employee_releases_route', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<AppRoutes />, { route: ROUTES.employeeReleases });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
    expect(
      screen.queryByRole('heading', { name: MY_RESOURCES_HEADING }),
    ).not.toBeInTheDocument();
  });
});
