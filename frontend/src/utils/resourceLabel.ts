import type { TFunction } from 'i18next';
import type { OccupancyItem } from '../types/occupancy';

// Etiqueta humana de un recurso ocupado: "Plaza 3005 · Planta 3" para plazas,
// "Puesto 12" para puestos (sin planta). Compartida por las vistas de liberacion
// (por fecha y por empleado/semana) para no duplicar el formato (S1192).
export function resourceLabel(
  item: Pick<OccupancyItem, 'resourceType' | 'resourceNumber' | 'floor'>,
  t: TFunction,
): string {
  if (item.resourceType === 'DESK') {
    return t('releases.byDate.resourceDesk', { number: item.resourceNumber });
  }
  return t('releases.byDate.resourcePark', {
    number: item.resourceNumber,
    floor: item.floor ?? '',
  });
}
