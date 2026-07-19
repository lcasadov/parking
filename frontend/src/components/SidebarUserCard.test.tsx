import { screen, waitFor } from '@testing-library/react';
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

    // El nombre completo aparece cuando /auth/me resuelve (findBy hace polling).
    expect(await screen.findByText('Ada Admin')).toBeInTheDocument();
    // El rol se traduce via account.role.<ROLE> (ES por defecto).
    expect(screen.getByText('Administrador')).toBeInTheDocument();
    // El avatar accesible con iniciales del usuario.
    expect(screen.getByRole('img', { name: /ada admin/i })).toHaveTextContent('AA');
  });

  // Cobertura reubicada desde el desaparecido AppHeader: la tarjeta de usuario del
  // sidebar es ahora el unico contenedor de logout + preferencias (idioma/tema) +
  // exportar mis datos. Verifica que ninguna funcionalidad se perdio al quitar el top-bar.
  it('should_open_menu_and_call_logout_when_logout_clicked', async () => {
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

    // El disparador solo se renderiza cuando /auth/me resuelve; findBy hace polling.
    const trigger = await screen.findByRole(
      'button',
      { name: /abrir menú de usuario|open user menu/i },
      { timeout: 10000 },
    );
    expect(screen.getByRole('img', { name: /ada admin/i })).toHaveTextContent('AA');

    await user.click(trigger);
    const logoutBtn = await screen.findByRole('button', { name: /cerrar sesión|log out/i });
    await user.click(logoutBtn);

    await waitFor(() => {
      expect(loggedOut).toBe(true);
    });
  });

  it('should_expose_language_theme_and_export_controls_inside_the_menu', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    const user = userEvent.setup();
    renderWithProviders(<SidebarUserCard />);

    const trigger = await screen.findByRole(
      'button',
      { name: /abrir menú de usuario|open user menu/i },
      { timeout: 10000 },
    );
    await user.click(trigger);

    // Idioma (segmented), tema (switch) y exportar mis datos conviven en el panel.
    expect(await screen.findByRole('switch')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^EN$/i })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /exportar mis datos|export my data/i }),
    ).toBeInTheDocument();
  });
});
