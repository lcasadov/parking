import {
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  configureParkingSpaces,
  createParkingSpace,
  getParkingSpace,
  listParkingSpaces,
  updateParkingSpace,
} from '../api/parkingSpacesApi';
import type {
  ParkingSpace,
  ParkingSpaceConfigureRequest,
  ParkingSpaceCreate,
  ParkingSpaceListParams,
  PageParkingSpace,
} from '../types/parkingSpace';

// Clave raiz de la cache de plazas (S1192: sin literales repetidos).
const PARKING_SPACES_KEY = 'parking-spaces';

export function parkingSpacesQueryKey(
  params: ParkingSpaceListParams,
): (string | ParkingSpaceListParams)[] {
  return [PARKING_SPACES_KEY, params];
}

export function useParkingSpacesQuery(
  params: ParkingSpaceListParams,
): UseQueryResult<PageParkingSpace> {
  return useQuery({
    queryKey: parkingSpacesQueryKey(params),
    queryFn: () => listParkingSpaces(params),
    placeholderData: (previous) => previous,
  });
}

// Resuelve el detalle (numero/label) de varias plazas por id en paralelo. Usado
// por vistas EMPLOYEE que solo conocen el resource_id de su plaza fija: GET
// /parking-spaces/{id} es accesible a EMPLOYEE, a diferencia del catalogo
// (analogo a useDesksByIdsQuery en hooks/useDesks.ts).
export function useParkingSpacesByIdsQuery(ids: number[]): Record<number, ParkingSpace> {
  const results = useQueries({
    queries: ids.map((id) => ({
      queryKey: [PARKING_SPACES_KEY, 'detail', id],
      queryFn: () => getParkingSpace(id),
    })),
  });
  const byId: Record<number, ParkingSpace> = {};
  results.forEach((result, index) => {
    if (result.data) {
      byId[ids[index]] = result.data;
    }
  });
  return byId;
}

// Invalida toda la cache de plazas tras una mutacion con exito.
function useInvalidateParkingSpaces(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [PARKING_SPACES_KEY] });
  };
}

export function useCreateParkingSpace(): UseMutationResult<ParkingSpace, unknown, ParkingSpaceCreate> {
  const invalidate = useInvalidateParkingSpaces();
  return useMutation({
    mutationFn: (body: ParkingSpaceCreate) => createParkingSpace(body),
    onSuccess: invalidate,
  });
}

export interface UpdateParkingSpaceVars {
  id: number;
  body: ParkingSpaceCreate;
}

export function useUpdateParkingSpace(): UseMutationResult<
  ParkingSpace,
  unknown,
  UpdateParkingSpaceVars
> {
  const invalidate = useInvalidateParkingSpaces();
  return useMutation({
    mutationFn: ({ id, body }: UpdateParkingSpaceVars) => updateParkingSpace(id, body),
    onSuccess: invalidate,
  });
}

export function useConfigureParkingSpaces(): UseMutationResult<
  ParkingSpace[],
  unknown,
  ParkingSpaceConfigureRequest
> {
  const invalidate = useInvalidateParkingSpaces();
  return useMutation({
    mutationFn: (body: ParkingSpaceConfigureRequest) => configureParkingSpaces(body),
    onSuccess: invalidate,
  });
}
