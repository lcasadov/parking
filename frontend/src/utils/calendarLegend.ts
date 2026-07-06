import type { TFunction } from 'i18next';
import type { LegendItem } from '../components/Legend';
import type { CalendarCellState } from '../types/calendar';

// Colores de la leyenda del calendario, alineados con los estados de celda
// (.cell-* en components.css). Tokens del design-system, sin hex sueltos.
const STATE_COLOR: Record<CalendarCellState, string> = {
  ASSIGNED: 'var(--green-soft)',
  RELEASED: 'var(--pink-soft)',
  REQUEST_PENDING: 'var(--amber-soft)',
  REQUEST_APPROVED: 'var(--blue-soft)',
  FREE: 'var(--neutral-soft)',
};

// Leyenda del calendario semanal ADMIN (mockup 01): 5 estados de celda.
export function adminCalendarLegend(t: TFunction): LegendItem[] {
  const states: CalendarCellState[] = [
    'ASSIGNED',
    'RELEASED',
    'REQUEST_PENDING',
    'REQUEST_APPROVED',
    'FREE',
  ];
  return states.map((state) => ({
    color: STATE_COLOR[state],
    label: t(`calendar.states.${state}`),
  }));
}

// Leyenda de "Mi Semana" (EMPLOYEE): comparte celdas pero sin solicitud aprobada.
export function myWeekLegend(t: TFunction): LegendItem[] {
  const states: Exclude<CalendarCellState, 'REQUEST_APPROVED'>[] = [
    'ASSIGNED',
    'RELEASED',
    'REQUEST_PENDING',
    'FREE',
  ];
  return states.map((state) => ({
    color: STATE_COLOR[state],
    label: t(`calendar.myWeek.states.${state}`),
  }));
}
