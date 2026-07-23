import { toIsoDate } from './requests';
import type { CalendarCellState, MyWeekDayState } from '../types/calendar';

// Utilidades de fecha para las vistas de calendario. Todas trabajan en fecha
// local sin componente horario (formato YYYY-MM-DD), coherente con el contrato.

// Valida un ISO date estricto (YYYY-MM-DD) y que represente una fecha real.
export function isValidIsoDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }
  const parsed = new Date(`${value}T00:00:00`);
  return !Number.isNaN(parsed.getTime()) && toIsoDate(parsed) === value;
}

// Lunes (ISO) de la semana que contiene la fecha dada (por defecto hoy).
export function mondayOfWeek(now: Date = new Date()): string {
  const local = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const weekday = local.getDay(); // 0 domingo .. 6 sabado
  const shiftToMonday = weekday === 0 ? -6 : 1 - weekday;
  local.setDate(local.getDate() + shiftToMonday);
  return toIsoDate(local);
}

// Suma dias a una fecha ISO y devuelve el nuevo ISO.
export function addDaysIso(dateIso: string, days: number): string {
  const parsed = new Date(`${dateIso}T00:00:00`);
  parsed.setDate(parsed.getDate() + days);
  return toIsoDate(parsed);
}

// Clave i18n del estado de celda del calendario admin.
export function calendarStateKey(state: CalendarCellState): string {
  return `calendar.states.${state}`;
}

// Clave i18n del estado de un dia de "Mi Semana".
export function myWeekStateKey(state: MyWeekDayState): string {
  return `calendar.myWeek.states.${state}`;
}

// Clase CSS del estado de celda (kebab-case): ASSIGNED -> cell-assigned, etc.
export function cellStateClass(state: CalendarCellState | MyWeekDayState): string {
  return `cell-${state.toLowerCase().replace(/_/g, '-')}`;
}

// Mapa estado de celda -> clase del design system (contrato §4, tokens --state-*).
// Reutiliza el mapa unico estado->color: ocupado=verde, liberado=azul,
// pendiente=amber, solicitud=naranja, libre=dashed. Sin hex sueltos.
const CALENDAR_STATE_CLASS: Record<CalendarCellState, string> = {
  ASSIGNED: 'state-occupied',
  RELEASED: 'state-released',
  REQUEST_PENDING: 'state-pending',
  REQUEST_APPROVED: 'state-request',
  FREE: 'state-free',
};

// Clase del design system (contrato §4) para el estado de una celda del calendario.
export function calendarStateClass(state: CalendarCellState): string {
  return CALENDAR_STATE_CLASS[state];
}

// Indice de dia de la semana (0 domingo .. 6 sabado) de una fecha ISO.
export function weekdayIndex(dateIso: string): number {
  return new Date(`${dateIso}T00:00:00`).getDay();
}

// Dia de la semana ISO-8601 (1 lunes .. 7 domingo) de una fecha ISO. Lo usan las
// asignaciones fijas, cuyo `dayOfWeek` sigue el estandar ISO (no el 0..6 de JS).
export function isoWeekday(dateIso: string): number {
  const jsDay = weekdayIndex(dateIso);
  return jsDay === 0 ? 7 : jsDay;
}

// Formato corto dia/mes (DD/MM) sin depender de la zona horaria.
export function dayMonth(dateIso: string): string {
  const [, month, day] = dateIso.split('-');
  return `${day}/${month}`;
}

// Numero de semana ISO-8601 (lunes como primer dia; semana 1 = la del primer
// jueves del año) a partir de un ISO date. Solo presentacion.
export function isoWeekNumber(dateIso: string): number {
  const date = new Date(`${dateIso}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return 0;
  }
  // Jueves de la semana actual (define el año ISO al que pertenece la semana).
  const dayNr = (date.getDay() + 6) % 7; // lunes=0 .. domingo=6
  date.setDate(date.getDate() - dayNr + 3);
  const firstThursday = new Date(date.getFullYear(), 0, 4);
  const firstDayNr = (firstThursday.getDay() + 6) % 7;
  firstThursday.setDate(firstThursday.getDate() - firstDayNr + 3);
  const msPerWeek = 7 * 24 * 60 * 60 * 1000;
  return 1 + Math.round((date.getTime() - firstThursday.getTime()) / msPerWeek);
}

// Rango de fechas de la semana (primer .. ultimo dia) formateado y localizado,
// p. ej. "11 may – 15 may 2026". Para el titulo serif del navegador de semana.
export function weekRangeLabel(startIso: string, endIso: string, locale: string): string {
  const start = new Date(`${startIso}T00:00:00`);
  const end = new Date(`${endIso}T00:00:00`);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    return `${startIso} – ${endIso}`;
  }
  const dayMonthFmt = new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short' });
  const fullFmt = new Intl.DateTimeFormat(locale, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
  return `${dayMonthFmt.format(start)} – ${fullFmt.format(end)}`;
}

// Formato largo localizado (p. ej. "sábado, 5 de julio de 2026" / "Saturday,
// July 5, 2026") a partir de un ISO date, sin desfase de zona horaria.
export function longDate(dateIso: string, locale: string): string {
  const parsed = new Date(`${dateIso}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) {
    return dateIso;
  }
  return new Intl.DateTimeFormat(locale, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(parsed);
}
