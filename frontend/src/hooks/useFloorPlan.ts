import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  getFloorPlan,
  requestDeskFromFloorPlan,
  updateDeskPosition,
} from '../api/floorPlanApi';
import type {
  DeskPositionUpdate,
  FloorPlanResponse,
  RequestDeskResult,
} from '../types/floorPlan';

// Clave raíz de la caché del plano (S1192: sin literales repetidos).
const FLOOR_PLAN_KEY = 'floor-plan';

export function floorPlanQueryKey(date: string): (string | boolean)[] {
  return [FLOOR_PLAN_KEY, date];
}

export function useFloorPlanQuery(
  date: string,
  enabled: boolean,
): UseQueryResult<FloorPlanResponse> {
  return useQuery({
    queryKey: floorPlanQueryKey(date),
    queryFn: () => getFloorPlan(date),
    enabled,
    placeholderData: (previous) => previous,
    retry: false,
  });
}

// Invalida el plano tras una mutación con éxito para refrescar los estados.
function useInvalidateFloorPlan(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [FLOOR_PLAN_KEY] });
  };
}

export interface RequestDeskVars {
  deskId: number;
  date: string;
}

export function useRequestDeskFromFloorPlan(): UseMutationResult<
  RequestDeskResult,
  unknown,
  RequestDeskVars
> {
  const invalidate = useInvalidateFloorPlan();
  return useMutation({
    mutationFn: ({ deskId, date }: RequestDeskVars) => requestDeskFromFloorPlan(deskId, date),
    onSuccess: invalidate,
  });
}

export interface UpdatePositionVars {
  deskId: number;
  body: DeskPositionUpdate;
}

export function useUpdateDeskPosition(): UseMutationResult<
  void,
  unknown,
  UpdatePositionVars
> {
  const invalidate = useInvalidateFloorPlan();
  return useMutation({
    mutationFn: ({ deskId, body }: UpdatePositionVars) => updateDeskPosition(deskId, body),
    onSuccess: invalidate,
  });
}
