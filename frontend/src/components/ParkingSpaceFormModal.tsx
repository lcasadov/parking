import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Input } from './Input';
import { Modal } from './Modal';
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

interface FormState {
  label: string;
  active: boolean;
}

function initialState(space?: ParkingSpace | null): FormState {
  return {
    label: space?.label ?? '',
    active: space?.active ?? true,
  };
}

export function ParkingSpaceFormModal({ space, onClose, onSaved }: ParkingSpaceFormModalProps) {
  const { t } = useTranslation();
  const isEdit = Boolean(space);
  const [values, setValues] = useState<FormState>(() => initialState(space));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const createMutation = useCreateParkingSpace();
  const updateMutation = useUpdateParkingSpace();
  const isSaving = createMutation.isPending || updateMutation.isPending;

  function setLabel(value: string): void {
    setValues((previous) => ({ ...previous, label: value }));
    setErrors((previous) => {
      if (!previous.label) {
        return previous;
      }
      const next = { ...previous };
      delete next.label;
      return next;
    });
  }

  function setActive(value: boolean): void {
    setValues((previous) => ({ ...previous, active: value }));
  }

  function validate(): Record<string, string> {
    const next: Record<string, string> = {};
    if (!values.label.trim()) {
      next.label = t('parkingSpaces.form.required');
    }
    return next;
  }

  // Mapea el error del servidor (409 label duplicado / 400 validacion) a errores
  // inline por campo.
  function handleServerError(error: unknown): void {
    const status = getStatus(error);
    if (status === HTTP_CONFLICT) {
      setErrors({ label: t('parkingSpaces.form.duplicateLabel') });
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
      label: values.label.trim(),
      active: values.active,
    };
    const options = { onSuccess: onSaved, onError: handleServerError };
    if (isEdit && space) {
      updateMutation.mutate({ id: space.id, body }, options);
    } else {
      createMutation.mutate(body, options);
    }
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    submit();
  }

  return (
    <Modal
      title={t(isEdit ? 'parkingSpaces.form.editTitle' : 'parkingSpaces.form.createTitle')}
      onClose={onClose}
    >
      <form id="parking-space-form" onSubmit={handleSubmit} noValidate>
        <Input
          label={t('parkingSpaces.form.label')}
          value={values.label}
          error={Boolean(errors.label)}
          hint={errors.label}
          maxLength={20}
          onChange={(event) => setLabel(event.target.value)}
        />
        <label className="checkbox-field">
          <input
            type="checkbox"
            checked={values.active}
            onChange={(event) => setActive(event.target.checked)}
          />
          {t('parkingSpaces.form.active')}
        </label>
        {errors.form ? (
          <p className="form-error" role="alert">
            {errors.form}
          </p>
        ) : null}
        <div className="modal-footer-inline">
          <Button variant="white" onClick={onClose}>
            {t('parkingSpaces.form.cancel')}
          </Button>
          <Button variant="green" submit disabled={isSaving}>
            {t('parkingSpaces.form.save')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
