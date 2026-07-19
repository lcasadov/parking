import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CreateRequestModal } from '../components/CreateRequestModal';
import { Legend } from '../components/Legend';
import { PageHeader } from '../components/PageHeader';
import { ReleaseResourceModal } from '../components/ReleaseResourceModal';
import { Spinner } from '../components/Spinner';
import { emitApiErrorToast } from '../api/events';
import { useAuth } from '../auth/useAuth';
import { useMyWeekQuery } from '../hooks/useCalendar';
import { useEmployeeFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { groupFixedAssignments } from '../utils/fixedAssignments';
import { myWeekLegend } from '../utils/calendarLegend';
import type { MyWeekDay, MyWeekDayState } from '../types/calendar';
import { addDaysIso, dayMonth, mondayOfWeek, myWeekStateKey, weekdayIndex } from '../utils/calendar';

const WEEK_LENGTH = 7;

// Variante de tarjeta (.week-card) y icono de acción por estado del día, según
// el mockup 07 (empleado móvil). Sin literales repetidos (S1192).
const WEEK_CARD_VARIANT: Record<MyWeekDayState, string> = {
  ASSIGNED: 'assigned',
  RELEASED: 'released',
  REQUEST_PENDING: 'request',
  FREE: 'free',
};
const WEEK_CARD_ICON: Record<MyWeekDayState, string> = {
  ASSIGNED: 'check',
  RELEASED: 'arrow-back',
  REQUEST_PENDING: 'clock',
  FREE: 'plus',
};

interface ReleaseTarget {
  parkingSpaceId: number;
  spaceLabel: string;
}

// Vista EMPLOYEE "Mi Semana" (consume GET /calendar/my-week). tasks §4.3.
// Solo recursos propios; el contrato NO incluye nombres de terceros, por lo que
// esta vista nunca los renderiza. Cada día se muestra como una tarjeta
// (.week-card) con color por estado, y al pie ofrece las acciones Solicitar y
// Liberar (mockup 07). El recurso fijo a liberar se deriva de las asignaciones
// fijas del empleado, ya que my-week solo transporta la etiqueta de la plaza.
export function MyWeekPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [weekStart, setWeekStart] = useState<string>(mondayOfWeek());
  const [isRequestOpen, setIsRequestOpen] = useState(false);
  const [isReleaseOpen, setIsReleaseOpen] = useState(false);

  const query = useMyWeekQuery(weekStart);
  const days = query.data?.days ?? [];

  const fixedQuery = useEmployeeFixedAssignmentsQuery(user?.employeeId ?? null);
  const releaseTarget = useMemo<ReleaseTarget | null>(() => {
    const parkingGroup = groupFixedAssignments(fixedQuery.data ?? []).find(
      (group) => group.resourceType === 'PARKING',
    );
    if (!parkingGroup) {
      return null;
    }
    return { parkingSpaceId: parkingGroup.parkingSpaceId, spaceLabel: `#${parkingGroup.parkingSpaceId}` };
  }, [fixedQuery.data]);

  function dayAbbr(date: string): string {
    return t(`calendar.weekdaysShort.${weekdayIndex(date)}`);
  }

  function spaceLine(day: MyWeekDay): string {
    return day.parkingSpaceLabel
      ? t('calendar.myWeek.space', { label: day.parkingSpaceLabel })
      : t('calendar.myWeek.noSpace');
  }

  function handleRequested(): void {
    setIsRequestOpen(false);
    void query.refetch();
    emitApiErrorToast('requests.mine.created');
  }

  function handleReleased(): void {
    setIsReleaseOpen(false);
    void query.refetch();
    emitApiErrorToast('releases.release.created');
  }

  return (
    <section className="my-week-page" aria-label={t('calendar.myWeek.title')}>
      <PageHeader
        eyebrow={t('calendar.myWeek.eyebrow')}
        title={t('calendar.myWeek.title')}
        description={t('calendar.myWeek.description')}
      />

      <nav className="calendar-toolbar" aria-label={t('calendar.myWeek.title')}>
        <Button
          variant="white"
          className="btn-icon-only"
          icon="chevron-left"
          aria-label={t('calendar.toolbar.previous')}
          onClick={() => setWeekStart((current) => addDaysIso(current, -WEEK_LENGTH))}
        />
        <span className="calendar-week-label" aria-live="polite">
          {t('calendar.toolbar.weekOf', { date: weekStart })}
        </span>
        <Button
          variant="white"
          className="btn-icon-only"
          icon="chevron-right"
          aria-label={t('calendar.toolbar.next')}
          onClick={() => setWeekStart((current) => addDaysIso(current, WEEK_LENGTH))}
        />
        <Button variant="white" onClick={() => setWeekStart(mondayOfWeek())}>
          {t('calendar.toolbar.today')}
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
              <li key={day.date} className={`week-card ${WEEK_CARD_VARIANT[day.state]}`}>
                <span className="day-abbr-m">
                  <span className="week-day-abbr">{dayAbbr(day.date)}</span>
                  <span className="week-day-date">{dayMonth(day.date)}</span>
                </span>
                <span className="desc">
                  <span className="week-day-state">{t(myWeekStateKey(day.state))}</span>
                  <span className="week-day-space">{spaceLine(day)}</span>
                </span>
                <i className={`ti ti-${WEEK_CARD_ICON[day.state]}`} aria-hidden="true" />
              </li>
            ))
          )}
        </ul>
      ) : null}

      {!query.isLoading && !query.isError ? <Legend items={myWeekLegend(t)} /> : null}

      <div className="week-actions">
        <Button
          variant="green"
          icon="plus"
          className="week-action-btn"
          onClick={() => setIsRequestOpen(true)}
        >
          {t('calendar.myWeek.requestAction')}
        </Button>
        <Button
          variant="white"
          icon="arrow-back"
          className="week-action-btn"
          disabled={releaseTarget === null}
          onClick={() => setIsReleaseOpen(true)}
        >
          {t('calendar.myWeek.releaseAction')}
        </Button>
      </div>

      {isRequestOpen ? (
        <CreateRequestModal onClose={() => setIsRequestOpen(false)} onCreated={handleRequested} />
      ) : null}

      {isReleaseOpen && releaseTarget ? (
        <ReleaseResourceModal
          parkingSpaceId={releaseTarget.parkingSpaceId}
          spaceLabel={releaseTarget.spaceLabel}
          onClose={() => setIsReleaseOpen(false)}
          onReleased={handleReleased}
        />
      ) : null}
    </section>
  );
}
