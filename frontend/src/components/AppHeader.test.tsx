import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AppHeader } from './AppHeader';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

describe('AppHeader', () => {
  it('should_render_brand_and_page_title', () => {
    renderWithProviders(<AppHeader pageTitle="Panel" />);
    expect(screen.getByText('parking')).toBeInTheDocument();
    expect(screen.getByText('Panel')).toBeInTheDocument();
  });

  it('should_open_user_menu_and_call_logout_when_logout_clicked', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    let loggedOut = false;
    server.use(
      http.post(`${MSW_BASE}/auth/logout`, () => {
        loggedOut = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AppHeader />);

    // Bug #12: el disparador del menu solo se renderiza cuando /auth/me
    // resuelve; findBy* hace polling hasta que aparece — determinista.
    const trigger = await screen.findByRole(
      'button',
      { name: /abrir menú de usuario|open user menu/i },
      { timeout: 10000 },
    );
    // El avatar con iniciales del usuario autenticado vive dentro del disparador.
    expect(screen.getByRole('img', { name: /ada admin/i })).toHaveTextContent('AA');

    // El popover (mockup 17) contiene el cierre de sesion.
    await user.click(trigger);
    const logoutBtn = await screen.findByRole('button', { name: /cerrar sesión|log out/i });
    await user.click(logoutBtn);

    await waitFor(() => {
      expect(loggedOut).toBe(true);
    });
  });

  it('should_toggle_language_control_inside_the_user_menu', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    const user = userEvent.setup();
    renderWithProviders(<AppHeader />);

    const trigger = await screen.findByRole(
      'button',
      { name: /abrir menú de usuario|open user menu/i },
      { timeout: 10000 },
    );
    await user.click(trigger);

    // Idioma (segmented) y tema (switch) conviven en el popover.
    expect(await screen.findByRole('switch')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^EN$/i })).toBeInTheDocument();
  });
});
