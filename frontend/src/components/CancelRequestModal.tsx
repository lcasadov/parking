import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCancelRequest } from '../hooks/useRequests';

interface CancelRequestModalProps {
  requestId: number;
  requestedDate: string;
  onClose: () => void;
  onCancelled: () => void;
}

const HTTP_CONFLICT = 409;

// Traduce el error del servidor (409 solicitud ya resuelta) a la clave i18n.
function toastKeyForError(error: unknown): string {
  if (getStatus(error) === HTTP_CONFLICT) {
    return 'requests.errors.alreadyResolved';
  }
  return 'requests.errors.generic';
}

// Modal EMPLOYEE: confirma la cancelacion de una solicitud propia en PENDING
// (POST /requests/{id}/cancel). Un 409 indica estado terminal (ya resuelta).
export function CancelRequestModal({
  requestId,
  requestedDate,
  onClose,
  onCancelled,
}: CancelRequestModalProps) {
  const { t } = useTranslation();
  const cancelMutation = useCancelRequest();

  function handleConfirm(): void {
    cancelMutation.mutate(requestId, {
      onSuccess: onCancelled,
      onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
    });
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('requests.cancel.keep')}
      </Button>
      <Button variant="red" onClick={handleConfirm} disabled={cancelMutation.isPending}>
        {t('requests.cancel.confirm')}
      </Button>
    </>
  );

  return (
    <Modal title={t('requests.cancel.title')} onClose={onClose} variant="red" footer={footer}>
      <p>{t('requests.cancel.body', { date: requestedDate })}</p>
    </Modal>
  );
}
