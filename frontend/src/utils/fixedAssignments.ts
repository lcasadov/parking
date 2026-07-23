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
//
// CRITICO (fix 7.1): un empleado puede tener MAS DE UN recurso del mismo tipo en
// dias distintos (p.ej. puesto 1 el lunes y puesto 3 el miercoles) porque cada fila
// de asignacion fija es independiente. Emparejar solo por `resourceType` (como hacia
// la version anterior via groupFixedAssignments) coge el PRIMER grupo de ese tipo y
// consolida mal: asignar el puesto 3 el miercoles heredaria y sobrescribiria los dias
// del puesto 1. Por eso esta funcion filtra por `resourceType` **y** `resourceId`
// (el recurso de la PROPIA celda que se esta asignando), y devuelve la union
// ordenada de esos dias con `dayToAdd`.
export function mergeFixedAssignmentDays(
  rows: FixedAssignment[],
  resourceType: ResourceType,
  resourceId: number,
  dayToAdd: number,
): number[] {
  const existing = rows
    .filter(
      (row) => (row.resourceType ?? 'PARKING') === resourceType && row.parkingSpaceId === resourceId,
    )
    .map((row) => row.dayOfWeek);
  if (existing.includes(dayToAdd)) {
    return [...existing].sort((a, b) => a - b);
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

// ---------------------------------------------------------------------------
// Mapa día→recurso (tarea 7.3): el modal de empleado permite asignar, por cada
// día de la semana y por cada tipo de recurso, QUÉ recurso concreto se asigna;
// pueden ser recursos distintos en días distintos (puesto 1 el lunes, puesto 3
// el miércoles). El índice único (empleado, tipo, día) garantiza un único recurso
// por día y tipo, así que un mapa `día → resourceId` es una representación fiel.
// ---------------------------------------------------------------------------

// Día ISO (1-7) -> resource_id asignado ese día para un tipo de recurso.
export type DayResourceMap = Record<number, number>;

// Reconstruye, a partir de las filas activas del empleado, el mapa día→recurso
// de cada tipo. Es el prefill del modal: refleja recursos distintos por día sin
// colapsarlos a un único recurso por tipo.
export function toEmployeeDayResourceMaps(
  rows: FixedAssignment[],
): { parking: DayResourceMap; desk: DayResourceMap } {
  const parking: DayResourceMap = {};
  const desk: DayResourceMap = {};
  for (const row of rows) {
    if (row.active === false) {
      continue;
    }
    const target = (row.resourceType ?? 'PARKING') === 'DESK' ? desk : parking;
    target[row.dayOfWeek] = row.parkingSpaceId;
  }
  return { parking, desk };
}

// Agrupa un mapa día→recurso por recurso, devolviendo para cada resource_id sus
// días ordenados. Es la base del guardado: un PUT por recurso con sus días.
export function groupDaysByResource(map: DayResourceMap): Map<number, number[]> {
  const groups = new Map<number, number[]>();
  for (const dayKey of Object.keys(map)) {
    const day = Number(dayKey);
    const resourceId = map[day];
    const days = groups.get(resourceId);
    if (days) {
      days.push(day);
    } else {
      groups.set(resourceId, [day]);
    }
  }
  for (const days of groups.values()) {
    days.sort((a, b) => a - b);
  }
  return groups;
}

// Días (ordenados) asignados a un recurso concreto dentro del mapa.
export function daysForResource(map: DayResourceMap, resourceId: number): number[] {
  return Object.keys(map)
    .map(Number)
    .filter((day) => map[day] === resourceId)
    .sort((a, b) => a - b);
}

// ¿`target` conserva TODOS los pares (día→recurso) de `prev`? Si es así, del
// estado previo al nuevo solo se han AÑADIDO días/recursos (ningún día cambió de
// recurso ni se retiró), luego basta con emitir PUTs incrementales sin revocar.
// Si NO es superset, hubo reasignaciones/retiradas que exigen limpiar el tipo
// antes de recrear (evita el 409 del índice único al mover un día entre recursos).
export function isDayResourceSuperset(target: DayResourceMap, prev: DayResourceMap): boolean {
  return Object.keys(prev).every((dayKey) => target[Number(dayKey)] === prev[Number(dayKey)]);
}

// Igualdad de dos listas de días ya ordenadas (para omitir PUTs redundantes).
export function sameDayList(a: number[] | undefined, b: number[]): boolean {
  if (!a || a.length !== b.length) {
    return false;
  }
  return a.every((value, index) => value === b[index]);
}

// Reescribe el mapa fijando el conjunto de días de `resourceId` a `days`,
// preservando los días asignados a OTROS recursos; si un día de `days` pertenecía
// a otro recurso, pasa a `resourceId` (un único recurso por día y tipo).
export function setResourceDays(
  map: DayResourceMap,
  resourceId: number,
  days: number[],
): DayResourceMap {
  const next: DayResourceMap = {};
  for (const dayKey of Object.keys(map)) {
    const day = Number(dayKey);
    if (map[day] !== resourceId) {
      next[day] = map[day];
    }
  }
  for (const day of days) {
    next[day] = resourceId;
  }
  return next;
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
