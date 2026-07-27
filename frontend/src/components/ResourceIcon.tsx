import type { ResourceType } from '../types/request';

// Icono del recurso, unificado en toda la app:
//  - Parking → "P" de Tabler (ti-parking), coherente con el resto.
//  - Puesto  → ordenador (ti-device-desktop), el mismo icono que usa el plano
//    para los puestos.
export function ResourceIcon({ type }: { type: ResourceType }) {
  const icon = type === 'PARKING' ? 'parking' : 'device-desktop';
  return <i className={`ti ti-${icon}`} aria-hidden="true" />;
}
