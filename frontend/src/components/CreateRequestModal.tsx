import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { DeskPickerModal, type PickedDesk } from './DeskPickerModal';
import { ResourceAvailabilityBanner } from './ResourceAvailabilityBanner';
import { getApiError, getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCreateRequest } from '../hooks/useRequests';
import { useApprovalModeQuery } from '../hooks/useSettings';
import { isTodayOrFuture, todayIso } from '../utils/requests';
import type { Request, RequestCreateRequest, ResourceType } from '../types/request';

interface CreateRequestModalProps {
  onClose: () => void;
  onCreated: () => void;
}

const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;
const NO_AVAILABILITY = 'NO_AVAILABILITY';

// Traduce el error del servidor a la clave i18n del toast (tasks §4.4 y §6.4).
// El 409 distingue entre solicitud duplicada y NO_AVAILABILITY (modo automatico sin
// plaza libre para la fecha).
function toastKeyForError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_CONFLICT) {
    return getApiError(error)?.error === NO_AVAILABILITY
      ? 'requests.errors.noAvailability'
      : 'requests.errors.duplicate';
  }
  if (status === HTTP_BAD_REQUEST) {
    return 'requests.errors.window';
  }
  return 'requests.errors.generic';
}

// Clave i18n del toast de exito: en modo automatico la solicitud nace APPROVED
// (asignacion inmediata); en modo manual queda PENDING (tasks §6.4).
function successToastKey(created: Request[]): string {
  return created.some((request) => request.status === 'APPROVED')
    ? 'requests.mine.createdApproved'
    : 'requests.mine.created';
}

// Modal EMPLOYEE: solicitud unificada. Para una misma fecha (hoy o cualquier
// fecha futura) el empleado puede pedir plaza y/o puesto. Cada recurso genera un
// Request independiente vía POST /requests con su `resourceType` (init-desks §4.2).
export function CreateRequestModal({ onClose, onCreated }: CreateRequestModalProps) {
  const { t } = useTranslation();
  const [date, setDate] = useState('');
  const [parkingSelected, setParkingSelected] = useState(true);
  const [deskSelected, setDeskSelected] = useState(false);
  const [selectedDesk, setSelectedDesk] = useState<PickedDesk | null>(null);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const createMutation = useCreateRequest();
  // Modo de aprobacion vigente (null si no se puede resolver, p.ej. AGENCIA):
  // en MANUAL el resourceId del puesto elegido se ignora en el backend, asi que
  // el modal debe avisar de que la eleccion es una preferencia (requests spec).
  const approvalModeQuery = useApprovalModeQuery();
  const isManualMode = approvalModeQuery.data === 'MANUAL';

  // El selector solo tiene sentido con una fecha valida (hoy o futura): usa esa
  // fecha para colorear la disponibilidad de los puestos.
  const canPickDesk = date !== '' && isTodayOrFuture(date);

  // Al desmarcar PUESTO se descarta el puesto elegido para no enviar un resourceId
  // obsoleto (el envio de plaza nunca lleva resourceId).
  function handleDeskToggle(checked: boolean): void {
    setDeskSelected(checked);
    if (!checked) {
      setSelectedDesk(null);
    }
  }

  function handleDeskPicked(desk: PickedDesk): void {
    setSelectedDesk(desk);
    setPickerOpen(false);
  }

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

  // Cuerpo del POST /requests por recurso: la solicitud de PUESTO con puesto
  // elegido incluye `resourceId`; la de PLAZA (o de puesto sin elegir) no.
  function buildBody(resourceType: ResourceType): RequestCreateRequest {
    const body: RequestCreateRequest = { requestedDate: date, resourceType };
    if (resourceType === 'DESK' && selectedDesk) {
      body.resourceId = selectedDesk.deskId;
    }
    return body;
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
    try {
      const created = await Promise.all(
        resources.map((resourceType) => createMutation.mutateAsync(buildBody(resourceType))),
      );
      emitApiErrorToast(successToastKey(created));
      onCreated();
    } catch (mutationError) {
      emitApiErrorToast(toastKeyForError(mutationError));
    }
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
        disabled={createMutation.isPending}
      >
        {t('requests.create.submit')}
      </Button>
    </>
  );

  return (
    <>
    <Dialog
      open
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
          <label className={`checkbox-field resource-option${parkingSelected ? ' is-selected' : ''}`}>
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
          <ResourceAvailabilityBanner date={date} resourceType="PARKING" />
          <label className={`checkbox-field resource-option${deskSelected ? ' is-selected' : ''}`}>
            <input
              type="checkbox"
              checked={deskSelected}
              onChange={(event) => handleDeskToggle(event.target.checked)}
            />
            <span className="resource-option-icon" aria-hidden="true">
              <i className="ti ti-armchair" />
            </span>
            <span className="resource-option-label">{t('requests.create.resourceDesk')}</span>
          </label>
          <ResourceAvailabilityBanner date={date} resourceType="DESK" />

          {deskSelected ? (
            <div className="desk-pick">
              {selectedDesk ? (
                <p className="desk-pick-chosen">
                  {t('requests.create.chosenDesk', { number: selectedDesk.deskNumber })}
                </p>
              ) : null}
              {selectedDesk && isManualMode ? (
                <p className="hint" role="status">
                  {t('requests.create.chosenDeskPreferenceNote')}
                </p>
              ) : null}
              <div className="desk-pick-actions">
                <Button
                  variant="white"
                  icon="map-pin"
                  disabled={!canPickDesk}
                  onClick={() => setPickerOpen(true)}
                >
                  {selectedDesk
                    ? t('requests.create.changeDesk')
                    : t('requests.create.chooseDesk')}
                </Button>
                {selectedDesk ? (
                  <Button variant="white" icon="x" onClick={() => setSelectedDesk(null)}>
                    {t('requests.create.removeDesk')}
                  </Button>
                ) : null}
              </div>
              {canPickDesk ? null : (
                <p className="hint">{t('requests.create.chooseDeskDateHint')}</p>
              )}
            </div>
          ) : null}
        </fieldset>

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}
      </form>
    </Dialog>
    {pickerOpen ? (
      <DeskPickerModal
        date={date}
        onPick={handleDeskPicked}
        onClose={() => setPickerOpen(false)}
      />
    ) : null}
    </>
  );
}
