import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, employeeMustChange } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

function renderGuarded(route: string) {
  return renderWithProviders(
    <Routes>
      <Route
        path="/admin"
        element={
          <ProtectedRoute>
            <span>admin-content</span>
          </ProtectedRoute>
        }
      />
      <Route path="/login" element={<span>login-page</span>} />
      <Route path="/change-password" element={<span>change-password-page</span>} />
    </Routes>,
    { route },
  );
}

describe('ProtectedRoute', () => {
  it('should_redirect_to_login_when_no_session', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () =>
        HttpResponse.json({ error: 'unauthorized', message: 'no' }, { status: 401 }),
      ),
    );

    renderGuarded('/admin');

    await waitFor(() => {
      expect(screen.getByText('login-page')).toBeInTheDocument();
    });
  });

  it('should_redirect_to_change_password_when_password_must_change', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeMustChange)));

    renderGuarded('/admin');

    await waitFor(() => {
      expect(screen.getByText('change-password-page')).toBeInTheDocument();
    });
  });

  it('should_render_children_when_authenticated', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));

    renderGuarded('/admin');

    await waitFor(() => {
      expect(screen.getByText('admin-content')).toBeInTheDocument();
    });
  });
});
