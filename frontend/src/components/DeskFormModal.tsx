import { RESOURCE_ICON } from '../utils/resourceIcon';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { FieldRow } from './FieldRow';
import { InfoBanner } from './InfoBanner';
import { Input } from './Input';
import { Toggle } from './Toggle';
import { getFieldErrors, getStatus } from '../api/apiError';
import { useCreateDesk, useDeleteDesk, useUpdateDesk } from '../hooks/useDesks';
import { useToast } from '../hooks/useToast';
import type { Desk, DeskCategory, DeskCreate } from '../types/desk';

interface DeskFormModalProps {
  desk?: Desk | null;
  onClose: () => void;
  onSaved: () => void;
}

const HTTP_CONFLICT = 409;
const HTTP_BAD_REQUEST = 400;
const DESK_MIN = 1;

const DESK_CATEGORIES: DeskCategory[] = ['STANDARD', 'EXECUTIVE'];

interface FormState {
  number: string;
  category: DeskCategory;
  active: boolean;
}

function initialState(desk?: Desk | null): FormState {
  return {
    number: desk ? String(desk.number) : '',
    category: desk?.category ?? 'STANDARD',
    active: desk?.active ?? true,
  };
}

// Modal ADMIN: alta/edición de un puesto (número 1-65 + categoría + activo).
// La posición fina (coordX/coordY) se edita en el plano; aquí se conserva o se
// centra por defecto en el alta (init-desks §4.1).
export function DeskFormModal({ desk, onClose, onSaved }: DeskFormModalProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const isEdit = Boolean(desk);
  const [values, setValues] = useState<FormState>(() => initialState(desk));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const createMutation = useCreateDesk();
  const updateMutation = useUpdateDesk();
  const deleteMutation = useDeleteDesk();
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

  function setCategory(value: DeskCategory): void {
    setValues((previous) => ({ ...previous, category: value }));
  }

  function setActive(value: boolean): void {
    setValues((previous) => ({ ...previous, active: value }));
  }

  function validate(): Record<string, string> {
    const next: Record<string, string> = {};
    const trimmed = values.number.trim();
    const parsed = Number(trimmed);
    if (trimmed === '' || Number.isNaN(parsed)) {
      next.number = t('desks.form.required');
    } else if (!Number.isInteger(parsed) || parsed < DESK_MIN) {
      next.number = t('desks.form.numberRange');
    }
    return next;
  }

  // Mapea el error del servidor (409 número duplicado / 400 rango o validación) a
  // errores inline por campo.
  function handleServerError(error: unknown): void {
    const status = getStatus(error);
    if (status === HTTP_CONFLICT) {
      setErrors({ number: t('desks.form.duplicateNumber') });
      return;
    }
    const fields = getFieldErrors(error);
    if (status === HTTP_BAD_REQUEST) {
      setErrors(Object.keys(fields).length > 0 ? fields : { number: t('desks.form.numberRange') });
      return;
    }
    setErrors({ form: t('desks.form.genericError') });
  }

  function submit(): void {
    // Un puesto nuevo nace sin coordenadas (queda "no colocado" hasta que el ADMIN
    // lo posiciona en el plano); en edición se conserva su posición actual.
    const body: DeskCreate = {
      number: Number(values.number.trim()),
      category: values.category,
      ...(isEdit && desk ? { coordX: desk.coordX, coordY: desk.coordY } : {}),
      active: values.active,
    };
    const options = {
      onSuccess: () => {
        toast.success(isEdit ? 'desks.toast.updated' : 'desks.toast.created');
        onSaved();
      },
      onError: handleServerError,
    };
    if (isEdit && desk) {
      updateMutation.mutate({ id: desk.id, body }, options);
    } else {
      createMutation.mutate(body, options);
    }
  }

  // Borra el puesto (hard delete). Un 409 = tiene reservas/historial → sugerir desactivar.
  function handleDelete(): void {
    if (!desk) {
      return;
    }
    deleteMutation.mutate(desk.id, {
      onSuccess: () => {
        toast.success('desks.toast.deleted');
        onSaved();
      },
      onError: (error) => {
        setConfirmingDelete(false);
        setErrors({
          form:
            getStatus(error) === HTTP_CONFLICT
              ? t('desks.form.deleteHasHistory')
              : t('desks.form.deleteError'),
        });
      },
    });
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

  const footer = confirmingDelete ? (
    <>
      <Button
        variant="white"
        onClick={() => setConfirmingDelete(false)}
        disabled={deleteMutation.isPending}
      >
        {t('desks.form.cancel')}
      </Button>
      <Button
        variant="red"
        icon="trash"
        onClick={handleDelete}
        disabled={deleteMutation.isPending}
      >
        {t('desks.form.deleteConfirmYes')}
      </Button>
    </>
  ) : (
    <>
      {isEdit ? (
        <Button
          variant="red"
          icon="trash"
          onClick={() => setConfirmingDelete(true)}
          style={{ marginRight: 'auto' }}
        >
          {t('desks.form.delete')}
        </Button>
      ) : null}
      <Button variant="white" onClick={onClose}>
        {t('desks.form.cancel')}
      </Button>
      <Button variant="green" icon="check" onClick={trySubmit} disabled={isSaving}>
        {t('desks.form.save')}
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
      title={t(isEdit ? 'desks.form.editTitle' : 'desks.form.createTitle')}
      icon={RESOURCE_ICON.DESK}
      tone="green"
      narrow
      footer={footer}
    >
      <form id="desk-form" onSubmit={handleSubmit} noValidate>
        {confirmingDelete ? (
          <InfoBanner variant="red" icon="alert-triangle">
            {t('desks.form.deleteConfirm')}
          </InfoBanner>
        ) : null}
        <FieldRow>
          <Input
            label={t('desks.form.number')}
            type="number"
            min={DESK_MIN}
            value={values.number}
            disabled={isEdit}
            error={Boolean(errors.number)}
            hint={isEdit ? t('desks.form.numberImmutable') : errors.number}
            onChange={(event) => setNumber(event.target.value)}
          />
          <div className="auth-field">
            <span className="field-label">{t('desks.form.statusLabel')}</span>
            <Toggle
              checked={values.active}
              onChange={setActive}
              label={t('desks.form.active')}
            />
            <p className="hint">{t('desks.form.activeHint')}</p>
          </div>
        </FieldRow>
        <div className="auth-field">
          <label className="field-label" htmlFor="desk-category">
            {t('desks.form.category')}
          </label>
          <select
            id="desk-category"
            className="field-input"
            value={values.category}
            onChange={(event) => setCategory(event.target.value as DeskCategory)}
          >
            {DESK_CATEGORIES.map((category) => (
              <option key={category} value={category}>
                {t(`desks.category.${category}`)}
              </option>
            ))}
          </select>
        </div>
        <InfoBanner variant="blue" icon="info-circle">
          {t('desks.form.holderInfo')}
        </InfoBanner>
        {errors.form ? (
          <p className="form-error" role="alert">
            {errors.form}
          </p>
        ) : null}
      </form>
    </Dialog>
  );
}
