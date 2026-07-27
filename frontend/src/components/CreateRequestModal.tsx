import { useMemo, useRef, useState, type FormEvent, type ReactNode } from 'react';
import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { InfoBanner } from './InfoBanner';
import { MultiSelectCalendar } from './MultiSelectCalendar';
import { ResourceChoiceCard } from './ResourceChoiceCard';
import { getApiError, getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useAuth } from '../auth/useAuth';
import { useBackClose } from '../hooks/useBackClose';
import { useCreateRequest, useMyRequestsQuery } from '../hooks/useRequests';
import { useEmployeeFixedAssignmentsQuery } from '../hooks/useFixedAssignments';
import { useToast } from '../hooks/useToast';
import { useApprovalModeQuery, useWeekendReservableQuery } from '../hooks/useSettings';
import { addDaysIso, isoWeekday, longDate, shortDayMonth } from '../utils/calendar';
import { groupFixedAssignments } from '../utils/fixedAssignments';
import { isTodayOrFuture, todayIso } from '../utils/requests';
import type { Request, RequestCreateRequest, ResourceType } from '../types/request';

// ¿Mostrar el aviso "se te reasignará tu fijo"? Hay fijo propio del tipo elegido
// ese día de la semana, hay fecha y no hay conflicto (no lo tienes ya reservado).
// Si sigue libre, el backend le reasigna EL SUYO (Feature A). Extraído a módulo
// para mantener baja la complejidad del componente (S3776).
function shouldShowFixedHint(
  date: string,
  groups: ReturnType<typeof groupFixedAssignments>,
  parkingSelected: boolean,
  deskSelected: boolean,
): boolean {
  if (date === '') {
    return false;
  }
  const weekday = isoWeekday(date);
  const has = (type: 'PARKING' | 'DESK') =>
    groups.some((group) => group.resourceType === type && group.days.includes(weekday));
  return (parkingSelected && has('PARKING')) || (deskSelected && has('DESK'));
}

// Alterna un día en la lista (calendario multi-select), respetando el tope `max`.
// Extraído a módulo para mantener baja la complejidad del componente (S3776).
function toggleDateInList(prev: string[], iso: string, max: number): string[] {
  if (prev.includes(iso)) {
    return prev.filter((d) => d !== iso);
  }
  if (prev.length >= max) {
    return prev;
  }
  return [...prev, iso].sort((a, b) => a.localeCompare(b));
}

// Días con una solicitud propia viva (APPROVED/PENDING) → se marcan con un punto en
// el calendario de reserva ("ya tienes algo ese día"). Extraído a módulo (S3776).
function reservedDaySet(
  requests: Request[] | undefined,
  resourceType: ResourceType,
): Set<string> {
  const set = new Set<string>();
  for (const request of requests ?? []) {
    const live = request.status === 'APPROVED' || request.status === 'PENDING';
    if (live && (request.resourceType ?? 'PARKING') === resourceType) {
      set.add(request.requestedDate);
    }
  }
  return set;
}

// Días seleccionados en los que YA tienes una reserva viva de ese recurso. Solo
// reservas vivas (no fijo): el fijo puede haberse liberado ese día y aún se puede
// re-reservar. Devuelve [] si el recurso no está seleccionado.
function alreadyReservedDates(
  dates: string[],
  selected: boolean,
  liveDays: ReadonlySet<string>,
): string[] {
  return selected ? dates.filter((date) => liveDays.has(date)) : [];
}

