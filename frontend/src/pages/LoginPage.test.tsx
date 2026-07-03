import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { LoginPage } from './LoginPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';

function renderLogin() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/admin" element={<span>admin-area</span>} />
      <Route path="/employee" element={<span>employee-area</span>} />
    </Routes>,
    { route: '/login' },
  );
}

describe('LoginPage', () => {
  it('should_render_form', () => {
    renderLogin();
    expect(screen.getByLabelText(/usuario|username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/contraseña|password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
  });

  it('should_redirect_by_role_on_success', async () => {
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/usuario|username/i), 'admin');
    await user.type(screen.getByLabelText(/contraseña|password/i), 'Secret123!');
    await user.click(screen.getByRole('button', { name: /entrar|sign in/i }));

    await waitFor(() => {
      expect(screen.getByText('admin-area')).toBeInTheDocument();
    });
  });

  it('should_show_inline_error_when_credentials_invalid', async () => {
    server.use(
      http.post(`${MSW_BASE}/auth/login`, () =>
        HttpResponse.json({ error: 'unauthorized', message: 'bad' }, { status: 401 }),
      ),
    );
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/usuario|username/i), 'nobody');
    await user.type(screen.getByLabelText(/contraseña|password/i), 'wrong');
    await user.click(screen.getByRole('button', { name: /entrar|sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
    expect(screen.queryByText('admin-area')).not.toBeInTheDocument();
  });
});
