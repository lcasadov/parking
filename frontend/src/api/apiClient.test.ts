import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';
import { apiClient } from './apiClient';
import { API_ERROR_TOAST, SESSION_EXPIRED, type ApiErrorToastDetail } from './events';
import { setSessionActive } from './sessionState';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';

const UNAUTHORIZED_BODY = { error: 'unauthorized', message: 'no' };

function respond401(path: string) {
  server.use(http.get(`${MSW_BASE}${path}`, () => HttpResponse.json(UNAUTHORIZED_BODY, { status: 401 })));
}

describe('apiClient interceptor', () => {
  let sessionListener: Mock;
  let toastListener: Mock;

  beforeEach(() => {
    sessionListener = vi.fn();
    toastListener = vi.fn();
    window.addEventListener(SESSION_EXPIRED, sessionListener);
    window.addEventListener(API_ERROR_TOAST, toastListener);
  });

  afterEach(() => {
    window.removeEventListener(SESSION_EXPIRED, sessionListener);
    window.removeEventListener(API_ERROR_TOAST, toastListener);
  });

  it('should_emit_session_expired_when_401_with_active_session', async () => {
    setSessionActive(true);
    respond401('/auth/me');

    await expect(apiClient.get('/auth/me')).rejects.toBeDefined();

    expect(sessionListener).toHaveBeenCalledTimes(1);
  });

  it('should_not_emit_session_expired_when_401_without_active_session', async () => {
    // Probe inicial GET /auth/me de un visitante anonimo: 401 sin sesion
    // previa NO es una sesion expirada (bug #9).
    respond401('/auth/me');

    await expect(apiClient.get('/auth/me')).rejects.toBeDefined();

    expect(sessionListener).not.toHaveBeenCalled();
  });

  it('should_not_emit_session_expired_when_401_on_login', async () => {
    // Credenciales invalidas en POST /auth/login -> error inline en LoginPage,
    // nunca el modal de sesion expirada (bug #9), incluso con sesion activa.
    setSessionActive(true);
    server.use(
      http.post(`${MSW_BASE}/auth/login`, () =>
        HttpResponse.json(UNAUTHORIZED_BODY, { status: 401 }),
      ),
    );

    await expect(apiClient.post('/auth/login', { login: 'x', password: 'y' })).rejects.toBeDefined();

    expect(sessionListener).not.toHaveBeenCalled();
  });

  it('should_emit_forbidden_toast_when_response_403', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () =>
        HttpResponse.json({ error: 'forbidden', message: 'no' }, { status: 403 }),
      ),
    );

    await expect(apiClient.get('/auth/me')).rejects.toBeDefined();

    expect(toastListener).toHaveBeenCalledTimes(1);
    const event = toastListener.mock.calls[0][0] as CustomEvent<ApiErrorToastDetail>;
    expect(event.detail.message).toBe('errors.forbidden');
    expect(sessionListener).not.toHaveBeenCalled();
  });

  it('should_emit_server_toast_when_response_5xx', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () =>
        HttpResponse.json({ error: 'internal', message: 'boom' }, { status: 500 }),
      ),
    );

    await expect(apiClient.get('/auth/me')).rejects.toBeDefined();

    expect(toastListener).toHaveBeenCalledTimes(1);
    const event = toastListener.mock.calls[0][0] as CustomEvent<ApiErrorToastDetail>;
    expect(event.detail.message).toBe('errors.server');
  });

  it('should_not_emit_any_event_when_client_error_4xx_not_auth', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () =>
        HttpResponse.json({ error: 'conflict', message: 'no' }, { status: 409 }),
      ),
    );

    await expect(apiClient.get('/auth/me')).rejects.toBeDefined();

    expect(sessionListener).not.toHaveBeenCalled();
    expect(toastListener).not.toHaveBeenCalled();
  });
});
