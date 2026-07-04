import { http, HttpResponse } from 'msw';
import type { ApiError, LoginRequest } from '../types/auth';
import { adminUser, employeeUser } from './fixtures';
import { defaultEmployeePage, employeeAlice } from './employeeFixtures';
import { defaultParkingSpacePage, spaceP01 } from './parkingSpaceFixtures';
import { defaultDeskPage, deskStandard } from './deskFixtures';
import {
  aliceAssignments,
  defaultFixedAssignmentPage,
} from './fixedAssignmentFixtures';
import type { FixedAssignment, FixedAssignmentPutRequest } from '../types/fixedAssignment';
import {
  defaultMyRequestsPage,
  defaultPendingRequestsPage,
  requestApproved,
  requestPending1,
  requestRejected,
} from './requestFixtures';
import type { RequestApproveRequest, RequestRejectRequest } from '../types/request';
import { defaultMyReleasesPage, releaseFuture } from './releaseFixtures';
import type { AdministrativeReleaseRequest, ReleaseCreateRequest } from '../types/release';
import {
  defaultVisitorPage,
  defaultVisitorReservationPage,
  reservationFuture,
  visitorCarla,
} from './visitorFixtures';
import type { VisitorReservationCreateRequest } from '../types/visitor';
import {
  adminCalendarFor,
  defaultAvailability,
  defaultMyWeek,
} from './calendarFixtures';
import { defaultAuditPage, defaultLoginLogPage } from './auditFixtures';

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

  http.get(`${BASE}/employees/me/export`, () =>
    HttpResponse.text('field,value\nlogin,emp', {
      headers: {
        'Content-Type': 'text/csv',
        'Content-Disposition': 'attachment; filename="my-data.csv"',
      },
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

  // ---- Desks (defaults; cada test los sobrescribe con server.use) ----
  http.get(`${BASE}/desks`, () => HttpResponse.json(defaultDeskPage)),

  http.post(`${BASE}/desks`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({ ...deskStandard, ...body, id: 99 }, { status: 201 });
  }),

  http.put(`${BASE}/desks/:id`, async ({ request, params }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({ ...deskStandard, ...body, id: Number(params.id) });
  }),

  // ---- FixedAssignments (defaults; cada test los sobrescribe con server.use) ----
  http.get(`${BASE}/fixed-assignments`, () => HttpResponse.json(defaultFixedAssignmentPage)),

  http.get(`${BASE}/fixed-assignments/employee/:employeeId`, ({ params }) => {
    if (Number(params.employeeId) === 10) {
      return HttpResponse.json(aliceAssignments);
    }
    return HttpResponse.json([] as FixedAssignment[]);
  }),

  http.put(`${BASE}/fixed-assignments/employee/:employeeId`, async ({ request, params }) => {
    const body = (await request.json()) as FixedAssignmentPutRequest;
    const employeeId = Number(params.employeeId);
    const created = body.daysOfWeek.map((day, index) => ({
      id: 900 + index,
      parkingSpaceId: body.parkingSpaceId,
      employeeId,
      dayOfWeek: day,
      active: true,
      createdById: 1,
      createdAt: '2026-03-01T09:00:00Z',
      revokedById: null,
      revokedAt: null,
    }));
    return HttpResponse.json(created);
  }),

  http.delete(
    `${BASE}/fixed-assignments/employee/:employeeId`,
    () => new HttpResponse(null, { status: 204 }),
  ),

  // ---- Requests (defaults; cada test los sobrescribe con server.use) ----
  http.get(`${BASE}/requests/mine`, () => HttpResponse.json(defaultMyRequestsPage)),

  http.post(`${BASE}/requests`, async ({ request }) => {
    const body = (await request.json()) as { requestedDate: string; resourceType?: string };
    return HttpResponse.json(
      {
        ...requestPending1,
        id: 999,
        requestedDate: body.requestedDate,
        resourceType: body.resourceType ?? 'PARKING',
      },
      { status: 201 },
    );
  }),

  http.get(`${BASE}/requests/pending`, () => HttpResponse.json(defaultPendingRequestsPage)),

  http.get(`${BASE}/requests/export`, () =>
    HttpResponse.text('id,status\n1,APPROVED', {
      headers: { 'Content-Type': 'text/csv' },
    }),
  ),

  http.get(`${BASE}/requests/mine/export`, () =>
    HttpResponse.text('id,status\n1,PENDING', {
      headers: { 'Content-Type': 'text/csv' },
    }),
  ),

  http.get(`${BASE}/requests/:id`, ({ params }) => {
    if (Number(params.id) === requestPending1.id) {
      return HttpResponse.json(requestPending1);
    }
    return HttpResponse.json(apiError('not_found', 'Request not found'), { status: 404 });
  }),

  http.post(`${BASE}/requests/:id/cancel`, ({ params }) =>
    HttpResponse.json({ ...requestPending1, id: Number(params.id), status: 'CANCELLED' }),
  ),

  http.post(`${BASE}/requests/:id/approve`, async ({ request, params }) => {
    const body = (await request.json()) as RequestApproveRequest;
    return HttpResponse.json({
      ...requestApproved,
      id: Number(params.id),
      parkingSpaceId: body.parkingSpaceId,
      approvalNote: body.approvalNote ?? null,
    });
  }),

  http.post(`${BASE}/requests/:id/reject`, async ({ request, params }) => {
    const body = (await request.json()) as RequestRejectRequest;
    return HttpResponse.json({
      ...requestRejected,
      id: Number(params.id),
      rejectionReasonCode: body.reasonCode,
      rejectionReason: body.rejectionReason ?? null,
    });
  }),

  // ---- Releases (defaults; cada test los sobrescribe con server.use) ----
  http.get(`${BASE}/releases/mine`, () => HttpResponse.json(defaultMyReleasesPage)),

  http.post(`${BASE}/releases`, async ({ request }) => {
    const body = (await request.json()) as ReleaseCreateRequest;
    return HttpResponse.json(
      {
        ...releaseFuture,
        id: 999,
        releaseDate: body.releaseDate,
        parkingSpaceId: body.parkingSpaceId ?? releaseFuture.parkingSpaceId,
        type: 'VOLUNTARY',
      },
      { status: 201 },
    );
  }),

  http.delete(`${BASE}/releases/:id`, () => new HttpResponse(null, { status: 204 })),

  http.post(`${BASE}/releases/administrative`, async ({ request }) => {
    const body = (await request.json()) as AdministrativeReleaseRequest;
    return HttpResponse.json(
      {
        ...releaseFuture,
        id: 998,
        employeeId: body.employeeId,
        parkingSpaceId: body.parkingSpaceId,
        releaseDate: body.releaseDate,
        reason: body.reason,
        type: 'ADMINISTRATIVE',
        releasedById: 1,
      },
      { status: 201 },
    );
  }),

  // ---- Visitors (defaults; cada test los sobrescribe con server.use) ----
  http.get(`${BASE}/visitors`, () => HttpResponse.json(defaultVisitorPage)),

  http.get(`${BASE}/visitors/:id`, ({ params }) => {
    if (Number(params.id) === visitorCarla.id) {
      return HttpResponse.json(visitorCarla);
    }
    return HttpResponse.json(apiError('not_found', 'Visitor not found'), { status: 404 });
  }),

  http.post(`${BASE}/visitors`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({ ...visitorCarla, ...body, id: 99 }, { status: 201 });
  }),

  http.put(`${BASE}/visitors/:id`, async ({ request, params }) => {
    const body = (await request.json()) as Record<string, unknown>;
    return HttpResponse.json({ ...visitorCarla, ...body, id: Number(params.id) });
  }),

  // ---- VisitorReservations (defaults; cada test los sobrescribe con server.use) ----
  http.get(`${BASE}/visitor-reservations`, () =>
    HttpResponse.json(defaultVisitorReservationPage),
  ),

  http.post(`${BASE}/visitor-reservations`, async ({ request }) => {
    const body = (await request.json()) as VisitorReservationCreateRequest;
    return HttpResponse.json(
      {
        ...reservationFuture,
        id: 999,
        visitorId: body.visitorId,
        parkingSpaceId: body.parkingSpaceId,
        reservationDate: body.reservationDate,
        notes: body.notes ?? null,
      },
      { status: 201 },
    );
  }),

  http.delete(
    `${BASE}/visitor-reservations/:id`,
    () => new HttpResponse(null, { status: 204 }),
  ),

  // ---- Availability / Calendar (defaults; cada test los sobrescribe) ----
  http.get(`${BASE}/availability`, ({ request }) => {
    const date = new URL(request.url).searchParams.get('date');
    if (!date) {
      return HttpResponse.json(apiError('validation', 'date is required'), { status: 400 });
    }
    return HttpResponse.json({ ...defaultAvailability, date });
  }),

  http.get(`${BASE}/calendar/admin`, ({ request }) => {
    const weekStart = new URL(request.url).searchParams.get('weekStart');
    if (!weekStart) {
      return HttpResponse.json(apiError('validation', 'weekStart is required'), { status: 400 });
    }
    return HttpResponse.json(adminCalendarFor(weekStart));
  }),

  http.get(`${BASE}/calendar/my-week`, ({ request }) => {
    const weekStart = new URL(request.url).searchParams.get('weekStart');
    return HttpResponse.json(
      weekStart ? { ...defaultMyWeek, weekStart } : defaultMyWeek,
    );
  }),

  // ---- Audit / LoginLog (defaults; cada test los sobrescribe con server.use) ----
  http.get(`${BASE}/audit/export`, () =>
    HttpResponse.text('id,action\n1,LOGIN', {
      headers: { 'Content-Type': 'text/csv' },
    }),
  ),

  http.get(`${BASE}/audit`, () => HttpResponse.json(defaultAuditPage)),

  http.get(`${BASE}/login-logs`, () => HttpResponse.json(defaultLoginLogPage)),
];

export { BASE as MSW_BASE };
