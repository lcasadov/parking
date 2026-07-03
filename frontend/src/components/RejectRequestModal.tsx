import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useRejectRequest } from '../hooks/useRequests';
import { REJECTION_FREE_TEXT_MIN, REJECTION_REASON_CODES } from '../utils/requests';
import type { RejectionReasonCode } from '../types/request';

interface RejectRequestModalProps {
  requestId: number;
  onClose: () => void;
  onRejected: () => void;
}

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
export function RejectRequestModal({ requestId, onClose, onRejected }: RejectRequestModalProps) {
  const { t } = useTranslation();
  const [reasonCode, setReasonCode] = useState<RejectionReasonCode | ''>('');
  const [detail, setDetail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const rejectMutation = useRejectRequest();

  const requiresDetail = reasonCode === 'OTHER';

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
        id: requestId,
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

  return (
    <Modal title={t('requests.reject.title')} onClose={onClose} variant="red">
      <form id="reject-request-form" onSubmit={handleSubmit} noValidate>
        <label className="field-label" htmlFor="reject-request-reason">
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

        <label className="field-label" htmlFor="reject-request-detail">
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

        <div className="modal-footer-inline">
          <Button variant="white" onClick={onClose}>
            {t('requests.reject.cancel')}
          </Button>
          <Button variant="red" submit disabled={rejectMutation.isPending}>
            {t('requests.reject.submit')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
