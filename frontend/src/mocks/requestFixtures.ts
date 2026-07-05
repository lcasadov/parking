import type { PageRequest, Request } from '../types/request';
import { maxRequestDateIso, todayIso } from '../utils/requests';

// Solicitudes de ejemplo (contrato #/components/schemas/Request).
// Fechas dentro de la ventana para reflejar datos realistas en los tests.
export const requestPending1: Request = {
  id: 501,
  employeeId: 2,
  requestedDate: todayIso(),
  status: 'PENDING',
  parkingSpaceId: null,
  approvalNote: null,
  rejectionReasonCode: null,
  rejectionReason: null,
  resolvedById: null,
  resolvedAt: null,
  createdAt: '2026-03-01T08:00:00Z',
};

export const requestPending2: Request = {
  id: 502,
  employeeId: 10,
  requestedDate: maxRequestDateIso(),
  status: 'PENDING',
  parkingSpaceId: null,
  approvalNote: null,
  rejectionReasonCode: null,
  rejectionReason: null,
  resolvedById: null,
  resolvedAt: null,
  createdAt: '2026-03-01T09:30:00Z',
};

// Solicitud pendiente de PUESTO (resourceType=DESK) para los flujos de puesto.
export const requestPendingDesk: Request = {
  id: 505,
  employeeId: 2,
  requestedDate: todayIso(),
  status: 'PENDING',
  resourceType: 'DESK',
  parkingSpaceId: null,
  deskId: null,
  approvalNote: null,
  rejectionReasonCode: null,
  rejectionReason: null,
  resolvedById: null,
  resolvedAt: null,
  createdAt: '2026-03-01T08:15:00Z',
};

export const requestApproved: Request = {
  id: 503,
  employeeId: 2,
  requestedDate: '2026-03-05',
  status: 'APPROVED',
  parkingSpaceId: 1,
  approvalNote: 'Plaza junto a la entrada',
  rejectionReasonCode: null,
  rejectionReason: null,
  resolvedById: 1,
  resolvedAt: '2026-03-02T10:00:00Z',
  createdAt: '2026-03-01T07:00:00Z',
};

export const requestRejected: Request = {
  id: 504,
  employeeId: 2,
  requestedDate: '2026-03-06',
  status: 'REJECTED',
  parkingSpaceId: null,
  approvalNote: null,
  rejectionReasonCode: 'NO_AVAILABILITY',
  rejectionReason: null,
  resolvedById: 1,
  resolvedAt: '2026-03-02T11:00:00Z',
  createdAt: '2026-03-01T06:00:00Z',
};

// Envuelve una lista en una PageRequest de una sola pagina.
export function pageOfRequests(
  content: Request[],
  overrides: Partial<PageRequest> = {},
): PageRequest {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size: 20,
    number: 0,
    first: true,
    last: true,
    ...overrides,
  };
}

// Mis solicitudes por defecto (empleado 2): una pendiente + resueltas.
export const defaultMyRequestsPage: PageRequest = pageOfRequests([
  requestPending1,
  requestApproved,
  requestRejected,
]);

// Bandeja pendiente por defecto (ADMIN), orden FIFO por createdAt ASC.
export const defaultPendingRequestsPage: PageRequest = pageOfRequests([
  requestPending1,
  requestPending2,
]);
