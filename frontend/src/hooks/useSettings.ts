import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import { getSettings, updateApprovalMode } from '../api/settingsApi';
import type { ApprovalMode, SystemSettings } from '../types/settings';

// Clave de cache del ajuste global (S1192: sin literales repetidos).
const SETTINGS_KEY = 'settings';

export function settingsQueryKey(): string[] {
  return [SETTINGS_KEY];
}

export function useSettingsQuery(): UseQueryResult<SystemSettings> {
  return useQuery({
    queryKey: settingsQueryKey(),
    queryFn: getSettings,
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
