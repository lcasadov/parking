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

// CRITICO (design §Risk D): el PUT /fixed-assignments/employee/{id} REEMPLAZA el
// conjunto de dias del empleado para ese tipo de recurso. Al asignar inline un dia
// nuevo desde una celda, hay que PRECARGAR los dias actuales y reenviar el conjunto
// COMPLETO; si solo se enviara el dia nuevo, se BORRARIAN los demas dias del empleado.
// Esta funcion toma las filas actuales del empleado, extrae los dias del recurso del
// tipo indicado y devuelve la union ordenada con `dayToAdd`.
export function mergeFixedAssignmentDays(
  rows: FixedAssignment[],
  resourceType: ResourceType,
  dayToAdd: number,
): number[] {
  const group = groupFixedAssignments(rows).find((entry) => entry.resourceType === resourceType);
  const existing = group?.days ?? [];
  if (existing.includes(dayToAdd)) {
    return [...existing];
  }
  return [...existing, dayToAdd].sort((a, b) => a - b);
}

// Une una lista con comas y un conector final localizado ("y" / "and"), p.ej.
// ["Lunes","Martes","Jueves"] -> "Lunes, Martes y Jueves".
export function joinWithAnd(items: string[], and: string): string {
  if (items.length <= 1) {
    return items[0] ?? '';
  }
  const head = items.slice(0, -1).join(', ');
  return `${head} ${and} ${items[items.length - 1]}`;
}

// Plaza fija y/o puesto fijo de un empleado, ya agrupados por tipo de recurso.
export interface EmployeeFixedResources {
  parking: FixedAssignmentGroup | null;
  desk: FixedAssignmentGroup | null;
}

// Reduce las filas planas de un empleado a su plaza fija (PARKING) y su puesto
// fijo (DESK). Reutiliza groupFixedAssignments: como los recursos son
// independientes, cada tipo aparece como mucho una vez.
export function toEmployeeFixedResources(rows: FixedAssignment[]): EmployeeFixedResources {
  const groups = groupFixedAssignments(rows);
  return {
    parking: groups.find((group) => group.resourceType === 'PARKING') ?? null,
    desk: groups.find((group) => group.resourceType === 'DESK') ?? null,
  };
}

// Indexa TODAS las asignaciones fijas por empleado, separando plaza y puesto.
// Se usa para pintar las columnas de la tabla de empleados de una sola pasada.
export function indexFixedResourcesByEmployee(
  rows: FixedAssignment[],
): Map<number, EmployeeFixedResources> {
  const byEmployee = new Map<number, FixedAssignment[]>();
  for (const row of rows) {
    const bucket = byEmployee.get(row.employeeId);
    if (bucket) {
      bucket.push(row);
    } else {
      byEmployee.set(row.employeeId, [row]);
    }
  }
  const result = new Map<number, EmployeeFixedResources>();
  for (const [employeeId, employeeRows] of byEmployee) {
    result.set(employeeId, toEmployeeFixedResources(employeeRows));
  }
  return result;
}
