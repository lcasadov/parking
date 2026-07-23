// Tipos del flujo de liberacion por empleado y semana (change
// admin-release-by-employee-week). Derivados del contrato docs/openapi.yaml
// (EmployeeOption, EmployeeWeekOccupancy, EmployeeWeekDay, OccupancyItem).

import type { OccupancyItem } from './occupancy';

// EmployeeOption: opcion minima de empleado (id + nombre) para el selector.
// Schema #/components/schemas/EmployeeOption.
export interface EmployeeOption {
  id: number;
  fullName: string;
}

// EmployeeWeekDay: reservas del empleado para un dia (plaza y/o puesto).
// Schema #/components/schemas/EmployeeWeekDay. Reutiliza OccupancyItem (misma
// forma que la ocupacion por fecha: recurso + origen + requestId).
export interface EmployeeWeekDay {
  date: string;
  reservations: OccupancyItem[];
}

// EmployeeWeekOccupancy: ocupacion semanal de un empleado (plaza y puesto).
// Schema #/components/schemas/EmployeeWeekOccupancy.
export interface EmployeeWeekOccupancy {
  employeeId: number;
  employeeName: string;
  weekStart: string;
  days: EmployeeWeekDay[];
}
