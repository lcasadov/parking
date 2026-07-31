import { apiClient } from './apiClient';
import type {
  VehicleHistoryEntry,
  VehicleReviewPageData,
  VehicleReviewParams,
  VehicleReviewRow,
} from '../types/employeeVehicleReview';
import type { VehicleStatus } from '../types/vehicle';

// Endpoints de la bandeja de validación de vehículos (ADMIN, change
// employee-vehicle-self-service, Fase 2). baseURL relativo del apiClient (.../api/v1).
const BASE = '/employee-vehicles';

export async function listVehicleReview(params: VehicleReviewParams): Promise<VehicleReviewPageData> {
  const { data } = await apiClient.get<VehicleReviewPageData>(BASE, {
    params: {
      // Spring vincula una lista CSV (`status=A,B`) a List<VehicleStatus>.
      status: params.statuses && params.statuses.length > 0 ? params.statuses.join(',') : undefined,
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'createdAt,asc',
    },
  });
  return data;
}

export async function pendingVehicleCount(): Promise<number> {
  const { data } = await apiClient.get<{ count: number }>(`${BASE}/pending-count`);
  return data.count;
}

export type VehicleStatusCounts = Partial<Record<VehicleStatus, number>>;

export async function vehicleStatusCounts(): Promise<VehicleStatusCounts> {
  const { data } = await apiClient.get<VehicleStatusCounts>(`${BASE}/counts`);
  return data;
}

export async function vehicleHistory(vehicleId: number): Promise<VehicleHistoryEntry[]> {
  const { data } = await apiClient.get<VehicleHistoryEntry[]>(`${BASE}/${vehicleId}/history`);
  return data;
}

export async function markVehicleInProgress(vehicleId: number): Promise<VehicleReviewRow> {
  const { data } = await apiClient.post<VehicleReviewRow>(`${BASE}/${vehicleId}/in-progress`);
  return data;
}

export async function approveVehicle(vehicleId: number): Promise<VehicleReviewRow> {
  const { data } = await apiClient.post<VehicleReviewRow>(`${BASE}/${vehicleId}/approve`);
  return data;
}

export async function rejectVehicle(vehicleId: number, reason: string): Promise<VehicleReviewRow> {
  const { data } = await apiClient.post<VehicleReviewRow>(`${BASE}/${vehicleId}/reject`, { reason });
  return data;
}

export async function confirmVehicleDeletion(vehicleId: number): Promise<void> {
  await apiClient.post(`${BASE}/${vehicleId}/confirm-deletion`);
}

export async function restoreVehicle(vehicleId: number): Promise<VehicleReviewRow> {
  const { data } = await apiClient.post<VehicleReviewRow>(`${BASE}/${vehicleId}/restore`);
  return data;
}

export async function changeVehicleStatus(
  vehicleId: number,
  status: VehicleStatus,
  reason?: string,
): Promise<VehicleReviewRow> {
  const { data } = await apiClient.post<VehicleReviewRow>(`${BASE}/${vehicleId}/status`, {
    status,
    reason,
  });
  return data;
}
