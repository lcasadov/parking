import { apiClient } from './apiClient';
import type { Vehicle, VehicleRequest } from '../types/vehicle';

// Endpoints self-service de vehículos del empleado autenticado (change
// employee-vehicle-self-service). El backend deriva el empleado del principal; el alta/edición
// dejan el vehículo PENDING de validación.
const BASE = '/me/vehicles';

export async function listMyVehicles(): Promise<Vehicle[]> {
  const { data } = await apiClient.get<Vehicle[]>(BASE);
  return data;
}

export async function createMyVehicle(body: VehicleRequest): Promise<Vehicle> {
  const { data } = await apiClient.post<Vehicle>(BASE, body);
  return data;
}

export async function updateMyVehicle(vehicleId: number, body: VehicleRequest): Promise<Vehicle> {
  const { data } = await apiClient.put<Vehicle>(`${BASE}/${vehicleId}`, body);
  return data;
}

export async function deleteMyVehicle(vehicleId: number): Promise<void> {
  await apiClient.delete(`${BASE}/${vehicleId}`);
}
