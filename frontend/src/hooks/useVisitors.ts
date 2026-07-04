import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  createVisitor,
  getVisitor,
  listVisitors,
  updateVisitor,
} from '../api/visitorsApi';
import type {
  PageVisitor,
  Visitor,
  VisitorCreateRequest,
  VisitorListParams,
} from '../types/visitor';

// Clave raiz de la cache de visitantes (S1192: sin literales repetidos).
const VISITORS_KEY = 'visitors';

export function visitorsQueryKey(params: VisitorListParams): (string | VisitorListParams)[] {
  return [VISITORS_KEY, params];
}

export function useVisitorsQuery(params: VisitorListParams): UseQueryResult<PageVisitor> {
  return useQuery({
    queryKey: visitorsQueryKey(params),
    queryFn: () => listVisitors(params),
    placeholderData: (previous) => previous,
  });
}

export function useVisitorQuery(id: number | null): UseQueryResult<Visitor> {
  return useQuery({
    queryKey: [VISITORS_KEY, 'detail', id],
    queryFn: () => getVisitor(id as number),
    enabled: id !== null,
  });
}

// Invalida toda la cache de visitantes tras una mutacion con exito.
function useInvalidateVisitors(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [VISITORS_KEY] });
  };
}

export function useCreateVisitor(): UseMutationResult<Visitor, unknown, VisitorCreateRequest> {
  const invalidate = useInvalidateVisitors();
  return useMutation({
    mutationFn: (body: VisitorCreateRequest) => createVisitor(body),
    onSuccess: invalidate,
  });
}

export interface UpdateVisitorVars {
  id: number;
  body: VisitorCreateRequest;
}

export function useUpdateVisitor(): UseMutationResult<Visitor, unknown, UpdateVisitorVars> {
  const invalidate = useInvalidateVisitors();
  return useMutation({
    mutationFn: ({ id, body }: UpdateVisitorVars) => updateVisitor(id, body),
    onSuccess: invalidate,
  });
}
