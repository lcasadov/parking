import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Input } from './Input';
import { Modal } from './Modal';
import type { VehicleForm } from './vehicleForm';

interface VehicleFormModalProps {
  form: VehicleForm;
  error: string | null;
  saving: boolean;
  // Prefijo de namespace i18n (p.ej. 'employees.vehicles' | 'visitors.vehicles' | 'vehicles.mine').
  ns: string;
  // Aviso opcional bajo el grid (p.ej. InfoBanner de "quedará pendiente de validación").
  notice?: ReactNode;
  onChange: (form: VehicleForm) => void;
  onSave: () => void;
  onCancel: () => void;
}

// Modal de alta/edición de un vehículo, montado encima del modal/página del propietario.
// No es un <form>: los botones del footer guardan/cancelan llamando directamente a los callbacks.
// Reutilizado por el CRUD admin (VehiclesPanel) y por el self-service del empleado (MyVehiclesPage).
export function VehicleFormModal({
  form,
  error,
  saving,
  ns,
  notice,
  onChange,
  onSave,
  onCancel,
}: VehicleFormModalProps) {
  const { t } = useTranslation();
  const title = form.id === null ? t(`${ns}.add`) : t(`${ns}.editTitle`);
  return (
    <Modal
      title={title}
      icon="car"
      narrow
      onClose={saving ? undefined : onCancel}
      footer={
        <>
          <Button variant="white" onClick={onCancel} disabled={saving}>
            {t('common.cancel')}
          </Button>
          <Button variant="green" icon="device-floppy" onClick={onSave} loading={saving}>
            {t(`${ns}.saveVehicle`)}
          </Button>
        </>
      }
    >
      <div className="vehicles-form-grid">
        <Input
          label={t(`${ns}.form.plate`)}
          value={form.licensePlate}
          error={Boolean(error)}
          onChange={(event) => onChange({ ...form, licensePlate: event.target.value })}
        />
        <Input
          label={t(`${ns}.form.brand`)}
          value={form.brand}
          onChange={(event) => onChange({ ...form, brand: event.target.value })}
        />
        <Input
          label={t(`${ns}.form.model`)}
          value={form.model}
          onChange={(event) => onChange({ ...form, model: event.target.value })}
        />
        <Input
          label={t(`${ns}.form.color`)}
          value={form.color}
          onChange={(event) => onChange({ ...form, color: event.target.value })}
        />
      </div>
      {notice}
      {error ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}
    </Modal>
  );
}
