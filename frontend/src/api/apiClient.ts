import axios, { AxiosError } from 'axios';
import { emitApiErrorToast, emitSessionExpired } from './events';

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

// Interceptor de respuesta:
//  401 -> evento "sesion expirada" (modal + vuelta a /login).
//  403 / 5xx -> toast generico.
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status;
    if (status === 401) {
      emitSessionExpired();
    } else if (status === 403) {
      emitApiErrorToast('errors.forbidden');
    } else if (status !== undefined && status >= 500) {
      emitApiErrorToast('errors.server');
    }
    return Promise.reject(error);
  },
);
