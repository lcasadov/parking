import { useQueries } from '@tanstack/react-query';
import { getAvailability } from '../api/calendarApi';
import { resourceAvailabilityQueryKey } from './useCalendar';
import type { AvailabilityResponse } from '../types/calendar';
import type { ResourceType } from '../types/request';
import type { EligibleResource } from '../components/wizard/wizardTypes';

// Resultado de la disponibilidad en vivo para el conjunto de fechas elegido.
export interface WizardAvailability {
  isLoading: boolean;
  isError: boolean;
  // Recursos libres en TODAS las fechas (intersección): candidatos elegibles.
  eligible: EligibleResource[];
  // Ids de recurso libres en ALGUNA fecha (para marcar en el plano lo elegible).
  eligibleIds: Set<number>;
  // ¿Hay al menos una fecha con alguna disponibilidad? (habilita "cualquier plaza").
  anyFreeSomeDate: boolean;
}

// Intersecta las respuestas de disponibilidad por fecha: un recurso es elegible
// solo si aparece libre en cada una de las fechas. Conserva la etiqueta humana.
function intersectAvailability(responses: AvailabilityResponse[]): EligibleResource[] {
  if (responses.length === 0) {
    return [];
  }
  const [first, ...rest] = responses;
  const labelById = new Map<number, string>();
  first.availableResources.forEach((item) => labelById.set(item.parkingSpaceId, item.label));
  let survivors = new Set<number>(labelById.keys());
  rest.forEach((response) => {
    const present = new Set(response.availableResources.map((item) => item.parkingSpaceId));
    survivors = new Set([...survivors].filter((id) => present.has(id)));
  });
  return [...survivors]
    .map((id) => ({ resourceId: id, label: labelById.get(id) ?? String(id) }))
    .sort((a, b) => a.label.localeCompare(b.label, undefined, { numeric: true }));
}

// Disponibilidad en vivo para el paso de ubicación: lanza una consulta de
// /availability por cada fecha seleccionada y deriva los recursos elegibles
// (libres en todas las fechas). Se desactiva mientras no haya fechas o tipo.
export function useWizardAvailability(
  dates: string[],
  resourceType: ResourceType | null,
  enabled: boolean,
): WizardAvailability {
  const active = enabled && resourceType !== null && dates.length > 0;
  const results = useQueries({
    queries: active
      ? dates.map((date) => ({
          queryKey: resourceAvailabilityQueryKey(date, resourceType as ResourceType),
          queryFn: () => getAvailability(date, resourceType as ResourceType),
          staleTime: 0,
        }))
      : [],
  });

  const isLoading = active && results.some((result) => result.isLoading);
  const isError = results.some((result) => result.isError);
  const responses = results
    .map((result) => result.data)
    .filter((data): data is AvailabilityResponse => data !== undefined);

  const ready = active && !isLoading && !isError && responses.length === dates.length;
  const eligible = ready ? intersectAvailability(responses) : [];
  const eligibleIds = new Set(eligible.map((resource) => resource.resourceId));
  const anyFreeSomeDate = responses.some((response) => response.availableResources.length > 0);

  return { isLoading, isError, eligible, eligibleIds, anyFreeSomeDate };
}
