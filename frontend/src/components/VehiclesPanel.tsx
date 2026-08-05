import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { ConfirmDialog } from './ConfirmDialog';
import { TableEmpty, TableError, TableSkeleton } from './TableStates';
import { VehicleFormModal } from './VehicleFormModal';
import { emptyVehicleForm, vehicleFormFrom, type VehicleForm } from './vehicleForm';
import { VehicleHistoryModal } from './VehicleHistoryModal';
import { getStatus } from '../api/apiError';
import type {
  DraftVehicle,
  Vehicle,
  VehicleRequest,
  VehiclesDraft,
  VehiclesHooks,
  VehicleStatus,
} from '../types/vehicle';

const HTTP_CONFLICT = 409;
const NONE = '—';

interface MutationCallbacks {
  onSuccess: () => void;
  onError?: (error: unknown) => void;
}

// Interfaz de datos que el CRUD presentacional consume, indistinta del origen: la lista viva
// (react-query, propietario con id) o el borrador en memoria (propietario aún sin id).
interface VehiclesStore {
  vehicles: Vehicle[];
  isLoading: boolean;
  isError: boolean;
  saving: boolean;
  deleting: boolean;
  onRetry: () => void;
  create: (body: VehicleRequest, cb: MutationCallbacks) => void;
  update: (id: number, body: VehicleRequest, cb: MutationCallbacks) => void;
  remove: (id: number, cb: MutationCallbacks) => void;
}

interface VehiclesPanelProps {
  // Id del propietario (empleado o visitante); null durante el alta (aún sin id).
  ownerId: number | null;
  // Hooks react-query del propietario concreto (endpoints anidados distintos, misma firma).
  hooks: VehiclesHooks;
  // Prefijo de namespace i18n del propietario: 'employees.vehicles' | 'visitors.vehicles'.
  ns: string;
  // Modo borrador: si el propietario aún no tiene id y se provee, el panel gestiona los
  // vehículos en memoria (sin API) y el formulario padre los persiste al crear. Sin él, un
  // propietario sin id muestra el aviso "guarda primero".
  draft?: VehiclesDraft;
  // Muestra la columna Estado + acceso al histórico (solo vehículos de empleado con validación;
  // los de visitante no lo pasan). Fase 2 del change employee-vehicle-self-service.
  showStatus?: boolean;
}

// Panel del tab "Vehículos" reutilizable entre el formulario de empleado y el de visitante
// (changes employee-vehicles / visitor-vehicles). Con id gestiona el CRUD contra la API; sin id
// pero con `draft`, acumula en memoria (el padre persiste al guardar); sin id ni `draft`, pide
// guardar primero. La UI del CRUD es común: solo cambia el origen de datos (store) y los textos.
export function VehiclesPanel({ ownerId, hooks, ns, draft, showStatus }: VehiclesPanelProps) {
  const { t } = useTranslation();
  if (ownerId !== null) {
    return (
      <div role="tabpanel" aria-label={t(`${ns}.tabLabel`)}>
        <LiveVehicles ownerId={ownerId} hooks={hooks} ns={ns} showStatus={showStatus} />
      </div>
    );
  }
  return (
    <div role="tabpanel" aria-label={t(`${ns}.tabLabel`)}>
      {draft ? (
        <DraftVehicles draft={draft} ns={ns} />
      ) : (
        <p className="form-hint">{t(`${ns}.saveFirst`)}</p>
      )}
    </div>
  );
}

// Contenedor "en vivo": store respaldado por react-query (propietario con id).
function LiveVehicles({
  ownerId,
  hooks,
  ns,
  showStatus,
}: {
  ownerId: number;
  hooks: VehiclesHooks;
  ns: string;
  showStatus?: boolean;
}) {
  const query = hooks.useList(ownerId);
  const createMutation = hooks.useCreate(ownerId);
  const updateMutation = hooks.useUpdate(ownerId);
  const deleteMutation = hooks.useDelete(ownerId);

  const store: VehiclesStore = {
    vehicles: query.data ?? [],
    isLoading: query.isLoading,
    isError: query.isError,
    saving: createMutation.isPending || updateMutation.isPending,
    deleting: deleteMutation.isPending,
    onRetry: () => void query.refetch(),
    create: (body, cb) => createMutation.mutate(body, cb),
    update: (id, body, cb) => updateMutation.mutate({ vehicleId: id, body }, cb),
    remove: (id, cb) => deleteMutation.mutate(id, cb),
  };
  return <VehiclesCrud store={store} ns={ns} showStatus={showStatus} />;
}

