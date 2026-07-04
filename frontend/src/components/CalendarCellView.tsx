import { useTranslation } from 'react-i18next';
import type { CalendarCell } from '../types/calendar';
import { calendarStateKey, cellStateClass } from '../utils/calendar';

// Celda del calendario admin: color por estado + etiqueta traducida.
// El admin sí puede ver el nombre del titular cuando el contrato lo aporta.
export function CalendarCellView({ cell }: { cell: CalendarCell }) {
  const { t } = useTranslation();
  const stateLabel = t(calendarStateKey(cell.state));
  return (
    <td className={`calendar-cell ${cellStateClass(cell.state)}`}>
      <span className="calendar-cell-state">{stateLabel}</span>
      {cell.employeeName ? (
        <span className="calendar-cell-name">{cell.employeeName}</span>
      ) : null}
    </td>
  );
}
