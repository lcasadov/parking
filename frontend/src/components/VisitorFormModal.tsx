import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Input } from './Input';
import { Modal } from './Modal';
import { getFieldErrors } from '../api/apiError';
import { useCreateVisitor, useUpdateVisitor } from '../hooks/useVisitors';
import type { Visitor, VisitorCreateRequest } from '../types/visitor';

interface VisitorFormModalProps {
  visitor?: Visitor | null;
  onClose: () => void;
  onSaved: () => void;
}

interface FormState {
  firstName: string;
  lastName: string;
  nationalId: string;
  licensePlate: string;
  company: string;
  usualReason: string;
}

function initialState(visitor?: Visitor | null): FormState {
  return {
    firstName: visitor?.firstName ?? '',
    lastName: visitor?.lastName ?? '',
    nationalId: visitor?.nationalId ?? '',
    licensePlate: visitor?.licensePlate ?? '',
    company: visitor?.company ?? '',
    usualReason: visitor?.usualReason ?? '',
  };
}

// Mapea ApiError.fields (409/400) a errores inline por campo, traduciendo el
// conflicto conocido de nationalId (documento unico).
function mapServerErrors(
  fields: Record<string, string>,
  translate: (key: string) => string,
): Record<string, string> {
  const mapped: Record<string, string> = {};
  Object.keys(fields).forEach((field) => {
    if (field === 'nationalId') {
      mapped.nationalId = translate('visitors.form.duplicateNationalId');
    } else {
      mapped[field] = fields[field];
    }
  });
  return mapped;
}

// Construye el cuerpo de la peticion normalizando opcionales vacios a undefined.
function toRequestBody(values: FormState): VisitorCreateRequest {
  return {
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    nationalId: values.nationalId.trim(),
    licensePlate: values.licensePlate.trim() || undefined,
    company: values.company.trim() || undefined,
    usualReason: values.usualReason.trim() || undefined,
  };
}

// Modal ADMIN: alta/edicion de ficha de visitante. nationalId unico -> 409
// mostrado inline (tasks §4.2); campos obligatorios validados en cliente.
export function VisitorFormModal({ visitor, onClose, onSaved }: VisitorFormModalProps) {
  const { t } = useTranslation();
  const isEdit = Boolean(visitor);
  const [values, setValues] = useState<FormState>(() => initialState(visitor));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const createMutation = useCreateVisitor();
  const updateMutation = useUpdateVisitor();
  const isSaving = createMutation.isPending || updateMutation.isPending;

  function setField(field: keyof FormState, value: string): void {
    setValues((previous) => ({ ...previous, [field]: value }));
    setErrors((previous) => {
      if (!previous[field]) {
        return previous;
      }
      const next = { ...previous };
      delete next[field];
      return next;
    });
  }

  function validate(): Record<string, string> {
    const required = t('visitors.form.required');
    const next: Record<string, string> = {};
    if (!values.firstName.trim()) {
      next.firstName = required;
    }
    if (!values.lastName.trim()) {
      next.lastName = required;
    }
    if (!values.nationalId.trim()) {
      next.nationalId = required;
    }
    return next;
  }

  function handleServerError(error: unknown): void {
    const mapped = mapServerErrors(getFieldErrors(error), t);
    if (Object.keys(mapped).length > 0) {
      setErrors(mapped);
    } else {
      setErrors({ form: t('visitors.form.genericError') });
    }
  }

  function persist(): void {
    const body = toRequestBody(values);
    const handlers = { onSuccess: onSaved, onError: handleServerError };
    if (isEdit) {
      updateMutation.mutate({ id: visitor!.id, body }, handlers);
    } else {
      createMutation.mutate(body, handlers);
    }
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    persist();
  }

  return (
    <Modal
      title={t(isEdit ? 'visitors.form.editTitle' : 'visitors.form.createTitle')}
      onClose={onClose}
    >
      <form id="visitor-form" onSubmit={handleSubmit} noValidate>
        <Input
          label={t('visitors.form.firstName')}
          value={values.firstName}
          error={Boolean(errors.firstName)}
          hint={errors.firstName}
          onChange={(event) => setField('firstName', event.target.value)}
        />
        <Input
          label={t('visitors.form.lastName')}
          value={values.lastName}
          error={Boolean(errors.lastName)}
          hint={errors.lastName}
          onChange={(event) => setField('lastName', event.target.value)}
        />
        <Input
          label={t('visitors.form.nationalId')}
          value={values.nationalId}
          error={Boolean(errors.nationalId)}
          hint={errors.nationalId}
          onChange={(event) => setField('nationalId', event.target.value)}
        />
        <Input
          label={t('visitors.form.licensePlate')}
          value={values.licensePlate}
          onChange={(event) => setField('licensePlate', event.target.value)}
        />
        <Input
          label={t('visitors.form.company')}
          value={values.company}
          onChange={(event) => setField('company', event.target.value)}
        />
        <Input
          label={t('visitors.form.usualReason')}
          value={values.usualReason}
          onChange={(event) => setField('usualReason', event.target.value)}
        />
        {errors.form ? (
          <p className="form-error" role="alert">
            {errors.form}
          </p>
        ) : null}
        <div className="modal-footer-inline">
          <Button variant="white" onClick={onClose}>
            {t('visitors.form.cancel')}
          </Button>
          <Button variant="green" submit disabled={isSaving}>
            {t('visitors.form.save')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
