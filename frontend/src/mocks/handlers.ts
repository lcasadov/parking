import { http, HttpResponse } from 'msw';
import type { ApiError, LoginRequest } from '../types/auth';
import { adminUser, employeeUser } from './fixtures';
import { defaultEmployeePage, employeeAlice } from './employeeFixtures';
import { defaultParkingSpacePage, spaceP01 } from './parkingSpaceFixtures';

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

  // ---- Employees (defaults; cada test los sobrescribe con server.use) ----
  http.get(`${BASE}/employees`, () => HttpResponse.json(defaultEmployeePage)),

  http.post(`${BASE}/employees`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({ ...employeeAlice, ...body, id: 99 }, { status: 201 });
  }),

  http.put(`${BASE}/employees/:id`, async ({ request, params }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({ ...employeeAlice, ...body, id: Number(params.id) });
  }),

  http.delete(`${BASE}/employees/:id`, () => new HttpResponse(null, { status: 204 })),

  http.post(
    `${BASE}/employees/:id/reactivate`,
    () => new HttpResponse(null, { status: 204 }),
  ),

  http.post(`${BASE}/employees/:id/reset-password`, () =>
    HttpResponse.json({ temporaryPassword: 'Temp0ral!23', mustChange: true }),
  ),

  http.get(`${BASE}/employees/export`, () =>
    HttpResponse.text('id,login\n10,aandersson', {
      headers: { 'Content-Type': 'text/csv' },
    }),
  ),

  // ---- ParkingSpaces (defaults; cada test los sobrescribe con server.use) ----
  http.get(`${BASE}/parking-spaces`, () => HttpResponse.json(defaultParkingSpacePage)),

  http.post(`${BASE}/parking-spaces/configure`, async ({ request }) => {
    const body = (await request.json()) as { total: number };
    const spaces = Array.from({ length: body.total }, (_, index) => ({
      ...spaceP01,
      id: index + 1,
      label: `P-${String(index + 1).padStart(2, '0')}`,
    }));
    return HttpResponse.json(spaces);
  }),

  http.post(`${BASE}/parking-spaces`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({ ...spaceP01, ...body, id: 99 }, { status: 201 });
  }),

  http.put(`${BASE}/parking-spaces/:id`, async ({ request, params }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({ ...spaceP01, ...body, id: Number(params.id) });
  }),
];

export { BASE as MSW_BASE };
