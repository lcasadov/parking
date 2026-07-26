import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { InfoBanner } from './InfoBanner';
import { ResourceAvailabilityBanner } from './ResourceAvailabilityBanner';
import { getApiError, getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useBackClose } from '../hooks/useBackClose';
import { useCreateRequest } from '../hooks/useRequests';
import { useToast } from '../hooks/useToast';
import { useApprovalModeQuery } from '../hooks/useSettings';
import { isTodayOrFuture, todayIso } from '../utils/requests';
import type { Request, RequestCreateRequest, ResourceType } from '../types/request';

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

// Envia una solicitud por cada recurso seleccionado (Promise.allSettled: un 409
// NO_AVAILABILITY de un recurso no debe ocultar el exito de otro). Extraida a
// nivel de modulo para mantener la complejidad cognitiva del componente baja
// (S3776) y para poder reutilizarla tanto en el envio inicial como en el
// reintento tras confirmar la lista de espera.
async function submitResources(
  resources: ResourceType[],
  isAutomaticMode: boolean,
  waitlistJoined: Set<ResourceType>,
  buildBody: (resourceType: ResourceType, waitlistJoined: Set<ResourceType>) => RequestCreateRequest,
  mutateAsync: (body: RequestCreateRequest) => Promise<Request>,
): Promise<SubmitOutcome> {
  const settled = await Promise.allSettled(
    resources.map((resourceType) => mutateAsync(buildBody(resourceType, waitlistJoined))),
  );
  const outcome: SubmitOutcome = { createdRequests: [], waitlistCandidates: [], otherError: null };
  settled.forEach((result, index) => {
    const resourceType = resources[index];
    if (result.status === 'fulfilled') {
      outcome.createdRequests.push(result.value);
      return;
    }
    const alreadyOptedIn = waitlistJoined.has(resourceType);
    if (isAutomaticMode && !alreadyOptedIn && isNoAvailabilityConflict(result.reason)) {
      outcome.waitlistCandidates.push(resourceType);
    } else if (outcome.otherError === null) {
      outcome.otherError = result.reason;
    }
  });
  return outcome;
}

// Estado inicial del formulario según la preselección opcional del héroe. Se
// extrae a nivel de módulo para no cargar la complejidad cognitiva del componente
// (S3776): con preselección de recurso solo se marca ese recurso; sin ella, plaza.
interface InitialSelection {
  date: string;
  parking: boolean;
  desk: boolean;
}

function initialSelection(presetDate?: string, presetResource?: ResourceType): InitialSelection {
  return {
    date: presetDate ?? '',
    parking: presetResource === undefined || presetResource === 'PARKING',
    desk: presetResource === 'DESK',
  };
}

interface ApprovalModeNoticeProps {
  isAutomaticMode: boolean;
  isManualMode: boolean;
}

// Aviso de modo de aprobación vigente (automático confirma al instante; manual
// queda pendiente). Extraído junto al resto de bloques informativos para
// mantener la complejidad del componente padre bajo control (S3776).
function ApprovalModeNotice({ isAutomaticMode, isManualMode }: ApprovalModeNoticeProps) {
  const { t } = useTranslation();
  if (isAutomaticMode) {
    return (
      <InfoBanner variant="green" icon="circle-check">
        {t('requests.create.automaticNotice')}
      </InfoBanner>
    );
  }
  if (isManualMode) {
    return (
      <InfoBanner variant="amber" icon="clock">
        {t('requests.create.manualNotice')}
      </InfoBanner>
    );
  }
  return null;
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
  const { t } = useTranslation();
  // En móvil, "atrás" cierra este modal (no cambia de ruta).
  useBackClose(onClose);
  const initial = initialSelection(presetDate, presetResource);
  const [date, setDate] = useState(initial.date);
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

  function selectedResources(): ResourceType[] {
    const resources: ResourceType[] = [];
    if (parkingSelected) {
      resources.push('PARKING');
    }
    if (deskSelected) {
      resources.push('DESK');
    }
    return resources;
  }

  function validate(resources: ResourceType[]): string | null {
    if (date === '') {
      return t('requests.create.requiredDate');
    }
    if (!isTodayOrFuture(date)) {
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
  function buildBody(resourceType: ResourceType, waitlistSet: Set<ResourceType>): RequestCreateRequest {
    const body: RequestCreateRequest = { requestedDate: date, resourceType };
    if (waitlistSet.has(resourceType)) {
      body.waitlist = true;
    }
    return body;
  }

  // Aplica el resultado de un envio (inicial o de reintento): si algun recurso
  // quedo pendiente de confirmar la lista de espera se ofrece el aviso y NO se
  // cierra el modal; si hay otro error se muestra el toast correspondiente;
  // solo se notifica el exito y se cierra cuando no queda nada pendiente.
  function applyOutcome(outcome: SubmitOutcome): void {
    if (outcome.waitlistCandidates.length > 0) {
      setWaitlistPrompt(outcome.waitlistCandidates);
      return;
    }
    if (outcome.otherError) {
      emitApiErrorToast(toastKeyForError(outcome.otherError));
      return;
    }
    if (outcome.createdRequests.length > 0) {
      toast.success(successToastKey(outcome.createdRequests));
      onCreated();
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
    const outcome = await submitResources(
      resources,
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
    const outcome = await submitResources(
      resources,
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
      <Button variant="green" submit form="create-request-form" disabled={createMutation.isPending}>
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
        <form id="create-request-form" onSubmit={handleSubmit} noValidate>
          <label className="field-label" htmlFor="create-request-date">
            {t('requests.create.date')}
          </label>
          <input
            id="create-request-date"
            type="date"
            className="field-input"
            value={date}
            min={todayIso()}
            onChange={(event) => setDate(event.target.value)}
          />
          <p className="hint">{t('requests.create.hint')}</p>

          <fieldset className="resource-fieldset">
            <legend className="field-label">{t('requests.create.resources')}</legend>
            <label
              className={`checkbox-field resource-option${parkingSelected ? ' is-selected' : ''}`}
            >
              <input
                type="checkbox"
                checked={parkingSelected}
                onChange={(event) => setParkingSelected(event.target.checked)}
              />
              <span className="resource-option-icon" aria-hidden="true">
                <i className="ti ti-parking" />
              </span>
              <span className="resource-option-label">{t('requests.create.resourceParking')}</span>
            </label>
            <ResourceAvailabilityBanner
              date={date}
              resourceType="PARKING"
              selected={parkingSelected}
              waitlistJoined={waitlistJoined.has('PARKING')}
              onJoinWaitlist={handleJoinWaitlist}
            />
            <label
              className={`checkbox-field resource-option${deskSelected ? ' is-selected' : ''}`}
            >
              <input
                type="checkbox"
                checked={deskSelected}
                onChange={(event) => setDeskSelected(event.target.checked)}
              />
              <span className="resource-option-icon" aria-hidden="true">
                <i className="ti ti-armchair" />
              </span>
              <span className="resource-option-label">{t('requests.create.resourceDesk')}</span>
            </label>
            <ResourceAvailabilityBanner
              date={date}
              resourceType="DESK"
              selected={deskSelected}
              waitlistJoined={waitlistJoined.has('DESK')}
              onJoinWaitlist={handleJoinWaitlist}
            />
          </fieldset>

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
        </form>
      </Dialog>
    </>
  );
}
