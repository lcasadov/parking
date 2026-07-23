import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { FieldValue } from './FieldValue';
import { InfoBanner } from './InfoBanner';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCreateRelease } from '../hooks/useReleases';
import { todayIso } from '../utils/releases';
import { longDate } from '../utils/calendar';
import type { ResourceType } from '../types/request';

interface ReleaseResourceModalProps {
  parkingSpaceId: number;
  spaceLabel: string;
  // Tipo del recurso fijo a liberar. Default PARKING (retrocompatible): el bug
  // #1.8 hacia que el puesto fijo se liberase siempre como PARKING porque el
  // resourceType nunca se enviaba. Solo se incluye en el envio cuando es DESK
  // (PARKING es el default del backend, se omite para no cambiar el contrato).
  resourceType?: ResourceType;
  // Cuando se libera un dia concreto (vista "Mi Semana" por-dia) la fecha ya esta
  // fijada: se muestra bloqueada y sin selector. Omitido, el empleado elige la fecha.
  presetDate?: string;
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
  resourceType = 'PARKING',
  presetDate,
  onClose,
  onReleased,
}: ReleaseResourceModalProps) {
  const { t, i18n } = useTranslation();
  const [date, setDate] = useState(presetDate ?? '');
  const [error, setError] = useState<string | null>(null);
  const releaseMutation = useCreateRelease();
  const daySummary = date === '' ? t('releases.release.summary.pendingDay') : longDate(date, i18n.language);

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    if (date === '') {
      setError(t('releases.release.requiredDate'));
      return;
    }
    setError(null);
    releaseMutation.mutate(
      {
        releaseDate: date,
        parkingSpaceId,
        ...(resourceType === 'DESK' ? { resourceType } : {}),
      },
      {
        onSuccess: onReleased,
        onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
      },
    );
  }

  return (
    <Modal title={t('releases.release.title')} onClose={onClose}>
      <form id="release-resource-form" onSubmit={handleSubmit} noValidate>
        <InfoBanner variant="green" icon="parking">
          {t('releases.release.fixedResource', { label: spaceLabel })}
        </InfoBanner>

        <label className="field-label" htmlFor="release-resource-date">
          {t('releases.release.date')}
        </label>
        {presetDate ? (
          <FieldValue readOnly>
            <span id="release-resource-date">{longDate(presetDate, i18n.language)}</span>
          </FieldValue>
        ) : (
          <input
            id="release-resource-date"
            type="date"
            className="field-input"
            value={date}
            min={todayIso()}
            onChange={(event) => setDate(event.target.value)}
          />
        )}
        <p className="hint">{t('releases.release.hint')}</p>

        <div className="release-summary">
          <p className="summary-label">{t('releases.release.summary.title')}</p>
          <label className="field-label" htmlFor="release-summary-resource">
            {t('releases.release.summary.resource')}
          </label>
          <FieldValue readOnly>
            <span id="release-summary-resource">{spaceLabel}</span>
          </FieldValue>
          <label className="field-label" htmlFor="release-summary-day">
            {t('releases.release.summary.day')}
          </label>
          <FieldValue readOnly>
            <span id="release-summary-day">{daySummary}</span>
          </FieldValue>
          <span className="field-label">{t('releases.release.summary.type')}</span>
          <FieldValue readOnly>
            <span className="pill pill-pink">{t('releases.release.summary.voluntary')}</span>
          </FieldValue>
        </div>

        <InfoBanner variant="blue" icon="users">
          {t('releases.release.summary.availableNote')}
        </InfoBanner>

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
