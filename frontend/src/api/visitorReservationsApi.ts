import { apiClient } from './apiClient';
import type {
  PageVisitorReservation,
  VisitorReservation,
  VisitorReservationCreateRequest,
  VisitorReservationListParams,
} from '../types/visitor';

// Endpoints de VisitorReservations segun docs/openapi.yaml. baseURL relativo.

const VISITOR_RESERVATIONS = '/visitor-reservations';

// Serializa los parametros de listado omitiendo los indefinidos.
function buildListParams(
  params: VisitorReservationListParams,
): Record<string, string | number> {
  const query: Record<string, string | number> = {};
  if (params.page !== undefined) {
    query.page = params.page;
  }
  if (params.size !== undefined) {
    query.size = params.size;
  }
  if (params.date !== undefined && params.date !== '') {
    query.date = params.date;
  }
  if (params.parkingSpaceId !== undefined) {
    query.parkingSpaceId = params.parkingSpaceId;
  }
  return query;
}

// GET /visitor-reservations (ADMIN, paginado): filtros date/parkingSpaceId.
export async function listVisitorReservations(
  params: VisitorReservationListParams = {},
): Promise<PageVisitorReservation> {
  const { data } = await apiClient.get<PageVisitorReservation>(VISITOR_RESERVATIONS, {
    params: buildListParams(params),
  });
  return data;
}

// POST /visitor-reservations (ADMIN): ocupa la plaza esa fecha (409 si ocupada).
export async function createVisitorReservation(
  body: VisitorReservationCreateRequest,
): Promise<VisitorReservation> {
  const { data } = await apiClient.post<VisitorReservation>(VISITOR_RESERVATIONS, body);
  return data;
}

// DELETE /visitor-reservations/{id} (ADMIN): anula una reserva futura (400 en pasada).
export async function cancelVisitorReservation(id: number): Promise<void> {
  await apiClient.delete(`${VISITOR_RESERVATIONS}/${id}`);
}
