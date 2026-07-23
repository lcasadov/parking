import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { App } from './App';
import { apiClient } from './api/apiClient';
import { server } from './mocks/server';
import { MSW_BASE } from './mocks/handlers';

// Regresion del bug #9: estos tests montan <App/> REAL (rutas +
// SessionExpiredModal + Toast compuestos como en produccion), que era el hueco
// de composicion que ocultaba el bug: los tests unitarios renderizaban
// LoginPage y SessionExpiredModal por separado y nunca juntos.

const ME_401 = http.get(`${MSW_BASE}/auth/me`, () =>
  HttpResponse.json({ error: 'unauthorized', message: 'no session' }, { status: 401 }),
);

function bootAppAt(path: string) {
  window.history.pushState({}, '', path);
  return render(<App />);
}

async function waitForLoginForm() {
  await waitFor(() => {
    expect(screen.getByRole('button', { name: /entrar|sign in/i })).toBeInTheDocument();
  });
}

describe('App composition (rutas + SessionExpiredModal)', () => {
  afterEach(() => {
    window.history.pushState({}, '', '/');
  });

  it('should_show_only_inline_error_without_modal_when_login_fails', async () => {
    server.use(
      ME_401,
      http.post(`${MSW_BASE}/auth/login`, () =>
        HttpResponse.json({ error: 'unauthorized', message: 'bad' }, { status: 401 }),
      ),
    );
    const user = userEvent.setup();
    bootAppAt('/login');
    await waitForLoginForm();

    await user.type(screen.getByLabelText(/usuario|username/i), 'nobody');
    await user.type(screen.getByLabelText(/^contraseña$|^password$/i), 'wrong');
    await user.click(screen.getByRole('button', { name: /entrar|sign in/i }));

    // Error inline generico visible...
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
    // ...y NINGUN modal de sesion expirada (spec auth-local:
    // "Login incorrecto muestra error inline").
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('should_redirect_to_login_without_modal_when_anonymous_boot', async () => {
    server.use(ME_401);
    bootAppAt('/admin');

    // Visitante anonimo: el probe GET /auth/me devuelve 401 -> redirige a
    // /login sin abrir el modal (no habia sesion que expirar).
    await waitForLoginForm();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(window.location.pathname).toBe('/login');
  });

  it('should_open_modal_and_return_to_login_when_active_session_expires', async () => {
    bootAppAt('/admin');

    // Sesion activa: el usuario admin ve su area. Al eliminarse el top-bar
    // (AppHeader), el rotulo "Administración" ya no lo aporta la barra superior
    // sino el eyebrow del PageHeader de la landing (/admin -> /admin/employees).
    await waitFor(() => {
      expect(screen.getByText(/administración|administration/i)).toHaveClass('page-eyebrow');
    });

    // La sesion caduca en el backend y una llamada posterior responde 401.
    server.use(ME_401);
    await expect(apiClient.get('/auth/me')).rejects.toBeDefined();

    // El interceptor abre el modal de sesion expirada...
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    // ...y al cerrarlo la SPA vuelve a /login (spec: "AND al cerrarlo
    // redirige a /login").
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /volver a iniciar|back to login/i }));

    await waitForLoginForm();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(window.location.pathname).toBe('/login');
  });
});
