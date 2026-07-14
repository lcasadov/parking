import { apiClient } from './apiClient';
import type { ApprovalMode, SystemSettings } from '../types/settings';

// Endpoints de SystemSettings segun docs/openapi.yaml. baseURL relativo del apiClient.

const SETTINGS = '/admin/settings';

// GET /admin/settings (ADMIN): modo de aprobacion global vigente + trazabilidad.
export async function getSettings(): Promise<SystemSettings> {
  const { data } = await apiClient.get<SystemSettings>(SETTINGS);
  return data;
}

// PUT /admin/settings (ADMIN): conmuta el modo de aprobacion global.
export async function updateApprovalMode(approvalMode: ApprovalMode): Promise<SystemSettings> {
  const { data } = await apiClient.put<SystemSettings>(SETTINGS, { approvalMode });
  return data;
}
