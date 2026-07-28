import { apiClient } from './apiClient';
import type {
  PageRequest,
  Request,
  RequestAdminAssignRequest,
  RequestAdminReassignRequest,
  RequestAdminSwapRequest,
  RequestApproveRequest,
  RequestCreateRequest,
  RequestListParams,
  RequestRejectRequest,
  RequestSwapResponse,
  ResourceType,
  SuggestedParkingSpace,
  SuggestedResource,
} from '../types/request';

// Endpoints de Requests segun docs/openapi.yaml. baseURL relativo del apiClient.

const REQUESTS = '/requests';

// Serializa los parametros de listado omitiendo los indefinidos.
function buildListParams(params: RequestListParams): Record<string, string | number> {
  const query: Record<string, string | number> = {};
  if (params.page !== undefined) {
    query.page = params.page;
  }
  if (params.size !== undefined) {
    query.size = params.size;
  }
  if (params.status !== undefined) {
    query.status = params.status;
  }
  if (params.from !== undefined) {
    query.from = params.from;
  }
  if (params.to !== undefined) {
    query.to = params.to;
  }
  return query;
}

// GET /requests/mine (EMPLOYEE, paginado): solo las solicitudes propias.
export async function listMyRequests(params: RequestListParams = {}): Promise<PageRequest> {
  const { data } = await apiClient.get<PageRequest>(`${REQUESTS}/mine`, {
    params: buildListParams(params),
  });
  return data;
}

// Serializa el cuerpo de creacion omitiendo los campos indefinidos: la solicitud
// de PUESTO con puesto elegido incluye `resourceId`; la de PLAZA (o de PUESTO sin
// elegir) no lo incluye (retrocompatibilidad, tasks §3.1). `waitlist` solo se
// envia cuando el empleado opta por la lista de espera (capability
// request-waitlist, tasks §6.1); omitido conserva el 409 NO_AVAILABILITY previo.
function buildCreateBody(body: RequestCreateRequest): RequestCreateRequest {
  const payload: RequestCreateRequest = { requestedDate: body.requestedDate };
  if (body.resourceType !== undefined) {
    payload.resourceType = body.resourceType;
  }
  if (body.resourceId !== undefined) {
    payload.resourceId = body.resourceId;
  }
  if (body.waitlist !== undefined) {
    payload.waitlist = body.waitlist;
  }
  return payload;
}

// POST /requests (EMPLOYEE): crea una solicitud en estado PENDING.
export async function createRequest(body: RequestCreateRequest): Promise<Request> {
  const { data } = await apiClient.post<Request>(REQUESTS, buildCreateBody(body));
  return data;
}

// Cuerpo de la asignacion puntual del admin omitiendo los campos indefinidos:
// PARKING sin recurso elegido se envia sin `resourceId` (auto-asignacion), DESK
// exige `resourceId`. `resourceType` se omite cuando es PARKING (default backend).
function buildAdminAssignBody(body: RequestAdminAssignRequest): RequestAdminAssignRequest {
  const payload: RequestAdminAssignRequest = {
    employeeId: body.employeeId,
    requestedDate: body.requestedDate,
  };
  if (body.resourceType !== undefined) {
    payload.resourceType = body.resourceType;
  }
  if (body.resourceId !== undefined) {
    payload.resourceId = body.resourceId;
  }
  return payload;
}

// POST /requests/admin (ADMIN): asignacion puntual de un recurso a un empleado
// para una fecha concreta; nace APPROVED (capability admin-punctual-assignment).
export async function adminAssignRequest(body: RequestAdminAssignRequest): Promise<Request> {
  const { data } = await apiClient.post<Request>(
    `${REQUESTS}/admin`,
    buildAdminAssignBody(body),
  );
  return data;
}

// POST /requests/admin/reassign (ADMIN): reasigna el recurso de una solicitud APPROVED
// futura a otro recurso libre del mismo tipo. Devuelve la solicitud ya actualizada
// (capability admin-resource-reassignment). 409 si el recurso destino no está libre.
export async function adminReassignRequest(
  body: RequestAdminReassignRequest,
): Promise<Request> {
  const { data } = await apiClient.post<Request>(`${REQUESTS}/admin/reassign`, body);
  return data;
}