// Aviso (gris, bajo las tarjetas) de los días seleccionados que ya tienes
// reservados y por tanto se omitirán. `anythingNew` indica si aún queda algún
// día×recurso por crear: si no, el mensaje pide elegir otro día/recurso (el botón
// queda deshabilitado). Devuelve null si no hay duplicados.
function duplicateNote(
  dupParking: string[],
  dupDesk: string[],
  anythingNew: boolean,
  locale: string,
  t: TFunction,
): string | null {
  if (dupParking.length === 0 && dupDesk.length === 0) {
    return null;
  }
  const fmt = (dates: string[]) => dates.map((d) => shortDayMonth(d, locale)).join(', ');
  const parts: string[] = [];
  if (dupParking.length > 0) {
    parts.push(t('requests.create.alreadyHave.parking', { dates: fmt(dupParking) }));
  }
  if (dupDesk.length > 0) {
    parts.push(t('requests.create.alreadyHave.desk', { dates: fmt(dupDesk) }));
  }
  const tail = anythingNew
    ? t('requests.create.alreadyHave.skipTail')
    : t('requests.create.alreadyHave.blockTail');
  return `${parts.join(' ')} ${tail}`;
}

// Texto-resumen de la selección de días bajo el calendario.
function reserveDateSummary(
  dates: string[],
  primaryDate: string,
  locale: string,
  t: TFunction,
): string {
  if (dates.length === 0) {
    return t('requests.create.pickDatePrompt');
  }
  if (dates.length === 1) {
    return longDate(primaryDate, locale);
  }
  return t('requests.create.selectedCount', { count: dates.length });
}

interface CreateRequestModalProps {
  onClose: () => void;
  onCreated: () => void;
  // Preselección opcional al abrir desde una tarjeta del héroe de "Mi Semana":
  // fecha (HOY/MAÑANA) y recurso libre concreto (plaza o puesto).
  presetDate?: string;
  presetResource?: ResourceType;
}

const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;
const NO_AVAILABILITY = 'NO_AVAILABILITY';
// Tope de días por reserva múltiple (decisión de producto): más días = "contacta
// con el administrador" (sería una asignación fija, no una reserva puntual).
const MAX_RESERVE_DAYS = 5;

// Un envio 409 es NO_AVAILABILITY (modo automatico sin hueco): distinto de la
// solicitud duplicada. Se reutiliza para clasificar candidatos a lista de
// espera (capability request-waitlist) y para el toast de error generico.
function isNoAvailabilityConflict(error: unknown): boolean {
  return getStatus(error) === HTTP_CONFLICT && getApiError(error)?.error === NO_AVAILABILITY;
}

// Traduce el error del servidor a la clave i18n del toast (tasks §4.4 y §6.4).
// El 409 distingue entre solicitud duplicada y NO_AVAILABILITY (modo automatico sin
// plaza libre para la fecha).
function toastKeyForError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_CONFLICT) {
    return isNoAvailabilityConflict(error) ? 'requests.errors.noAvailability' : 'requests.errors.duplicate';
  }
  if (status === HTTP_BAD_REQUEST) {
    return 'requests.errors.window';
  }
  return 'requests.errors.generic';
}

// Clave i18n del toast de exito: una solicitud nacida en lista de espera
// (capability request-waitlist) tiene prioridad sobre el aviso de aprobacion
// instantanea; en modo automatico sin espera nace APPROVED; en manual PENDING
// (tasks §6.4).
function successToastKey(created: Request[]): string {
  if (created.some((request) => request.waitlisted)) {
    return 'requests.mine.createdWaitlisted';
  }
  return created.some((request) => request.status === 'APPROVED')
    ? 'requests.mine.createdApproved'
    : 'requests.mine.created';
}

// Resultado de un intento de envio (posiblemente varios recursos a la vez):
// separa lo creado con exito de los candidatos a lista de espera (409
// NO_AVAILABILITY en modo automatico, aun sin optar por `waitlist`) y de
// cualquier otro error a mostrar tal cual.
interface SubmitOutcome {
  createdRequests: Request[];
  waitlistCandidates: ResourceType[];
  otherError: unknown | null;
}

// Un par (FECHA × RECURSO) a enviar. Los días que ya tienes RESERVADOS para ese
// recurso se excluyen antes de llegar aquí (ver buildSubmitPairs).
interface SubmitPair {
  date: string;
  resourceType: ResourceType;
}

