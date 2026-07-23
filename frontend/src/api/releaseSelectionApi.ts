import { apiClient } from './apiClient';
import type { EmployeeOption, EmployeeWeekOccupancy } from '../types/releaseSelection';

// Endpoints de solo lectura que alimentan el selector del flujo de liberacion
// por empleado y semana (ADMIN/AGENCIA), segun docs/openapi.yaml.

const RELEASES_EMPLOYEES = '/releases/employees';

// GET /releases/employees (ADMIN/AGENCIA): empleados seleccionables (id + nombre).
export async function listSelectableReleaseEmployees(): Promise<EmployeeOption[]> {
  const { data } = await apiClient.get<EmployeeOption[]>(RELEASES_EMPLOYEES);
  return data;
}

// GET /releases/employees/{employeeId}/occupancy?weekStart=YYYY-MM-DD
// (ADMIN/AGENCIA): ocupacion del empleado en la semana indicada (plaza y puesto
// por dia, con origen y requestId cuando proviene de una solicitud aprobada).
export async function getEmployeeWeekOccupancy(
  employeeId: number,
  weekStart: string,
): Promise<EmployeeWeekOccupancy> {
  const { data } = await apiClient.get<EmployeeWeekOccupancy>(
    `${RELEASES_EMPLOYEES}/${employeeId}/occupancy`,
    { params: { weekStart } },
  );
  return data;
}
