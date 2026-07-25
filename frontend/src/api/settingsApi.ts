import { apiClient } from './apiClient';
import type { ApprovalMode, SystemSettings } from '../types/settings';

// Endpoints de SystemSettings segun docs/openapi.yaml. baseURL relativo del apiClient.

const SETTINGS = '/admin/settings';
const APPROVAL_MODE = '/settings/approval-mode';

// GET /admin/settings (ADMIN): modo de aprobacion global vigente + trazabilidad.
export async function getSettings(): Promise<SystemSettings> {
  const { data } = await apiClient.get<SystemSettings>(SETTINGS);
  return data;
}

// GET /settings/approval-mode (cualquier autenticado): modo de aprobacion vigente
// sin exigir rol ADMIN. Lo usa la solicitud unificada (EMPLOYEE) para saber si el
// modo es MANUAL y avisar de que el puesto elegido es una "preferencia" (requests
// spec). Sustituye al antiguo getApprovalModeIfAllowed (tolerante a 403 sobre el
// endpoint ADMIN-only), ya innecesario porque este endpoint es de lectura publica
// para autenticados.
export async function getApprovalMode(): Promise<ApprovalMode> {
  const { data } = await apiClient.get<{ approvalMode: ApprovalMode }>(APPROVAL_MODE);
  return data.approvalMode;
}

// PUT /admin/settings (ADMIN): conmuta el modo de aprobacion global.
export async function updateApprovalMode(approvalMode: ApprovalMode): Promise<SystemSettings> {
  const { data } = await apiClient.put<SystemSettings>(SETTINGS, { approvalMode });
  return data;
}