// Construye los pares (recurso × día) a enviar EXCLUYENDO los días en los que ya
// tienes una reserva viva de ese recurso (evita el 409 de duplicado y el falso
// "ya tienes" al mezclar días). El fijo NO cuenta como reserva: puede estar
// liberado ese día y se re-reserva (Feature A). `liveDaysFor` da, por recurso, el
// conjunto de días ISO con reserva propia viva.
function buildSubmitPairs(
  resources: ResourceType[],
  dates: string[],
  liveDaysFor: (resourceType: ResourceType) => ReadonlySet<string>,
): SubmitPair[] {
  const pairs: SubmitPair[] = [];
  for (const resourceType of resources) {
    const live = liveDaysFor(resourceType);
    for (const date of dates) {
      if (!live.has(date)) {
        pairs.push({ date, resourceType });
      }
    }
  }
  return pairs;
}

// Envia una solicitud por cada par (FECHA × RECURSO) ya filtrado
// (Promise.allSettled: un 409 NO_AVAILABILITY de uno no debe ocultar el exito de
// otro). Soporta multi-dia (reserva de varios dias de golpe, max 5). Los
// candidatos a lista de espera se agregan por tipo de recurso (unicos). Extraida a
// nivel de modulo para mantener baja la complejidad del componente (S3776).
async function submitResources(
  pairs: SubmitPair[],
  isAutomaticMode: boolean,
  waitlistJoined: Set<ResourceType>,
  buildBody: (
    resourceType: ResourceType,
    date: string,
    waitlistJoined: Set<ResourceType>,
  ) => RequestCreateRequest,
  mutateAsync: (body: RequestCreateRequest) => Promise<Request>,
): Promise<SubmitOutcome> {
  const settled = await Promise.allSettled(
    pairs.map(({ date, resourceType }) => mutateAsync(buildBody(resourceType, date, waitlistJoined))),
  );
  const outcome: SubmitOutcome = { createdRequests: [], waitlistCandidates: [], otherError: null };
  const waitlistSet = new Set<ResourceType>();
  settled.forEach((result, index) => {
    const { resourceType } = pairs[index];
    if (result.status === 'fulfilled') {
      outcome.createdRequests.push(result.value);
      return;
    }
    const alreadyOptedIn = waitlistJoined.has(resourceType);
    if (isAutomaticMode && !alreadyOptedIn && isNoAvailabilityConflict(result.reason)) {
      waitlistSet.add(resourceType);
    } else if (outcome.otherError === null) {
      outcome.otherError = result.reason;
    }
  });
  outcome.waitlistCandidates = [...waitlistSet];
  return outcome;
}

// Estado inicial del formulario según la preselección opcional del héroe. Se
// extrae a nivel de módulo para no cargar la complejidad cognitiva del componente
// (S3776): con preselección de recurso solo se marca ese recurso; sin ella no se
// marca nada (el usuario elige explícitamente qué reservar).
interface InitialSelection {
  date: string;
  parking: boolean;
  desk: boolean;
}

function initialSelection(presetDate?: string, presetResource?: ResourceType): InitialSelection {
  return {
    date: presetDate ?? '',
    parking: presetResource === 'PARKING',
    desk: presetResource === 'DESK',
  };
}

interface ApprovalModeNoticeProps {
  isAutomaticMode: boolean;
  isManualMode: boolean;
}

// Aviso de modo de aprobación vigente (automático confirma al instante; manual
// queda pendiente). Texto gris sin fondo (nota informativa) para no confundirse
// con las tarjetas de recurso. Extraído para mantener baja la complejidad (S3776).
function ApprovalModeNotice({ isAutomaticMode, isManualMode }: ApprovalModeNoticeProps) {
  const { t } = useTranslation();
  if (isAutomaticMode) {
    return <ModalNote icon="circle-check">{t('requests.create.automaticNotice')}</ModalNote>;
  }
  if (isManualMode) {
    return <ModalNote icon="clock">{t('requests.create.manualNotice')}</ModalNote>;
  }
  return null;
}

