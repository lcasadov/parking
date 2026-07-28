import { apiClient } from './apiClient';
import type { ApprovalMode, ParkingLocation, SystemSettings } from '../types/settings';

// Endpoints de SystemSettings segun docs/openapi.yaml. baseURL relativo del apiClient.

const SETTINGS = '/admin/settings';
const APPROVAL_MODE = '/settings/approval-mode';
const PARKING_ADDRESS = '/settings/parking-address';
const ADMIN_PARKING_ADDRESS = '/admin/settings/parking-address';
const WEEKEND_RESERVABLE = '/settings/weekend-reservable';
const ADMIN_WEEKEND_RESERVABLE = '/admin/settings/weekend-reservable';

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

// GET /settings/parking-address (cualquier autenticado): ubicación del parking para
// el botón "Ir al parking" del empleado. Endpoint EMPLOYEE-safe (el catálogo admin no).
// Devuelve dirección + coordenadas opcionales del punto exacto fijado en el mapa.
export async function getParkingAddress(): Promise<ParkingLocation> {
  const { data } = await apiClient.get<{
    parkingAddress: string | null;
    parkingLat: number | null;
    parkingLng: number | null;
  }>(PARKING_ADDRESS);
  return {
    address: data.parkingAddress,
    lat: data.parkingLat ?? null,
    lng: data.parkingLng ?? null,
  };
}

// PUT /admin/settings/parking-address (ADMIN): fija o borra (address null/vacío) la
// ubicación. Las coordenadas son opcionales (punto exacto del mapa); se descartan si
// no hay dirección.
export async function updateParkingAddress(
  location: ParkingLocation,
): Promise<SystemSettings> {
  const { data } = await apiClient.put<SystemSettings>(ADMIN_PARKING_ADDRESS, {
    parkingAddress: location.address,
    parkingLat: location.lat,
    parkingLng: location.lng,
  });
  return data;
}

// GET /settings/weekend-reservable (cualquier autenticado): si se admite reservar
// en fin de semana. Lo usan Mi Semana y el calendario de reserva para ocultar/
// deshabilitar sábados y domingos.
export async function getWeekendReservable(): Promise<boolean> {
  const { data } = await apiClient.get<{ weekendReservable: boolean }>(WEEKEND_RESERVABLE);
  return data.weekendReservable;
}

// PUT /admin/settings/weekend-reservable (ADMIN): activa/desactiva las reservas en finde.
export async function updateWeekendReservable(
  weekendReservable: boolean,
): Promise<SystemSettings> {
  const { data } = await apiClient.put<SystemSettings>(ADMIN_WEEKEND_RESERVABLE, {
    weekendReservable,
  });
  return data;
}
