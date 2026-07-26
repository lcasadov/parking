import type { TFunction } from 'i18next';
import type { LegendItem } from '../components/Legend';
import type { CalendarCellState } from '../types/calendar';

// Colores de la leyenda del calendario, alineados con el mapa estado->color del
// design system (contrato §4, tokens --state-*). Sin hex sueltos: ocupado=verde,
// liberado=azul, pendiente=amber, solicitud=naranja, libre=neutro.
const STATE_COLOR: Record<CalendarCellState, string> = {
  ASSIGNED: 'var(--state-occupied-bg)',
  RELEASED: 'var(--state-released-bg)',
  REQUEST_PENDING: 'var(--state-pending-bg)',
  REQUEST_APPROVED: 'var(--state-request-bg)',
  VISITOR_RESERVATION: 'var(--state-occupied-bg)',
  FREE: 'var(--state-free-bg)',
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
// Puntos de la leyenda de "Mi Semana": colores SATURADOS y bien diferenciados
// (verde/cian/rosa/gris), no los tintes de fondo suaves (que se confunden entre
// sí). Coinciden en tono con el fondo de cada fila de recurso.
const MY_WEEK_LEGEND_COLOR: Record<'ASSIGNED' | 'RELEASED' | 'REQUEST_PENDING' | 'FREE', string> = {
  ASSIGNED: 'var(--accent)', // verde — lo tienes
  RELEASED: 'var(--brand-blue)', // cian — liberado
  REQUEST_PENDING: 'var(--pink-text)', // rosa — pendiente de confirmación
  FREE: 'var(--ink-faint)', // gris — sin reservar
};

export function myWeekLegend(t: TFunction): LegendItem[] {
  const states: (keyof typeof MY_WEEK_LEGEND_COLOR)[] = [
    'ASSIGNED',
    'RELEASED',
    'REQUEST_PENDING',
    'FREE',
  ];
  return states.map((state) => ({
    color: MY_WEEK_LEGEND_COLOR[state],
    label: t(`calendar.myWeek.states.${state}`),
  }));
}
