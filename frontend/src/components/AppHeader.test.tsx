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

  it('should_call_logout_when_logout_clicked', async () => {
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

    // Bug #12: el boton de logout solo se renderiza cuando la query /auth/me
    // resuelve; en una ejecucion fria de la suite completa con cobertura ese
    // primer render puede superar el timeout por defecto de findBy* (1000 ms).
    // Timeout generoso por-query (dentro del testTimeout de 15 s): findBy*
    // hace polling, asi que resuelve en cuanto aparece el boton — determinista.
    const logoutBtn = await screen.findByRole(
      'button',
      { name: /cerrar sesión|log out/i },
      { timeout: 10000 },
    );
    await user.click(logoutBtn);

    await waitFor(() => {
      expect(loggedOut).toBe(true);
    });
  });
});
