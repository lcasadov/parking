import { apiClient } from './apiClient';
import type {
  AdminWeeklyCalendarResponse,
  AvailabilityResponse,
  MyWeekResponse,
} from '../types/calendar';

// Endpoints de Availability / Calendar segun docs/openapi.yaml.
// baseURL relativo del apiClient.

const AVAILABILITY = '/availability';
const CALENDAR_ADMIN = '/calendar/admin';
const CALENDAR_MY_WEEK = '/calendar/my-week';

// GET /availability?date=YYYY-MM-DD: recursos disponibles para una fecha.
export async function getAvailability(date: string): Promise<AvailabilityResponse> {
  const { data } = await apiClient.get<AvailabilityResponse>(AVAILABILITY, {
    params: { date },
  });
  return data;
}

// GET /calendar/admin?weekStart=YYYY-MM-DD (ADMIN): calendario semanal completo.
export async function getAdminCalendar(
  weekStart: string,
): Promise<AdminWeeklyCalendarResponse> {
  const { data } = await apiClient.get<AdminWeeklyCalendarResponse>(CALENDAR_ADMIN, {
    params: { weekStart },
  });
  return data;
}

// GET /calendar/my-week[?weekStart=YYYY-MM-DD]: vista personal; weekStart opcional
// (semana actual si se omite). Nunca expone nombres de terceros.
export async function getMyWeek(weekStart?: string): Promise<MyWeekResponse> {
  const params = weekStart ? { weekStart } : undefined;
  const { data } = await apiClient.get<MyWeekResponse>(CALENDAR_MY_WEEK, { params });
  return data;
}
