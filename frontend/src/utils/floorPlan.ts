import { AxiosError } from 'axios';
import type { DeskState, FloorPlanDesk } from '../types/floorPlan';

// Orden canónico de estados para leyenda, filtros y contadores (sin literales
// repetidos, S1192).
export const DESK_STATES: DeskState[] = ['FREE', 'RELEASED', 'MINE', 'REQUESTED', 'ASSIGNED'];

// Símbolo del puesto de dirección (EXECUTIVE) en marcador y leyenda.
export const EXECUTIVE_SYMBOL = '◆';

// Límites del zoom del plano (escala CSS transform).
export const ZOOM_MIN = 0.6;
export const ZOOM_MAX = 2.5;
export const ZOOM_STEP = 0.2;

// Devuelve la clase de estado del marcador (kebab-case): FREE -> floor-marker-free.
export function markerStateClass(state: DeskState): string {
  return `floor-marker-${state.toLowerCase()}`;
}

// Clase de pill del design-system (§6.3) para el estado de un puesto, usada en
// la variante admin "ocupación del día" del panel lateral.
const STATE_PILL: Record<DeskState, string> = {
  FREE: 'pill-gray',
  RELEASED: 'pill-pink',
  MINE: 'pill-blue',
  REQUESTED: 'pill-amber',
  ASSIGNED: 'pill-green',
};

export function deskStatePillClass(state: DeskState): string {
  return STATE_PILL[state];
}

// Cuenta los puestos por estado (para los contadores de los chips de filtro).
export function countByState(desks: FloorPlanDesk[]): Record<DeskState, number> {
  const counts: Record<DeskState, number> = {
    FREE: 0,
    RELEASED: 0,
    MINE: 0,
    REQUESTED: 0,
    ASSIGNED: 0,
  };
  for (const desk of desks) {
    counts[desk.state] += 1;
  }
  return counts;
}

// ¿Coincide el puesto con el filtro activo? `EXECUTIVE` filtra por categoría; el
// resto por estado. `null` = sin filtro (coincide todo).
export function matchesFilter(
  desk: FloorPlanDesk,
  filter: DeskState | 'EXECUTIVE' | null,
): boolean {
  if (filter === null) {
    return true;
  }
  if (filter === 'EXECUTIVE') {
    return desk.category === 'EXECUTIVE';
  }
  return desk.state === filter;
}

// ¿Coincide el puesto con la búsqueda por número del panel lateral? Vacío = todo.
export function matchesDeskSearch(desk: FloorPlanDesk, query: string): boolean {
  const trimmed = query.trim();
  if (trimmed === '') {
    return true;
  }
  return String(desk.deskNumber).includes(trimmed);
}

// Ajusta la escala del zoom al rango permitido con 2 decimales.
export function clampScale(scale: number): number {
  const rounded = Math.round(scale * 100) / 100;
  return Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, rounded));
}

// Un puesto está colocado si tiene ambas coordenadas definidas.
export function isPlaced(desk: FloorPlanDesk): boolean {
  return desk.coordX !== null && desk.coordY !== null;
}

// Ajusta un porcentaje al rango [0, 100] con 2 decimales.
export function clampPercent(value: number): number {
  const rounded = Math.round(value * 100) / 100;
  return Math.min(100, Math.max(0, rounded));
}

// Nueva coordenada relativa a partir del desplazamiento del ratón sobre la imagen.
export function nextCoord(base: number, clientDelta: number, size: number): number {
  if (size <= 0) {
    return clampPercent(base);
  }
  return clampPercent(base + (clientDelta / size) * 100);
}

// Un 400 del plano corresponde a la ventana de solicitud (OUTSIDE_REQUEST_WINDOW).
export function isOutsideWindowError(error: unknown): boolean {
  return error instanceof AxiosError && error.response?.status === 400;
}
