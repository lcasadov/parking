import axios, { AxiosError } from 'axios';
import { emitApiErrorToast, emitSessionExpired } from './events';
import { isSessionActive } from './sessionState';

// baseURL relativo: identico en dev (via dev-proxy de Vite) y prod (same-origin).
// VITE_API_URL es un override OPCIONAL (vacio por defecto).
const RELATIVE_BASE = '/parking-api/api/v1';
const baseURL = import.meta.env.VITE_API_URL
  ? `${import.meta.env.VITE_API_URL}${RELATIVE_BASE}`
  : RELATIVE_BASE;

export const apiClient = axios.create({
  baseURL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

const LOGIN_PATH = '/auth/login';

// Fix bug #9: un 401 solo es "sesion expirada" si la peticion se hizo mientras
// la SPA creia tener una sesion activa (flag sincronizado por AuthProvider).
// Quedan excluidos:
//  - POST /auth/login: credenciales invalidas -> error inline en LoginPage
//    (spec auth-local, scenario "Login incorrecto muestra error inline").
//  - El probe inicial GET /auth/me de un visitante anonimo: aun no hay sesion,
//    asi que isSessionActive() es false y no se emite nada.
function isSessionExpired401(error: AxiosError): boolean {
  const isLoginRequest = error.config?.url?.endsWith(LOGIN_PATH) ?? false;
  return isSessionActive() && !isLoginRequest;
}

// Interceptor de respuesta:
//  401 (sesion que se creia activa, fuera de /auth/login) -> evento "sesion
//  expirada" (modal + vuelta a /login).
//  403 / 5xx -> toast generico.
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status;
    if (status === 401) {
      if (isSessionExpired401(error)) {
        emitSessionExpired();
      }
    } else if (status === 403) {
      emitApiErrorToast('errors.forbidden');
    } else if (status !== undefined && status >= 500) {
      emitApiErrorToast('errors.server');
    }
    return Promise.reject(error);
  },
);
