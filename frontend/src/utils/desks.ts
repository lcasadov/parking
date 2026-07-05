import type { Desk } from '../types/desk';

// Etiqueta legible de un puesto ('D-05'), espejo del `label` que devuelve
// GET /availability?resourceType=DESK. Se usa donde solo se dispone del listado
// crudo de puestos (selector de asignacion fija, columna de la tabla).
const DESK_LABEL_PAD = 2;

export function deskLabel(deskNumber: number): string {
  return `D-${String(deskNumber).padStart(DESK_LABEL_PAD, '0')}`;
}

// Mapa id -> etiqueta de puesto, analogo al de plazas.
export function buildDeskLabels(desks: Desk[]): Map<number, string> {
  const map = new Map<number, string>();
  for (const desk of desks) {
    map.set(desk.id, deskLabel(desk.number));
  }
  return map;
}
