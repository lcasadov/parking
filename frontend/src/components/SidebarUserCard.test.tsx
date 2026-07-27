import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { SidebarUserCard } from './SidebarUserCard';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

describe('SidebarUserCard', () => {
  it('should_render_authenticated_user_name_and_role', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<SidebarUserCard />);

    expect(await screen.findByText('Ada Admin')).toBeInTheDocument();
    expect(screen.getByText('Administrador')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /ada admin/i })).toHaveTextContent('AA');
  });

  // La píldora de perfil abre un diálogo de confirmación y solo cierra sesión al
  // confirmar (change reservas-employee-admin-reassign, Feature F: sin popover).
  it('should_confirm_before_logging_out', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    let loggedOut = false;
    server.use(
      http.post(`${MSW_BASE}/auth/logout`, () => {
        loggedOut = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<SidebarUserCard />);

    const trigger = await screen.findByRole(
      'button',
      { name: /cerrar sesión|log out/i },
      { timeout: 10000 },
    );
    await user.click(trigger);

    // Se abre el diálogo de confirmación; el logout aún no se ha llamado.
    const dialog = await screen.findByRole('dialog');
    expect(loggedOut).toBe(false);

    const confirm = within(dialog).getByRole('button', { name: /cerrar sesión|log out/i });
    await user.click(confirm);
    await waitFor(() => {
      expect(loggedOut).toBe(true);
    });
  });

  // El popover redundante desapareció: idioma/tema viven en el pie del sidebar y
  // "Exportar mis datos" se retiró. El diálogo de logout no los expone.
  it('should_not_expose_language_theme_or_export_in_the_logout_dialog', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    const user = userEvent.setup();
    renderWithProviders(<SidebarUserCard />);

    const trigger = await screen.findByRole(
      'button',
      { name: /cerrar sesión|log out/i },
      { timeout: 10000 },
    );
    await user.click(trigger);
    await screen.findByRole('dialog');

    expect(
      screen.queryByRole('button', { name: /exportar mis datos|export my data/i }),
    ).not.toBeInTheDocument();
    // Botón de cancelar presente para no cerrar sesión por error.
    expect(screen.getByRole('button', { name: /cancelar|cancel/i })).toBeInTheDocument();
  });
});
