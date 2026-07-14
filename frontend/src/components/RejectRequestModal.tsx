import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { InfoBanner } from './InfoBanner';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useRejectRequest } from '../hooks/useRequests';
import { longDate } from '../utils/calendar';
import { REJECTION_FREE_TEXT_MIN, REJECTION_REASON_CODES } from '../utils/requests';
import type { Employee } from '../types/employee';
import type { RejectionReasonCode, Request, ResourceType } from '../types/request';

// Modo del modal: rechazo clásico de una PENDING, o cancelación (por el ADMIN) de una
// solicitud APPROVED para liberar el recurso. Ambos usan el mismo endpoint reject en el
// backend (una APPROVED que pasa a REJECTED libera la plaza/puesto), pero cambian la copia.
type RejectMode = 'reject' | 'cancel';

interface RejectRequestModalProps {
  request: Request;
  // Empleado solicitante (contexto del banner); opcional si no está en el lookup.
  employee?: Employee;
  // 'reject' (por defecto) rechaza una PENDING; 'cancel' cancela una APPROVED liberando el recurso.
  mode?: RejectMode;
  onClose: () => void;
  onRejected: () => void;
}

const FORM_ID = 'reject-request-form';
const HTTP_BAD_REQUEST = 400;

// Traduce el error del servidor (400 validacion del motivo) a la clave i18n.
function toastKeyForError(error: unknown): string {
  if (getStatus(error) === HTTP_BAD_REQUEST) {
    return 'requests.errors.rejectInvalid';
  }
  return 'requests.errors.generic';
}

// Modal ADMIN: rechaza una solicitud con un motivo del catalogo. Cuando el motivo
// es OTHER, el texto libre (>=5 caracteres) es obligatorio (POST /requests/{id}/reject).
export function RejectRequestModal({
  request,
  employee,
  mode = 'reject',
  onClose,
  onRejected,
}: RejectRequestModalProps) {
  const { t, i18n } = useTranslation();
  const [reasonCode, setReasonCode] = useState<RejectionReasonCode | ''>('');
  const [detail, setDetail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const rejectMutation = useRejectRequest();

  const isCancel = mode === 'cancel';
  const copyKey = isCancel ? 'requests.cancelApproved' : 'requests.reject';
  const requiresDetail = reasonCode === 'OTHER';
  const resourceType: ResourceType = request.resourceType ?? 'PARKING';
  const employeeName = employee
    ? `${employee.firstName} ${employee.lastName}`
    : `#${request.employeeId}`;

  function validate(): string | null {
    if (reasonCode === '') {
      return t('requests.reject.requiredReason');
    }
    if (requiresDetail && detail.trim().length < REJECTION_FREE_TEXT_MIN) {
      return t('requests.reject.requiredDetail');
    }
    return null;
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    const trimmedDetail = detail.trim();
    rejectMutation.mutate(
      {
        id: request.id,
        body: {
          reasonCode: reasonCode as RejectionReasonCode,
          ...(trimmedDetail === '' ? {} : { rejectionReason: trimmedDetail }),
        },
      },
      {
        onSuccess: onRejected,
        onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
      },
    );
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('requests.reject.cancel')}
      </Button>
      <Button variant="red" icon="x" submit form={FORM_ID} disabled={rejectMutation.isPending}>
        {t(`${copyKey}.submit`)}
      </Button>
    </>
  );

  return (
    <Modal
      title={t(`${copyKey}.title`)}
      icon="alert-triangle"
      onClose={onClose}
      variant="red"
      narrow
      footer={footer}
    >
      <InfoBanner variant="red">
        {t(`${copyKey}.intro`, {
          resource: t(`requests.resourceType.${resourceType}`).toLowerCase(),
          name: employeeName,
          date: longDate(request.requestedDate, i18n.language),
        })}
      </InfoBanner>

      <form id={FORM_ID} onSubmit={handleSubmit} noValidate>
        <label className="field-label red" htmlFor="reject-request-reason">
          {t('requests.reject.reason')}
        </label>
        <select
          id="reject-request-reason"
          className="field-input"
          value={reasonCode}
          onChange={(event) => setReasonCode(event.target.value as RejectionReasonCode | '')}
        >
          <option value="">{t('requests.reject.selectReason')}</option>
          {REJECTION_REASON_CODES.map((code) => (
            <option key={code} value={code}>
              {t(`requests.reasonCodes.${code}`)}
            </option>
          ))}
        </select>

        <label className="field-label red" htmlFor="reject-request-detail">
          {t('requests.reject.detail')}
        </label>
        <textarea
          id="reject-request-detail"
          className="field-input"
          value={detail}
          maxLength={500}
          required={requiresDetail}
          onChange={(event) => setDetail(event.target.value)}
        />
        <p className="hint">{t('requests.reject.detailHint')}</p>

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}
      </form>

      <InfoBanner variant="blue" icon="mail">
        {t(`${copyKey}.emailNotice`)}
      </InfoBanner>
    </Modal>
  );
}
