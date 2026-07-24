import { useQueries } from '@tanstack/react-query';
import { getSuggestedSpace } from '../api/requestsApi';
import type { SuggestedParkingSpace } from '../types/request';

// Plaza sugerida por fecha: la que la auto-asignación daría al empleado según su
// categoría/planta, resuelta ANTES de confirmar. `space` es null mientras carga.
export interface SuggestedSpaceByDate {
  date: string;
  isLoading: boolean;
  isError: boolean;
  space: SuggestedParkingSpace | null;
}

// Resultado agregado del preview de auto-asignación para el conjunto de fechas.
export interface SuggestedSpaces {
  isLoading: boolean;
  isError: boolean;
  byDate: SuggestedSpaceByDate[];
}

export function suggestedSpaceQueryKey(
  employeeId: number | null,
  date: string,
): (string | number | null)[] {
  return ['requests', 'suggested-space', employeeId, date];
}

// Preview de auto-asignación por categoría: lanza GET /requests/admin/suggested-space
// por cada fecha para el empleado destino, de modo que el resumen muestre qué plaza
// exacta se asignaría en cada día ANTES de confirmar. Se desactiva si no aplica
// (sin empleado, sin fechas, o el usuario eligió plaza concreta / puesto).
export function useSuggestedSpaces(
  employeeId: number | null,
  dates: string[],
  enabled: boolean,
): SuggestedSpaces {
  const active = enabled && employeeId !== null && dates.length > 0;
  const results = useQueries({
    queries: active
      ? dates.map((date) => ({
          queryKey: suggestedSpaceQueryKey(employeeId, date),
          queryFn: () => getSuggestedSpace(employeeId as number, date),
          staleTime: 0,
        }))
      : [],
  });

  const isLoading = active && results.some((result) => result.isLoading);
  const isError = results.some((result) => result.isError);
  const byDate: SuggestedSpaceByDate[] = active
    ? dates.map((date, index) => ({
        date,
        isLoading: results[index]?.isLoading ?? false,
        isError: results[index]?.isError ?? false,
        space: results[index]?.data ?? null,
      }))
    : [];

  return { isLoading, isError, byDate };
}
