import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import { getApprovalMode, getSettings, updateApprovalMode } from '../api/settingsApi';
import type { ApprovalMode, SystemSettings } from '../types/settings';

// Clave de cache del ajuste global (S1192: sin literales repetidos).
const SETTINGS_KEY = 'settings';
const APPROVAL_MODE_SCOPE = 'approval-mode';
const STALE_TIME_MS = 5 * 60 * 1000;

export function settingsQueryKey(): string[] {
  return [SETTINGS_KEY];
}

export function useSettingsQuery(): UseQueryResult<SystemSettings> {
  return useQuery({
    queryKey: settingsQueryKey(),
    queryFn: getSettings,
  });
}

// Modo de aprobacion vigente para cualquier autenticado (GET /settings/approval-mode,
// sin exigir rol ADMIN). Usado por la solicitud unificada del EMPLOYEE (requests spec:
// aviso de "preferencia" en modo MANUAL, y claridad automatico vs pendiente).
export function useApprovalModeQuery(): UseQueryResult<ApprovalMode> {
  return useQuery({
    queryKey: [SETTINGS_KEY, APPROVAL_MODE_SCOPE],
    queryFn: getApprovalMode,
    staleTime: STALE_TIME_MS,
  });
}

export function useUpdateApprovalMode(): UseMutationResult<
  SystemSettings,
  unknown,
  ApprovalMode
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (approvalMode: ApprovalMode) => updateApprovalMode(approvalMode),
    onSuccess: (data) => {
      queryClient.setQueryData(settingsQueryKey(), data);
    },
  });
}
