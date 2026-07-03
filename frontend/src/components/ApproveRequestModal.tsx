import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useApproveRequest } from '../hooks/useRequests';
import type { ParkingSpace } from '../types/parkingSpace';

interface ApproveRequestModalProps {
  requestId: number;
  spaces: ParkingSpace[];
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

// Modal ADMIN: aprueba una solicitud asignando una plaza y una nota opcional
// (POST /requests/{id}/approve). Un 409 indica plaza no disponible o colision.
export function ApproveRequestModal({
  requestId,
  spaces,
  onClose,
  onApproved,
}: ApproveRequestModalProps) {
  const { t } = useTranslation();
  const [spaceId, setSpaceId] = useState<number | ''>('');
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | null>(null);
  const approveMutation = useApproveRequest();

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    if (spaceId === '') {
      setError(t('requests.approve.requiredSpace'));
      return;
    }
    setError(null);
    const trimmedNote = note.trim();
    approveMutation.mutate(
      {
        id: requestId,
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
    <Modal title={t('requests.approve.title')} onClose={onClose}>
      <form id="approve-request-form" onSubmit={handleSubmit} noValidate>
        <label className="field-label" htmlFor="approve-request-space">
          {t('requests.approve.space')}
        </label>
        <select
          id="approve-request-space"
          className="field-input"
          value={spaceId}
          onChange={(event) =>
            setSpaceId(event.target.value === '' ? '' : Number(event.target.value))
          }
        >
          <option value="">{t('requests.approve.selectSpace')}</option>
          {spaces.map((space) => (
            <option key={space.id} value={space.id}>
              {space.label}
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
