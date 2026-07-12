import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { SessionExpiredModal } from './SessionExpiredModal';
import { emitSessionExpired } from '../api/events';
import { useAuth } from '../auth/useAuth';
import { renderWithProviders } from '../test/renderWithProviders';

// Sonda que expone el estado de sesion del AuthContext para poder afirmar
// que el cierre del modal limpia el usuario (clearUser).
function SessionProbe() {
  const { user } = useAuth();
  return <span data-testid="session-probe">{user ? user.login : 'anonymous'}</span>;
}

function renderModalWithRoutes() {
  return renderWithProviders(
    <>
      <Routes>
        <Route path="/admin" element={<span>admin-page</span>} />
        <Route path="/login" element={<span>login-page</span>} />
      </Routes>
      <SessionExpiredModal />
      <SessionProbe />
    </>,
    { route: '/admin' },
  );
}

describe('SessionExpiredModal', () => {
  it('should_open_modal_when_session_expired_event', async () => {
    renderModalWithRoutes();

    // No visible hasta que el interceptor emite el evento.
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    act(() => emitSessionExpired());

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    // Mockup 19: encabezado del cuerpo y nota de fase (banner azul).
    expect(screen.getByText(/tu sesión ha caducado|your session has expired/i)).toBeInTheDocument();
    expect(screen.getByText(/fase 2|phase 2/i)).toBeInTheDocument();
  });

  it('should_clear_session_and_navigate_to_login_when_closed', async () => {
    renderModalWithRoutes();
    // Handler MSW por defecto: sesion admin activa.
    await waitFor(() => {
      expect(screen.getByTestId('session-probe')).toHaveTextContent('admin');
    });

    act(() => emitSessionExpired());
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /volver a iniciar|back to login/i }));

    // Spec auth-local, scenario "Sesion expirada": AND al cerrarlo redirige a /login.
    await waitFor(() => {
      expect(screen.getByText('login-page')).toBeInTheDocument();
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByTestId('session-probe')).toHaveTextContent('anonymous');
  });
});
