import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  getEmployeeWeekOccupancy,
  listSelectableReleaseEmployees,
} from '../api/releaseSelectionApi';
import { createAdministrativeRelease } from '../api/releasesApi';
import { adminCancelRequest } from '../api/requestsApi';
import type { EmployeeOption, EmployeeWeekOccupancy } from '../types/releaseSelection';
import type { OccupancyOrigin } from '../types/occupancy';
import type { ResourceType } from '../types/request';

// Claves raiz de cache (S1192: sin literales repetidos).
const RELEASES_KEY = 'releases';
const EMPLOYEES_SCOPE = 'employees';
const OCCUPANCY_SCOPE = 'occupancy';
const REQUESTS_KEY = 'requests';
const OCCUPANCY_KEY = 'occupancy';

export function selectableEmployeesQueryKey(): string[] {
  return [RELEASES_KEY, EMPLOYEES_SCOPE];
}

// Empleados seleccionables para liberacion (ADMIN/AGENCIA).
export function useSelectableReleaseEmployeesQuery(): UseQueryResult<EmployeeOption[]> {
  return useQuery({
    queryKey: selectableEmployeesQueryKey(),
    queryFn: listSelectableReleaseEmployees,
  });
}

export function employeeWeekOccupancyQueryKey(
  employeeId: number | null,
  weekStart: string,
): (string | number | null)[] {
  return [RELEASES_KEY, EMPLOYEES_SCOPE, employeeId, OCCUPANCY_SCOPE, weekStart];
}

// Ocupacion semanal del empleado seleccionado. `enabled` la desactiva hasta que
// hay empleado elegido (evita disparar la consulta con id nulo).
export function useEmployeeWeekOccupancyQuery(
  employeeId: number | null,
  weekStart: string,
): UseQueryResult<EmployeeWeekOccupancy> {
  return useQuery({
    queryKey: employeeWeekOccupancyQueryKey(employeeId, weekStart),
    queryFn: () => getEmployeeWeekOccupancy(employeeId as number, weekStart),
    enabled: employeeId !== null && weekStart !== '',
    placeholderData: (previous) => previous,
  });
}

// Una reserva marcada para liberar en el lote. El origen decide el mecanismo:
// asignacion fija -> liberacion administrativa; solicitud aprobada -> admin-cancel.
export interface BatchReleaseItem {
  employeeId: number;
  resourceType: ResourceType;
  resourceId: number;
  releaseDate: string;
  origin: OccupancyOrigin;
  requestId?: number | null;
}

export interface BatchReleaseVars {
  items: BatchReleaseItem[];
  reason: string;
}

// Resultado del lote: cuantas se liberaron y cuantas fallaron (no transaccional).
export interface BatchReleaseResult {
  released: number;
  failed: number;
}

// Libera UNA reserva por su mecanismo segun el origen.
function releaseOne(item: BatchReleaseItem, reason: string): Promise<unknown> {
  if (item.origin === 'REQUEST_APPROVED' && typeof item.requestId === 'number') {
    return adminCancelRequest(item.requestId, reason);
  }
  return createAdministrativeRelease({
    employeeId: item.employeeId,
    parkingSpaceId: item.resourceId,
    resourceType: item.resourceType,
    releaseDate: item.releaseDate,
    reason,
  });
}

// Liberacion en lote con un unico motivo: recorre cada reserva marcada llamando
// al endpoint correcto por origen. No es transaccional (fuera de alcance): usa
// allSettled para tolerar errores parciales y reportar liberadas/fallidas.
export function useBatchRelease(): UseMutationResult<
  BatchReleaseResult,
  unknown,
  BatchReleaseVars
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ items, reason }: BatchReleaseVars): Promise<BatchReleaseResult> => {
      const settled = await Promise.allSettled(items.map((item) => releaseOne(item, reason)));
      const released = settled.filter((outcome) => outcome.status === 'fulfilled').length;
      return { released, failed: settled.length - released };
    },
    onSettled: () => {
      // Ambos dominios pueden haber cambiado (releases + requests) y la ocupacion.
      void queryClient.invalidateQueries({ queryKey: [RELEASES_KEY] });
      void queryClient.invalidateQueries({ queryKey: [REQUESTS_KEY] });
      void queryClient.invalidateQueries({ queryKey: [OCCUPANCY_KEY] });
    },
  });
}
