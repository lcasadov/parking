import { useMemo, useState, type ReactElement } from 'react';
import { motion, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { CancelRequestModal } from '../components/CancelRequestModal';
import { CreateRequestModal } from '../components/CreateRequestModal';
import { Legend } from '../components/Legend';
import { PageHeader } from '../components/PageHeader';
import { DeskMapButton } from '../components/DeskMapButton';
import { ParkingDirectionsButton } from '../components/ParkingDirectionsButton';
import { ReleaseResourceModal } from '../components/ReleaseResourceModal';
import { ResourceIcon } from '../components/ResourceIcon';
import { WaitlistBadge } from '../components/WaitlistBadge';
import { useAuth } from '../auth/useAuth';
import { useMyWeekQuery } from '../hooks/useCalendar';
import { useEmployeeFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { useWeekendReservableQuery } from '../hooks/useSettings';
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
  weekRangeLabel,
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
// Clave i18n del toast al liberar un recurso (por tipo): "Plaza de parking
// liberada" / "Puesto de trabajo liberado".
function releasedToastKey(resourceType: ResourceType): string {
  return resourceType === 'PARKING'
    ? 'calendar.myWeek.released.PARKING'
    : 'calendar.myWeek.released.DESK';
}

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
  | {
      kind: 'CANCEL_REQUEST';
      requestId: number;
      date: string;
      pending: boolean;
      resourceType: ResourceType;
    }
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
    return {
      kind: 'CANCEL_REQUEST',
      requestId: view.requestId,
      date,
      pending: view.requestStatus === 'PENDING',
      resourceType: view.resourceType,
    };
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

// Marcador tipo señalización del recurso: prefijo P (plaza) / D (puesto) + numeral
// con punto medio (P·1004 / D·02). Sin recurso → guion.
function markerLabel(view: ResourceDayView): string {
  if (!view.label) {
    return '—';
  }
  const digits = view.label.replace(/\D/g, '');
  const prefix = view.resourceType === 'PARKING' ? 'P' : 'D';
  return digits ? `${prefix}·${digits}` : view.label;
}

// ¿El recurso es una asignación FIJA (no una solicitud puntual)? ASSIGNED con
// etiqueta y sin estado de solicitud propia.
function isFixedResource(view: ResourceDayView): boolean {
  return view.state === 'ASSIGNED' && !view.requestStatus && Boolean(view.label);
}

// Reservable en 1 toque: libre (FREE) o LIBERADO (lo soltaste ese día y puedes
// volver a reclamarlo). En ambos casos el recurso está disponible para ti.
function isReservable(view: ResourceDayView): boolean {
  return isFreeResource(view) || view.state === 'RELEASED';
}

// Tienes ese recurso asignado ese día (con etiqueta concreta): habilita el botón
// "Mapa" (puesto) o "Ir al parking" (plaza).
function hasAssignedDesk(view: ResourceDayView): boolean {
  return view.resourceType === 'DESK' && view.state === 'ASSIGNED' && Boolean(view.label);
}

function hasAssignedParking(view: ResourceDayView): boolean {
  return view.resourceType === 'PARKING' && view.state === 'ASSIGNED' && Boolean(view.label);
}

// Sábado (6) o domingo (0). Cuando el admin no admite reserva en finde, esos días
// se ocultan de "Próximos días" y no ofrecen reservar.
function isWeekendIso(date: string): boolean {
  const day = weekdayIndex(date);
  return day === 0 || day === 6;
}

// ¿Mostrar el botón "Reservar"? Recurso reservable (libre o liberado → puedes
// reclamarlo), sin acción de liberar, y —si el admin no admite finde— que no sea
// sábado/domingo. Un día liberado ofrece "Reservar" (reusa el alta: te da tu fijo
// si sigue libre, si no otro; el backend valida disponibilidad).
function showReserve(
  view: ResourceDayView,
  date: string,
  hasAction: boolean,
  weekendReservable: boolean,
): boolean {
  return !hasAction && isReservable(view) && (weekendReservable || !isWeekendIso(date));
}

// Vista EMPLOYEE "Mi Semana" (consume GET /calendar/my-week). Rediseño empleado:
// arriba un HÉROE con HOY y MAÑANA (plaza + puesto de un vistazo, con reserva o
// liberación en un toque) y, debajo, la tira semanal navegable. Multi-recurso:
// cada día muestra el estado de la PLAZA y del PUESTO de forma independiente.
export function MyWeekPage() {
  const { t, i18n } = useTranslation();
  const { user } = useAuth();
  const toast = useToast();
  const reduceMotion = useReducedMotion();
  // Fin de semana: si el admin no lo admite, se ocultan sáb/dom del empleado.
  const weekendReservable = useWeekendReservableQuery().data ?? false;
  const [isRequestOpen, setIsRequestOpen] = useState(false);
  const [reservePreset, setReservePreset] = useState<ReservePreset | null>(null);
  const [releaseAction, setReleaseAction] = useState<ReleaseAction | null>(null);
  // Semana visible en "Próximos días" (0 = la ventana inmediata desde pasado
  // mañana). El héroe HOY/MAÑANA permanece fijo; solo navega esta tira.
  const [weekOffset, setWeekOffset] = useState(0);

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
  const todayDay = heroDays.find((day) => day.date === todayDate);
  const heroLoading = heroQuery.isLoading || heroNextQuery.isLoading;

  // "Próximos días": una SEMANA LABORAL completa (L–V; L–D si se admite finde),
  // navegable por semanas. offset 0 = semana actual, mostrando solo los días > MAÑANA
  // (el héroe ya cubre HOY y MAÑANA) y no pasados → a media semana puede quedar vacía.
  // No se retrocede por debajo de 0 (no mostramos semanas pasadas).
  const shownWeekStart = addDaysIso(mondayOfWeek(), weekOffset * WEEK_LENGTH);
  const shownWeekEnd = addDaysIso(shownWeekStart, weekendReservable ? 6 : 4);
  const weekQuery = useMyWeekQuery(shownWeekStart);
  const upcomingDays = useMemo(() => {
    return (weekQuery.data?.days ?? [])
      .filter((day) => day.date >= todayDate)
      .filter((day) => weekendReservable || !isWeekendIso(day.date))
      .sort((a, b) => a.date.localeCompare(b.date));
  }, [weekQuery.data, todayDate, weekendReservable]);
  const upcomingLoading = weekQuery.isLoading;
  const upcomingReady = !upcomingLoading && !weekQuery.isError;

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
      const label = t(`calendar.myWeek.resourceLabel.${view.resourceType}`, { label: view.label });
      // Un recurso liberado conserva su etiqueta (es tu fija), pero hay que dejar
      // CLARO que ese día está liberado (si no, parece asignado y en un color raro).
      return view.state === 'RELEASED'
        ? `${label} · ${t('calendar.myWeek.states.RELEASED')}`
        : label;
    }
    // Sin recurso ese día: mensaje específico por tipo ("Sin plaza/puesto este día").
    if (view.state === 'FREE') {
      return t(`calendar.myWeek.noResource.${view.resourceType}`);
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
    void weekQuery.refetch();
  }

  function handleRequested(): void {
    // El toast de éxito lo emite el propio modal (toast.success con el mensaje
    // correcto según aprobada/pendiente/lista de espera). Aquí NO se emite otro
    // (antes salía un segundo toast en rojo "Solicitud creada.").
    closeReserve();
    refetchAll();
  }

  function closeReleaseAction(): void {
    setReleaseAction(null);
  }

  function handleReleased(): void {
    const resourceType =
      releaseAction?.kind === 'FIXED_RELEASE' ? releaseAction.resourceType : 'PARKING';
    setReleaseAction(null);
    refetchAll();
    toast.success(releasedToastKey(resourceType));
  }

  // "Liberar" y "Cancelar" comparten modal (CancelRequestModal), pero el mensaje
  // difiere: renunciar a una solicitud PENDIENTE = "solicitud cancelada";
  // liberar una reserva que YA tenías (aprobada) = "plaza/puesto liberado".
  function handleCancelled(): void {
    const action = releaseAction;
    const isRelease = action?.kind === 'CANCEL_REQUEST' && !action.pending;
    const resourceType = action?.kind === 'CANCEL_REQUEST' ? action.resourceType : 'PARKING';
    setReleaseAction(null);
    refetchAll();
    if (isRelease) {
      toast.success(releasedToastKey(resourceType));
    } else {
      // Cancelar una solicitud es una acción completada con éxito → toast verde
      // (antes salía en rojo por usar emitApiErrorToast).
      toast.success('requests.cancel.done');
    }
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
          mode={releaseAction.pending ? 'cancel' : 'release'}
          onClose={closeReleaseAction}
          onCancelled={handleCancelled}
        />
      );
    }
    return null;
  }

  // Slot COMPACTO de recurso en la línea de tránsito (tema Wayfinding): lámpara de
  // estado + marcador mono (P·12 / D·02). El slot es accionable — reservable → abre
  // reserva; liberable → abre liberación — para no perder función en la vista de un
  // vistazo (las acciones con texto viven en el héroe HOY/MAÑANA).
  function renderResource(view: ResourceDayView, date: string): ReactElement {
    const action = resolveReleaseAction(view, date, fixedGroupFor(view.resourceType));
    const isCancel = action?.kind === 'CANCEL_REQUEST' && view.requestStatus === 'PENDING';
    const reservable = showReserve(view, date, Boolean(action), weekendReservable);
    const variant = RESOURCE_STATE_VARIANT[view.state];
    const kindClass = view.resourceType === 'PARKING' ? 'is-parking' : 'is-desk';
    const marker = markerLabel(view);
    const kindName = t(`calendar.myWeek.resourceKind.${view.resourceType}`);
    const actionHint = reservable ? 'ti-plus' : action ? 'ti-arrow-back-up' : null;
    const actionText = reservable
      ? t('calendar.myWeek.hero.reserve')
      : action
        ? isCancel
          ? t('calendar.myWeek.cancelAction')
          : t('calendar.myWeek.releaseAction')
        : null;

    const inner = (
      <>
        <span className="mw-slot-lamp" aria-hidden="true" />
        <span className="mw-slot-icon" aria-hidden="true">
          <ResourceIcon type={view.resourceType} />
        </span>
        <span className="mw-slot-marker">{marker}</span>
        {actionText ? (
          <span className="mw-slot-act">
            <i className={`ti ${actionHint}`} aria-hidden="true" /> {actionText}
          </span>
        ) : null}
      </>
    );

    const slotClass = `mw-slot ${variant} ${kindClass}`;

    if (reservable) {
      return (
        <li key={view.resourceType} className="mw-slot-item">
          <button
            type="button"
            className={slotClass}
            aria-label={`${t('calendar.myWeek.hero.reserve')} · ${kindName} · ${resourceLine(view)}`}
            onClick={() => openReserve({ date, resource: view.resourceType })}
          >
            {inner}
          </button>
        </li>
      );
    }
    if (action) {
      const label = isCancel ? t('calendar.myWeek.cancelAction') : t('calendar.myWeek.releaseAction');
      return (
        <li key={view.resourceType} className="mw-slot-item">
          <button
            type="button"
            className={slotClass}
            aria-label={`${label} · ${kindName} · ${resourceLine(view)}`}
            onClick={() => setReleaseAction(action)}
          >
            {inner}
          </button>
        </li>
      );
    }
    return (
      <li key={view.resourceType} className="mw-slot-item">
        <div className={`${slotClass} is-static`} title={`${kindName} · ${resourceLine(view)}`}>
          {inner}
        </div>
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
          <ResourceIcon type={view.resourceType} />
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
        <span className="mw-res-actions">
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
          {showReserve(view, date, Boolean(action), weekendReservable) ? (
            <Button
              variant="green"
              icon="plus"
              className="mw-res-action"
              onClick={() => openReserve({ date, resource: view.resourceType })}
            >
              {t('calendar.myWeek.hero.reserve')}
            </Button>
          ) : null}
          {hasAssignedDesk(view) && view.label ? (
            <DeskMapButton deskLabel={view.label} date={date} className="mw-res-action" />
          ) : null}
          {hasAssignedParking(view) ? <ParkingDirectionsButton className="mw-res-action" /> : null}
        </span>
      </li>
    );
  }

  // Recurso principal del shield (HOY): la plaza con etiqueta, o el puesto con
  // etiqueta, o el primer recurso como referencia (aunque esté libre).
  function primaryHeroView(day: MyWeekDay): ResourceDayView {
    const views = resourceViews(day);
    return (
      views.find((v) => v.resourceType === 'PARKING' && Boolean(v.label)) ??
      views.find((v) => Boolean(v.label)) ??
      views[0]
    );
  }

  // Fila COMPACTA del panel del shield: icono + lámpara, tipo + marcador, y pill
  // "Fija" (asignación fija) o el estado. Sin acciones (van en un pie único).
  function renderShieldRow(view: ResourceDayView): ReactElement {
    const variant = RESOURCE_STATE_VARIANT[view.state];
    const isDesk = view.resourceType === 'DESK';
    const kindName = t(`calendar.myWeek.resourceKind.${view.resourceType}`);
    const fixed = isFixedResource(view);
    const tag = fixed ? t('calendar.myWeek.heroShield.fixed') : t(`calendar.myWeek.states.${view.state}`);
    // Liberado o libre → HOY no tienes ese recurso: muestra "Sin plaza/puesto" en
    // vez del número (el D·01 liberado confundía, parecía asignado).
    const empty = view.state === 'RELEASED' || view.state === 'FREE' || !view.label;
    const markerText = empty
      ? t(isDesk ? 'calendar.myWeek.heroShield.noDesk' : 'calendar.myWeek.heroShield.noParking')
      : markerLabel(view);
    return (
      <li key={view.resourceType} className={`mw-srow ${variant}${empty ? ' is-empty' : ''}`}>
        <span className="mw-srow-ic" aria-hidden="true">
          <ResourceIcon type={view.resourceType} />
          <span className="mw-srow-dot" />
        </span>
        <span className="mw-srow-info">
          <span className="mw-srow-kind">{kindName}</span>
          <span className="mw-srow-marker">{markerText}</span>
        </span>
        <span className={`mw-srow-tag${fixed ? ' is-fixed' : ''}`}>{tag}</span>
      </li>
    );
  }

  // Acciones del héroe HOY recogidas en un ÚNICO pie (reservar / liberar / plano /
  // ir al parking), etiquetadas por recurso para no perder claridad ni función.
  function heroActions(day: MyWeekDay, date: string): ReactElement[] {
    const buttons: ReactElement[] = [];
    resourceViews(day).forEach((view) => {
      const action = resolveReleaseAction(view, date, fixedGroupFor(view.resourceType));
      const reservable = showReserve(view, date, Boolean(action), weekendReservable);
      const kindLower = t(
        view.resourceType === 'DESK'
          ? 'calendar.myWeek.heroShield.kindLabelDesk'
          : 'calendar.myWeek.heroShield.kindLabelParking',
      ).toLowerCase();
      if (reservable) {
        buttons.push(
          <Button
            key={`reserve-${view.resourceType}`}
            variant="green"
            icon="plus"
            onClick={() => openReserve({ date, resource: view.resourceType })}
          >
            {t('calendar.myWeek.hero.reserve')} {kindLower}
          </Button>,
        );
      } else if (action) {
        const isCancel = action.kind === 'CANCEL_REQUEST' && view.requestStatus === 'PENDING';
        buttons.push(
          <Button
            key={`release-${view.resourceType}`}
            variant="white"
            icon="arrow-back-up"
            onClick={() => setReleaseAction(action)}
          >
            {(isCancel ? t('calendar.myWeek.cancelAction') : t('calendar.myWeek.releaseAction'))} {kindLower}
          </Button>,
        );
      }
      if (hasAssignedDesk(view) && view.label) {
        buttons.push(
          <DeskMapButton key={`map-${view.resourceType}`} deskLabel={view.label} date={date} />,
        );
      }
      if (hasAssignedParking(view)) {
        buttons.push(<ParkingDirectionsButton key={`dir-${view.resourceType}`} />);
      }
    });
    return buttons;
  }

  // Héroe HOY como MARCADOR (shield): numeral gigante del recurso + panel de filas
  // compactas + pie de acciones único (tema Wayfinding).
  function renderHeroShield(date: string, day: MyWeekDay): ReactElement {
    const primary = primaryHeroView(day);
    const variant = RESOURCE_STATE_VARIANT[primary.state];
    const isDesk = primary.resourceType === 'DESK';
    const your = t(isDesk ? 'calendar.myWeek.heroShield.yourDesk' : 'calendar.myWeek.heroShield.yourParking');
    const kind = t(isDesk ? 'calendar.myWeek.heroShield.kindDesk' : 'calendar.myWeek.heroShield.kindParking');
    const actions = heroActions(day, date);
    return (
      <article className={`mw-hero-card mw-hero-shield is-today ${variant}`}>
        <div className="mw-shield">
          <div className="mw-shield-tag">
            <span>
              {your} · {t('calendar.myWeek.hero.today')}
            </span>
            <span>
              {dayAbbr(date)} · {dayMonth(date)}
            </span>
          </div>
          <div className="mw-shield-num">{markerLabel(primary)}</div>
          <div className="mw-shield-sub">
            <span>{kind}</span>
            <span className="mw-shield-state">
              <i aria-hidden="true" /> {t(`calendar.myWeek.states.${primary.state}`)}
            </span>
          </div>
        </div>
        <div className="mw-shield-side">
          <ul className="mw-srows">{resourceViews(day).map((view) => renderShieldRow(view))}</ul>
          {actions.length > 0 ? <div className="mw-shield-acts">{actions}</div> : null}
        </div>
      </article>
    );
  }

  function renderHeroCard(
    when: 'today' | 'tomorrow',
    date: string,
    day?: MyWeekDay,
  ): ReactElement | null {
    // Fin de semana no reservable → no se muestra la tarjeta del héroe de ese día.
    if (!weekendReservable && isWeekendIso(date)) {
      return null;
    }
    // HOY con datos → shield marcador; el resto (MAÑANA / cargando) → tarjeta compacta.
    if (when === 'today' && day) {
      return renderHeroShield(date, day);
    }
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
    const isTomorrow = day.date === addDaysIso(todayIso(), 1);
    const stationMod = isToday ? ' is-today' : isTomorrow ? ' is-tomorrow' : '';
    return (
      <motion.li
        key={day.date}
        className={`week-day-card mw-station${stationMod}`}
        initial={reduceMotion ? { opacity: 1 } : { opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: DUR.slow, ease: EASE.out, delay: reduceMotion ? 0 : index * 0.04 }}
      >
        {/* Nodo de estación sobre el carril de la línea de tránsito (tema Wayfinding). */}
        {isToday ? <span className="mw-station-hoy" aria-hidden="true">{t('calendar.myWeek.hero.today')}</span> : null}
        {isTomorrow ? (
          <span className="mw-station-manana" aria-hidden="true">{t('calendar.myWeek.hero.tomorrow')}</span>
        ) : null}
        <span className="mw-station-node" aria-hidden="true" />
        <div className="week-day-head">
          <span className="week-day-abbr">{dayAbbr(day.date)}</span>
          <span className="week-day-date">{dayMonth(day.date)}</span>
          {isToday ? (
            <span className="week-day-today-badge">{t('calendar.toolbar.today')}</span>
          ) : isTomorrow ? (
            <span className="week-day-tomorrow-badge">{t('calendar.myWeek.hero.tomorrow')}</span>
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
      </div>

      {/* PRÓXIMOS DÍAS: ventana de 7 días navegable por semanas (el héroe con
          HOY/MAÑANA se queda fijo arriba). */}
      <section className="mw-week" aria-label={t('calendar.myWeek.weekTitle')}>
        <div className="mw-week-head">
          <h2 className="mw-week-title">{t('calendar.myWeek.weekTitle')}</h2>
          <div className="mw-week-nav">
            <span className="mw-week-range">
              {weekRangeLabel(shownWeekStart, shownWeekEnd, i18n.language)}
            </span>
            <button
              type="button"
              className="mw-week-nav-btn"
              disabled={weekOffset === 0}
              aria-label={t('calendar.myWeek.prevWeek')}
              onClick={() => setWeekOffset((offset) => Math.max(0, offset - 1))}
            >
              <i className="ti ti-chevron-left" aria-hidden="true" />
            </button>
            <button
              type="button"
              className="mw-week-nav-btn"
              aria-label={t('calendar.myWeek.nextWeek')}
              onClick={() => setWeekOffset((offset) => offset + 1)}
            >
              <i className="ti ti-chevron-right" aria-hidden="true" />
            </button>
          </div>
        </div>

        {upcomingLoading ? (
          <ul className="my-week-list" aria-hidden="true">
            {[0, 1, 2].map((i) => (
              <li key={i} className="week-day-card week-day-skeleton" />
            ))}
          </ul>
        ) : null}

        {weekQuery.isError ? (
          <p className="form-error" role="alert">
            {t('calendar.myWeek.loadError')}
          </p>
        ) : null}

        {upcomingReady ? (
          <div className="mw-week-scroll">
            <ul className="my-week-list">
              {upcomingDays.length === 0 ? (
                <li className="my-week-empty">{t('calendar.myWeek.emptyWeek')}</li>
              ) : (
                upcomingDays.map((day, index) => renderDay(day, index))
              )}
            </ul>
          </div>
        ) : null}

        {upcomingReady ? <Legend items={myWeekLegend(t)} /> : null}
      </section>

      {/* Acción principal SIEMPRE visible (sticky en móvil). */}
      <div className="mw-cta">
        <Button variant="green" icon="plus" className="mw-cta-btn" onClick={() => openReserve()}>
          {t('layout.nav.newReservation')}
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
