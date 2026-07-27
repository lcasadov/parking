import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { longDate } from '../utils/calendar';
import { toIsoDate } from '../utils/requests';

interface MultiSelectCalendarProps {
  // Fechas seleccionadas (ISO). Selección múltiple por clic (toggle).
  selected: Set<string>;
  onToggle: (iso: string) => void;
  // Primer día seleccionable (los anteriores se muestran deshabilitados).
  minIso: string;
  // Cuando es false, no se pueden seleccionar MÁS días (tope alcanzado).
  canSelectMore?: boolean;
  // Deshabilita sábados y domingos (cuando no se admite reserva en fin de semana).
  weekendDisabled?: boolean;
  // Marcas POR RECURSO: días (ISO) con reserva propia VIVA (APPROVED/PENDING) →
  // un punto de plaza y/o puesto por día. Refleja solo lo que ya tienes reservado
  // (coincide con el bloqueo de la tarjeta); el fijo no cuenta (puede re-reservarse).
  parkingDays?: ReadonlySet<string>;
  deskDays?: ReadonlySet<string>;
  // Días (ISO) ya LIBERADOS → fondo azul, para verlos de un vistazo (modal de ausencia).
  releasedDays?: ReadonlySet<string>;
}

interface DayCell {
  iso: string;
  day: number;
  inMonth: boolean;
  disabled: boolean;
  isToday: boolean;
  isSelected: boolean;
  isWeekend: boolean;
  hasParking: boolean;
  hasDesk: boolean;
  isReleased: boolean;
}

const EMPTY_STR: ReadonlySet<string> = new Set<string>();

function weekdayLabels(locale: string): string[] {
  const fmt = new Intl.DateTimeFormat(locale, { weekday: 'short' });
  return Array.from({ length: 7 }, (_, i) => fmt.format(new Date(2024, 0, 1 + i)));
}

function dayClass(cell: DayCell): string {
  const parts = ['rc-day'];
  if (!cell.inMonth) parts.push('is-out');
  if (cell.disabled) parts.push('is-disabled');
  if (cell.isToday) parts.push('is-today');
  // El fondo "liberado" (azul) va antes de is-selected: al seleccionar, el verde gana.
  if (cell.isReleased && !cell.isSelected && cell.inMonth) parts.push('is-released');
  if (cell.isSelected) parts.push('is-selected');
  if (cell.isWeekend && !cell.isSelected && cell.inMonth) parts.push('is-weekend');
  return parts.join(' ');
}

interface GridConfig {
  selected: Set<string>;
  minIso: string;
  canSelectMore: boolean;
  weekendDisabled: boolean;
  parkingDays: ReadonlySet<string>;
  deskDays: ReadonlySet<string>;
  releasedDays: ReadonlySet<string>;
}

// Matriz de 6 semanas (lunes primero) con selección/tope/marcas por recurso.
function buildGrid(year: number, month: number, cfg: GridConfig): DayCell[] {
  const first = new Date(year, month, 1);
  const weekdayMon = (first.getDay() + 6) % 7;
  const start = new Date(year, month, 1 - weekdayMon);
  const cells: DayCell[] = [];
  for (let i = 0; i < 42; i += 1) {
    const d = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
    const iso = toIsoDate(d);
    const weekday = d.getDay();
    const isWeekend = weekday === 0 || weekday === 6;
    const isSelected = cfg.selected.has(iso);
    const blocked = iso < cfg.minIso || (!isSelected && !cfg.canSelectMore) || (cfg.weekendDisabled && isWeekend);
    cells.push({
      iso,
      day: d.getDate(),
      inMonth: d.getMonth() === month,
      disabled: blocked,
      isToday: iso === cfg.minIso,
      isSelected,
      isWeekend,
      hasParking: cfg.parkingDays.has(iso),
      hasDesk: cfg.deskDays.has(iso),
      isReleased: cfg.releasedDays.has(iso),
    });
  }
  return cells;
}

// Calendario mensual de SELECCIÓN MÚLTIPLE (días sueltos). Marca con hasta dos
// puntos por día lo que ya tienes (plaza / puesto). Reutilizado por el modal de
// ausencia y por la reserva multi-día.
export function MultiSelectCalendar({
  selected,
  onToggle,
  minIso,
  canSelectMore = true,
  weekendDisabled = false,
  parkingDays = EMPTY_STR,
  deskDays = EMPTY_STR,
  releasedDays = EMPTY_STR,
}: MultiSelectCalendarProps) {
  const { t, i18n } = useTranslation();
  const anchor = new Date(`${minIso}T00:00:00`);
  const [view, setView] = useState({ year: anchor.getFullYear(), month: anchor.getMonth() });

  const cells = useMemo(
    () =>
      buildGrid(view.year, view.month, {
        selected,
        minIso,
        canSelectMore,
        weekendDisabled,
        parkingDays,
        deskDays,
        releasedDays,
      }),
    [view, selected, minIso, canSelectMore, weekendDisabled, parkingDays, deskDays, releasedDays],
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
            disabled={cell.disabled}
            aria-pressed={cell.isSelected}
            aria-current={cell.isToday ? 'date' : undefined}
            aria-label={longDate(cell.iso, i18n.language)}
            onClick={() => onToggle(cell.iso)}
          >
            <span className="rc-day-num">{cell.day}</span>
            {cell.hasParking || cell.hasDesk ? (
              <span className="rc-day-marks" aria-hidden="true">
                {cell.hasParking ? <span className="rc-day-dot is-parking" /> : null}
                {cell.hasDesk ? <span className="rc-day-dot is-desk" /> : null}
              </span>
            ) : null}
          </button>
        ))}
      </div>
    </div>
  );
}
