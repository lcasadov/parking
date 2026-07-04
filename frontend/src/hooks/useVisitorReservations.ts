import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  cancelVisitorReservation,
  createVisitorReservation,
  listVisitorReservations,
} from '../api/visitorReservationsApi';
import type {
  PageVisitorReservation,
  VisitorReservation,
  VisitorReservationCreateRequest,
  VisitorReservationListParams,
} from '../types/visitor';

// Clave raiz de la cache de reservas de visita (S1192: sin literales repetidos).
const RESERVATIONS_KEY = 'visitor-reservations';

export function visitorReservationsQueryKey(
  params: VisitorReservationListParams,
): (string | VisitorReservationListParams)[] {
  return [RESERVATIONS_KEY, params];
}

export function useVisitorReservationsQuery(
  params: VisitorReservationListParams,
): UseQueryResult<PageVisitorReservation> {
  return useQuery({
    queryKey: visitorReservationsQueryKey(params),
    queryFn: () => listVisitorReservations(params),
    placeholderData: (previous) => previous,
  });
}

// Invalida toda la cache de reservas tras una mutacion con exito.
function useInvalidateReservations(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [RESERVATIONS_KEY] });
  };
}

export function useCreateVisitorReservation(): UseMutationResult<
  VisitorReservation,
  unknown,
  VisitorReservationCreateRequest
> {
  const invalidate = useInvalidateReservations();
  return useMutation({
    mutationFn: (body: VisitorReservationCreateRequest) => createVisitorReservation(body),
    onSuccess: invalidate,
  });
}

export function useCancelVisitorReservation(): UseMutationResult<void, unknown, number> {
  const invalidate = useInvalidateReservations();
  return useMutation({
    mutationFn: (id: number) => cancelVisitorReservation(id),
    onSuccess: invalidate,
  });
}
