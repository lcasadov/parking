import { useTranslation } from 'react-i18next';
import type { CalendarCell } from '../types/calendar';
import { calendarStateKey, calendarStateClass } from '../utils/calendar';

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
  const classes = ['calendar-cell', 'state', calendarStateClass(cell.state)];
  if (isToday) {
    classes.push('is-today');
  }
  if (dimmed) {
    classes.push('is-dimmed');
  }

  const content = (
    <>
      <span className="calendar-cell-state">{stateLabel}</span>
      {cell.employeeName ? (
        <span className="calendar-cell-name">{cell.employeeName}</span>
      ) : null}
    </>
  );

  if (onActivate) {
    classes.push('is-actionable');
    return (
      <td className={classes.join(' ')}>
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

  return <td className={classes.join(' ')}>{content}</td>;
}
