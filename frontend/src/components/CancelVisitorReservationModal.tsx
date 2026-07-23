import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCancelVisitorReservation } from '../hooks/useVisitorReservations';

interface CancelVisitorReservationModalProps {
  reservationId: number;
  reservationDate: string;
  onClose: () => void;
  onCancelled: () => void;
}

const HTTP_BAD_REQUEST = 400;

// Traduce el error del servidor a la clave i18n del toast. Un 400 indica que la
// reserva es pasada y ya no puede anularse (tasks §4.5).
function toastKeyForError(error: unknown): string {
  if (getStatus(error) === HTTP_BAD_REQUEST) {
    return 'visitors.reservations.errors.past';
  }
  return 'visitors.reservations.errors.generic';
}

// Modal ADMIN: confirma la anulacion de una reserva de visita futura
// (DELETE /visitor-reservations/{id}).
export function CancelVisitorReservationModal({
  reservationId,
  reservationDate,
  onClose,
  onCancelled,
}: CancelVisitorReservationModalProps) {
  const { t } = useTranslation();
  const cancelMutation = useCancelVisitorReservation();

  function handleConfirm(): void {
    cancelMutation.mutate(reservationId, {
      onSuccess: onCancelled,
      onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
    });
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('visitors.reservations.cancelModal.keep')}
      </Button>
      <Button variant="red" onClick={handleConfirm} disabled={cancelMutation.isPending}>
        {t('visitors.reservations.cancelModal.confirm')}
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
      title={t('visitors.reservations.cancelModal.title')}
      tone="red"
      narrow
      footer={footer}
    >
      <p>{t('visitors.reservations.cancelModal.body', { date: reservationDate })}</p>
    </Dialog>
  );
}
