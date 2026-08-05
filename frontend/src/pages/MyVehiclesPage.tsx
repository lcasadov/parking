import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { InfoBanner } from '../components/InfoBanner';
import { PageHeader } from '../components/PageHeader';
import { TableEmpty, TableError, TableSkeleton } from '../components/TableStates';
import { VehicleFormModal } from '../components/VehicleFormModal';
import { emptyVehicleForm, vehicleFormFrom, type VehicleForm } from '../components/vehicleForm';
import { getStatus } from '../api/apiError';
import {
  useCreateMyVehicle,
  useDeleteMyVehicle,
  useMyVehiclesQuery,
  useUpdateMyVehicle,
} from '../hooks/useMyVehicles';
import { useToast } from '../hooks/useToast';
import type { Vehicle, VehicleRequest, VehicleStatus } from '../types/vehicle';

const NS = 'vehicles.mine';
const HTTP_CONFLICT = 409;
const NONE = '—';

const STATUS_CLASS: Record<VehicleStatus, string> = {
  PENDING: 'status-pending',
  IN_PROGRESS: 'status-in-progress',
  APPROVED: 'status-approved',
  REJECTED: 'status-rejected',
  PENDING_DELETION: 'status-rejected',
};

// Sección "Mis vehículos" del portal del empleado (change employee-vehicle-self-service, Fase 1):
// el empleado da de alta/modifica/borra sus vehículos. El alta y la edición dejan el vehículo
// PENDING de validación por un administrador; el rechazo muestra su motivo.
export function MyVehiclesPage() {
  const { t } = useTranslation();
  const toast = useToast();
  const query = useMyVehiclesQuery();
  const createMutation = useCreateMyVehicle();
  const updateMutation = useUpdateMyVehicle();
  const deleteMutation = useDeleteMyVehicle();

  const [form, setForm] = useState<VehicleForm | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Vehicle | null>(null);

  const vehicles = query.data ?? [];
  const saving = createMutation.isPending || updateMutation.isPending;
  const ready = !query.isLoading && !query.isError;

  function openCreate(): void {
    setForm(emptyVehicleForm());
    setError(null);
  }

  function openEdit(vehicle: Vehicle): void {
    setForm(vehicleFormFrom(vehicle));
    setError(null);
  }

  function closeForm(): void {
    setForm(null);
    setError(null);
  }

  function save(): void {
    if (!form) {
      return;
    }
    const plate = form.licensePlate.trim();
    if (plate === '') {
      setError(t(`${NS}.plateRequired`));
      return;
    }
    const body: VehicleRequest = {
      licensePlate: plate,
      brand: form.brand.trim() || undefined,
      model: form.model.trim() || undefined,
      color: form.color.trim() || undefined,
    };
    const options = {
      onSuccess: () => {
        closeForm();
        toast.success(`${NS}.savedToast`);
      },
      onError: (err: unknown) =>
        setError(getStatus(err) === HTTP_CONFLICT ? t(`${NS}.plateTaken`) : t(`${NS}.saveError`)),
    };
    if (form.id === null) {
      createMutation.mutate(body, options);
    } else {
      updateMutation.mutate({ vehicleId: form.id, body }, options);
    }
  }

  function confirmDelete(): void {
    if (!deleteTarget) {
      return;
    }
    deleteMutation.mutate(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) });
  }

  return (
    <section className="my-vehicles-page" aria-label={t(`${NS}.title`)}>
      <PageHeader
        eyebrow={t(`${NS}.eyebrow`)}
        title={t(`${NS}.title`)}
        description={t(`${NS}.description`)}
        actions={
          <Button variant="green" icon="plus" onClick={openCreate}>
            {t(`${NS}.add`)}
          </Button>
        }
      />

      {query.isLoading ? <TableSkeleton label={t('common.loading')} columns={3} /> : null}

      {query.isError ? (
        <TableError
          message={t(`${NS}.loadError`)}
          retryLabel={t('common.retry')}
          onRetry={() => void query.refetch()}
        />
      ) : null}

      {ready && vehicles.length === 0 ? (
        <TableEmpty icon="car" message={t(`${NS}.empty`)} />
      ) : null}

      {ready && vehicles.length > 0 ? (
        <ul className="mv-cards" aria-label={t(`${NS}.title`)}>
          {vehicles.map((vehicle) => (
            <VehicleCard key={vehicle.id} vehicle={vehicle} onEdit={openEdit} onDelete={setDeleteTarget} />
          ))}
        </ul>
      ) : null}

      {form ? (
        <VehicleFormModal
          form={form}
          error={error}
          saving={saving}
          ns={NS}
          notice={
            <InfoBanner variant="amber" icon="clock">
              {t(`${NS}.formNotice`)}
            </InfoBanner>
          }
          onChange={setForm}
          onSave={save}
          onCancel={closeForm}
        />
      ) : null}

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        tone="red"
        icon="trash"
        title={t(`${NS}.deleteTitle`)}
        description={deleteTarget ? t(`${NS}.deleteBody`, { plate: deleteTarget.licensePlate }) : ''}
        confirmLabel={t(`${NS}.confirmDelete`)}
        loading={deleteMutation.isPending}
        onConfirm={confirmDelete}
      />
    </section>
  );
}

interface VehicleCardProps {
  vehicle: Vehicle;
  onEdit: (vehicle: Vehicle) => void;
  onDelete: (vehicle: Vehicle) => void;
}

// Tarjeta de un vehículo propio: matrícula + badge de estado, marca·modelo·color, y el motivo
// de rechazo cuando el estado es REJECTED. Acciones editar / borrar.
function VehicleCard({ vehicle, onEdit, onDelete }: VehicleCardProps) {
  const { t } = useTranslation();
  const status: VehicleStatus = vehicle.status ?? 'PENDING';
  const meta = [vehicle.brand, vehicle.model, vehicle.color].filter(Boolean).join(' · ') || NONE;
  return (
    <li className="mv-card">
      <div className="mv-card-body">
        <div className="mv-card-head">
          <span className="mono mv-plate">{vehicle.licensePlate}</span>
          <span className={`status-badge ${STATUS_CLASS[status]}`}>{t(`vehicles.status.${status}`)}</span>
        </div>
        <span className="mv-card-meta">{meta}</span>
        {status === 'REJECTED' && vehicle.rejectionReason ? (
          <InfoBanner variant="red" icon="alert-triangle">
            {t(`${NS}.rejectedReason`, { reason: vehicle.rejectionReason })}
          </InfoBanner>
        ) : null}
      </div>
      <div className="mv-card-actions">
        <Button variant="white" icon="pencil" onClick={() => onEdit(vehicle)}>
          {t(`${NS}.edit`)}
        </Button>
        <Button variant="red" icon="trash" onClick={() => onDelete(vehicle)}>
          {t(`${NS}.delete`)}
        </Button>
      </div>
    </li>
  );
}
