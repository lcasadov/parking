import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  createMyVehicle,
  deleteMyVehicle,
  listMyVehicles,
  updateMyVehicle,
} from '../api/myVehiclesApi';
import type { Vehicle, VehicleRequest, VehicleUpdateVars } from '../types/vehicle';

// Clave de caché de los vehículos propios del empleado (self-service).
const MY_VEHICLES_KEY = 'my-vehicles';

export function myVehiclesQueryKey(): string[] {
  return [MY_VEHICLES_KEY];
}

export function useMyVehiclesQuery(): UseQueryResult<Vehicle[]> {
  return useQuery({ queryKey: myVehiclesQueryKey(), queryFn: listMyVehicles });
}

function useInvalidateMyVehicles(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: myVehiclesQueryKey() });
  };
}

export function useCreateMyVehicle(): UseMutationResult<Vehicle, unknown, VehicleRequest> {
  const invalidate = useInvalidateMyVehicles();
  return useMutation({ mutationFn: createMyVehicle, onSuccess: invalidate });
}

export function useUpdateMyVehicle(): UseMutationResult<Vehicle, unknown, VehicleUpdateVars> {
  const invalidate = useInvalidateMyVehicles();
  return useMutation({
    mutationFn: ({ vehicleId, body }: VehicleUpdateVars) => updateMyVehicle(vehicleId, body),
    onSuccess: invalidate,
  });
}

export function useDeleteMyVehicle(): UseMutationResult<void, unknown, number> {
  const invalidate = useInvalidateMyVehicles();
  return useMutation({ mutationFn: deleteMyVehicle, onSuccess: invalidate });
}
