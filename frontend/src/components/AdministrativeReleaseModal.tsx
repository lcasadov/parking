import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCreateAdministrativeRelease } from '../hooks/useReleases';
import { todayIso } from '../utils/releases';
import type { Employee } from '../types/employee';
import type { ParkingSpace } from '../types/parkingSpace';
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
  employees?: Employee[];
  spaces?: ParkingSpace[];
  prefill?: AdministrativeReleasePrefill;
  onClose: () => void;
  onCreated: () => void;
}

const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;

// Traduce el error del servidor (400 ventana/motivo, 409 sin asignacion /
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

// Modal ADMIN: libera el recurso fijo de un empleado para una fecha concreta con
// motivo obligatorio (POST /releases/administrative). Dos modos:
//  - seleccion libre (empleado/plaza/fecha en desplegables), y
//  - pre-rellenado (`prefill`) desde "Liberar por fecha": campos fijos, solo motivo.
export function AdministrativeReleaseModal({
  employees = [],
  spaces = [],
  prefill,
  onClose,
  onCreated,
}: AdministrativeReleaseModalProps) {
  const { t } = useTranslation();
  const [employeeId, setEmployeeId] = useState<number | ''>(prefill?.employeeId ?? '');
  const [spaceId, setSpaceId] = useState<number | ''>(prefill?.parkingSpaceId ?? '');
  const [date, setDate] = useState(prefill?.releaseDate ?? '');
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const createMutation = useCreateAdministrativeRelease();

  function validate(): string | null {
    if (employeeId === '') {
      return t('releases.admin.requiredEmployee');
    }
    if (spaceId === '') {
      return t('releases.admin.requiredSpace');
    }
    if (date === '') {
      return t('releases.admin.requiredDate');
    }
    if (reason.trim() === '') {
      return t('releases.admin.requiredReason');
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
    createMutation.mutate(
      {
        employeeId: Number(employeeId),
        parkingSpaceId: Number(spaceId),
        releaseDate: date,
        reason: reason.trim(),
        ...(prefill ? { resourceType: prefill.resourceType } : {}),
      },
      {
        onSuccess: onCreated,
        onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
      },
    );
  }

  return (
    <Modal title={t('releases.admin.title')} onClose={onClose} variant="red">
      <form id="administrative-release-form" onSubmit={handleSubmit} noValidate>
        {prefill ? (
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
        ) : (
          <>
            <label className="field-label" htmlFor="administrative-release-employee">
              {t('releases.admin.employee')}
            </label>
            <select
              id="administrative-release-employee"
              className="field-input"
              value={employeeId}
              onChange={(event) =>
                setEmployeeId(event.target.value === '' ? '' : Number(event.target.value))
              }
            >
              <option value="">{t('releases.admin.selectEmployee')}</option>
              {employees.map((employee) => (
                <option key={employee.id} value={employee.id}>
                  {`${employee.firstName} ${employee.lastName}`}
                </option>
              ))}
            </select>

            <label className="field-label" htmlFor="administrative-release-space">
              {t('releases.admin.space')}
            </label>
            <select
              id="administrative-release-space"
              className="field-input"
              value={spaceId}
              onChange={(event) =>
                setSpaceId(event.target.value === '' ? '' : Number(event.target.value))
              }
            >
              <option value="">{t('releases.admin.selectSpace')}</option>
              {spaces.map((space) => (
                <option key={space.id} value={space.id}>
                  {space.label}
                </option>
              ))}
            </select>

            <label className="field-label" htmlFor="administrative-release-date">
              {t('releases.admin.date')}
            </label>
            <input
              id="administrative-release-date"
              type="date"
              className="field-input"
              value={date}
              min={todayIso()}
              onChange={(event) => setDate(event.target.value)}
            />
          </>
        )}

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

        <div className="modal-footer-inline">
          <Button variant="white" onClick={onClose}>
            {t('releases.admin.cancel')}
          </Button>
          <Button variant="green" submit disabled={createMutation.isPending}>
            {t('releases.admin.submit')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
