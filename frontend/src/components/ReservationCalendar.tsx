import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { longDate } from '../utils/calendar';
import { toIsoDate } from '../utils/requests';

interface ReservationCalendarProps {
  // Fecha seleccionada (ISO YYYY-MM-DD) o '' si aún no hay ninguna.
  value: string;
  // Primer día seleccionable (hoy). Los días anteriores se muestran deshabilitados.
  minIso: string;
  onSelect: (iso: string) => void;
}

interface DayCell {
  iso: string;
  day: number;
  inMonth: boolean;
  isPast: boolean;
  isToday: boolean;
  isSelected: boolean;
  isWeekend: boolean;
}

// Matriz de 6 semanas (42 celdas, lunes primero) del mes indicado. Comparar ISO
// como string funciona porque YYYY-MM-DD es lexicográficamente ordenado.
function buildMonthGrid(
  year: number,
  month: number,
  minIso: string,
  selected: string,
  todayIso: string,
): DayCell[] {
  const first = new Date(year, month, 1);
  const weekdayMon = (first.getDay() + 6) % 7; // 0 = lunes … 6 = domingo
  const start = new Date(year, month, 1 - weekdayMon);
  const cells: DayCell[] = [];
  for (let i = 0; i < 42; i += 1) {
    const d = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
    const iso = toIsoDate(d);
    const weekday = d.getDay();
    cells.push({
      iso,
      day: d.getDate(),
      inMonth: d.getMonth() === month,
      isPast: iso < minIso,
      isToday: iso === todayIso,
      isSelected: selected !== '' && iso === selected,
      isWeekend: weekday === 0 || weekday === 6,
    });
  }
  return cells;
}

// Clase de una celda día. Extraída a módulo para no cargar la complejidad
// cognitiva del componente (S3776).
function dayClass(cell: DayCell): string {
  const parts = ['rc-day'];
  if (!cell.inMonth) parts.push('is-out');
  if (cell.isPast) parts.push('is-past');
  if (cell.isToday) parts.push('is-today');
  if (cell.isSelected) parts.push('is-selected');
  if (cell.isWeekend) parts.push('is-weekend');
  return parts.join(' ');
}

// Etiquetas de día de la semana (lunes primero), localizadas. 2024-01-01 fue lunes.
function weekdayLabels(locale: string): string[] {
  const fmt = new Intl.DateTimeFormat(locale, { weekday: 'short' });
  return Array.from({ length: 7 }, (_, i) => fmt.format(new Date(2024, 0, 1 + i)));
}

// Calendario mensual de selección de fecha para la reserva rápida. Días pasados
// deshabilitados; hoy resaltado con anillo; día elegido en esmeralda sólido.
export function ReservationCalendar({ value, minIso, onSelect }: ReservationCalendarProps) {
  const { t, i18n } = useTranslation();
  const anchor = value !== '' ? new Date(`${value}T00:00:00`) : new Date(`${minIso}T00:00:00`);
  const [view, setView] = useState({ year: anchor.getFullYear(), month: anchor.getMonth() });

  const cells = useMemo(
    () => buildMonthGrid(view.year, view.month, minIso, value, minIso),
    [view, minIso, value],
  );
  const dows = useMemo(() => weekdayLabels(i18n.language), [i18n.language]);
  const monthTitle = new Intl.DateTimeFormat(i18n.language, {
    month: 'long',
    year: 'numeric',
  }).format(new Date(view.year, view.month, 1));

  function shiftMonth(delta: number): void {
    setView((current) => {
      const d = new Date(current.year, current.month + delta, 1);
      return { year: d.getFullYear(), month: d.getMonth() };
    });
  }

  return (
    <div className="rc-cal">
      <div className="rc-cal-head">
        <button
          type="button"
          className="rc-cal-nav"
          onClick={() => shiftMonth(-1)}
          aria-label={t('requests.create.calendar.prev')}
        >
          <i className="ti ti-chevron-left" aria-hidden="true" />
        </button>
        <span className="rc-cal-title">{monthTitle}</span>
        <button
          type="button"
          className="rc-cal-nav"
          onClick={() => shiftMonth(1)}
          aria-label={t('requests.create.calendar.next')}
        >
          <i className="ti ti-chevron-right" aria-hidden="true" />
        </button>
      </div>
      <div className="rc-cal-dow">
        {dows.map((label) => (
          <span key={label} className="rc-cal-dow-cell">
            {label}
          </span>
        ))}
      </div>
      <div className="rc-cal-grid">
        {cells.map((cell) => (
          <button
            key={cell.iso}
            type="button"
            className={dayClass(cell)}
            disabled={cell.isPast}
            aria-pressed={cell.isSelected}
            aria-current={cell.isToday ? 'date' : undefined}
            aria-label={longDate(cell.iso, i18n.language)}
            onClick={() => onSelect(cell.iso)}
          >
            {cell.day}
          </button>
        ))}
      </div>
    </div>
  );
}
