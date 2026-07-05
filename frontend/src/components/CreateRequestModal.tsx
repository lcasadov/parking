import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { ResourceAvailabilityBanner } from './ResourceAvailabilityBanner';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCreateRequest } from '../hooks/useRequests';
import { isWithinWindow, maxRequestDateIso, todayIso } from '../utils/requests';
import type { ResourceType } from '../types/request';

interface CreateRequestModalProps {
  onClose: () => void;
  onCreated: () => void;
}

const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;

// Traduce el error del servidor (400 ventana / 409 duplicado) a la clave i18n del
// toast correspondiente (tasks §4.4).
function toastKeyForError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_CONFLICT) {
    return 'requests.errors.duplicate';
  }
  if (status === HTTP_BAD_REQUEST) {
    return 'requests.errors.window';
  }
  return 'requests.errors.generic';
}

// Modal EMPLOYEE: solicitud unificada. Para una misma fecha (ventana hoy..hoy+14)
// el empleado puede pedir plaza y/o puesto. Cada recurso seleccionado genera un
// Request independiente vía POST /requests con su `resourceType` (init-desks §4.2).
export function CreateRequestModal({ onClose, onCreated }: CreateRequestModalProps) {
  const { t } = useTranslation();
  const [date, setDate] = useState('');
  const [parkingSelected, setParkingSelected] = useState(true);
  const [deskSelected, setDeskSelected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const createMutation = useCreateRequest();

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
    if (!isWithinWindow(date)) {
      return t('requests.create.outsideWindow');
    }
    if (resources.length === 0) {
      return t('requests.create.requiredResource');
    }
    return null;
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
      await Promise.all(
        resources.map((resourceType) =>
          createMutation.mutateAsync({ requestedDate: date, resourceType }),
        ),
      );
      onCreated();
    } catch (mutationError) {
      emitApiErrorToast(toastKeyForError(mutationError));
    }
  }

  return (
    <Modal title={t('requests.create.title')} onClose={onClose}>
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
          max={maxRequestDateIso()}
          onChange={(event) => setDate(event.target.value)}
        />
        <p className="hint">{t('requests.create.hint')}</p>

        <fieldset className="resource-fieldset">
          <legend className="field-label">{t('requests.create.resources')}</legend>
          <label className="checkbox-field">
            <input
              type="checkbox"
              checked={parkingSelected}
              onChange={(event) => setParkingSelected(event.target.checked)}
            />
            {t('requests.create.resourceParking')}
          </label>
          <ResourceAvailabilityBanner date={date} resourceType="PARKING" />
          <label className="checkbox-field">
            <input
              type="checkbox"
              checked={deskSelected}
              onChange={(event) => setDeskSelected(event.target.checked)}
            />
            {t('requests.create.resourceDesk')}
          </label>
          <ResourceAvailabilityBanner date={date} resourceType="DESK" />
        </fieldset>

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}

        <div className="modal-footer-inline">
          <Button variant="white" onClick={onClose}>
            {t('requests.create.cancel')}
          </Button>
          <Button variant="green" submit disabled={createMutation.isPending}>
            {t('requests.create.submit')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
