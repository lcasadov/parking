import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  getApprovalMode,
  getParkingAddress,
  getSettings,
  getWeekendReservable,
  updateApprovalMode,
  updateParkingAddress,
  updateWeekendReservable,
} from '../api/settingsApi';
import type { ApprovalMode, SystemSettings } from '../types/settings';

// Clave de cache del ajuste global (S1192: sin literales repetidos).
const SETTINGS_KEY = 'settings';
const APPROVAL_MODE_SCOPE = 'approval-mode';
const PARKING_ADDRESS_SCOPE = 'parking-address';
const WEEKEND_SCOPE = 'weekend-reservable';
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

// Dirección del parking, EMPLOYEE-safe (GET /settings/parking-address). La usa el
// botón "Ir al parking" de Mi Semana.
export function useParkingAddressQuery(): UseQueryResult<string | null> {
  return useQuery({
    queryKey: [SETTINGS_KEY, PARKING_ADDRESS_SCOPE],
    queryFn: getParkingAddress,
    staleTime: STALE_TIME_MS,
  });
}

// Actualiza (o borra con null/vacío) la dirección del parking (ADMIN). Refresca la
// caché admin y la lectura EMPLOYEE-safe.
export function useUpdateParkingAddress(): UseMutationResult<
  SystemSettings,
  unknown,
  string | null
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (parkingAddress: string | null) => updateParkingAddress(parkingAddress),
    onSuccess: (data) => {
      queryClient.setQueryData(settingsQueryKey(), data);
      queryClient.setQueryData([SETTINGS_KEY, PARKING_ADDRESS_SCOPE], data.parkingAddress ?? null);
    },
  });
}

// Si se admiten reservas en finde, EMPLOYEE-safe (Mi Semana + calendario de reserva).
export function useWeekendReservableQuery(): UseQueryResult<boolean> {
  return useQuery({
    queryKey: [SETTINGS_KEY, WEEKEND_SCOPE],
    queryFn: getWeekendReservable,
    staleTime: STALE_TIME_MS,
  });
}

// Activa/desactiva las reservas en finde (ADMIN); refresca caché admin + lectura.
export function useUpdateWeekendReservable(): UseMutationResult<SystemSettings, unknown, boolean> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (weekendReservable: boolean) => updateWeekendReservable(weekendReservable),
    onSuccess: (data) => {
      queryClient.setQueryData(settingsQueryKey(), data);
      queryClient.setQueryData([SETTINGS_KEY, WEEKEND_SCOPE], data.weekendReservable ?? false);
    },
  });
}
