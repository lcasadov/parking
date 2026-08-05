import { apiClient } from './apiClient';
import type { VisitorVehicle, VisitorVehicleRequest } from '../types/visitorVehicle';

// Endpoints del CRUD de vehículos de visitante (change visitor-vehicles), anidados bajo
// el visitante. baseURL relativo del apiClient.
function base(visitorId: number): string {
  return `/visitors/${visitorId}/vehicles`;
}

// GET /visitors/{visitorId}/vehicles (ADMIN): lista de vehículos del visitante.
export async function listVisitorVehicles(visitorId: number): Promise<VisitorVehicle[]> {
  const { data } = await apiClient.get<VisitorVehicle[]>(base(visitorId));
  return data;
}

// POST /visitors/{visitorId}/vehicles (ADMIN): alta de un vehículo.
export async function createVisitorVehicle(
  visitorId: number,
  body: VisitorVehicleRequest,
): Promise<VisitorVehicle> {
  const { data } = await apiClient.post<VisitorVehicle>(base(visitorId), body);
  return data;
}

// PUT /visitors/{visitorId}/vehicles/{vehicleId} (ADMIN): edición de un vehículo.
export async function updateVisitorVehicle(
  visitorId: number,
  vehicleId: number,
  body: VisitorVehicleRequest,
): Promise<VisitorVehicle> {
  const { data } = await apiClient.put<VisitorVehicle>(`${base(visitorId)}/${vehicleId}`, body);
  return data;
}

// DELETE /visitors/{visitorId}/vehicles/{vehicleId} (ADMIN): borrado de un vehículo.
export async function deleteVisitorVehicle(visitorId: number, vehicleId: number): Promise<void> {
  await apiClient.delete(`${base(visitorId)}/${vehicleId}`);
}
