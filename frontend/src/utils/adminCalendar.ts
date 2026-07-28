import type {
  AdminWeeklyCalendarResponse,
  CalendarCellState,
  CalendarRow,
} from '../types/calendar';

// Derivaciones de presentacion sobre el calendario semanal admin. Todas operan
// exclusivamente sobre los datos ya cargados por useAdminCalendarQuery: no
// consultan endpoints ni alteran el origen de datos (solo capa de presentacion).

// Resumen de la semana para las 4 tarjetas de cabecera.
export interface AdminCalendarSummary {
  spaces: number; // plazas activas (filas)
  assignments: number; // celdas asignadas u ocupadas por solicitud aprobada
  releases: number; // celdas liberadas
  requests: number; // solicitudes pendientes
}

// Instantanea de un unico dia (columna del calendario), para la fila de KPIs del
// modo activo (total del recurso / ocupados / libres / liberados ese dia). Opera
// sobre las filas ya cargadas por useAdminCalendarQuery; no consulta endpoints.
export interface DaySnapshot {
  total: number; // recursos del tipo activo (filas)
  occupied: number; // ocupados ese dia (asignacion fija o solicitud aprobada)
  free: number; // libres ese dia
  released: number; // liberados ese dia
  pending: number; // con solicitud pendiente ese dia
}

// Cuenta los estados de la columna `date` (un dia) en las filas ya cargadas. Si
// una fila no tiene celda para esa fecha, se ignora (no aporta a los contadores).
export function summarizeDay(rows: CalendarRow[], date: string): DaySnapshot {
  let occupied = 0;
  let free = 0;
  let released = 0;
  let pending = 0;
  for (const row of rows) {
    const cell = row.cells.find((item) => item.date === date);
    if (!cell) {
      continue;
    }
    if (cell.state === 'ASSIGNED' || cell.state === 'REQUEST_APPROVED' || cell.state === 'VISITOR_RESERVATION') {
      occupied += 1;
    } else if (cell.state === 'FREE') {
      free += 1;
    } else if (cell.state === 'RELEASED') {
      released += 1;
    } else if (cell.state === 'REQUEST_PENDING') {
      pending += 1;
    }
  }
  return { total: rows.length, occupied, free, released, pending };
}

// Cuenta estados a partir de las filas ya cargadas. Cada celda se cuenta una vez.
export function summarizeAdminCalendar(rows: CalendarRow[]): AdminCalendarSummary {
  let assignments = 0;
  let releases = 0;
  let requests = 0;
  for (const row of rows) {
    for (const cell of row.cells) {
      if (cell.state === 'ASSIGNED' || cell.state === 'REQUEST_APPROVED' || cell.state === 'VISITOR_RESERVATION') {
        assignments += 1;
      } else if (cell.state === 'RELEASED') {
        releases += 1;
      } else if (cell.state === 'REQUEST_PENDING') {
        requests += 1;
      }
    }
  }
  return { spaces: rows.length, assignments, releases, requests };
}

// Escapa un valor para CSV (RFC 4180): comillas si contiene coma, comilla o salto.
function csvCell(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

// Serializa la rejilla ya cargada a CSV (plaza x dia con el estado de cada celda).
// Reutiliza los mismos datos y etiquetas visibles; no invoca ningun endpoint.
export function buildAdminCalendarCsv(
  data: AdminWeeklyCalendarResponse,
  spaceHeader: string,
  dayLabel: (iso: string) => string,
  stateLabel: (state: CalendarCellState) => string,
): string {
  const rows: string[][] = [[spaceHeader, ...data.days.map(dayLabel)]];
  for (const row of data.rows) {
    rows.push([row.label, ...row.cells.map((cell) => stateLabel(cell.state))]);
  }
  return rows.map((cols) => cols.map(csvCell).join(',')).join('\r\n');
}