// Nota informativa dentro del modal: icono + texto gris, SIN fondo (a diferencia
// de InfoBanner, que se confundía con las tarjetas). role=status para lectores.
function ModalNote({ icon, children }: { icon?: string; children: ReactNode }) {
  return (
    <p className="rc-note" role="status">
      {icon ? <i className={`ti ti-${icon}`} aria-hidden="true" /> : null}
      <span>{children}</span>
    </p>
  );
}

interface WaitlistRetryBannerProps {
  resourceTypes: ResourceType[] | null;
  onConfirm: () => void;
  onDismiss: () => void;
}

// Confirmación tras un 409 NO_AVAILABILITY en modo automático (capability
// request-waitlist): ofrece apuntarse en vez de bloquear la solicitud.
function WaitlistRetryBanner({ resourceTypes, onConfirm, onDismiss }: WaitlistRetryBannerProps) {
  const { t } = useTranslation();
  if (!resourceTypes) {
    return null;
  }
  return (
    <InfoBanner variant="amber" icon="clock">
      <p>{t('requests.create.waitlist.retryPrompt')}</p>
      <div className="waitlist-retry-actions">
        <Button variant="green" onClick={onConfirm}>
          {t('requests.create.waitlist.retryConfirm')}
        </Button>
        <Button variant="white" onClick={onDismiss}>
          {t('requests.create.waitlist.retryDismiss')}
        </Button>
      </div>
    </InfoBanner>
  );
}

