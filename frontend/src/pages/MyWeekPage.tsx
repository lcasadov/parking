import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { useMyWeekQuery } from '../hooks/useCalendar';
import type { MyWeekDay } from '../types/calendar';
import {
  addDaysIso,
  cellStateClass,
  dayMonth,
  mondayOfWeek,
  myWeekStateKey,
  weekdayIndex,
} from '../utils/calendar';

const WEEK_LENGTH = 7;

// Vista EMPLOYEE "Mi Semana" (consume GET /calendar/my-week). tasks §4.3.
// Solo recursos propios; el contrato NO incluye nombres de terceros, por lo que
// esta vista nunca los renderiza.
export function MyWeekPage() {
  const { t } = useTranslation();
  const [weekStart, setWeekStart] = useState<string>(mondayOfWeek());

  const query = useMyWeekQuery(weekStart);
  const days = query.data?.days ?? [];

  function dayHeader(date: string): string {
    return `${t(`calendar.weekdaysShort.${weekdayIndex(date)}`)} ${dayMonth(date)}`;
  }

  function spaceLine(day: MyWeekDay): string {
    return day.parkingSpaceLabel
      ? t('calendar.myWeek.space', { label: day.parkingSpaceLabel })
      : t('calendar.myWeek.noSpace');
  }

  return (
    <section className="my-week-page" aria-labelledby="my-week-title">
      <header className="page-header">
        <h1 id="my-week-title" className="section-title">
          {t('calendar.myWeek.title')}
        </h1>
      </header>

      <nav className="calendar-toolbar" aria-label={t('calendar.myWeek.title')}>
        <Button
          variant="white"
          icon="chevron-left"
          onClick={() => setWeekStart((current) => addDaysIso(current, -WEEK_LENGTH))}
        >
          {t('calendar.toolbar.previous')}
        </Button>
        <span className="calendar-week-label" aria-live="polite">
          {t('calendar.toolbar.weekOf', { date: weekStart })}
        </span>
        <Button variant="white" onClick={() => setWeekStart(mondayOfWeek())}>
          {t('calendar.toolbar.today')}
        </Button>
        <Button
          variant="white"
          icon="chevron-right"
          onClick={() => setWeekStart((current) => addDaysIso(current, WEEK_LENGTH))}
        >
          {t('calendar.toolbar.next')}
        </Button>
      </nav>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('calendar.myWeek.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
        <ul className="my-week-list">
          {days.length === 0 ? (
            <li className="my-week-empty">{t('calendar.myWeek.empty')}</li>
          ) : (
            days.map((day) => (
              <li key={day.date} className={`my-week-day ${cellStateClass(day.state)}`}>
                <span className="my-week-day-header">{dayHeader(day.date)}</span>
                <span className="my-week-day-state">{t(myWeekStateKey(day.state))}</span>
                <span className="my-week-day-space">{spaceLine(day)}</span>
              </li>
            ))
          )}
        </ul>
      ) : null}
    </section>
  );
}
