import type { FixedAssignment } from '../types/fixedAssignment';

// Dias ISO validos: 1 (lunes) .. 7 (domingo).
export const WEEK_DAYS: readonly number[] = [1, 2, 3, 4, 5, 6, 7];

// Grupo de asignaciones fijas de un empleado sobre una misma plaza, con sus dias.
export interface FixedAssignmentGroup {
  key: string;
  employeeId: number;
  parkingSpaceId: number;
  days: number[];
}

// Agrupa las filas (una por dia) por empleado+plaza y ordena los dias ascendente.
// La revocacion actua por empleado; el detalle muestra plaza + dias.
export function groupFixedAssignments(rows: FixedAssignment[]): FixedAssignmentGroup[] {
  const groups = new Map<string, FixedAssignmentGroup>();
  for (const row of rows) {
    const key = `${row.employeeId}:${row.parkingSpaceId}`;
    const existing = groups.get(key);
    if (existing) {
      existing.days.push(row.dayOfWeek);
    } else {
      groups.set(key, {
        key,
        employeeId: row.employeeId,
        parkingSpaceId: row.parkingSpaceId,
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
