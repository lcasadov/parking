import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCancelRequest } from '../hooks/useRequests';

interface CancelRequestModalProps {
  requestId: number;
  requestedDate: string;
  onClose: () => void;
  onCancelled: () => void;
  // 'cancel' (por defecto): renunciar a una solicitud aún PENDIENTE.
  // 'release': liberar una reserva que YA tienes (aprobada) → recurso disponible
  // para otra persona ese día. Solo cambia el texto; la operación es la misma.
  mode?: 'cancel' | 'release';
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
  mode = 'cancel',
}: CancelRequestModalProps) {
  const { t } = useTranslation();
  const cancelMutation = useCancelRequest();
  // Prefijo i18n según el modo: liberar (aprobada) vs cancelar (pendiente).
  const keys = mode === 'release' ? 'requests.releaseRequest' : 'requests.cancel';

  function handleConfirm(): void {
    cancelMutation.mutate(requestId, {
      onSuccess: onCancelled,
      onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
    });
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t(`${keys}.keep`)}
      </Button>
      <Button variant="red" onClick={handleConfirm} loading={cancelMutation.isPending}>
        {t(`${keys}.confirm`)}
      </Button>
    </>
  );

  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open) {
          onClose();
        }
      }}
      title={t(`${keys}.title`)}
      tone="red"
      narrow
      footer={footer}
    >
      <p>{t(`${keys}.body`, { date: requestedDate })}</p>
    </Dialog>
  );
}
