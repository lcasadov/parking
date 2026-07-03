import { http, HttpResponse } from 'msw';
import type { ApiError, LoginRequest } from '../types/auth';
import { adminUser, employeeUser } from './fixtures';

// baseURL relativo del cliente -> los handlers cubren la misma ruta.
const BASE = '/parking-api/api/v1';

function apiError(error: string, message: string): ApiError {
  return { error, message, timestamp: new Date().toISOString() };
}

// Handlers por defecto: sesion valida (admin) en /auth/me, login segun login.
// Cada test puede sobrescribirlos con server.use(...).
export const handlers = [
  http.get(`${BASE}/auth/me`, () => HttpResponse.json(adminUser)),

  http.post(`${BASE}/auth/login`, async ({ request }) => {
    const body = (await request.json()) as LoginRequest;
    if (body.login === 'admin') {
      return HttpResponse.json(adminUser);
    }
    if (body.login === 'emp') {
      return HttpResponse.json(employeeUser);
    }
    return HttpResponse.json(apiError('unauthorized', 'Invalid credentials'), { status: 401 });
  }),

  http.post(`${BASE}/auth/logout`, () => new HttpResponse(null, { status: 204 })),

  http.post(`${BASE}/auth/change-password`, () => new HttpResponse(null, { status: 204 })),
];

export { BASE as MSW_BASE };
