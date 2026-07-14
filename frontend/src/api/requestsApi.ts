import { apiClient } from './apiClient';
import type {
  PageRequest,
  Request,
  RequestApproveRequest,
  RequestCreateRequest,
  RequestListParams,
  RequestRejectRequest,
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
// elegir) no lo incluye (retrocompatibilidad, tasks §3.1).
function buildCreateBody(body: RequestCreateRequest): RequestCreateRequest {
  const payload: RequestCreateRequest = { requestedDate: body.requestedDate };
  if (body.resourceType !== undefined) {
    payload.resourceType = body.resourceType;
  }
  if (body.resourceId !== undefined) {
    payload.resourceId = body.resourceId;
  }
  return payload;
}

// POST /requests (EMPLOYEE): crea una solicitud en estado PENDING.
export async function createRequest(body: RequestCreateRequest): Promise<Request> {
  const { data } = await apiClient.post<Request>(REQUESTS, buildCreateBody(body));
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
