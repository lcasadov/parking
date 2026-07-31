import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { InfoBanner } from './InfoBanner';
import { getStatus } from '../api/apiError';
import { useChangeVehicleStatus } from '../hooks/useEmployeeVehicleReview';
import type { VehicleStatus } from '../types/vehicle';

const HTTP_CONFLICT = 409;
// Estados a los que el admin puede cambiar manualmente (PENDING_DELETION lo inicia el empleado).
const OPTIONS: VehicleStatus[] = ['PENDING', 'IN_PROGRESS', 'APPROVED', 'REJECTED'];

interface VehicleStatusModalProps {
  vehicleId: number;
  plate: string;
  current: VehicleStatus;
  onClose: () => void;
  onChanged: () => void;
  onConflict: () => void;
}

// Modal de cambio manual de estado de un vehículo (change employee-vehicle-self-service, Fase 2):
// selector de estado destino; si es Rechazado pide motivo (texto libre); si es Aprobado recuerda
// confirmar que ya puede acceder al aparcamiento (mutua). Permite corregir un aprobado por error.
export function VehicleStatusModal({
  vehicleId,
  plate,
  current,
  onClose,
  onChanged,
  onConflict,
}: VehicleStatusModalProps) {
  const { t } = useTranslation();
  const selectId = useId();
  const [status, setStatus] = useState<VehicleStatus>(current);
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState(false);
  const changeMutation = useChangeVehicleStatus();

  function submit(): void {
    if (status === 'REJECTED' && reason.trim() === '') {
      setReasonError(true);
      return;
    }
    changeMutation.mutate(
      { vehicleId, status, reason: status === 'REJECTED' ? reason.trim() : undefined },
      {
        onSuccess: onChanged,
        onError: (err) => {
          if (getStatus(err) === HTTP_CONFLICT) {
            onConflict();
          }
        },
      },
    );
  }

  return (
    <Dialog
      open
      onOpenChange={(next) => {
        if (!next) onClose();
      }}
      icon="repeat"
      title={t('vehicles.review.changeStatus.title', { plate })}
      footer={
        <>
          <Button variant="white" onClick={onClose} disabled={changeMutation.isPending}>
            {t('common.cancel')}
          </Button>
          <Button
            variant="green"
            icon="check"
            onClick={submit}
            loading={changeMutation.isPending}
            disabled={status === current && status !== 'REJECTED'}
          >
            {t('vehicles.review.changeStatus.confirm')}
          </Button>
        </>
      }
    >
      <div className="auth-field">
        <label className="field-label" htmlFor={selectId}>
          {t('vehicles.review.changeStatus.label')}
        </label>
        <select
          id={selectId}
          className="field-input"
          value={status}
          onChange={(event) => setStatus(event.target.value as VehicleStatus)}
        >
          {OPTIONS.map((option) => (
            <option key={option} value={option}>
              {t(`vehicles.status.${option}`)}
            </option>
          ))}
        </select>
      </div>

      {status === 'APPROVED' ? (
        <InfoBanner variant="amber" icon="alert-triangle">
          {t('vehicles.review.changeStatus.approveNote')}
        </InfoBanner>
      ) : null}

      {status === 'REJECTED' ? (
        <div className="auth-field">
          <label className="field-label" htmlFor={`${selectId}-reason`}>
            {t('vehicles.review.changeStatus.reasonLabel')}
          </label>
          <textarea
            id={`${selectId}-reason`}
            className={`field-input${reasonError ? ' has-error' : ''}`}
            rows={3}
            value={reason}
            onChange={(event) => {
              setReason(event.target.value);
              if (reasonError) setReasonError(false);
            }}
          />
          {reasonError ? (
            <p className="form-error" role="alert">
              {t('vehicles.review.changeStatus.reasonRequired')}
            </p>
          ) : null}
        </div>
      ) : null}
    </Dialog>
  );
}
