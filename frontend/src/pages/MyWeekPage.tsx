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
import { toEmployeeFixedResources, type FixedAssignmentGroup } from '../utils/fixedAssignments';
import { myWeekLegend } from '../utils/calendarLegend';
import { isPastDate } from '../utils/releases';
import type { MyWeekDay, MyWeekDayState } from '../types/calendar';
import type { RequestStatus, ResourceType } from '../types/request';
import { addDaysIso, dayMonth, mondayOfWeek, myWeekStateKey, weekdayIndex } from '../utils/calendar';

const WEEK_LENGTH = 7;

// Variante de tarjeta (color por estado) e icono por estado del recurso, según el
// mockup 07 (empleado móvil). Sin literales repetidos (S1192).
const RESOURCE_STATE_VARIANT: Record<MyWeekDayState, string> = {
  ASSIGNED: 'assigned',
  RELEASED: 'released',
  REQUEST_PENDING: 'request',
  FREE: 'free',
};
const RESOURCE_ICON: Record<ResourceType, string> = {
  PARKING: 'parking',
  DESK: 'armchair',
};

// Vista por-recurso (plaza o puesto) de un día: el contrato de "Mi Semana" lleva
// ambos recursos en paralelo (design §D4). Se deriva una vista por cada recurso.
interface ResourceDayView {
  resourceType: ResourceType;
  state: MyWeekDayState;
  label: string | null;
  requestStatus: RequestStatus | null;
  requestId: number | null;
}

// Cómo se libera el recurso de un día: cancelando la solicitud propia (APPROVED
// futura / PENDING) o creando un Release sobre la asignación fija.
type ReleaseAction =
  | { kind: 'CANCEL_REQUEST'; requestId: number; date: string }
  | {
      kind: 'FIXED_RELEASE';
      parkingSpaceId: number;
      spaceLabel: string;
      resourceType: ResourceType;
      date: string;
    };

// Deriva las dos vistas de recurso (plaza y puesto) de un día de "Mi Semana".
// Los campos `desk*` describen el puesto; los base describen la plaza. Un recurso
// sin datos de puesto se trata como FREE (retrocompatible con backend anterior).
function resourceViews(day: MyWeekDay): ResourceDayView[] {
  return [
    {
      resourceType: 'PARKING',
      state: day.state,
      label: day.parkingSpaceLabel ?? null,
      requestStatus: day.requestStatus ?? null,
      requestId: day.requestId ?? null,
    },
    {
      resourceType: 'DESK',
      state: day.deskState ?? 'FREE',
      label: day.deskLabel ?? null,
      requestStatus: day.deskRequestStatus ?? null,
      requestId: day.deskRequestId ?? null,
    },
  ];
}

// Determina si el recurso del día es liberable y por qué mecanismo. Un recurso
// ocupado por la solicitud propia (PENDING, o APPROVED futura) se libera
// cancelándola; una asignación fija futura, creando un Release.
function releaseKindForView(
  view: ResourceDayView,
  date: string,
): 'CANCEL_REQUEST' | 'FIXED_RELEASE' | null {
  const hasRequest = typeof view.requestId === 'number';
  if (hasRequest && view.requestStatus === 'PENDING') {
    return 'CANCEL_REQUEST';
  }
  if (hasRequest && view.requestStatus === 'APPROVED' && !isPastDate(date)) {
    return 'CANCEL_REQUEST';
  }
  if (view.state === 'ASSIGNED' && !view.requestStatus && Boolean(view.label) && !isPastDate(date)) {
    return 'FIXED_RELEASE';
  }
  return null;
}

// Resuelve la acción de liberación concreta de un recurso del día (o null si no es
// liberable): cancelar la solicitud propia, o crear un Release sobre el recurso fijo.
function resolveReleaseAction(
  view: ResourceDayView,
  date: string,
  fixedGroup: FixedAssignmentGroup | null,
): ReleaseAction | null {
  const kind = releaseKindForView(view, date);
  if (kind === 'CANCEL_REQUEST' && typeof view.requestId === 'number') {
    return { kind: 'CANCEL_REQUEST', requestId: view.requestId, date };
  }
  if (kind === 'FIXED_RELEASE' && fixedGroup !== null) {
    return {
      kind: 'FIXED_RELEASE',
      parkingSpaceId: fixedGroup.parkingSpaceId,
      spaceLabel: view.label ?? `#${fixedGroup.parkingSpaceId}`,
      resourceType: view.resourceType,
      date,
    };
  }
  return null;
}

