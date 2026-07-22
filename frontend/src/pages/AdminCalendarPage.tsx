import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CalendarCellView } from '../components/CalendarCellView';
import { Legend } from '../components/Legend';
import { Spinner } from '../components/Spinner';
import { useAdminCalendarQuery } from '../hooks/useCalendar';
import { adminCalendarLegend } from '../utils/calendarLegend';
import { summarizeAdminCalendar, buildAdminCalendarCsv } from '../utils/adminCalendar';
import { triggerBlobDownload } from '../utils/download';
import type { CalendarCellState } from '../types/calendar';
import {
  addDaysIso,
  calendarStateKey,
  dayMonth,
  isoWeekNumber,
  mondayOfWeek,
  weekdayIndex,
  weekRangeLabel,
} from '../utils/calendar';
import { todayIso } from '../utils/requests';

const WEEK_LENGTH = 7;

// Estados del calendario en orden de lectura (S1192: sin literales repetidos).
const ALL_STATES: CalendarCellState[] = [
  'ASSIGNED',
  'RELEASED',
  'REQUEST_PENDING',
  'REQUEST_APPROVED',
  'FREE',
];

// Punto de color por estado para los chips del filtro (mapa estado->color, §4).
const STATE_DOT: Record<CalendarCellState, string> = {
  ASSIGNED: 'var(--state-occupied-bg)',
  RELEASED: 'var(--state-released-bg)',
  REQUEST_PENDING: 'var(--state-pending-bg)',
  REQUEST_APPROVED: 'var(--state-request-bg)',
  FREE: 'var(--state-free-bg)',
};

