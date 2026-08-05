import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  createEmployeeVehicle,
  deleteEmployeeVehicle,
  listEmployeeVehicles,
  updateEmployeeVehicle,
} from '../api/employeeVehiclesApi';
import type { EmployeeVehicle, EmployeeVehicleRequest } from '../types/employeeVehicle';
import type { VehiclesHooks } from '../types/vehicle';

// Clave raíz de la caché de vehículos de empleado (S1192: sin literales repetidos).
const VEHICLES_KEY = 'employee-vehicles';

export function employeeVehiclesQueryKey(employeeId: number | null): (string | number | null)[] {
  return [VEHICLES_KEY, employeeId];
}

// Lista de vehículos del empleado. `enabled` la desactiva hasta que hay empleado (id),
// de modo que el tab no consulta durante el alta de un empleado nuevo.
export function useEmployeeVehiclesQuery(
  employeeId: number | null,
): UseQueryResult<EmployeeVehicle[]> {
  return useQuery({
    queryKey: employeeVehiclesQueryKey(employeeId),
    queryFn: () => listEmployeeVehicles(employeeId as number),
    enabled: employeeId !== null,
  });
}

// Invalida la lista de vehículos del empleado tras una mutación con éxito.
function useInvalidateVehicles(employeeId: number): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: employeeVehiclesQueryKey(employeeId) });
  };
}

export function useCreateEmployeeVehicle(
  employeeId: number,
): UseMutationResult<EmployeeVehicle, unknown, EmployeeVehicleRequest> {
  const invalidate = useInvalidateVehicles(employeeId);
  return useMutation({
    mutationFn: (body: EmployeeVehicleRequest) => createEmployeeVehicle(employeeId, body),
    onSuccess: invalidate,
  });
}

export interface UpdateEmployeeVehicleVars {
  vehicleId: number;
  body: EmployeeVehicleRequest;
}

export function useUpdateEmployeeVehicle(
  employeeId: number,
): UseMutationResult<EmployeeVehicle, unknown, UpdateEmployeeVehicleVars> {
  const invalidate = useInvalidateVehicles(employeeId);
  return useMutation({
    mutationFn: ({ vehicleId, body }: UpdateEmployeeVehicleVars) =>
      updateEmployeeVehicle(employeeId, vehicleId, body),
    onSuccess: invalidate,
  });
}

export function useDeleteEmployeeVehicle(
  employeeId: number,
): UseMutationResult<void, unknown, number> {
  const invalidate = useInvalidateVehicles(employeeId);
  return useMutation({
    mutationFn: (vehicleId: number) => deleteEmployeeVehicle(employeeId, vehicleId),
    onSuccess: invalidate,
  });
}

// Bundle de hooks que consume el panel genérico de vehículos (VehiclesPanel) para empleados.
export const employeeVehiclesHooks: VehiclesHooks = {
  useList: useEmployeeVehiclesQuery,
  useCreate: useCreateEmployeeVehicle,
  useUpdate: useUpdateEmployeeVehicle,
  useDelete: useDeleteEmployeeVehicle,
};
