import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  approveRequest,
  cancelRequest,
  createRequest,
  listMyRequests,
  listPendingRequests,
  listRequestsByStatus,
  rejectRequest,
} from '../api/requestsApi';
import type {
  PageRequest,
  Request,
  RequestApproveRequest,
  RequestCreateRequest,
  RequestListParams,
  RequestRejectRequest,
} from '../types/request';

// Claves raiz de cache (S1192: sin literales repetidos).
const REQUESTS_KEY = 'requests';
const MINE_SCOPE = 'mine';
const PENDING_SCOPE = 'pending';
const BY_STATUS_SCOPE = 'byStatus';

export function myRequestsQueryKey(
  params: RequestListParams,
): (string | RequestListParams)[] {
  return [REQUESTS_KEY, MINE_SCOPE, params];
}

export function pendingRequestsQueryKey(
  params: RequestListParams,
): (string | RequestListParams)[] {
  return [REQUESTS_KEY, PENDING_SCOPE, params];
}

export function useMyRequestsQuery(params: RequestListParams): UseQueryResult<PageRequest> {
  return useQuery({
    queryKey: myRequestsQueryKey(params),
    queryFn: () => listMyRequests(params),
    placeholderData: (previous) => previous,
  });
}

export function usePendingRequestsQuery(
  params: RequestListParams,
): UseQueryResult<PageRequest> {
  return useQuery({
    queryKey: pendingRequestsQueryKey(params),
    queryFn: () => listPendingRequests(params),
    placeholderData: (previous) => previous,
  });
}

export function requestsByStatusQueryKey(
  params: RequestListParams,
): (string | RequestListParams)[] {
  return [REQUESTS_KEY, BY_STATUS_SCOPE, params];
}

// Listado admin por estado (aprobadas / rechazadas / todas). `enabled` evita el fetch
// cuando la pestaña activa es la de pendientes (que usa usePendingRequestsQuery, FIFO).
export function useRequestsByStatusQuery(
  params: RequestListParams,
  enabled: boolean,
): UseQueryResult<PageRequest> {
  return useQuery({
    queryKey: requestsByStatusQueryKey(params),
    queryFn: () => listRequestsByStatus(params),
    enabled,
    placeholderData: (previous) => previous,
  });
}

// Invalida toda la cache de solicitudes tras una mutacion con exito.
function useInvalidateRequests(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [REQUESTS_KEY] });
  };
}

export function useCreateRequest(): UseMutationResult<Request, unknown, RequestCreateRequest> {
  const invalidate = useInvalidateRequests();
  return useMutation({
    mutationFn: (body: RequestCreateRequest) => createRequest(body),
    onSuccess: invalidate,
  });
}

export function useCancelRequest(): UseMutationResult<Request, unknown, number> {
  const invalidate = useInvalidateRequests();
  return useMutation({
    mutationFn: (id: number) => cancelRequest(id),
    onSuccess: invalidate,
  });
}

export interface ApproveRequestVars {
  id: number;
  body: RequestApproveRequest;
}

export function useApproveRequest(): UseMutationResult<Request, unknown, ApproveRequestVars> {
  const invalidate = useInvalidateRequests();
  return useMutation({
    mutationFn: ({ id, body }: ApproveRequestVars) => approveRequest(id, body),
    onSuccess: invalidate,
  });
}

export interface RejectRequestVars {
  id: number;
  body: RequestRejectRequest;
}

export function useRejectRequest(): UseMutationResult<Request, unknown, RejectRequestVars> {
  const invalidate = useInvalidateRequests();
  return useMutation({
    mutationFn: ({ id, body }: RejectRequestVars) => rejectRequest(id, body),
    onSuccess: invalidate,
  });
}