// Contenedor "borrador": store en memoria (propietario aún sin id). No hay red ni errores de
// servidor; la unicidad definitiva la valida el backend al persistir tras crear el propietario.
function DraftVehicles({ draft, ns }: { draft: VehiclesDraft; ns: string }) {
  const { vehicles: drafts, onChange } = draft;
  const nextTempId = drafts.reduce((max, d) => Math.max(max, d.tempId), 0) + 1;
  const asVehicle = (d: DraftVehicle): Vehicle => ({
    id: d.tempId,
    licensePlate: d.licensePlate,
    brand: d.brand ?? null,
    model: d.model ?? null,
    color: d.color ?? null,
    createdAt: '',
  });

  const store: VehiclesStore = {
    vehicles: drafts.map(asVehicle),
    isLoading: false,
    isError: false,
    saving: false,
    deleting: false,
    onRetry: () => {},
    create: (body, cb) => {
      onChange([...drafts, { tempId: nextTempId, ...body }]);
      cb.onSuccess();
    },
    update: (id, body, cb) => {
      onChange(drafts.map((d) => (d.tempId === id ? { tempId: id, ...body } : d)));
      cb.onSuccess();
    },
    remove: (id, cb) => {
      onChange(drafts.filter((d) => d.tempId !== id));
      cb.onSuccess();
    },
  };
  return <VehiclesCrud store={store} ns={ns} />;
}

