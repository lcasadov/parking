import { type ReactElement } from 'react';
import { useTranslation } from 'react-i18next';
import type { CalendarCell } from '../types/calendar';
import { calendarStateKey, calendarStateClass } from '../utils/calendar';

// Iniciales del titular para la celda control-room (mockup "M.G"): primera letra
// del nombre + primera del apellido, unidas por punto. Un solo termino -> 1 letra.
function occupantInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return '';
  }
  const first = parts[0].charAt(0);
  const last = parts.length > 1 ? parts[parts.length - 1].charAt(0) : '';
  return (last ? `${first}.${last}` : first).toUpperCase();
}

// Celda del calendario admin: color por estado (mapa estado->color del design
// system, contrato §4) + tag de estado + titular. El admin sí puede ver el
// nombre del titular cuando el contrato lo aporta. `isToday` resalta la columna
// del día actual; `dimmed` atenúa la celda cuando el filtro de estados la oculta.
// Cuando se pasa `onActivate`, la celda es accionable (asignar/liberar en
// contexto, weekly-assignment spec) y se renderiza como un botón accesible.
export function CalendarCellView({
  cell,
  isToday = false,
  dimmed = false,
  onActivate,
  actionLabel,
}: {
  cell: CalendarCell;
  isToday?: boolean;
  dimmed?: boolean;
  onActivate?: () => void;
  actionLabel?: string;
}) {
  const { t } = useTranslation();
  const stateLabel = t(calendarStateKey(cell.state));
  const isFree = cell.state === 'FREE';
  // Estados "ocupados" que muestran INICIALES del titular en la celda (mockup):
  // asignacion fija y solicitud aprobada. Liberado/pendiente muestran su palabra
  // de estado (mas legible que unas iniciales sin ocupacion efectiva).
  const showInitials =
    (cell.state === 'ASSIGNED' || cell.state === 'REQUEST_APPROVED') && Boolean(cell.employeeName);
  const classes = ['calendar-cell', 'state', calendarStateClass(cell.state)];
  if (isToday) {
    classes.push('is-today');
  }
  if (dimmed) {
    classes.push('is-dimmed');
  }

  // Contenido de la celda (control-room limpio):
  // · FREE            -> glifo "+" (invita a asignar), estado en sr-only.
  // · ocupado         -> INICIALES del titular; estado y nombre completo en
  //                      sr-only (accesibilidad + trazabilidad) + title al hover.
  // · liberado/pend.  -> palabra de estado visible; nombre (si lo hay) en sr-only.
  // En todos los casos la etiqueta de estado y el nombre viven como nodos de texto
  // propios en el DOM (lectores de pantalla y equivalencia semantica).
  let content: ReactElement;
  if (isFree) {
    content = (
      <>
        <span className="calendar-cell-plus" aria-hidden="true">
          +
        </span>
        <span className="sr-only">{stateLabel}</span>
      </>
    );
  } else if (showInitials) {
    content = (
      <>
        <span className="calendar-cell-initials mono" aria-hidden="true">
          {occupantInitials(cell.employeeName ?? '')}
        </span>
        <span className="sr-only calendar-cell-state">{stateLabel}</span>
        <span className="sr-only calendar-cell-name">{cell.employeeName}</span>
      </>
    );
  } else {
    content = (
      <>
        <span className="calendar-cell-state">{stateLabel}</span>
        {cell.employeeName ? (
          <span className="sr-only calendar-cell-name">{cell.employeeName}</span>
        ) : null}
      </>
    );
  }
  const cellTitle = showInitials ? (cell.employeeName ?? undefined) : undefined;

  if (onActivate) {
    classes.push('is-actionable');
    return (
      <td className={classes.join(' ')} title={cellTitle}>
        <button
          type="button"
          className="calendar-cell-action"
          aria-label={actionLabel}
          onClick={onActivate}
        >
          {content}
        </button>
      </td>
    );
  }

  return (
    <td className={classes.join(' ')} title={cellTitle}>
      {content}
    </td>
  );
}
