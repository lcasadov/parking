import { apiClient } from './apiClient';
import type { EmployeeVehicle, EmployeeVehicleRequest } from '../types/employeeVehicle';

// Endpoints del CRUD de vehículos de empleado (change employee-vehicles), anidados bajo
// el empleado. baseURL relativo del apiClient.
function base(employeeId: number): string {
  return `/employees/${employeeId}/vehicles`;
}

// GET /employees/{employeeId}/vehicles (ADMIN): lista de vehículos del empleado.
export async function listEmployeeVehicles(employeeId: number): Promise<EmployeeVehicle[]> {
  const { data } = await apiClient.get<EmployeeVehicle[]>(base(employeeId));
  return data;
}

// POST /employees/{employeeId}/vehicles (ADMIN): alta de un vehículo.
export async function createEmployeeVehicle(
  employeeId: number,
  body: EmployeeVehicleRequest,
): Promise<EmployeeVehicle> {
  const { data } = await apiClient.post<EmployeeVehicle>(base(employeeId), body);
  return data;
}

// PUT /employees/{employeeId}/vehicles/{vehicleId} (ADMIN): edición de un vehículo.
export async function updateEmployeeVehicle(
  employeeId: number,
  vehicleId: number,
  body: EmployeeVehicleRequest,
): Promise<EmployeeVehicle> {
  const { data } = await apiClient.put<EmployeeVehicle>(`${base(employeeId)}/${vehicleId}`, body);
  return data;
}

// DELETE /employees/{employeeId}/vehicles/{vehicleId} (ADMIN): borrado de un vehículo.
export async function deleteEmployeeVehicle(employeeId: number, vehicleId: number): Promise<void> {
  await apiClient.delete(`${base(employeeId)}/${vehicleId}`);
}
