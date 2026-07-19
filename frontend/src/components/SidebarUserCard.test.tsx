import { screen } from '@testing-library/react';
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
});
