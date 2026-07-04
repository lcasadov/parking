import { apiClient } from './apiClient';
import type {
  PageVisitor,
  Visitor,
  VisitorCreateRequest,
  VisitorListParams,
} from '../types/visitor';

// Endpoints de Visitors segun docs/openapi.yaml. baseURL relativo del apiClient.

const VISITORS = '/visitors';

// Serializa los parametros de listado omitiendo los indefinidos/vacios.
function buildListParams(params: VisitorListParams): Record<string, string | number> {
  const query: Record<string, string | number> = {};
  if (params.page !== undefined) {
    query.page = params.page;
  }
  if (params.size !== undefined) {
    query.size = params.size;
  }
  if (params.q !== undefined && params.q !== '') {
    query.q = params.q;
  }
  return query;
}

// GET /visitors (ADMIN, paginado): buscador por nationalId/nombre/matricula.
export async function listVisitors(params: VisitorListParams = {}): Promise<PageVisitor> {
  const { data } = await apiClient.get<PageVisitor>(VISITORS, {
    params: buildListParams(params),
  });
  return data;
}

// GET /visitors/{id} (ADMIN): detalle de un visitante.
export async function getVisitor(id: number): Promise<Visitor> {
  const { data } = await apiClient.get<Visitor>(`${VISITORS}/${id}`);
  return data;
}

// POST /visitors (ADMIN): crea una ficha (nationalId unico -> 409 en colision).
export async function createVisitor(body: VisitorCreateRequest): Promise<Visitor> {
  const { data } = await apiClient.post<Visitor>(VISITORS, body);
  return data;
}

// PUT /visitors/{id} (ADMIN): modifica la ficha; afecta solo a futuras reservas.
export async function updateVisitor(id: number, body: VisitorCreateRequest): Promise<Visitor> {
  const { data } = await apiClient.put<Visitor>(`${VISITORS}/${id}`, body);
  return data;
}
