import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { ROUTES } from './paths';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

function useSession(user: typeof adminUser): void {
  server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(user)));
}

describe('Calendar / Availability RBAC', () => {
  it('should_renderAdminCalendar_when_adminOpensCalendarRoute', async () => {
    useSession(adminUser);
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminCalendar });

    expect(
      await screen.findByRole('heading', { name: /plazas de parking|parking spaces/i }),
    ).toBeInTheDocument();
  });

  it('should_redirectAvailabilityRouteToOccupancyGrid_when_adminOpensAvailabilityRoute', async () => {
    // "Disponibilidad" se retiró: su ruta antigua redirige a la Ocupación (rejilla
    // semanal), donde su función vive como filtro rápido "Solo libres". Los filtros
    // rápidos están siempre visibles, así que su grupo prueba que cargó la rejilla.
    useSession(adminUser);
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminAvailability });

    expect(
      await screen.findByRole('group', { name: /filtrar por estado|filter by state/i }),
    ).toBeInTheDocument();
  });

  it('should_redirectToLogin_when_employeeOpensAdminCalendarRoute', async () => {
    useSession(employeeUser);
    renderWithProviders(<AppRoutes />, { route: ROUTES.adminCalendar });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
    expect(
      screen.queryByRole('heading', { name: /plazas de parking|parking spaces/i }),
    ).not.toBeInTheDocument();
  });

  it('should_renderMyWeek_when_employeeOpensMyWeekRoute', async () => {
    useSession(employeeUser);
    renderWithProviders(<AppRoutes />, { route: ROUTES.employeeMyWeek });

    expect(
      await screen.findByRole('heading', { name: /mi semana|my week/i }),
    ).toBeInTheDocument();
  });

  it('should_redirectToLogin_when_adminOpensMyWeekRoute', async () => {
    useSession(adminUser);
    renderWithProviders(<AppRoutes />, { route: ROUTES.employeeMyWeek });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
    });
    expect(
      screen.queryByRole('heading', { name: /mi semana|my week/i }),
    ).not.toBeInTheDocument();
  });
});
