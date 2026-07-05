import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { getAdminCalendar, getAvailability, getMyWeek } from '../api/calendarApi';
import type {
  AdminWeeklyCalendarResponse,
  AvailabilityResponse,
  MyWeekResponse,
} from '../types/calendar';
import type { ResourceType } from '../types/request';
import { isValidIsoDate } from '../utils/calendar';
import { isWithinWindow } from '../utils/requests';

// Claves raiz de cache (S1192: sin literales repetidos).
const CALENDAR_KEY = 'calendar';
const AVAILABILITY_SCOPE = 'availability';
const ADMIN_SCOPE = 'admin';
const MY_WEEK_SCOPE = 'my-week';

export function availabilityQueryKey(date: string): string[] {
  return [CALENDAR_KEY, AVAILABILITY_SCOPE, date];
}

export function resourceAvailabilityQueryKey(date: string, resourceType: ResourceType): string[] {
  return [CALENDAR_KEY, AVAILABILITY_SCOPE, resourceType, date];
}

export function adminCalendarQueryKey(weekStart: string): string[] {
  return [CALENDAR_KEY, ADMIN_SCOPE, weekStart];
}

export function myWeekQueryKey(weekStart?: string): string[] {
  return [CALENDAR_KEY, MY_WEEK_SCOPE, weekStart ?? 'current'];
}

// Disponibilidad por fecha; solo consulta cuando la fecha ISO es valida.
export function useAvailabilityQuery(date: string): UseQueryResult<AvailabilityResponse> {
  return useQuery({
    queryKey: availabilityQueryKey(date),
    queryFn: () => getAvailability(date),
    enabled: isValidIsoDate(date),
  });
}

// Disponibilidad por recurso (plaza/puesto) para una fecha, usada como banner
// informativo en la solicitud unificada. Solo consulta si la fecha cae dentro de
// la ventana de reserva (hoy..+14d).
export function useResourceAvailabilityQuery(
  date: string,
  resourceType: ResourceType,
): UseQueryResult<AvailabilityResponse> {
  return useQuery({
    queryKey: resourceAvailabilityQueryKey(date, resourceType),
    queryFn: () => getAvailability(date, resourceType),
    enabled: isValidIsoDate(date) && isWithinWindow(date),
  });
}

// Calendario semanal admin; solo consulta con un weekStart valido.
export function useAdminCalendarQuery(
  weekStart: string,
): UseQueryResult<AdminWeeklyCalendarResponse> {
  return useQuery({
    queryKey: adminCalendarQueryKey(weekStart),
    queryFn: () => getAdminCalendar(weekStart),
    enabled: isValidIsoDate(weekStart),
    placeholderData: (previous) => previous,
  });
}

// Mi Semana; weekStart opcional (semana actual si se omite).
export function useMyWeekQuery(weekStart?: string): UseQueryResult<MyWeekResponse> {
  return useQuery({
    queryKey: myWeekQueryKey(weekStart),
    queryFn: () => getMyWeek(weekStart),
    placeholderData: (previous) => previous,
  });
}
