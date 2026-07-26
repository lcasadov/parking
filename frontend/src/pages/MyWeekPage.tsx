import { useMemo, useState, type ReactElement } from 'react';
import { motion, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CancelRequestModal } from '../components/CancelRequestModal';
import { CreateRequestModal } from '../components/CreateRequestModal';
import { Legend } from '../components/Legend';
import { PageHeader } from '../components/PageHeader';
import { ReleaseResourceModal } from '../components/ReleaseResourceModal';
import { Spinner } from '../components/Spinner';
import { WaitlistBadge } from '../components/WaitlistBadge';
import { emitApiErrorToast } from '../api/events';
import { useAuth } from '../auth/useAuth';
import { useMyWeekQuery } from '../hooks/useCalendar';
import { useEmployeeFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { useToast } from '../hooks/useToast';
import { toEmployeeFixedResources, type FixedAssignmentGroup } from '../utils/fixedAssignments';
import { myWeekLegend } from '../utils/calendarLegend';
import { isPastDate } from '../utils/releases';
import { todayIso } from '../utils/requests';
import { DUR, EASE } from '../theme/motion';
import type { MyWeekDay, MyWeekDayState } from '../types/calendar';
import type { RequestStatus, ResourceType } from '../types/request';
import {
  addDaysIso,
  dayMonth,
  mondayOfWeek,
  myWeekStateKey,
  weekdayIndex,
} from '../utils/calendar';

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
  waitlisted: boolean;
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

// Preselección al abrir el modal de reserva desde una tarjeta concreta del héroe:
// fecha (HOY/MAÑANA) y recurso libre (plaza o puesto). Sin preselección desde la
// acción principal fija.
interface ReservePreset {
  date?: string;
  resource?: ResourceType;
}

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
      waitlisted: day.waitlisted ?? false,
    },
    {
      resourceType: 'DESK',
      state: day.deskState ?? 'FREE',
      label: day.deskLabel ?? null,
      requestStatus: day.deskRequestStatus ?? null,
      requestId: day.deskRequestId ?? null,
      waitlisted: day.deskWaitlisted ?? false,
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
  if (
    view.state === 'ASSIGNED' &&
    !view.requestStatus &&
    Boolean(view.label) &&
    !isPastDate(date)
  ) {
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

// Un recurso está "libre" (reservable en 1 toque) cuando no tiene reserva ni
// asignación: estado FREE y sin etiqueta de recurso.
function isFreeResource(view: ResourceDayView): boolean {
  return view.state === 'FREE' && !view.label;
}

// Vista EMPLOYEE "Mi Semana" (consume GET /calendar/my-week). Rediseño empleado:
// arriba un HÉROE con HOY y MAÑANA (plaza + puesto de un vistazo, con reserva o
// liberación en un toque) y, debajo, la tira semanal navegable. Multi-recurso:
// cada día muestra el estado de la PLAZA y del PUESTO de forma independiente.
export function MyWeekPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const toast = useToast();
  const reduceMotion = useReducedMotion();
  const [isRequestOpen, setIsRequestOpen] = useState(false);
  const [reservePreset, setReservePreset] = useState<ReservePreset | null>(null);
  const [releaseAction, setReleaseAction] = useState<ReleaseAction | null>(null);

  // El héroe (HOY/MAÑANA) y la tira "próximos días" comparten UNA sola fuente de
  // datos: la semana actual + la siguiente. Así un mismo día no puede mostrar
  // estados distintos en el héroe y en la tira, y la tira nunca enseña días
  // pasados. Se consultan dos semanas para cubrir el borde domingo→lunes (mañana
  // cae en la semana siguiente).
  const heroWeekStart = mondayOfWeek();
  const heroQuery = useMyWeekQuery(heroWeekStart);
  const heroNextQuery = useMyWeekQuery(addDaysIso(heroWeekStart, WEEK_LENGTH));
  const heroDays = useMemo(() => {
    // Une semana actual + siguiente y DEDUPLICA por fecha (defensivo: un día nunca
    // debe aparecer dos veces aunque ambas consultas se solapen). Prevalece la
    // primera aparición (semana actual).
    const merged = [...(heroQuery.data?.days ?? []), ...(heroNextQuery.data?.days ?? [])];
    const byDate = new Map<string, MyWeekDay>();
    for (const day of merged) {
      if (!byDate.has(day.date)) {
        byDate.set(day.date, day);
      }
    }
    return [...byDate.values()];
  }, [heroQuery.data, heroNextQuery.data]);
  const todayDate = todayIso();
  const tomorrowDate = addDaysIso(todayDate, 1);
  const todayDay = heroDays.find((day) => day.date === todayDate);
  const tomorrowDay = heroDays.find((day) => day.date === tomorrowDate);
  const heroLoading = heroQuery.isLoading || heroNextQuery.isLoading;
  const heroReady = !heroLoading && !heroQuery.isError && !heroNextQuery.isError;

  // "Próximos días": desde PASADO MAÑANA (el héroe ya cubre HOY y MAÑANA) hasta 7
  // días vista, tomados de heroDays → sin días pasados y sin discrepancias con el héroe.
  const upcomingEnd = addDaysIso(todayDate, 7);
  const upcomingDays = heroDays.filter(
    (day) => day.date > tomorrowDate && day.date <= upcomingEnd,
  );

  const fixedQuery = useEmployeeFixedAssignmentsQuery(user?.employeeId ?? null);
  const fixedResources = useMemo(
    () => toEmployeeFixedResources(fixedQuery.data ?? []),
    [fixedQuery.data],
  );

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

  function openReserve(preset: ReservePreset | null = null): void {
    setReservePreset(preset);
    setIsRequestOpen(true);
  }

  function closeReserve(): void {
    setIsRequestOpen(false);
    setReservePreset(null);
  }

  function refetchAll(): void {
    void heroQuery.refetch();
    void heroNextQuery.refetch();
  }

  function handleRequested(): void {
    closeReserve();
    refetchAll();
    emitApiErrorToast('requests.mine.created');
  }

  function closeReleaseAction(): void {
    setReleaseAction(null);
  }

  function handleReleased(): void {
    setReleaseAction(null);
    refetchAll();
    toast.success('releases.release.created');
  }

  function handleCancelled(): void {
    setReleaseAction(null);
    refetchAll();
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
    // "Cancelar" SOLO cuando es una solicitud aún PENDIENTE (no tienes el recurso);
    // si ya lo tienes (aprobada o asignación fija) la acción es "Liberar".
    const isCancel = action?.kind === 'CANCEL_REQUEST' && view.requestStatus === 'PENDING';
    return (
      <li key={view.resourceType} className={`week-resource ${RESOURCE_STATE_VARIANT[view.state]}`}>
        <span className="week-resource-icon" aria-hidden="true">
          <i className={`ti ti-${RESOURCE_ICON[view.resourceType]}`} />
        </span>
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

  // Fila de recurso del HÉROE: como renderResource pero más grande y con dos
  // añadidos clave para el empleado — aviso "pendiente · la ubicación puede
  // cambiar" cuando la solicitud está PENDING, y botón "Reservar" en un toque
  // cuando el recurso está libre (preselecciona fecha y recurso en el modal).
  function renderHeroResource(view: ResourceDayView, date: string): ReactElement {
    const action = resolveReleaseAction(view, date, fixedGroupFor(view.resourceType));
    // "Cancelar" SOLO cuando es una solicitud aún PENDIENTE (no tienes el recurso);
    // si ya lo tienes (aprobada o asignación fija) la acción es "Liberar".
    const isCancel = action?.kind === 'CANCEL_REQUEST' && view.requestStatus === 'PENDING';
    const isPending = view.requestStatus === 'PENDING';
    return (
      <li key={view.resourceType} className={`mw-res ${RESOURCE_STATE_VARIANT[view.state]}`}>
        <span className="mw-res-icon" aria-hidden="true">
          <i className={`ti ti-${RESOURCE_ICON[view.resourceType]}`} />
        </span>
        <span className="mw-res-info">
          <span className="mw-res-kind">
            {t(`calendar.myWeek.resourceKind.${view.resourceType}`)}
          </span>
          <span className="mw-res-state">{resourceLine(view)}</span>
          {isPending && view.waitlisted ? <WaitlistBadge /> : null}
          {isPending && !view.waitlisted ? (
            <span className="mw-res-pending" role="status">
              <i className="ti ti-clock" aria-hidden="true" /> {t('requests.pendingBanner.message')}
            </span>
          ) : null}
        </span>
        {action ? (
          <Button
            variant="white"
            icon="arrow-back-up"
            className="mw-res-action"
            onClick={() => setReleaseAction(action)}
          >
            {isCancel ? t('calendar.myWeek.cancelAction') : t('calendar.myWeek.releaseAction')}
          </Button>
        ) : null}
        {!action && isFreeResource(view) ? (
          <Button
            variant="green"
            icon="plus"
            className="mw-res-action"
            onClick={() => openReserve({ date, resource: view.resourceType })}
          >
            {t('calendar.myWeek.hero.reserve')}
          </Button>
        ) : null}
      </li>
    );
  }

  function renderHeroCard(when: 'today' | 'tomorrow', date: string, day?: MyWeekDay): ReactElement {
    return (
      <article className={`mw-hero-card${when === 'today' ? ' is-today' : ''}`}>
        <header className="mw-hero-head">
          <span className="mw-hero-when">{t(`calendar.myWeek.hero.${when}`)}</span>
          <span className="mw-hero-date">
            {dayAbbr(date)} · {dayMonth(date)}
          </span>
        </header>
        {day ? (
          <ul className="mw-hero-resources">
            {resourceViews(day).map((view) => renderHeroResource(view, date))}
          </ul>
        ) : (
          <p className="mw-hero-empty">
            {heroLoading ? t('common.loading') : t('calendar.myWeek.hero.noData')}
          </p>
        )}
      </article>
    );
  }

  // Entrada escalonada de las tarjetas de día (mockup 07): cada tarjeta aparece
  // con un ligero desplazamiento vertical + fade, con un pequeño desfase por
  // índice para dar sensación de "lista viva" sin ser ruidoso. Se anula bajo
  // prefers-reduced-motion (solo cambia opacidad, nunca posición).
  function renderDay(day: MyWeekDay, index: number): ReactElement {
    const isToday = day.date === todayIso();
    return (
      <motion.li
        key={day.date}
        className={`week-day-card${isToday ? ' is-today' : ''}`}
        initial={reduceMotion ? { opacity: 1 } : { opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: DUR.slow, ease: EASE.out, delay: reduceMotion ? 0 : index * 0.04 }}
      >
        <div className="week-day-head">
          <span className="week-day-abbr">{dayAbbr(day.date)}</span>
          <span className="week-day-date">{dayMonth(day.date)}</span>
          {isToday ? (
            <span className="week-day-today-badge">{t('calendar.toolbar.today')}</span>
          ) : null}
        </div>
        <ul className="week-day-resources">
          {resourceViews(day).map((view) => renderResource(view, day.date))}
        </ul>
      </motion.li>
    );
  }

  return (
    <section className="my-week-page" aria-label={t('calendar.myWeek.title')}>
      <PageHeader
        eyebrow={t('calendar.myWeek.eyebrow')}
        title={t('calendar.myWeek.title')}
        description={t('calendar.myWeek.description')}
      />

      {/* HÉROE: lo que tengo HOY y MAÑANA, de un vistazo. */}
      <div className="mw-hero" aria-label={t('calendar.myWeek.hero.sectionLabel')}>
        {renderHeroCard('today', todayDate, todayDay)}
        {renderHeroCard('tomorrow', tomorrowDate, tomorrowDay)}
      </div>

      {/* PRÓXIMOS DÍAS: continuación del héroe (desde pasado mañana), misma fuente. */}
      <section className="mw-week" aria-label={t('calendar.myWeek.weekTitle')}>
        <div className="mw-week-head">
          <h2 className="mw-week-title">{t('calendar.myWeek.weekTitle')}</h2>
        </div>

        {heroLoading ? <Spinner /> : null}

        {heroQuery.isError || heroNextQuery.isError ? (
          <p className="form-error" role="alert">
            {t('calendar.myWeek.loadError')}
          </p>
        ) : null}

        {heroReady ? (
          <ul className="my-week-list">
            {upcomingDays.length === 0 ? (
              <li className="my-week-empty">{t('calendar.myWeek.empty')}</li>
            ) : (
              upcomingDays.map((day, index) => renderDay(day, index))
            )}
          </ul>
        ) : null}

        {heroReady ? <Legend items={myWeekLegend(t)} /> : null}
      </section>

      {/* Acción principal SIEMPRE visible (sticky en móvil). */}
      <div className="mw-cta">
        <Button variant="green" icon="plus" className="mw-cta-btn" onClick={() => openReserve()}>
          {t('calendar.myWeek.requestAction')}
        </Button>
      </div>

      {isRequestOpen ? (
        <CreateRequestModal
          presetDate={reservePreset?.date}
          presetResource={reservePreset?.resource}
          onClose={closeReserve}
          onCreated={handleRequested}
        />
      ) : null}

      {renderReleaseModal()}
    </section>
  );
}