// Vista EMPLOYEE "Mi Semana" (consume GET /calendar/my-week). Multi-recurso
// (employee-portal spec, restructure-admin-workflows): cada día muestra el estado
// de la PLAZA y del PUESTO de forma independiente, y ofrece liberar/cancelar por
// recurso. Ya no rotula "sin plaza" cuando el empleado tiene puesto ese día.
export function MyWeekPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [weekStart, setWeekStart] = useState<string>(mondayOfWeek());
  const [isRequestOpen, setIsRequestOpen] = useState(false);
  const [releaseAction, setReleaseAction] = useState<ReleaseAction | null>(null);

  const query = useMyWeekQuery(weekStart);
  const days = query.data?.days ?? [];

  const fixedQuery = useEmployeeFixedAssignmentsQuery(user?.employeeId ?? null);
  const fixedResources = useMemo(
    () => toEmployeeFixedResources(fixedQuery.data ?? []),
    [fixedQuery.data],
  );

  const showContent = !query.isLoading && !query.isError;

  function fixedGroupFor(resourceType: ResourceType): FixedAssignmentGroup | null {
    return resourceType === 'DESK' ? fixedResources.desk : fixedResources.parking;
  }

  function dayAbbr(date: string): string {
    return t(`calendar.weekdaysShort.${weekdayIndex(date)}`);
  }

  function resourceLine(view: ResourceDayView): string {
    if (view.label) {
      return t(`calendar.myWeek.resourceLabel.${view.resourceType}`, { label: view.label });
    }
    return t(myWeekStateKey(view.state));
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
    void query.refetch();
    emitApiErrorToast('releases.release.created');
  }

  function handleCancelled(): void {
    setReleaseAction(null);
    void query.refetch();
    emitApiErrorToast('requests.cancel.done');
  }

  // Modal de liberación según el mecanismo resuelto (cancelar solicitud / Release fijo).
  function renderReleaseModal(): ReactElement | null {
    if (releaseAction?.kind === 'FIXED_RELEASE') {
      return (
        <ReleaseResourceModal
          parkingSpaceId={releaseAction.parkingSpaceId}
          spaceLabel={releaseAction.spaceLabel}
          resourceType={releaseAction.resourceType}
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

  function renderResource(view: ResourceDayView, date: string): ReactElement {
    const action = resolveReleaseAction(view, date, fixedGroupFor(view.resourceType));
    const isCancel = action?.kind === 'CANCEL_REQUEST';
    return (
      <li
        key={view.resourceType}
        className={`week-resource ${RESOURCE_STATE_VARIANT[view.state]}`}
      >
        <i className={`ti ti-${RESOURCE_ICON[view.resourceType]}`} aria-hidden="true" />
        <span className="week-resource-info">
          <span className="week-resource-kind">
            {t(`calendar.myWeek.resourceKind.${view.resourceType}`)}
          </span>
          <span className="week-resource-state">{resourceLine(view)}</span>
        </span>
        {action ? (
          <Button
            variant="white"
            icon="arrow-back-up"
            className="week-resource-action"
            onClick={() => setReleaseAction(action)}
          >
            {isCancel ? t('calendar.myWeek.cancelAction') : t('calendar.myWeek.releaseAction')}
          </Button>
        ) : null}
      </li>
    );
  }

  function renderDay(day: MyWeekDay): ReactElement {
    return (
      <li key={day.date} className="week-day-card">
        <div className="week-day-head">
          <span className="week-day-abbr">{dayAbbr(day.date)}</span>
          <span className="week-day-date">{dayMonth(day.date)}</span>
        </div>
        <ul className="week-day-resources">
          {resourceViews(day).map((view) => renderResource(view, day.date))}
        </ul>
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
      </div>

      {isRequestOpen ? (
        <CreateRequestModal onClose={() => setIsRequestOpen(false)} onCreated={handleRequested} />
      ) : null}

      {renderReleaseModal()}
    </section>
  );
}
