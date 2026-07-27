import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  adminAssignRequest,
  adminCancelRequest,
  approveRequest,
  cancelRequest,
  createRequest,
  getSuggestedResource,
  listMyRequests,
  listPendingRequests,
  listRequestsByStatus,
  rejectRequest,
  resendRequest,
} from '../api/requestsApi';
import { isValidIsoDate } from '../utils/calendar';
import { isTodayOrFuture } from '../utils/requests';
import type {
  PageRequest,
  Request,
  RequestAdminAssignRequest,
  RequestApproveRequest,
  RequestCreateRequest,
  RequestListParams,
  RequestRejectRequest,
  ResourceType,
  SuggestedResource,
} from '../types/request';

// Claves raiz de cache (S1192: sin literales repetidos).
const REQUESTS_KEY = 'requests';
// Crear/cancelar una solicitud cambia el estado del dia en el calendario
// (Mi Semana) y la disponibilidad, ambos bajo 'calendar'. Sin invalidarlo, el
// modal de reserva leia un estado obsoleto (p.ej. ASSIGNED tras liberar) y no
// dejaba volver a reservar el recurso liberado.
const CALENDAR_KEY = 'calendar';
const MINE_SCOPE = 'mine';
const PENDING_SCOPE = 'pending';
const BY_STATUS_SCOPE = 'byStatus';
const SUGGESTED_SCOPE = 'suggested';

// Preview EMPLOYEE-safe del recurso que la auto-asignación daría al empleado ese
// día (Feature: feedback de auto-asignación). Solo consulta con fecha válida y
// futura/hoy, y cuando `enabled` (el recurso está seleccionado en el modal).
export function useSuggestedResourceQuery(
  date: string,
  resourceType: ResourceType,
  enabled: boolean,
): UseQueryResult<SuggestedResource> {
  return useQuery({
    queryKey: [REQUESTS_KEY, SUGGESTED_SCOPE, resourceType, date],
    queryFn: () => getSuggestedResource(date, resourceType),
    enabled: enabled && isValidIsoDate(date) && isTodayOrFuture(date),
  });
}

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

// Invalida la cache de solicitudes Y la del calendario (Mi Semana +
// disponibilidad) tras una mutacion con exito: crear/cancelar una solicitud
// cambia ambos dominios.
function useInvalidateRequests(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [REQUESTS_KEY] });
    void queryClient.invalidateQueries({ queryKey: [CALENDAR_KEY] });
  };
}

export function useCreateRequest(): UseMutationResult<Request, unknown, RequestCreateRequest> {
  const invalidate = useInvalidateRequests();
  return useMutation({
    mutationFn: (body: RequestCreateRequest) => createRequest(body),
    onSuccess: invalidate,
  });
}

// Asignacion puntual del admin (capability admin-punctual-assignment): crea una
// Request que nace APPROVED. Invalida solicitudes y calendario para reflejar la
// ocupacion inmediatamente en la rejilla de Ocupacion.
export function useAdminAssignRequest(): UseMutationResult<
  Request,
  unknown,
  RequestAdminAssignRequest
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: RequestAdminAssignRequest) => adminAssignRequest(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [REQUESTS_KEY] });
      void queryClient.invalidateQueries({ queryKey: [CALENDAR_KEY] });
    },
  });
}

export function useCancelRequest(): UseMutationResult<Request, unknown, number> {
  const invalidate = useInvalidateRequests();
  return useMutation({
    mutationFn: (id: number) => cancelRequest(id),
    onSuccess: invalidate,
  });
}

export interface AdminCancelRequestVars {
  id: number;
  reason: string;
}

// Cancelacion administrativa de una solicitud APPROVED futura (ADMIN): libera el
// recurso ocupado por la solicitud para esa fecha (change release-occupied-resource).
export function useAdminCancelRequest(): UseMutationResult<Request, unknown, AdminCancelRequestVars> {
  const invalidate = useInvalidateRequests();
  return useMutation({
    mutationFn: ({ id, reason }: AdminCancelRequestVars) => adminCancelRequest(id, reason),
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

// Reenvio de aviso a los admins (PendingConfirmationBanner, EMPLOYEE dueño de la
// solicitud PENDING). Invalida "mis solicitudes" y el calendario ("Mi Semana"
// consume CALENDAR_KEY) para reflejar el nuevo lastRemindedAt.
export function useResendRequest(): UseMutationResult<Request, unknown, number> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => resendRequest(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [REQUESTS_KEY] });
      void queryClient.invalidateQueries({ queryKey: [CALENDAR_KEY] });
    },
  });
}
