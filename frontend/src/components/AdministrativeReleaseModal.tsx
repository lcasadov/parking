import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCreateAdministrativeRelease } from '../hooks/useReleases';
import type { ResourceType } from '../types/request';

// Datos pre-rellenados cuando el modal se abre desde la vista "Liberar por fecha":
// empleado, recurso, fecha y tipo ya resueltos; el ADMIN solo confirma el motivo.
export interface AdministrativeReleasePrefill {
  employeeId: number;
  employeeName: string;
  parkingSpaceId: number;
  resourceLabel: string;
  releaseDate: string;
  resourceType: ResourceType;
}

interface AdministrativeReleaseModalProps {
  prefill: AdministrativeReleasePrefill;
  onClose: () => void;
  onCreated: () => void;
}

const FORM_ID = 'administrative-release-form';
const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;

// Traduce el error del servidor (400 ventana/motivo, 409 sin asignacion /
// duplicado) a la clave i18n del toast correspondiente.
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

// Modal ADMIN de confirmacion de "Liberar por fecha" cuando el recurso proviene de
// una asignacion fija: libera el recurso del empleado para la fecha ya resuelta
// (POST /releases/administrative) con motivo obligatorio. El empleado, recurso,
// fecha y tipo llegan pre-rellenados (`prefill`); aqui solo se escribe el motivo.
// El flujo de liberacion por empleado y semana vive en AdministrativeReleasesPage.
export function AdministrativeReleaseModal({
  prefill,
  onClose,
  onCreated,
}: AdministrativeReleaseModalProps) {
  const { t } = useTranslation();
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const createMutation = useCreateAdministrativeRelease();

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    if (reason.trim() === '') {
      setError(t('releases.admin.requiredReason'));
      return;
    }
    setError(null);
    createMutation.mutate(
      {
        employeeId: prefill.employeeId,
        parkingSpaceId: prefill.parkingSpaceId,
        releaseDate: prefill.releaseDate,
        reason: reason.trim(),
        resourceType: prefill.resourceType,
      },
      {
        onSuccess: onCreated,
        onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
      },
    );
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('releases.admin.cancel')}
      </Button>
      <Button variant="green" submit form={FORM_ID} disabled={createMutation.isPending}>
        {t('releases.admin.submit')}
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
      title={t('releases.admin.title')}
      tone="red"
      footer={footer}
    >
      <form id={FORM_ID} onSubmit={handleSubmit} noValidate>
        <dl className="release-prefill" aria-label={t('releases.byDate.summary')}>
          <div className="release-prefill-row">
            <dt>{t('releases.admin.employee')}</dt>
            <dd>{prefill.employeeName}</dd>
          </div>
          <div className="release-prefill-row">
            <dt>{t(`releases.byDate.resourceType.${prefill.resourceType}`)}</dt>
            <dd>{prefill.resourceLabel}</dd>
          </div>
          <div className="release-prefill-row">
            <dt>{t('releases.admin.date')}</dt>
            <dd>{prefill.releaseDate}</dd>
          </div>
        </dl>

        <label className="field-label" htmlFor="administrative-release-reason">
          {t('releases.admin.reason')}
        </label>
        <textarea
          id="administrative-release-reason"
          className="field-input"
          value={reason}
          onChange={(event) => setReason(event.target.value)}
        />
        <p className="hint">{t('releases.admin.reasonHint')}</p>

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}
      </form>
    </Dialog>
  );
}
