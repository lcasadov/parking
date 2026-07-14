import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { getOccupancyByDate } from '../api/occupancyApi';
import type { OccupancyResponse } from '../types/occupancy';

// Clave raiz de cache (S1192: sin literales repetidos).
const OCCUPANCY_KEY = 'occupancy';

export function occupancyQueryKey(date: string): (string)[] {
  return [OCCUPANCY_KEY, date];
}

// Ocupacion por fecha (ADMIN). `enabled` desactiva la consulta hasta tener fecha.
export function useOccupancyQuery(date: string): UseQueryResult<OccupancyResponse> {
  return useQuery({
    queryKey: occupancyQueryKey(date),
    queryFn: () => getOccupancyByDate(date),
    enabled: date !== '',
    placeholderData: (previous) => previous,
  });
}
