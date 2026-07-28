import { apiClient } from './apiClient';
import type {
  ParkingSpace,
  ParkingSpaceConfigureRequest,
  ParkingSpaceCreate,
  ParkingSpaceListParams,
  PageParkingSpace,
} from '../types/parkingSpace';

// Endpoints de ParkingSpaces segun docs/openapi.yaml. baseURL relativo del apiClient.

const PARKING_SPACES = '/parking-spaces';

// Serializa los parametros de listado omitiendo los indefinidos.
function buildListParams(params: ParkingSpaceListParams): Record<string, string | number | boolean> {
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
  if (params.floor !== undefined) {
    query.floor = params.floor;
  }
  return query;
}

export async function listParkingSpaces(
  params: ParkingSpaceListParams = {},
): Promise<PageParkingSpace> {
  const { data } = await apiClient.get<PageParkingSpace>(PARKING_SPACES, {
    params: buildListParams(params),
  });
  return data;
}

// GET /parking-spaces/{id} (ADMIN o EMPLOYEE): detalle de una plaza. A diferencia
// del catalogo (ADMIN-only), este endpoint es accesible a EMPLOYEE, lo que permite
// resolver el numero real de una plaza fija (simetrico a getDesk en desksApi.ts).
export async function getParkingSpace(id: number): Promise<ParkingSpace> {
  const { data } = await apiClient.get<ParkingSpace>(`${PARKING_SPACES}/${id}`);
  return data;
}

export async function createParkingSpace(body: ParkingSpaceCreate): Promise<ParkingSpace> {
  const { data } = await apiClient.post<ParkingSpace>(PARKING_SPACES, body);
  return data;
}

export async function updateParkingSpace(
  id: number,
  body: ParkingSpaceCreate,
): Promise<ParkingSpace> {
  const { data } = await apiClient.put<ParkingSpace>(`${PARKING_SPACES}/${id}`, body);
  return data;
}

export async function configureParkingSpaces(
  body: ParkingSpaceConfigureRequest,
): Promise<ParkingSpace[]> {
  const { data } = await apiClient.post<ParkingSpace[]>(`${PARKING_SPACES}/configure`, body);
  return data;
}
