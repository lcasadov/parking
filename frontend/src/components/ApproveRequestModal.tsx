import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { FieldRow } from './FieldRow';
import { FieldValue } from './FieldValue';
import { InfoBanner } from './InfoBanner';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useApprovalAvailabilityQuery } from '../hooks/useCalendar';
import { useApproveRequest } from '../hooks/useRequests';
import { longDate } from '../utils/calendar';
import type { Employee } from '../types/employee';
import type { Request, RequestStatus, ResourceType } from '../types/request';

interface ApproveRequestModalProps {
  request: Request;
  // Empleado solicitante (contexto read-only); opcional si no está en el lookup.
  employee?: Employee;
  onClose: () => void;
  onApproved: () => void;
}

const FORM_ID = 'approve-request-form';
const HTTP_CONFLICT = 409;

// Tono de pill por estado de la solicitud (mockup: PENDIENTE en ambar).
const STATUS_PILL_TONE: Record<RequestStatus, string> = {
  PENDING: 'pill-amber',
  APPROVED: 'pill-green',
  REJECTED: 'pill-pink',
  CANCELLED: 'pill-gray',
};

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
export function ApproveRequestModal({
  request,
  employee,
  onClose,
  onApproved,
}: ApproveRequestModalProps) {
  const { t, i18n } = useTranslation();
  const resourceType: ResourceType = request.resourceType ?? 'PARKING';
  const labels = labelsFor(resourceType);
  const availabilityQuery = useApprovalAvailabilityQuery(request.requestedDate, resourceType);
  const resources = availabilityQuery.data?.availableResources ?? [];

  const [spaceId, setSpaceId] = useState<number | ''>('');
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | null>(null);
  const approveMutation = useApproveRequest();

  const employeeName = employee
    ? `${employee.firstName} ${employee.lastName}`
    : `#${request.employeeId}`;
  const department = employee?.department ?? t('requests.approve.context.noDepartment');
  const showFreeHint = !availabilityQuery.isLoading && !availabilityQuery.isError;

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

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('requests.approve.cancel')}
      </Button>
      <Button variant="green" icon="check" submit form={FORM_ID} disabled={approveMutation.isPending}>
        {t('requests.approve.submit')}
      </Button>
    </>
  );

  return (
    <Modal title={t(labels.title)} icon="calendar-check" onClose={onClose} footer={footer}>
      <FieldRow>
        <div>
          <span className="field-label">{t('requests.approve.context.employee')}</span>
          <FieldValue readOnly>{employeeName}</FieldValue>
        </div>
        <div>
          <span className="field-label">{t('requests.approve.context.department')}</span>
          <FieldValue readOnly>{department}</FieldValue>
        </div>
      </FieldRow>

      <FieldRow>
        <div>
          <span className="field-label">{t('requests.approve.context.day')}</span>
          <FieldValue readOnly withIcon>
            <span>
              <i className="ti ti-calendar green-icon" aria-hidden="true" />{' '}
              {longDate(request.requestedDate, i18n.language)}
            </span>
          </FieldValue>
        </div>
        <div>
          <span className="field-label">{t('requests.approve.context.status')}</span>
          <div>
            <span className={`pill ${STATUS_PILL_TONE[request.status]}`}>
              {t(`requests.status.${request.status}`)}
            </span>
          </div>
        </div>
      </FieldRow>

      <FieldRow>
        <div>
          <span className="field-label">{t('requests.approve.context.resourceType')}</span>
          <div>
            <span className={`pill ${resourceType === 'DESK' ? 'pill-blue' : 'pill-gray'}`}>
              {t(`requests.resourceType.${resourceType}`)}
            </span>
          </div>
        </div>
        <div />
      </FieldRow>

      <div className="divider" />

      <form id={FORM_ID} onSubmit={handleSubmit} noValidate>
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
        {showFreeHint ? (
          <p className="hint">{t('requests.approve.freeResources', { count: resources.length })}</p>
        ) : null}

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
      </form>

      <InfoBanner variant="blue" icon="mail">
        {employee?.email
          ? t('requests.approve.emailNotice', { email: employee.email })
          : t('requests.approve.emailNoticeGeneric')}
      </InfoBanner>
    </Modal>
  );
}
