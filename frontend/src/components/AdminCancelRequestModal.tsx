import { useRef, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useAdminCancelRequest } from '../hooks/useRequests';

// Datos del recurso ocupado por la solicitud que el ADMIN va a liberar cancelandola.
export interface AdminCancelRequestPrefill {
  requestId: number;
  employeeName: string;
  resourceLabel: string;
  releaseDate: string;
}

interface AdminCancelRequestModalProps {
  prefill: AdminCancelRequestPrefill;
  onClose: () => void;
  onCancelled: () => void;
}

const FORM_ID = 'admin-cancel-request-form';
const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;
// El backend exige motivo de 5..500 caracteres (@Size); validamos el minimo en cliente.
const REASON_MIN = 5;

// Traduce el error del servidor (400 motivo, 409 no APPROVED o pasada) a la clave i18n.
function toastKeyForError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_CONFLICT) {
    return 'requests.errors.alreadyResolved';
  }
  if (status === HTTP_BAD_REQUEST) {
    return 'requests.adminCancel.errors.reason';
  }
  return 'requests.errors.generic';
}

// Modal ADMIN "Liberar por fecha" cuando el recurso proviene de una solicitud APPROVED:
// libera el recurso cancelando la solicitud (POST /requests/{id}/admin-cancel) con motivo
// obligatorio (change release-occupied-resource).
export function AdminCancelRequestModal({
  prefill,
  onClose,
  onCancelled,
}: AdminCancelRequestModalProps) {
  const { t } = useTranslation();
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const reasonRef = useRef<HTMLTextAreaElement>(null);
  const adminCancelMutation = useAdminCancelRequest();

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    if (reason.trim().length < REASON_MIN) {
      setError(t('requests.adminCancel.requiredReason'));
      reasonRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      reasonRef.current?.focus();
      return;
    }
    setError(null);
    adminCancelMutation.mutate(
      { id: prefill.requestId, reason: reason.trim() },
      {
        onSuccess: onCancelled,
        onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
      },
    );
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('requests.adminCancel.cancel')}
      </Button>
      <Button variant="red" submit form={FORM_ID} disabled={adminCancelMutation.isPending}>
        {t('requests.adminCancel.submit')}
      </Button>
    </>
  );

  return (
    <Dialog
      open
      onOpenChange={(next) => {
        if (!next) {
          onClose();
        }
      }}
      title={t('requests.adminCancel.title')}
      tone="red"
      footer={footer}
    >
      <form id={FORM_ID} onSubmit={handleSubmit} noValidate>
        <dl className="release-prefill" aria-label={t('requests.adminCancel.summary')}>
          <div className="release-prefill-row">
            <dt>{t('requests.adminCancel.employee')}</dt>
            <dd>{prefill.employeeName}</dd>
          </div>
          <div className="release-prefill-row">
            <dt>{t('requests.adminCancel.resource')}</dt>
            <dd>{prefill.resourceLabel}</dd>
          </div>
          <div className="release-prefill-row">
            <dt>{t('requests.adminCancel.date')}</dt>
            <dd>{prefill.releaseDate}</dd>
          </div>
        </dl>

        <label className="field-label" htmlFor="admin-cancel-request-reason">
          {t('requests.adminCancel.reason')}
        </label>
        <textarea
          id="admin-cancel-request-reason"
          ref={reasonRef}
          className="field-input"
          value={reason}
          onChange={(event) => setReason(event.target.value)}
        />
        <p className="hint">{t('requests.adminCancel.reasonHint')}</p>

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}
      </form>
    </Dialog>
  );
}
