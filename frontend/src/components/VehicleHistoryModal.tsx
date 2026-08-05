import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { TableEmpty, TableError, TableSkeleton } from './TableStates';
import { useVehicleHistoryQuery } from '../hooks/useEmployeeVehicleReview';
import type { VehicleHistoryEntry } from '../types/employeeVehicleReview';

interface VehicleHistoryModalProps {
  vehicleId: number;
  plate: string;
  onClose: () => void;
}

// Modal con el histórico de cambios de un vehículo (change employee-vehicle-self-service, Fase 2):
// timeline de alta, ediciones (con datos previos), cambios de estado y solicitud de borrado.
export function VehicleHistoryModal({ vehicleId, plate, onClose }: VehicleHistoryModalProps) {
  const { t } = useTranslation();
  const query = useVehicleHistoryQuery(vehicleId);
  const entries = query.data ?? [];

  return (
    <Dialog
      open
      onOpenChange={(next) => {
        if (!next) onClose();
      }}
      icon="history"
      title={t('vehicles.review.history.title', { plate })}
      footer={
        <Button variant="white" onClick={onClose}>
          {t('common.close')}
        </Button>
      }
    >
      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={2} /> : null}
      {query.isError ? (
        <TableError
          message={t('vehicles.review.history.loadError')}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}
      {!query.isLoading && !query.isError && entries.length === 0 ? (
        <TableEmpty icon="history" message={t('vehicles.review.history.empty')} />
      ) : null}
      {entries.length > 0 ? (
        <ul className="vehicle-history">
          {entries.map((entry) => (
            <HistoryItem key={entry.id} entry={entry} />
          ))}
        </ul>
      ) : null}
    </Dialog>
  );
}

function HistoryItem({ entry }: { entry: VehicleHistoryEntry }) {
  const { t, i18n } = useTranslation();
  const actor = entry.actorRole ? t(`vehicles.review.history.actor.${entry.actorRole}`) : '';
  const when = new Date(entry.createdAt).toLocaleString(i18n.language, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
  return (
    <li className="vehicle-history-item">
      <div className="vehicle-history-head">
        <span className="vehicle-history-event">{describe(entry, t)}</span>
        <span className="vehicle-history-actor">
          {actor ? `${actor} · ` : ''}
          {when}
        </span>
      </div>
      {entry.note ? <span className="vehicle-history-note">{entry.note}</span> : null}
      {entry.eventType === 'EDITED' && entry.previousData ? (
        <span className="vehicle-history-note">
          {t('vehicles.review.history.previousData', {
            plate: entry.previousData.licensePlate,
            brand: entry.previousData.brand ?? '—',
            model: entry.previousData.model ?? '—',
            color: entry.previousData.color ?? '—',
          })}
        </span>
      ) : null}
    </li>
  );
}

function describe(entry: VehicleHistoryEntry, t: TFunction): string {
  if (entry.eventType === 'STATUS_CHANGED' && entry.toStatus) {
    return t('vehicles.review.history.statusChanged', {
      status: t(`vehicles.status.${entry.toStatus}`),
    });
  }
  return t(`vehicles.review.history.event.${entry.eventType}`);
}
