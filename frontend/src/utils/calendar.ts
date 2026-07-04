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

// Indice de dia de la semana (0 domingo .. 6 sabado) de una fecha ISO.
export function weekdayIndex(dateIso: string): number {
  return new Date(`${dateIso}T00:00:00`).getDay();
}

// Formato corto dia/mes (DD/MM) sin depender de la zona horaria.
export function dayMonth(dateIso: string): string {
  const [, month, day] = dateIso.split('-');
  return `${day}/${month}`;
}
