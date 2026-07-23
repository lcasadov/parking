import { apiClient } from './apiClient';
import type { ApprovalMode, SystemSettings } from '../types/settings';

// Endpoints de SystemSettings segun docs/openapi.yaml. baseURL relativo del apiClient.

const SETTINGS = '/admin/settings';
const HTTP_FORBIDDEN = 403;

// GET /admin/settings (ADMIN): modo de aprobacion global vigente + trazabilidad.
export async function getSettings(): Promise<SystemSettings> {
  const { data } = await apiClient.get<SystemSettings>(SETTINGS);
  return data;
}

// GET /admin/settings tolerante a 403. El endpoint esta restringido a ADMIN
// (@PreAuthorize hasRole('ADMIN') en el backend); un EMPLOYEE recibe 403. Lo usa
// la solicitud unificada (EMPLOYEE) solo para saber si el modo es MANUAL y avisar
// de que el puesto elegido es una "preferencia" (requests spec). `validateStatus`
// acepta el 403 como respuesta valida para no lanzar ni disparar el toast global
// de "sin permisos" del interceptor Axios; se resuelve `null` (modo desconocido).
export async function getApprovalModeIfAllowed(): Promise<ApprovalMode | null> {
  const { data, status } = await apiClient.get<SystemSettings>(SETTINGS, {
    validateStatus: (value) => value === 200 || value === HTTP_FORBIDDEN,
  });
  return status === HTTP_FORBIDDEN ? null : data.approvalMode;
}

// PUT /admin/settings (ADMIN): conmuta el modo de aprobacion global.
export async function updateApprovalMode(approvalMode: ApprovalMode): Promise<SystemSettings> {
  const { data } = await apiClient.put<SystemSettings>(SETTINGS, { approvalMode });
  return data;
}
