import { apiClient } from './apiClient';
import type { OccupancyResponse } from '../types/occupancy';

// Endpoint de Occupancy (Liberar por fecha). baseURL relativo del apiClient.

const OCCUPANCY = '/occupancy';

// GET /occupancy?date=YYYY-MM-DD (ADMIN): recursos (plazas y puestos) ocupados esa
// fecha, cada uno con su titular y el origen de la ocupacion.
export async function getOccupancyByDate(date: string): Promise<OccupancyResponse> {
  const { data } = await apiClient.get<OccupancyResponse>(OCCUPANCY, {
    params: { date },
  });
  return data;
}
