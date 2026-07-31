import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  approveVehicle,
  changeVehicleStatus,
  confirmVehicleDeletion,
  listVehicleReview,
  markVehicleInProgress,
  pendingVehicleCount,
  rejectVehicle,
  restoreVehicle,
  vehicleHistory,
  vehicleStatusCounts,
  type VehicleStatusCounts,
} from '../api/employeeVehicleReviewApi';
import type {
  VehicleHistoryEntry,
  VehicleReviewPageData,
  VehicleReviewParams,
  VehicleReviewRow,
} from '../types/employeeVehicleReview';
import type { VehicleStatus } from '../types/vehicle';

const REVIEW_KEY = 'employee-vehicle-review';
const COUNT_KEY = 'employee-vehicle-pending-count';
const COUNTS_KEY = 'employee-vehicle-status-counts';
const HISTORY_KEY = 'employee-vehicle-history';

export function useVehicleReviewQuery(params: VehicleReviewParams): UseQueryResult<VehicleReviewPageData> {
  return useQuery({
    queryKey: [REVIEW_KEY, (params.statuses ?? []).join(',') || 'ALL', params.page ?? 0],
    queryFn: () => listVehicleReview(params),
  });
}

export function usePendingVehicleCountQuery(): UseQueryResult<number> {
  return useQuery({ queryKey: [COUNT_KEY], queryFn: pendingVehicleCount });
}

export function useVehicleStatusCountsQuery(): UseQueryResult<VehicleStatusCounts> {
  return useQuery({ queryKey: [COUNTS_KEY], queryFn: vehicleStatusCounts });
}

export function useVehicleHistoryQuery(vehicleId: number | null): UseQueryResult<VehicleHistoryEntry[]> {
  return useQuery({
    queryKey: [HISTORY_KEY, vehicleId],
    queryFn: () => vehicleHistory(vehicleId as number),
    enabled: vehicleId !== null,
  });
}

// Invalida el listado de la bandeja + el contador de pendientes tras una decisión.
function useInvalidateReview(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [REVIEW_KEY] });
    void queryClient.invalidateQueries({ queryKey: [COUNT_KEY] });
    void queryClient.invalidateQueries({ queryKey: [COUNTS_KEY] });
  };
}

function useReviewAction<TResult>(
  action: (vehicleId: number) => Promise<TResult>,
): UseMutationResult<TResult, unknown, number> {
  const invalidate = useInvalidateReview();
  return useMutation({ mutationFn: action, onSuccess: invalidate });
}

export function useMarkVehicleInProgress(): UseMutationResult<VehicleReviewRow, unknown, number> {
  return useReviewAction(markVehicleInProgress);
}

export function useApproveVehicle(): UseMutationResult<VehicleReviewRow, unknown, number> {
  return useReviewAction(approveVehicle);
}

export interface RejectVehicleVars {
  vehicleId: number;
  reason: string;
}

export function useRejectVehicle(): UseMutationResult<VehicleReviewRow, unknown, RejectVehicleVars> {
  const invalidate = useInvalidateReview();
  return useMutation({
    mutationFn: ({ vehicleId, reason }: RejectVehicleVars) => rejectVehicle(vehicleId, reason),
    onSuccess: invalidate,
  });
}

export function useConfirmVehicleDeletion(): UseMutationResult<void, unknown, number> {
  return useReviewAction(confirmVehicleDeletion);
}

export function useRestoreVehicle(): UseMutationResult<VehicleReviewRow, unknown, number> {
  return useReviewAction(restoreVehicle);
}

export interface ChangeStatusVars {
  vehicleId: number;
  status: VehicleStatus;
  reason?: string;
}

export function useChangeVehicleStatus(): UseMutationResult<VehicleReviewRow, unknown, ChangeStatusVars> {
  const invalidate = useInvalidateReview();
  return useMutation({
    mutationFn: ({ vehicleId, status, reason }: ChangeStatusVars) =>
      changeVehicleStatus(vehicleId, status, reason),
    onSuccess: invalidate,
  });
}
