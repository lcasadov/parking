import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCancelRelease } from '../hooks/useReleases';

interface CancelReleaseModalProps {
  releaseId: number;
  releaseDate: string;
  onClose: () => void;
  onCancelled: () => void;
}

const HTTP_FORBIDDEN = 403;
const HTTP_CONFLICT = 409;

// Traduce el error del servidor a la clave i18n del toast. Un 403 indica BOLA
// (liberacion de otro empleado); un 409, una liberacion ya no anulable (pasada).
// El interceptor Axios ya emite un toast generico para 403, por lo que aqui solo
// se sobreescribe con un mensaje mas especifico cuando aplica.
function toastKeyForError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_FORBIDDEN) {
    return 'releases.errors.forbidden';
  }
  if (status === HTTP_CONFLICT) {
    return 'releases.errors.conflict';
  }
  return 'releases.errors.generic';
}

// Modal EMPLOYEE: confirma la anulacion de una liberacion futura propia
// (DELETE /releases/{id}).
export function CancelReleaseModal({
  releaseId,
  releaseDate,
  onClose,
  onCancelled,
}: CancelReleaseModalProps) {
  const { t } = useTranslation();
  const cancelMutation = useCancelRelease();

  function handleConfirm(): void {
    cancelMutation.mutate(releaseId, {
      onSuccess: onCancelled,
      onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
    });
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('releases.cancel.keep')}
      </Button>
      <Button variant="red" onClick={handleConfirm} disabled={cancelMutation.isPending}>
        {t('releases.cancel.confirm')}
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
      title={t('releases.cancel.title')}
      tone="green"
      narrow
      footer={footer}
    >
      <p>{t('releases.cancel.body', { date: releaseDate })}</p>
    </Dialog>
  );
}
