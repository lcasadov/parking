import { useMemo, useState, type ReactElement } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CancelRequestModal } from '../components/CancelRequestModal';
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
import { isPastDate } from '../utils/releases';
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

// Cómo se libera el recurso de un día: cancelando la solicitud propia (APPROVED
// futura / PENDING) o creando un Release sobre la asignación fija.
type ReleaseKind = 'CANCEL_REQUEST' | 'FIXED_RELEASE';

// Acción de liberación resuelta para el día seleccionado (change release-occupied-resource).
type ReleaseAction =
  | { kind: 'CANCEL_REQUEST'; requestId: number; date: string }
  | { kind: 'FIXED_RELEASE'; parkingSpaceId: number; spaceLabel: string; date: string };

// Determina si el recurso del día es liberable y por qué mecanismo. Un día ocupado
// por la solicitud propia (PENDING, o APPROVED futura) se libera cancelándola; un día
// de asignación fija futura, creando un Release. "Mi Semana" es una vista por-día:
// la acción se refiere siempre al día, no a un recurso global.
function releaseKindForDay(day: MyWeekDay): ReleaseKind | null {
  const hasRequest = typeof day.requestId === 'number';
  if (hasRequest && day.requestStatus === 'PENDING') {
    return 'CANCEL_REQUEST';
  }
  if (hasRequest && day.requestStatus === 'APPROVED' && !isPastDate(day.date)) {
    return 'CANCEL_REQUEST';
  }
  if (
    day.state === 'ASSIGNED' &&
    !day.requestStatus &&
    Boolean(day.parkingSpaceLabel) &&
    !isPastDate(day.date)
  ) {
    return 'FIXED_RELEASE';
  }
  return null;
}

// Resuelve la acción de liberación concreta del día (o null si no es liberable):
// cancelar la solicitud propia, o crear un Release sobre la plaza fija del empleado.
function resolveReleaseAction(
  day: MyWeekDay,
  releaseTarget: ReleaseTarget | null,
): ReleaseAction | null {
  const kind = releaseKindForDay(day);
  if (kind === 'CANCEL_REQUEST' && typeof day.requestId === 'number') {
    return { kind: 'CANCEL_REQUEST', requestId: day.requestId, date: day.date };
  }
  if (kind === 'FIXED_RELEASE' && releaseTarget !== null) {
    return {
      kind: 'FIXED_RELEASE',
      parkingSpaceId: releaseTarget.parkingSpaceId,
      spaceLabel: day.parkingSpaceLabel ?? releaseTarget.spaceLabel,
      date: day.date,
    };
  }
  return null;
}

// Vista EMPLOYEE "Mi Semana" (consume GET /calendar/my-week). tasks §4.3.
// Solo recursos propios; el contrato NO incluye nombres de terceros, por lo que
// esta vista nunca los renderiza. Cada día se muestra como una tarjeta
// (.week-card) con color por estado. Al seleccionar un día con recurso liberable
// (solicitud propia o asignación fija) se habilita "Liberar", que actúa sobre ese día.
export function MyWeekPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [weekStart, setWeekStart] = useState<string>(mondayOfWeek());
  const [isRequestOpen, setIsRequestOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [releaseAction, setReleaseAction] = useState<ReleaseAction | null>(null);

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

  const selectedDay = days.find((day) => day.date === selectedDate) ?? null;
  // Acción de liberación del día seleccionado; null si el día no es liberable.
  // El botón "Liberar" se habilita solo cuando existe una acción resoluble.
  const pendingRelease = selectedDay ? resolveReleaseAction(selectedDay, releaseTarget) : null;
  const canRelease = pendingRelease !== null;

  const showContent = !query.isLoading && !query.isError;

  // Un día es seleccionable si su recurso es liberable por alguno de los dos mecanismos.
  function isSelectable(day: MyWeekDay): boolean {
    return resolveReleaseAction(day, releaseTarget) !== null;
  }

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

  function closeReleaseAction(): void {
    setReleaseAction(null);
  }

  function handleReleased(): void {
    setReleaseAction(null);
    setSelectedDate(null);
    void query.refetch();
    emitApiErrorToast('releases.release.created');
  }

  function handleCancelled(): void {
    setReleaseAction(null);
    setSelectedDate(null);
    void query.refetch();
    emitApiErrorToast('requests.cancel.done');
  }

  // Abre el modal correspondiente a la acción de liberación del día seleccionado.
  function handleReleaseClick(): void {
    if (pendingRelease) {
      setReleaseAction(pendingRelease);
    }
  }

  // Modal de liberación según el mecanismo resuelto (cancelar solicitud / Release fijo).
  function renderReleaseModal(): ReactElement | null {
    if (releaseAction?.kind === 'FIXED_RELEASE') {
      return (
        <ReleaseResourceModal
          parkingSpaceId={releaseAction.parkingSpaceId}
          spaceLabel={releaseAction.spaceLabel}
          presetDate={releaseAction.date}
          onClose={closeReleaseAction}
          onReleased={handleReleased}
        />
      );
    }
    if (releaseAction?.kind === 'CANCEL_REQUEST') {
      return (
        <CancelRequestModal
          requestId={releaseAction.requestId}
          requestedDate={releaseAction.date}
          onClose={closeReleaseAction}
          onCancelled={handleCancelled}
        />
      );
    }
    return null;
  }

  function renderDayBody(day: MyWeekDay): ReactElement {
    return (
      <>
        <span className="day-abbr-m">
          <span className="week-day-abbr">{dayAbbr(day.date)}</span>
          <span className="week-day-date">{dayMonth(day.date)}</span>
        </span>
        <span className="desc">
          <span className="week-day-state">{t(myWeekStateKey(day.state))}</span>
          <span className="week-day-space">{spaceLine(day)}</span>
        </span>
        <i className={`ti ti-${WEEK_CARD_ICON[day.state]}`} aria-hidden="true" />
      </>
    );
  }

  function renderDay(day: MyWeekDay): ReactElement {
    const selectable = isSelectable(day);
    const selected = selectable && day.date === selectedDate;
    const className = `week-card ${WEEK_CARD_VARIANT[day.state]}${selected ? ' selected' : ''}`;
    return (
      <li key={day.date} className={className}>
        {selectable ? (
          <button
            type="button"
            className="week-card-body"
            aria-pressed={selected}
            onClick={() => setSelectedDate(day.date)}
          >
            {renderDayBody(day)}
          </button>
        ) : (
          <div className="week-card-body">{renderDayBody(day)}</div>
        )}
      </li>
    );
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

      {showContent ? (
        <ul className="my-week-list">
          {days.length === 0 ? (
            <li className="my-week-empty">{t('calendar.myWeek.empty')}</li>
          ) : (
            days.map((day) => renderDay(day))
          )}
        </ul>
      ) : null}

      {showContent ? <Legend items={myWeekLegend(t)} /> : null}

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
          disabled={!canRelease}
          onClick={handleReleaseClick}
        >
          {t('calendar.myWeek.releaseAction')}
        </Button>
      </div>

      {isRequestOpen ? (
        <CreateRequestModal onClose={() => setIsRequestOpen(false)} onCreated={handleRequested} />
      ) : null}

      {renderReleaseModal()}
    </section>
  );
}
