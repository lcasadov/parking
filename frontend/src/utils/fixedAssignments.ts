import type { FixedAssignment } from '../types/fixedAssignment';
import type { ResourceType } from '../types/request';

// Dias ISO validos: 1 (lunes) .. 7 (domingo).
export const WEEK_DAYS: readonly number[] = [1, 2, 3, 4, 5, 6, 7];

// Grupo de asignaciones fijas de un empleado sobre un mismo recurso, con sus dias.
// `parkingSpaceId` transporta el resource_id generico; `resourceType` lo discrimina.
export interface FixedAssignmentGroup {
  key: string;
  employeeId: number;
  parkingSpaceId: number;
  resourceType: ResourceType;
  days: number[];
}

// Agrupa las filas (una por dia) por empleado+recurso+tipo y ordena los dias
// ascendente. Un empleado puede tener plaza fija y puesto fijo (grupos distintos).
// La revocacion actua por empleado; el detalle muestra recurso + dias.
export function groupFixedAssignments(rows: FixedAssignment[]): FixedAssignmentGroup[] {
  const groups = new Map<string, FixedAssignmentGroup>();
  for (const row of rows) {
    const resourceType: ResourceType = row.resourceType ?? 'PARKING';
    const key = `${row.employeeId}:${row.parkingSpaceId}:${resourceType}`;
    const existing = groups.get(key);
    if (existing) {
      existing.days.push(row.dayOfWeek);
    } else {
      groups.set(key, {
        key,
        employeeId: row.employeeId,
        parkingSpaceId: row.parkingSpaceId,
        resourceType,
        days: [row.dayOfWeek],
      });
    }
  }
  const result = Array.from(groups.values());
  for (const group of result) {
    group.days.sort((a, b) => a - b);
  }
  return result;
}

// Conjunto ordenado de dias distintos de una lista de asignaciones.
export function distinctDays(rows: FixedAssignment[]): number[] {
  const days = new Set<number>();
  for (const row of rows) {
    days.add(row.dayOfWeek);
  }
  return Array.from(days).sort((a, b) => a - b);
}

// Alterna la pertenencia de un dia en una lista seleccionada (para el multiselect).
export function toggleDay(days: number[], day: number): number[] {
  if (days.includes(day)) {
    return days.filter((value) => value !== day);
  }
  return [...days, day].sort((a, b) => a - b);
}