// CRUD presentacional común: lista + modal de alta/edición (encima del form del propietario) +
// borrado con diálogo aparte. No conoce el origen de datos: opera sobre `store`.
function VehiclesCrud({
  store,
  ns,
  showStatus,
}: {
  store: VehiclesStore;
  ns: string;
  showStatus?: boolean;
}) {
  const { t } = useTranslation();
  const [form, setForm] = useState<VehicleForm | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Vehicle | null>(null);
  const [historyTarget, setHistoryTarget] = useState<Vehicle | null>(null);

  const { vehicles } = store;

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
      setError(t(`${ns}.plateRequired`));
      return;
    }
    const body: VehicleRequest = {
      licensePlate: plate,
      brand: form.brand.trim() || undefined,
      model: form.model.trim() || undefined,
      color: form.color.trim() || undefined,
    };
    const cb: MutationCallbacks = {
      onSuccess: closeForm,
      onError: (err: unknown) =>
        setError(getStatus(err) === HTTP_CONFLICT ? t(`${ns}.plateTaken`) : t(`${ns}.saveError`)),
    };
    if (form.id === null) {
      store.create(body, cb);
    } else {
      store.update(form.id, body, cb);
    }
  }

  function confirmDelete(): void {
    if (!deleteTarget) {
      return;
    }
    store.remove(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) });
  }

  return (
    <div className="vehicles-panel">
      {store.isLoading ? <TableSkeleton label={t('common.loading')} columns={5} /> : null}

      {store.isError ? (
        <TableError
          message={t(`${ns}.loadError`)}
          retryLabel={t('common.retry')}
          onRetry={store.onRetry}
        />
      ) : null}

      {!store.isLoading && !store.isError && vehicles.length === 0 ? (
        <TableEmpty icon="car" message={t(`${ns}.empty`)} />
      ) : null}

      {!store.isLoading && !store.isError && vehicles.length > 0 ? (
        <VehiclesTable
          vehicles={vehicles}
          onEdit={openEdit}
          onDelete={setDeleteTarget}
          onHistory={showStatus ? setHistoryTarget : undefined}
          ns={ns}
          showStatus={showStatus}
        />
      ) : null}

      <div className="vehicles-add">
        <Button variant="green" icon="plus" onClick={openCreate}>
          {t(`${ns}.add`)}
        </Button>
      </div>

      {form ? (
        <VehicleFormModal
          form={form}
          error={error}
          saving={store.saving}
          ns={ns}
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
        title={t(`${ns}.deleteTitle`)}
        description={deleteTarget ? t(`${ns}.deleteBody`, { plate: deleteTarget.licensePlate }) : ''}
        confirmLabel={t(`${ns}.confirmDelete`)}
        loading={store.deleting}
        onConfirm={confirmDelete}
      />

      {showStatus && historyTarget ? (
        <VehicleHistoryModal
          vehicleId={historyTarget.id}
          plate={historyTarget.licensePlate}
          onClose={() => setHistoryTarget(null)}
        />
      ) : null}
    </div>
  );
}

const STATUS_BADGE: Record<VehicleStatus, string> = {
  PENDING: 'status-pending',
  IN_PROGRESS: 'status-in-progress',
  APPROVED: 'status-approved',
  REJECTED: 'status-rejected',
  PENDING_DELETION: 'status-rejected',
};

interface VehiclesTableProps {
  vehicles: Vehicle[];
  ns: string;
  onEdit: (vehicle: Vehicle) => void;
  onDelete: (vehicle: Vehicle) => void;
  onHistory?: (vehicle: Vehicle) => void;
  showStatus?: boolean;
}

// Tabla de vehículos con acciones de editar y borrar (la confirmación de borrado y el
// formulario viven en modales aparte, no inline). Con `showStatus`, añade la columna Estado y el
// acceso al histórico (vehículos de empleado con validación).
function VehiclesTable({ vehicles, ns, onEdit, onDelete, onHistory, showStatus }: VehiclesTableProps) {
  const { t } = useTranslation();
  return (
    <div className="table-scroll table-cards-mobile">
      <table className="table">
        <thead>
          <tr className="table-header">
            <th scope="col">{t(`${ns}.columns.brand`)}</th>
            <th scope="col">{t(`${ns}.columns.model`)}</th>
            <th scope="col">{t(`${ns}.columns.plate`)}</th>
            <th scope="col">{t(`${ns}.columns.color`)}</th>
            {showStatus ? <th scope="col">{t('vehicles.review.columns.status')}</th> : null}
            <th scope="col">{t(`${ns}.columns.actions`)}</th>
          </tr>
        </thead>
        <tbody>
          {vehicles.map((vehicle) => (
            <tr key={vehicle.id} className="table-row">
              <td data-label={t(`${ns}.columns.brand`)}>{vehicle.brand ?? NONE}</td>
              <td data-label={t(`${ns}.columns.model`)}>{vehicle.model ?? NONE}</td>
              <td className="mono" data-label={t(`${ns}.columns.plate`)}>
                {vehicle.licensePlate}
              </td>
              <td data-label={t(`${ns}.columns.color`)}>{vehicle.color ?? NONE}</td>
              {showStatus ? (
                <td data-label={t('vehicles.review.columns.status')}>
                  {vehicle.status ? (
                    <span className={`status-badge ${STATUS_BADGE[vehicle.status]}`}>
                      {t(`vehicles.status.${vehicle.status}`)}
                    </span>
                  ) : (
                    NONE
                  )}
                </td>
              ) : null}
              <td className="table-actions" data-label={t(`${ns}.columns.actions`)}>
                <Button variant="white" icon="pencil" onClick={() => onEdit(vehicle)}>
                  {t(`${ns}.edit`)}
                </Button>
                <Button variant="red" icon="trash" onClick={() => onDelete(vehicle)}>
                  {t(`${ns}.delete`)}
                </Button>
                {onHistory ? (
                  <Button variant="white" icon="history" onClick={() => onHistory(vehicle)}>
                    {t('vehicles.review.actions.history')}
                  </Button>
                ) : null}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
