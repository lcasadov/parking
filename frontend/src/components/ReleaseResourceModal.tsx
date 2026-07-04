import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCreateRelease } from '../hooks/useReleases';
import { todayIso } from '../utils/releases';

interface ReleaseResourceModalProps {
  parkingSpaceId: number;
  spaceLabel: string;
  onClose: () => void;
  onReleased: () => void;
}

const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;

// Traduce el error del servidor (400 ventana/fecha pasada, 409 sin asignacion /
// duplicado) a la clave i18n del toast correspondiente (tasks §4.4).
function toastKeyForError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_CONFLICT) {
    return 'releases.errors.conflict';
  }
  if (status === HTTP_BAD_REQUEST) {
    return 'releases.errors.window';
  }
  return 'releases.errors.generic';
}

// Modal EMPLOYEE: libera la plaza fija propia para una fecha presente o futura
// (POST /releases). El input restringe min=hoy; el backend valida ventana y la
// existencia de asignacion fija para ese dia.
export function ReleaseResourceModal({
  parkingSpaceId,
  spaceLabel,
  onClose,
  onReleased,
}: ReleaseResourceModalProps) {
  const { t } = useTranslation();
  const [date, setDate] = useState('');
  const [error, setError] = useState<string | null>(null);
  const releaseMutation = useCreateRelease();

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    if (date === '') {
      setError(t('releases.release.requiredDate'));
      return;
    }
    setError(null);
    releaseMutation.mutate(
      { releaseDate: date, parkingSpaceId },
      {
        onSuccess: onReleased,
        onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
      },
    );
  }

  return (
    <Modal title={t('releases.release.title')} onClose={onClose}>
      <form id="release-resource-form" onSubmit={handleSubmit} noValidate>
        <label className="field-label" htmlFor="release-resource-space">
          {t('releases.release.space')}
        </label>
        <input
          id="release-resource-space"
          className="field-input"
          value={spaceLabel}
          readOnly
        />

        <label className="field-label" htmlFor="release-resource-date">
          {t('releases.release.date')}
        </label>
        <input
          id="release-resource-date"
          type="date"
          className="field-input"
          value={date}
          min={todayIso()}
          onChange={(event) => setDate(event.target.value)}
        />
        <p className="hint">{t('releases.release.hint')}</p>

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}

        <div className="modal-footer-inline">
          <Button variant="white" onClick={onClose}>
            {t('releases.release.cancel')}
          </Button>
          <Button variant="green" submit disabled={releaseMutation.isPending}>
            {t('releases.release.submit')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