// Modal EMPLOYEE: solicitud unificada. Para una misma fecha (hoy o cualquier
// fecha futura) el empleado puede pedir plaza y/o puesto. Cada recurso genera un
// Request independiente vía POST /requests con su `resourceType` (init-desks §4.2).
export function CreateRequestModal({
  onClose,
  onCreated,
  presetDate,
  presetResource,
}: CreateRequestModalProps) {
  const { t, i18n } = useTranslation();
  // En móvil, "atrás" cierra este modal (no cambia de ruta). El hook ya no llama a
  // history.back() en el cleanup (rompía la apertura en StrictMode).
  useBackClose(onClose);
  const initial = initialSelection(presetDate, presetResource);
  // Reserva MULTI-DÍA (máx 5): días sueltos elegidos en el calendario. El primero
  // (más cercano) es el "día principal" que gobierna la disponibilidad/conflicto/aviso.
  const [dates, setDates] = useState<string[]>(initial.date ? [initial.date] : []);
  const primaryDate = dates[0] ?? '';
  // Sección de tarjetas de recurso: destino del scroll al elegir un día (móvil).
  const resourceSectionRef = useRef<HTMLElement>(null);
  const [parkingSelected, setParkingSelected] = useState(initial.parking);
  const [deskSelected, setDeskSelected] = useState(initial.desk);
  const [error, setError] = useState<string | null>(null);
  // Recursos por los que el empleado ha optado explicitamente por la lista de
  // espera (banner de disponibilidad en 0, o confirmacion tras un 409). Se
  // incluyen como `waitlist: true` en el siguiente envio de ese recurso.
  const [waitlistJoined, setWaitlistJoined] = useState<Set<ResourceType>>(new Set());
  // Recursos que acaban de recibir 409 NO_AVAILABILITY en modo automatico sin
  // haber optado aun: se ofrece confirmar el apunte a la lista de espera en vez
  // de bloquear la solicitud (employee-portal spec §1, escenario "Reintento").
  const [waitlistPrompt, setWaitlistPrompt] = useState<ResourceType[] | null>(null);
  const createMutation = useCreateRequest();
  const toast = useToast();
  // Modo de aprobacion vigente (null si no se puede resolver, p.ej. AGENCIA):
  // en MANUAL el resourceId del puesto elegido se ignora en el backend, asi que
  // el modal debe avisar de que la eleccion es una preferencia (requests spec).
  const approvalModeQuery = useApprovalModeQuery();
  const isManualMode = approvalModeQuery.data === 'MANUAL';
  const isAutomaticMode = approvalModeQuery.data === 'AUTOMATIC';
  // Fin de semana: si el admin no lo admite, el calendario deshabilita sáb/dom.
  const weekendReservable = useWeekendReservableQuery().data ?? false;
  // Días con reserva propia viva en los próximos ~3 meses → punto en el calendario.
  const markQuery = useMyRequestsQuery({
    from: todayIso(),
    to: addDaysIso(todayIso(), 90),
    size: 100,
  });
  // Asignaciones fijas del empleado (para el aviso "se te reasignará tu fijo" y para
  // marcar en el calendario los días de fijo).
  const { user } = useAuth();
  const fixedGroups = groupFixedAssignments(
    useEmployeeFixedAssignmentsQuery(user?.employeeId ?? null).data ?? [],
  );
  // Marcas del calendario POR RECURSO: días con reserva propia viva (APPROVED/PENDING)
  // y días de asignación fija → dos puntos (plaza / puesto) por día, para ver qué
  // tienes cada día. El "candado por-día" en las tarjetas se quitó: al enviar, los
  // días que ya tengas ese recurso se omiten y el resumen lo indica (evita el falso
  // "ya tienes" al mezclar días con y sin recurso).
  const parkingDays = useMemo(() => reservedDaySet(markQuery.data?.content, 'PARKING'), [markQuery.data]);
  const deskDays = useMemo(() => reservedDaySet(markQuery.data?.content, 'DESK'), [markQuery.data]);

  const effectiveParking = parkingSelected;
  const effectiveDesk = deskSelected;
  const showFixedHint = shouldShowFixedHint(primaryDate, fixedGroups, parkingSelected, deskSelected);

  // Días seleccionados que ya tienes RESERVADOS (reserva viva) para cada recurso
  // elegido → se omiten al enviar y se avisan en gris bajo las tarjetas.
  const liveDaysFor = (resourceType: ResourceType): ReadonlySet<string> =>
    resourceType === 'PARKING' ? parkingDays : deskDays;
  const dupParkingDates = useMemo(
    () => alreadyReservedDates(dates, parkingSelected, parkingDays),
    [dates, parkingSelected, parkingDays],
  );
  const dupDeskDates = useMemo(
    () => alreadyReservedDates(dates, deskSelected, deskDays),
    [dates, deskSelected, deskDays],
  );
  // Nº de pares (recurso × día) NUEVOS a crear (excluye lo ya reservado). Si es 0,
  // no hay nada que enviar → botón deshabilitado.
  const newPairsCount =
    (parkingSelected ? dates.length - dupParkingDates.length : 0) +
    (deskSelected ? dates.length - dupDeskDates.length : 0);
  const dupNote = duplicateNote(
    dupParkingDates,
    dupDeskDates,
    newPairsCount > 0,
    i18n.language,
    t,
  );
  // Tarjeta bloqueada ("Ya tienes plaza este día") cuando TODOS los días
  // seleccionados ya los tienes reservados (reserva viva) para ese recurso: no hay
  // nada nuevo que pedir. Solo reservas vivas (no el fijo, que puede re-reservarse).
  // Independiente de la selección: la tarjeta se bloquea sin tener que marcarla.
  const multiDay = dates.length > 1;
  const takenParking = dates.length > 0 && dates.every((d) => parkingDays.has(d));
  const takenDesk = dates.length > 0 && dates.every((d) => deskDays.has(d));

  // Alterna un día en la selección (calendario multi-select). Respeta el tope de 5.
  // Al añadir el primer día, lleva la vista a las tarjetas (móvil).
  function toggleReserveDate(iso: string): void {
    setDates((prev) => toggleDateInList(prev, iso, MAX_RESERVE_DAYS));
    requestAnimationFrame(() => {
      resourceSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }

  function selectedResources(): ResourceType[] {
    const resources: ResourceType[] = [];
    if (effectiveParking) {
      resources.push('PARKING');
    }
    if (effectiveDesk) {
      resources.push('DESK');
    }
    return resources;
  }

  function validate(resources: ResourceType[]): string | null {
    if (dates.length === 0) {
      return t('requests.create.requiredDate');
    }
    if (dates.some((d) => !isTodayOrFuture(d))) {
      return t('requests.create.outsideWindow');
    }
    if (resources.length === 0) {
      return t('requests.create.requiredResource');
    }
    return null;
  }

  // Cuerpo del POST /requests por recurso. En la reserva rápida el puesto NO lleva
  // `resourceId`: el backend lo auto-asigna por categoría (capability
  // desk-auto-assignment). `waitlist: true` cuando el empleado ya optó por la lista
  // de espera para ese recurso (capability request-waitlist).
  function buildBody(
    resourceType: ResourceType,
    date: string,
    waitlistSet: Set<ResourceType>,
  ): RequestCreateRequest {
    const body: RequestCreateRequest = { requestedDate: date, resourceType };
    if (waitlistSet.has(resourceType)) {
      body.waitlist = true;
    }
    return body;
  }

  // Aplica el resultado de un envio (inicial o de reintento): si algun recurso
  // quedo pendiente de confirmar la lista de espera se ofrece el aviso y NO se
  // cierra el modal. Si al menos un dia/recurso se creo, cuenta como exito y se
  // cierra (exito parcial: al reservar varios dias, que uno ya lo tuvieras no
  // debe presentarse como error). Solo se muestra el toast de error cuando NADA
  // se pudo crear.
  function applyOutcome(outcome: SubmitOutcome): void {
    if (outcome.waitlistCandidates.length > 0) {
      setWaitlistPrompt(outcome.waitlistCandidates);
      return;
    }
    if (outcome.createdRequests.length > 0) {
      toast.success(successToastKey(outcome.createdRequests));
      onCreated();
      return;
    }
    if (outcome.otherError) {
      emitApiErrorToast(toastKeyForError(outcome.otherError));
    }
  }

  async function handleSubmit(event: FormEvent): Promise<void> {
    event.preventDefault();
    const resources = selectedResources();
    const validationError = validate(resources);
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    setWaitlistPrompt(null);
    const pairs = buildSubmitPairs(resources, dates, liveDaysFor);
    if (pairs.length === 0) {
      return; // todo lo seleccionado ya lo tienes reservado (botón deshabilitado)
    }
    const outcome = await submitResources(
      pairs,
      isAutomaticMode,
      waitlistJoined,
      buildBody,
      createMutation.mutateAsync,
    );
    applyOutcome(outcome);
  }

  // Confirmacion tras el 409 NO_AVAILABILITY (escenario "Reintento" de
  // employee-portal spec §1): reenvia SOLO los recursos ofrecidos con
  // `waitlist: true` y marca el opt-in para el resto de la sesion del modal.
  async function confirmWaitlistJoin(): Promise<void> {
    if (!waitlistPrompt) {
      return;
    }
    const resources = waitlistPrompt;
    const nextWaitlistJoined = new Set(waitlistJoined);
    resources.forEach((resourceType) => nextWaitlistJoined.add(resourceType));
    setWaitlistJoined(nextWaitlistJoined);
    setWaitlistPrompt(null);
    const pairs = buildSubmitPairs(resources, dates, liveDaysFor);
    const outcome = await submitResources(
      pairs,
      isAutomaticMode,
      nextWaitlistJoined,
      buildBody,
      createMutation.mutateAsync,
    );
    applyOutcome(outcome);
  }

  function dismissWaitlistPrompt(): void {
    setWaitlistPrompt(null);
  }

  // Opt-in proactivo desde el banner de disponibilidad (0 libres): no envia
  // nada por si mismo, solo marca el recurso para que el siguiente envio
  // incluya `waitlist: true` (employee-portal spec §1, escenario "no bloquea").
  function handleJoinWaitlist(resourceType: ResourceType): void {
    setWaitlistJoined((previous) => new Set(previous).add(resourceType));
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('requests.create.cancel')}
      </Button>
      <Button
        variant="green"
        submit
        form="create-request-form"
        loading={createMutation.isPending}
        disabled={newPairsCount === 0}
      >
        {t('requests.create.submit')}
      </Button>
    </>
  );

  return (
    <>
      <Dialog
        open
        fullScreen
        onOpenChange={(open) => {
          if (!open) {
            onClose();
          }
        }}
        title={t('requests.create.title')}
        icon="calendar-plus"
        footer={footer}
      >
        <form id="create-request-form" onSubmit={handleSubmit} noValidate className="rc-form">
          <div className="rc-layout">
            <section className="rc-col rc-col-cal">
              <p className="rc-section-label">
                <i className="ti ti-calendar-event" aria-hidden="true" />
                {t('requests.create.date')}
              </p>
              <MultiSelectCalendar
                selected={new Set(dates)}
                onToggle={toggleReserveDate}
                minIso={todayIso()}
                canSelectMore={dates.length < MAX_RESERVE_DAYS}
                weekendDisabled={!weekendReservable}
                parkingDays={parkingDays}
                deskDays={deskDays}
              />
              <div className="rc-cal-legend" aria-hidden="true">
                <span>
                  <span className="rc-day-dot is-parking" /> {t('requests.create.legend.parking')}
                </span>
                <span>
                  <span className="rc-day-dot is-desk" /> {t('requests.create.legend.desk')}
                </span>
              </div>
              <p
                className={`rc-selected-date${dates.length === 0 ? ' is-empty' : ''}`}
                role="status"
                aria-live="polite"
              >
                {reserveDateSummary(dates, primaryDate, i18n.language, t)}
              </p>
              {dates.length >= MAX_RESERVE_DAYS ? (
                <p className="hint rc-max-note">
                  {t('requests.create.maxDays', { max: MAX_RESERVE_DAYS })}
                </p>
              ) : null}
            </section>

            <section className="rc-col rc-col-res" ref={resourceSectionRef}>
              <p className="rc-section-label">
                <i className="ti ti-checkup-list" aria-hidden="true" />
                {t('requests.create.resources')}
              </p>
              <ResourceChoiceCard
                resourceType="PARKING"
                title={t('requests.create.resourceParking')}
                date={primaryDate}
                selected={parkingSelected}
                taken={takenParking}
                multiDay={multiDay}
                waitlistJoined={waitlistJoined.has('PARKING')}
                onToggle={() => setParkingSelected((value) => !value)}
                onJoinWaitlist={handleJoinWaitlist}
              />
              <ResourceChoiceCard
                resourceType="DESK"
                title={t('requests.create.resourceDesk')}
                date={primaryDate}
                selected={deskSelected}
                taken={takenDesk}
                multiDay={multiDay}
                waitlistJoined={waitlistJoined.has('DESK')}
                onToggle={() => setDeskSelected((value) => !value)}
                onJoinWaitlist={handleJoinWaitlist}
              />

              {dupNote ? <p className="rc-dup-note" role="status">{dupNote}</p> : null}

              {showFixedHint ? (
                <ModalNote icon="star">{t('requests.create.fixedHint')}</ModalNote>
              ) : null}

              <ApprovalModeNotice isAutomaticMode={isAutomaticMode} isManualMode={isManualMode} />

              <WaitlistRetryBanner
                resourceTypes={waitlistPrompt}
                onConfirm={() => void confirmWaitlistJoin()}
                onDismiss={dismissWaitlistPrompt}
              />

              {error ? (
                <p className="form-error" role="alert">
                  {error}
                </p>
              ) : null}
            </section>
          </div>
        </form>
      </Dialog>
    </>
  );
}
