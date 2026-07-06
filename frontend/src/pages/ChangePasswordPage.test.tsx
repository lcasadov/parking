import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { ChangePasswordPage } from './ChangePasswordPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';

function renderChangePassword() {
  return renderWithProviders(
    <Routes>
      <Route path="/change-password" element={<ChangePasswordPage />} />
      <Route path="/employee" element={<span>employee-area</span>} />
      <Route path="/admin" element={<span>admin-area</span>} />
    </Routes>,
    { route: '/change-password' },
  );
}

describe('ChangePasswordPage', () => {
  it('should_show_first_access_intro_banner', () => {
    renderChangePassword();
    expect(
      screen.getByText(/debes establecer una nueva contraseña|must set a new password/i),
    ).toBeInTheDocument();
  });

  it('should_validate_policy_before_submit', async () => {
    const user = userEvent.setup();
    renderChangePassword();

    await user.type(screen.getByLabelText(/contraseña actual|current password/i), 'old');
    await user.type(screen.getByLabelText(/^nueva contraseña$|^new password$/i), 'weak');

    // El boton de submit debe permanecer deshabilitado cuando la politica no se cumple.
    expect(screen.getByRole('button', { name: /cambiar contraseña|change password/i })).toBeDisabled();
  });

  it('should_submit_when_policy_met', async () => {
    let received = false;
    server.use(
      http.post(`${MSW_BASE}/auth/change-password`, () => {
        received = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderChangePassword();

    await user.type(screen.getByLabelText(/contraseña actual|current password/i), 'OldPass99!');
    await user.type(screen.getByLabelText(/^nueva contraseña$|^new password$/i), 'Str0ng!Pass99');
    await user.type(
      screen.getByLabelText(/confirmar|confirm/i),
      'Str0ng!Pass99',
    );
    await user.click(
      screen.getByRole('button', { name: /cambiar contraseña|change password/i }),
    );

    await waitFor(() => {
      expect(received).toBe(true);
    });
  });
});