// Rejilla de calendario semanal ADMIN (consume GET /calendar/admin). Presentacion
// ALEATICA (change redesign-weekly-assignment): cabecera + tarjetas resumen +
// navegador de semana + grid plaza x dia coloreado por el mapa estado->color.
// Mismos datos y endpoints que la version previa (solo capa de presentacion).
export function AdminCalendarPage() {
  const { t, i18n } = useTranslation();
  const [weekStart, setWeekStart] = useState<string>(mondayOfWeek());
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [activeStates, setActiveStates] = useState<Set<CalendarCellState>>(
    () => new Set(ALL_STATES),
  );

  const query = useAdminCalendarQuery(weekStart);
  const days = query.data?.days ?? [];
  const rows = useMemo(() => query.data?.rows ?? [], [query.data]);
  const today = todayIso();

  const summary = useMemo(() => summarizeAdminCalendar(rows), [rows]);
  const rangeLabel =
    days.length > 0 ? weekRangeLabel(days[0], days[days.length - 1], i18n.language) : '';
  const weekNumber = isoWeekNumber(weekStart);

  function goPrevious(): void {
    setWeekStart((current) => addDaysIso(current, -WEEK_LENGTH));
  }

  function goNext(): void {
    setWeekStart((current) => addDaysIso(current, WEEK_LENGTH));
  }

  function goToday(): void {
    setWeekStart(mondayOfWeek());
  }

  function toggleState(state: CalendarCellState): void {
    setActiveStates((current) => {
      const next = new Set(current);
      if (next.has(state)) {
        next.delete(state);
      } else {
        next.add(state);
      }
      return next;
    });
  }

  function handleExport(): void {
    if (!query.data) {
      return;
    }
    const csv = buildAdminCalendarCsv(
      query.data,
      t('calendar.space'),
      (iso) => `${t(`calendar.weekdaysShort.${weekdayIndex(iso)}`)} ${dayMonth(iso)}`,
      (state) => t(calendarStateKey(state)),
    );
    // BOM UTF-8 para que Excel detecte la codificacion en el CSV.
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
    triggerBlobDownload(blob, `${t('calendar.export.filename')}-${weekStart}.csv`);
  }

  const showGrid = !query.isLoading && !query.isError;

  return (
    <section className="admin-calendar-page" aria-labelledby="admin-calendar-title">
      <header className="page-header">
        <div className="page-heading">
          <span className="page-eyebrow">{t('calendar.admin.eyebrow')}</span>
          <h1 id="admin-calendar-title" className="section-title">
            {t('calendar.admin.title')}
          </h1>
          <p className="page-description">{t('calendar.admin.description')}</p>
        </div>
        <div className="page-actions">
          <Button
            variant="white"
            icon="filter"
            aria-pressed={isFilterOpen}
            onClick={() => setIsFilterOpen((open) => !open)}
          >
            {t('calendar.actions.filter')}
          </Button>
          <Button
            variant="green"
            icon="download"
            disabled={rows.length === 0}
            onClick={handleExport}
          >
            {t('calendar.actions.export')}
          </Button>
        </div>
      </header>

      <div className="calendar-summary">
        <SummaryCard tone="ink" value={summary.spaces} label={t('calendar.summary.spaces')} />
        <SummaryCard
          tone="green"
          value={summary.assignments}
          label={t('calendar.summary.assignments')}
        />
        <SummaryCard
          tone="blue"
          value={summary.releases}
          label={t('calendar.summary.releases')}
        />
        <SummaryCard
          tone="orange"
          value={summary.requests}
          label={t('calendar.summary.requests')}
        />
      </div>

      <nav className="week-nav" aria-label={t('calendar.admin.title')}>
        <div className="week-nav-controls">
          <Button
            variant="white"
            className="btn-icon-only"
            icon="chevron-left"
            aria-label={t('calendar.toolbar.previous')}
            onClick={goPrevious}
          />
          <Button
            variant="white"
            className="btn-icon-only"
            icon="chevron-right"
            aria-label={t('calendar.toolbar.next')}
            onClick={goNext}
          />
          <Button variant="white" onClick={goToday}>
            {t('calendar.toolbar.today')}
          </Button>
        </div>
        <div className="week-nav-range">
          <span className="week-range" aria-live="polite">
            {rangeLabel || t('calendar.toolbar.weekOf', { date: weekStart })}
          </span>
          <span className="week-number">{t('calendar.weekNav.week', { number: weekNumber })}</span>
        </div>
        <Legend items={adminCalendarLegend(t)} />
      </nav>

      {isFilterOpen ? (
        <div className="chip-filters calendar-state-filter" role="group" aria-label={t('calendar.actions.filter')}>
          {ALL_STATES.map((state) => {
            const isActive = activeStates.has(state);
            return (
              <button
                key={state}
                type="button"
                className={`chip-filter${isActive ? ' is-active' : ''}`}
                aria-pressed={isActive}
                onClick={() => toggleState(state)}
              >
                <span className="cf-dot" style={{ background: STATE_DOT[state] }} aria-hidden="true" />
                {t(calendarStateKey(state))}
              </button>
            );
          })}
        </div>
      ) : null}

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('calendar.loadError')}
        </p>
      ) : null}

      {showGrid ? (
        <div className="table-scroll">
          <table className="table calendar-grid">
            <caption className="sr-only">{t('calendar.admin.title')}</caption>
            <thead>
              <tr className="table-header">
                <th scope="col">{t('calendar.space')}</th>
                {days.map((date) => (
                  <th key={date} scope="col" className={date === today ? 'is-today' : undefined}>
                    <span className="day-head-abbr">
                      {t(`calendar.weekdaysShort.${weekdayIndex(date)}`)}
                    </span>
                    <span className="day-head-date">{dayMonth(date)}</span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={days.length + 1} className="table-empty">
                    {t('calendar.empty')}
                  </td>
                </tr>
              ) : (
                rows.map((row) => (
                  <tr key={row.parkingSpaceId} className="table-row">
                    <th scope="row" className="calendar-space-cell">
                      <span className="calendar-space-label">{row.label}</span>
                      <span className="calendar-space-zone" aria-hidden="true">
                        #{row.parkingSpaceId}
                      </span>
                    </th>
                    {row.cells.map((cell) => (
                      <CalendarCellView
                        key={cell.date}
                        cell={cell}
                        isToday={cell.date === today}
                        dimmed={!activeStates.has(cell.state)}
                      />
                    ))}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

// Tarjeta de resumen (numeral serif + etiqueta), coloreada por metrica.
function SummaryCard({
  tone,
  value,
  label,
}: {
  tone: 'ink' | 'green' | 'blue' | 'orange';
  value: number;
  label: string;
}) {
  return (
    <div className={`summary-card summary-card--${tone}`}>
      <span className="summary-card-value">{value}</span>
      <span className="summary-card-label">{label}</span>
    </div>
  );
}
