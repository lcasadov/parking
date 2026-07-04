import { AxiosError } from 'axios';
import type { DeskState, FloorPlanDesk } from '../types/floorPlan';

// Devuelve la clase de estado del marcador (kebab-case): FREE -> floor-marker-free.
export function markerStateClass(state: DeskState): string {
  return `floor-marker-${state.toLowerCase()}`;
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
