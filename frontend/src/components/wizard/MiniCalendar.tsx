import { useTranslation } from 'react-i18next';
import { buildMonthGrid, isDateSelected, isPastDate, shiftMonth } from '../../utils/wizardDates';
import { todayIso } from '../../utils/requests';
import type { WizardState } from './wizardTypes';

interface MiniCalendarProps {
  // Primer día del mes mostrado (ISO).
  anchor: string;
  onAnchorChange: (anchor: string) => void;
  state: WizardState;
  onPick: (iso: string) => void;
}

const WEEKDAY_KEYS = ['1', '2', '3', '4', '5', '6', '7'] as const;

// Etiqueta del mes/año en el idioma activo (p. ej. "julio 2026").
function monthTitle(anchor: string, locale: string): string {
  const label = new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' }).format(
    new Date(`${anchor}T00:00:00`),
  );
  return label.charAt(0).toUpperCase() + label.slice(1);
}

// Calendario mensual compacto (semana empezando en lunes) reutilizado por los tres
// modos del paso de fechas. Marca el día de hoy, deshabilita fechas pasadas y pinta
// el estado seleccionado (día, extremos y relleno del rango) según el modo activo.
export function MiniCalendar({ anchor, onAnchorChange, state, onPick }: MiniCalendarProps) {
  const { t, i18n } = useTranslation();
  const cells = buildMonthGrid(anchor);
  const today = todayIso();

  return (
    <div className="rzw-cal">
      <div className="rzw-cal-head">
        <button
          type="button"
          className="rzw-cal-nav"
          aria-label={t('wizard.dates.prevMonth')}
          onClick={() => onAnchorChange(shiftMonth(anchor, -1))}
        >
          <i className="ti ti-chevron-left" aria-hidden="true" />
        </button>
        <span className="rzw-cal-title" aria-live="polite">
          {monthTitle(anchor, i18n.language)}
        </span>
        <button
          type="button"
          className="rzw-cal-nav"
          aria-label={t('wizard.dates.nextMonth')}
          onClick={() => onAnchorChange(shiftMonth(anchor, 1))}
        >
          <i className="ti ti-chevron-right" aria-hidden="true" />
        </button>
      </div>
      <div className="rzw-cal-grid rzw-cal-dow" aria-hidden="true">
        {WEEKDAY_KEYS.map((key) => (
          <span key={key} className="rzw-cal-dow-cell">
            {t(`common.weekdayChip.${key}`)}
          </span>
        ))}
      </div>
      <div className="rzw-cal-grid">
        {cells.map((cell) => {
          const past = isPastDate(cell.iso);
          const selected = isDateSelected(state, cell.iso);
          const isRange = state.dateMode === 'RANGE';
          const isEdge =
            isRange && (cell.iso === state.rangeStart || cell.iso === state.rangeEnd);
          // Días interiores del rango: relleno suave (los extremos van sólidos).
          const isRangeMid = selected && isRange && !isEdge;
          const classes = [
            'rzw-cal-cell',
            cell.inMonth ? '' : 'is-out',
            cell.iso === today ? 'is-today' : '',
            selected ? 'is-selected' : '',
            isEdge ? 'is-edge' : '',
            isRangeMid ? 'is-range-mid' : '',
          ]
            .filter(Boolean)
            .join(' ');
          return (
            <button
              key={cell.iso}
              type="button"
              className={classes}
              disabled={past}
              aria-pressed={selected}
              onClick={() => onPick(cell.iso)}
            >
              {Number(cell.iso.slice(-2))}
            </button>
          );
        })}
      </div>
    </div>
  );
}
