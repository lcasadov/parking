import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  createVisitorVehicle,
  deleteVisitorVehicle,
  listVisitorVehicles,
  updateVisitorVehicle,
} from '../api/visitorVehiclesApi';
import type { VisitorVehicle, VisitorVehicleRequest } from '../types/visitorVehicle';
import type { VehiclesHooks } from '../types/vehicle';

// Clave raíz de la caché de vehículos de visitante (S1192: sin literales repetidos).
const VEHICLES_KEY = 'visitor-vehicles';

export function visitorVehiclesQueryKey(visitorId: number | null): (string | number | null)[] {
  return [VEHICLES_KEY, visitorId];
}

// Lista de vehículos del visitante. `enabled` la desactiva hasta que hay visitante (id),
// de modo que el tab no consulta durante el alta de un visitante nuevo.
export function useVisitorVehiclesQuery(visitorId: number | null): UseQueryResult<VisitorVehicle[]> {
  return useQuery({
    queryKey: visitorVehiclesQueryKey(visitorId),
    queryFn: () => listVisitorVehicles(visitorId as number),
    enabled: visitorId !== null,
  });
}

// Invalida la lista de vehículos del visitante tras una mutación con éxito.
function useInvalidateVehicles(visitorId: number): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: visitorVehiclesQueryKey(visitorId) });
  };
}

export function useCreateVisitorVehicle(
  visitorId: number,
): UseMutationResult<VisitorVehicle, unknown, VisitorVehicleRequest> {
  const invalidate = useInvalidateVehicles(visitorId);
  return useMutation({
    mutationFn: (body: VisitorVehicleRequest) => createVisitorVehicle(visitorId, body),
    onSuccess: invalidate,
  });
}

export interface UpdateVisitorVehicleVars {
  vehicleId: number;
  body: VisitorVehicleRequest;
}

export function useUpdateVisitorVehicle(
  visitorId: number,
): UseMutationResult<VisitorVehicle, unknown, UpdateVisitorVehicleVars> {
  const invalidate = useInvalidateVehicles(visitorId);
  return useMutation({
    mutationFn: ({ vehicleId, body }: UpdateVisitorVehicleVars) =>
      updateVisitorVehicle(visitorId, vehicleId, body),
    onSuccess: invalidate,
  });
}

export function useDeleteVisitorVehicle(
  visitorId: number,
): UseMutationResult<void, unknown, number> {
  const invalidate = useInvalidateVehicles(visitorId);
  return useMutation({
    mutationFn: (vehicleId: number) => deleteVisitorVehicle(visitorId, vehicleId),
    onSuccess: invalidate,
  });
}

// Bundle de hooks que consume el panel genérico de vehículos (VehiclesPanel) para visitantes.
export const visitorVehiclesHooks: VehiclesHooks = {
  useList: useVisitorVehiclesQuery,
  useCreate: useCreateVisitorVehicle,
  useUpdate: useUpdateVisitorVehicle,
  useDelete: useDeleteVisitorVehicle,
};
