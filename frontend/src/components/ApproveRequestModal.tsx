import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useApprovalAvailabilityQuery } from '../hooks/useCalendar';
import { useApproveRequest } from '../hooks/useRequests';
import type { Request, ResourceType } from '../types/request';

interface ApproveRequestModalProps {
  request: Request;
  onClose: () => void;
  onApproved: () => void;
}

const HTTP_CONFLICT = 409;

// Traduce el error del servidor (409 no disponible/concurrencia) a la clave i18n
// del toast; el resto (400 validacion, 5xx) cae en el mensaje generico (§4.4).
function toastKeyForError(error: unknown): string {
  if (getStatus(error) === HTTP_CONFLICT) {
    return 'requests.errors.unavailable';
  }
  return 'requests.errors.generic';
}

// Etiquetas i18n dependientes del tipo de recurso de la solicitud.
function labelsFor(resourceType: ResourceType): {
  title: string;
  field: string;
  select: string;
  required: string;
} {
  if (resourceType === 'DESK') {
    return {
      title: 'requests.approve.titleDesk',
      field: 'requests.approve.resourceDesk',
      select: 'requests.approve.selectDesk',
      required: 'requests.approve.requiredDesk',
    };
  }
  return {
    title: 'requests.approve.titleParking',
    field: 'requests.approve.resourceParking',
    select: 'requests.approve.selectParking',
    required: 'requests.approve.requiredParking',
  };
}

// Modal ADMIN: aprueba una solicitud asignando el recurso del tipo correcto
// (plaza o puesto). Lee `resourceType` de la solicitud y ofrece los recursos
// disponibles para la fecha via GET /availability?date&resourceType. El id elegido
// viaja en `parkingSpaceId` (resource_id generico). Un 409 = recurso no disponible.
export function ApproveRequestModal({ request, onClose, onApproved }: ApproveRequestModalProps) {
  const { t } = useTranslation();
  const resourceType: ResourceType = request.resourceType ?? 'PARKING';
  const labels = labelsFor(resourceType);
  const availabilityQuery = useApprovalAvailabilityQuery(request.requestedDate, resourceType);
  const resources = availabilityQuery.data?.availableResources ?? [];

  const [spaceId, setSpaceId] = useState<number | ''>('');
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | null>(null);
  const approveMutation = useApproveRequest();

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    if (spaceId === '') {
      setError(t(labels.required));
      return;
    }
    setError(null);
    const trimmedNote = note.trim();
    approveMutation.mutate(
      {
        id: request.id,
        body: {
          parkingSpaceId: Number(spaceId),
          ...(trimmedNote === '' ? {} : { approvalNote: trimmedNote }),
        },
      },
      {
        onSuccess: onApproved,
        onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
      },
    );
  }

  return (
    <Modal title={t(labels.title)} onClose={onClose}>
      <form id="approve-request-form" onSubmit={handleSubmit} noValidate>
        <label className="field-label" htmlFor="approve-request-space">
          {t(labels.field)}
        </label>
        <select
          id="approve-request-space"
          className="field-input"
          value={spaceId}
          disabled={availabilityQuery.isLoading}
          onChange={(event) =>
            setSpaceId(event.target.value === '' ? '' : Number(event.target.value))
          }
        >
          <option value="">
            {availabilityQuery.isLoading ? t('requests.approve.loadingResources') : t(labels.select)}
          </option>
          {resources.map((resource) => (
            <option key={resource.parkingSpaceId} value={resource.parkingSpaceId}>
              {resource.label}
            </option>
          ))}
        </select>

        <label className="field-label" htmlFor="approve-request-note">
          {t('requests.approve.note')}
        </label>
        <textarea
          id="approve-request-note"
          className="field-input"
          value={note}
          maxLength={500}
          onChange={(event) => setNote(event.target.value)}
        />

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}

        <div className="modal-footer-inline">
          <Button variant="white" onClick={onClose}>
            {t('requests.approve.cancel')}
          </Button>
          <Button variant="green" submit disabled={approveMutation.isPending}>
            {t('requests.approve.submit')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
