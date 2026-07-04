import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CalendarCellView } from '../components/CalendarCellView';
import { Spinner } from '../components/Spinner';
import { useAdminCalendarQuery } from '../hooks/useCalendar';
import {
  addDaysIso,
  dayMonth,
  mondayOfWeek,
  weekdayIndex,
} from '../utils/calendar';

const WEEK_LENGTH = 7;

// Rejilla de calendario semanal ADMIN (consume GET /calendar/admin). tasks §4.2.
// Semana en columnas x plazas en filas; celdas coloreadas por CalendarCellState.
export function AdminCalendarPage() {
  const { t } = useTranslation();
  const [weekStart, setWeekStart] = useState<string>(mondayOfWeek());

  const query = useAdminCalendarQuery(weekStart);
  const days = query.data?.days ?? [];
  const rows = query.data?.rows ?? [];

  function goPrevious(): void {
    setWeekStart((current) => addDaysIso(current, -WEEK_LENGTH));
  }

  function goNext(): void {
    setWeekStart((current) => addDaysIso(current, WEEK_LENGTH));
  }

  function goToday(): void {
    setWeekStart(mondayOfWeek());
  }

  function dayHeader(date: string): string {
    return `${t(`calendar.weekdaysShort.${weekdayIndex(date)}`)} ${dayMonth(date)}`;
  }

  return (
    <section className="admin-calendar-page" aria-labelledby="admin-calendar-title">
      <header className="page-header">
        <h1 id="admin-calendar-title" className="section-title">
          {t('calendar.admin.title')}
        </h1>
      </header>

      <nav className="calendar-toolbar" aria-label={t('calendar.admin.title')}>
        <Button variant="white" icon="chevron-left" onClick={goPrevious}>
          {t('calendar.toolbar.previous')}
        </Button>
        <span className="calendar-week-label" aria-live="polite">
          {t('calendar.toolbar.weekOf', { date: weekStart })}
        </span>
        <Button variant="white" onClick={goToday}>
          {t('calendar.toolbar.today')}
        </Button>
        <Button variant="white" icon="chevron-right" onClick={goNext}>
          {t('calendar.toolbar.next')}
        </Button>
      </nav>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('calendar.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
        <div className="table-scroll">
          <table className="table calendar-grid">
            <caption className="sr-only">{t('calendar.admin.title')}</caption>
            <thead>
              <tr className="table-header">
                <th scope="col">{t('calendar.space')}</th>
                {days.map((date) => (
                  <th key={date} scope="col">
                    {dayHeader(date)}
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
                    <th scope="row">{row.label}</th>
                    {row.cells.map((cell) => (
                      <CalendarCellView key={cell.date} cell={cell} />
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
