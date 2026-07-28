import { addDaysIso, isoWeekday } from './calendar';
import { toIsoDate, todayIso } from './requests';
import type { DateMode, ParkingChoice, WizardState } from '../components/wizard/wizardTypes';

// Una celda del calendario mensual: fecha ISO + si pertenece al mes mostrado.
export interface MonthCell {
  iso: string;
  inMonth: boolean;
}

// Primer día (ISO) del mes de una fecha dada.
export function firstOfMonth(iso: string): string {
  return `${iso.slice(0, 7)}-01`;
}

// Desplaza un mes (delta en meses) devolviendo el primer día del mes resultante.
export function shiftMonth(iso: string, delta: number): string {
  const [year, month] = iso.split('-').map(Number);
  const base = new Date(year, month - 1 + delta, 1);
  return toIsoDate(base);
}

// Rejilla 6×7 (semana empezando en lunes) para el mes de `anchor`. Rellena con
// los días adyacentes para que no haya huecos y la cuadrícula quede completa.
export function buildMonthGrid(anchor: string): MonthCell[] {
  const first = firstOfMonth(anchor);
  const monthKey = first.slice(0, 7);
  // isoWeekday: 1 (lunes) … 7 (domingo). El primer hueco es weekday-1 días antes.
  const lead = isoWeekday(first) - 1;
  const start = addDaysIso(first, -lead);
  const cells: MonthCell[] = [];
  for (let index = 0; index < 42; index += 1) {
    const iso = addDaysIso(start, index);
    cells.push({ iso, inMonth: iso.slice(0, 7) === monthKey });
  }
  return cells;
}

// Enumera un intervalo continuo (ambos extremos incluidos). Devuelve [] si el
// intervalo está incompleto o invertido.
export function enumerateRange(startIso: string, endIso: string): string[] {
  if (startIso === '' || endIso === '' || endIso < startIso) {
    return [];
  }
  const dates: string[] = [];
  let cursor = startIso;
  while (cursor <= endIso) {
    dates.push(cursor);
    cursor = addDaysIso(cursor, 1);
  }
  return dates;
}

// Fechas canónicas (ordenadas, únicas) derivadas del modo activo del asistente.
export function resolveWizardDates(state: WizardState): string[] {
  if (state.dateMode === 'SINGLE') {
    return state.singleDate === '' ? [] : [state.singleDate];
  }
  if (state.dateMode === 'RANGE') {
    return enumerateRange(state.rangeStart, state.rangeEnd);
  }
  return [...state.scatterDates].sort((a, b) => a.localeCompare(b));
}

// Alterna una fecha en la lista de días sueltos (añade si falta, quita si está).
export function toggleScatterDate(dates: string[], iso: string): string[] {
  return dates.includes(iso) ? dates.filter((date) => date !== iso) : [...dates, iso];
}

// ¿Está una celda dentro de la selección actual (para pintar el estado activo)?
export function isDateSelected(state: WizardState, iso: string): boolean {
  if (state.dateMode === 'SINGLE') {
    return state.singleDate === iso;
  }
  if (state.dateMode === 'SCATTER') {
    return state.scatterDates.includes(iso);
  }
  return iso >= state.rangeStart && state.rangeStart !== '' && iso <= state.rangeEnd && state.rangeEnd !== '';
}

// Una fecha es pasada (no seleccionable) si es anterior a hoy.
export function isPastDate(iso: string, now: Date = new Date()): boolean {
  return iso < todayIso(now);
}

// El modo de fechas está completo cuando produce al menos una fecha válida.
export function hasValidSelection(mode: DateMode, dates: string[]): boolean {
  return mode !== undefined && dates.length > 0;
}

// ¿La opción de parking es una plaza concreta (no auto-asignación)?
export function isSpecificParking(choice: ParkingChoice | null): choice is number {
  return typeof choice === 'number';
}
