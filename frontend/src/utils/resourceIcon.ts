import type { ResourceType } from '../types/request';

// FUENTE ÚNICA del icono de cada recurso (nombre Tabler sin el prefijo "ti-").
// Cambiar aquí propaga el icono a TODA la app (tabs, botones, tarjetas, plano…).
//  - Parking → "P" de Tabler (ti-parking).
//  - Puesto  → ordenador (ti-device-desktop), el mismo que usa el plano.
export const RESOURCE_ICON: Record<ResourceType, string> = {
  PARKING: 'parking',
  DESK: 'device-desktop',
};