// POST /requests/admin/swap (ADMIN): intercambia atómicamente los recursos de dos
// solicitudes APPROVED de la misma fecha y tipo. Devuelve ambas solicitudes ya
// intercambiadas (capability admin-resource-reassignment).
export async function adminSwapRequests(
  body: RequestAdminSwapRequest,
): Promise<RequestSwapResponse> {
  const { data } = await apiClient.post<RequestSwapResponse>(`${REQUESTS}/admin/swap`, body);
  return data;
}

// GET /requests/admin/suggested-space?employeeId&date (ADMIN): vista previa de la
// plaza que la auto-asignacion daria al empleado esa fecha (segun categoria/planta),
// sin crear la asignacion. Alimenta el resumen del asistente antes de confirmar.
export async function getSuggestedSpace(
  employeeId: number,
  date: string,
): Promise<SuggestedParkingSpace> {
  const { data } = await apiClient.get<SuggestedParkingSpace>(`${REQUESTS}/admin/suggested-space`, {
    params: { employeeId, date },
  });
  return data;
}

// GET /requests/suggested?date&resourceType (EMPLOYEE): preview del recurso que la
// auto-asignación le daría (incluye la preferencia por el fijo propio). Sin crear nada.
export async function getSuggestedResource(
  date: string,
  resourceType: ResourceType,
): Promise<SuggestedResource> {
  const { data } = await apiClient.get<SuggestedResource>(`${REQUESTS}/suggested`, {
    params: { date, resourceType },
  });
  return data;
}

// GET /requests/pending (ADMIN, paginado): pendientes en orden FIFO.
export async function listPendingRequests(params: RequestListParams = {}): Promise<PageRequest> {
  const { data } = await apiClient.get<PageRequest>(`${REQUESTS}/pending`, {
    params: buildListParams(params),
  });
  return data;
}

// GET /requests (ADMIN, paginado): solicitudes por estado (aprobadas / rechazadas / todas)
// en orden de actividad reciente. `status` opcional: si se omite, devuelve todas.
export async function listRequestsByStatus(params: RequestListParams = {}): Promise<PageRequest> {
  const { data } = await apiClient.get<PageRequest>(REQUESTS, {
    params: buildListParams(params),
  });
  return data;
}

// GET /requests/{id} (ADMIN): detalle de una solicitud.
export async function getRequest(id: number): Promise<Request> {
  const { data } = await apiClient.get<Request>(`${REQUESTS}/${id}`);
  return data;
}

// POST /requests/{id}/cancel (EMPLOYEE): cancela la propia solicitud PENDING.
export async function cancelRequest(id: number): Promise<Request> {
  const { data } = await apiClient.post<Request>(`${REQUESTS}/${id}/cancel`);
  return data;
}

// POST /requests/{id}/admin-cancel (ADMIN): cancela la solicitud APPROVED futura de
// cualquier empleado con motivo obligatorio; libera el recurso para esa fecha.
export async function adminCancelRequest(id: number, reason: string): Promise<Request> {
  const { data } = await apiClient.post<Request>(`${REQUESTS}/${id}/admin-cancel`, { reason });
  return data;
}

// POST /requests/{id}/approve (ADMIN): aprueba asignando plaza.
export async function approveRequest(
  id: number,
  body: RequestApproveRequest,
): Promise<Request> {
  const { data } = await apiClient.post<Request>(`${REQUESTS}/${id}/approve`, body);
  return data;
}

// POST /requests/{id}/reject (ADMIN): rechaza con motivo del catalogo.
export async function rejectRequest(id: number, body: RequestRejectRequest): Promise<Request> {
  const { data } = await apiClient.post<Request>(`${REQUESTS}/${id}/reject`, body);
  return data;
}

// POST /requests/{id}/resend (EMPLOYEE, dueño de la solicitud PENDING): re-notifica
// a los admins. 403 si no es tuya; 409 REQUEST_NOT_PENDING / RESEND_TOO_SOON.
export async function resendRequest(id: number): Promise<Request> {
  const { data } = await apiClient.post<Request>(`${REQUESTS}/${id}/resend`);
  return data;
}
