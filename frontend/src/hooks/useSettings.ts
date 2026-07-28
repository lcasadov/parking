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
import { updateNotificationChannels } from '../api/pushApi';
import type { ApprovalMode, ParkingLocation, SystemSettings } from '../types/settings';

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
      // Propaga a la lectura EMPLOYEE-safe ['settings','approval-mode'] (CreateRequestModal):
      // sin esto, el aviso "Se confirmará al instante" / "Quedará pendiente" quedaba con el
      // valor anterior hasta 5 min (staleTime) tras cambiar el modo desde Ajustes.
      queryClient.setQueryData([SETTINGS_KEY, APPROVAL_MODE_SCOPE], data.approvalMode);
    },
  });
}

// Dirección del parking, EMPLOYEE-safe (GET /settings/parking-address). La usa el
// botón "Ir al parking" de Mi Semana.
export function useParkingAddressQuery(): UseQueryResult<ParkingLocation> {
  return useQuery({
    queryKey: [SETTINGS_KEY, PARKING_ADDRESS_SCOPE],
    queryFn: getParkingAddress,
    staleTime: STALE_TIME_MS,
  });
}

// Actualiza (o borra con address null/vacío) la ubicación del parking (ADMIN). Refresca
// la caché admin y la lectura EMPLOYEE-safe (dirección + coordenadas).
export function useUpdateParkingAddress(): UseMutationResult<
  SystemSettings,
  unknown,
  ParkingLocation
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (location: ParkingLocation) => updateParkingAddress(location),
    onSuccess: (data) => {
      queryClient.setQueryData(settingsQueryKey(), data);
      queryClient.setQueryData<ParkingLocation>([SETTINGS_KEY, PARKING_ADDRESS_SCOPE], {
        address: data.parkingAddress ?? null,
        lat: data.parkingLat ?? null,
        lng: data.parkingLng ?? null,
      });
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

// Interruptores globales de canal de notificación (email/push), change push-notifications.
export function useUpdateNotificationChannels(): UseMutationResult<
  SystemSettings,
  unknown,
  { email: boolean; push: boolean }
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ email, push }: { email: boolean; push: boolean }) =>
      updateNotificationChannels(email, push),
    onSuccess: (data) => {
      queryClient.setQueryData(settingsQueryKey(), data);
    },
  });
}
