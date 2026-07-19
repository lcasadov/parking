import { useTranslation } from 'react-i18next';
import type { CalendarCell } from '../types/calendar';
import { calendarStateKey, calendarStateClass } from '../utils/calendar';

// Celda del calendario admin: color por estado (mapa estado->color del design
// system, contrato §4) + tag de estado + titular. El admin sí puede ver el
// nombre del titular cuando el contrato lo aporta. `isToday` resalta la columna
// del día actual; `dimmed` atenúa la celda cuando el filtro de estados la oculta.
export function CalendarCellView({
  cell,
  isToday = false,
  dimmed = false,
}: {
  cell: CalendarCell;
  isToday?: boolean;
  dimmed?: boolean;
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
  return (
    <td className={classes.join(' ')}>
      <span className="calendar-cell-state">{stateLabel}</span>
      {cell.employeeName ? (
        <span className="calendar-cell-name">{cell.employeeName}</span>
      ) : null}
    </td>
  );
}
