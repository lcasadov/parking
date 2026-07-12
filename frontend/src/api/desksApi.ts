import { apiClient } from './apiClient';
import type { Desk, DeskCreate, DeskListParams, PageDesk } from '../types/desk';

// Endpoints de Desks según docs/openapi.yaml. baseURL relativo del apiClient.

const DESKS = '/desks';

// Serializa los parámetros de listado omitiendo los indefinidos.
function buildListParams(params: DeskListParams): Record<string, string | number | boolean> {
  const query: Record<string, string | number | boolean> = {};
  if (params.page !== undefined) {
    query.page = params.page;
  }
  if (params.size !== undefined) {
    query.size = params.size;
  }
  if (params.active !== undefined) {
    query.active = params.active;
  }
  if (params.category !== undefined) {
    query.category = params.category;
  }
  return query;
}

export async function listDesks(params: DeskListParams = {}): Promise<PageDesk> {
  const { data } = await apiClient.get<PageDesk>(DESKS, {
    params: buildListParams(params),
  });
  return data;
}

export async function createDesk(body: DeskCreate): Promise<Desk> {
  const { data } = await apiClient.post<Desk>(DESKS, body);
  return data;
}

export async function updateDesk(id: number, body: DeskCreate): Promise<Desk> {
  const { data } = await apiClient.put<Desk>(`${DESKS}/${id}`, body);
  return data;
}

// Activa/desactiva un puesto. El endpoint de actualización (PUT) NO modifica el
// estado; la activación tiene su endpoint dedicado (bug #83).
export async function setDeskActivation(id: number, active: boolean): Promise<Desk> {
  const { data } = await apiClient.patch<Desk>(`${DESKS}/${id}/activation`, { active });
  return data;
}
