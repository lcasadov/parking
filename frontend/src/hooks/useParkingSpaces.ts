import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  configureParkingSpaces,
  createParkingSpace,
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
