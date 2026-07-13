import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { FieldRow } from './FieldRow';
import { InfoBanner } from './InfoBanner';
import { Input } from './Input';
import { Modal } from './Modal';
import { Toggle } from './Toggle';
import { getFieldErrors, getStatus } from '../api/apiError';
import { useCreateParkingSpace, useUpdateParkingSpace } from '../hooks/useParkingSpaces';
import type { ParkingSpace, ParkingSpaceCreate } from '../types/parkingSpace';

interface ParkingSpaceFormModalProps {
  space?: ParkingSpace | null;
  onClose: () => void;
  onSaved: () => void;
}

const HTTP_CONFLICT = 409;
const HTTP_BAD_REQUEST = 400;
const MIN_NUMBER = 1000;
const SPACES_PER_FLOOR = 1000;

interface FormState {
  number: string;
  active: boolean;
}

function initialState(space?: ParkingSpace | null): FormState {
  return {
    number: space?.number !== undefined ? String(space.number) : '',
    active: space?.active ?? true,
  };
}

// Planta derivada del numero introducido (`floor = number / 1000`), replicando la
// derivacion del backend. Devuelve '' mientras el numero no sea valido.
function derivedFloor(rawNumber: string): string {
  const parsed = Number(rawNumber);
  if (!rawNumber.trim() || !Number.isInteger(parsed) || parsed < MIN_NUMBER) {
    return '';
  }
  return String(Math.floor(parsed / SPACES_PER_FLOOR));
}

export function ParkingSpaceFormModal({ space, onClose, onSaved }: ParkingSpaceFormModalProps) {
  const { t } = useTranslation();
  const isEdit = Boolean(space);
  const [values, setValues] = useState<FormState>(() => initialState(space));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const createMutation = useCreateParkingSpace();
  const updateMutation = useUpdateParkingSpace();
  const isSaving = createMutation.isPending || updateMutation.isPending;

  function setNumber(value: string): void {
    setValues((previous) => ({ ...previous, number: value }));
    setErrors((previous) => {
      if (!previous.number) {
        return previous;
      }
      const next = { ...previous };
      delete next.number;
      return next;
    });
  }

  function setActive(value: boolean): void {
    setValues((previous) => ({ ...previous, active: value }));
  }

  function validate(): Record<string, string> {
    const next: Record<string, string> = {};
    const parsed = Number(values.number);
    if (!values.number.trim()) {
      next.number = t('parkingSpaces.form.required');
    } else if (!Number.isInteger(parsed) || parsed < MIN_NUMBER) {
      next.number = t('parkingSpaces.form.numberInvalid');
    }
    return next;
  }

  // Mapea el error del servidor (409 numero duplicado / 400 validacion) a errores
  // inline por campo.
  function handleServerError(error: unknown): void {
    const status = getStatus(error);
    if (status === HTTP_CONFLICT) {
      setErrors({ number: t('parkingSpaces.form.duplicateNumber') });
      return;
    }
    const fields = getFieldErrors(error);
    if (status === HTTP_BAD_REQUEST && Object.keys(fields).length > 0) {
      setErrors(fields);
      return;
    }
    setErrors({ form: t('parkingSpaces.form.genericError') });
  }

  function submit(): void {
    const body: ParkingSpaceCreate = {
      number: Number(values.number),
      active: values.active,
    };
    const options = { onSuccess: onSaved, onError: handleServerError };
    if (isEdit && space) {
      updateMutation.mutate({ id: space.id, body }, options);
    } else {
      createMutation.mutate(body, options);
    }
  }

  // Valida y envia; compartida por el onSubmit del form (tecla Enter) y el boton
  // GUARDAR de la barra de acciones (que vive fuera del <form>).
  function trySubmit(): void {
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    submit();
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    trySubmit();
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('parkingSpaces.form.cancel')}
      </Button>
      <Button variant="green" icon="check" onClick={trySubmit} disabled={isSaving}>
        {t('parkingSpaces.form.save')}
      </Button>
    </>
  );

  return (
    <Modal
      title={t(isEdit ? 'parkingSpaces.form.editTitle' : 'parkingSpaces.form.createTitle')}
      icon="parking"
      narrow
      onClose={onClose}
      footer={footer}
    >
      <form id="parking-space-form" onSubmit={handleSubmit} noValidate>
        <FieldRow>
          <Input
            label={t('parkingSpaces.form.number')}
            type="number"
            inputMode="numeric"
            min={MIN_NUMBER}
            step={1}
            value={values.number}
            error={Boolean(errors.number)}
            hint={errors.number ?? t('parkingSpaces.form.numberHint')}
            onChange={(event) => setNumber(event.target.value)}
          />
          <Input
            label={t('parkingSpaces.form.floorLabel')}
            value={derivedFloor(values.number)}
            hint={t('parkingSpaces.form.floorHint')}
            readOnly
            placeholder="—"
          />
          <div className="auth-field">
            <span className="field-label">{t('parkingSpaces.form.statusLabel')}</span>
            <Toggle
              checked={values.active}
              onChange={setActive}
              label={t('parkingSpaces.form.active')}
            />
            <p className="hint">{t('parkingSpaces.form.activeHint')}</p>
          </div>
        </FieldRow>
        <InfoBanner variant="blue" icon="info-circle">
          {t('parkingSpaces.form.holderInfo')}
        </InfoBanner>
        {errors.form ? (
          <p className="form-error" role="alert">
            {errors.form}
          </p>
        ) : null}
      </form>
    </Modal>
  );
}
