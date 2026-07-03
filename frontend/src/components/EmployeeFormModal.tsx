import { useId, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Input } from './Input';
import { Modal } from './Modal';
import { getFieldErrors } from '../api/apiError';
import { useCreateEmployee, useUpdateEmployee } from '../hooks/useEmployees';
import type { Employee, EmployeeCreate, EmployeeUpdate, Role } from '../types/employee';

interface EmployeeFormModalProps {
  employee?: Employee | null;
  onClose: () => void;
  onSaved: () => void;
}

interface FormState {
  firstName: string;
  lastName: string;
  login: string;
  email: string;
  department: string;
  mobilePhone: string;
  licensePlate: string;
  role: Role;
  isCorporate: boolean;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function initialState(employee?: Employee | null): FormState {
  return {
    firstName: employee?.firstName ?? '',
    lastName: employee?.lastName ?? '',
    login: employee?.login ?? '',
    email: employee?.email ?? '',
    department: employee?.department ?? '',
    mobilePhone: employee?.mobilePhone ?? '',
    licensePlate: employee?.licensePlate ?? '',
    role: employee?.role ?? 'EMPLOYEE',
    isCorporate: employee?.isCorporate ?? false,
  };
}

// Mapea ApiError.fields (409/400) a errores inline por campo, traduciendo los
// conflictos conocidos de login/email.
function mapServerErrors(
  fields: Record<string, string>,
  translate: (key: string) => string,
): Record<string, string> {
  const mapped: Record<string, string> = {};
  Object.keys(fields).forEach((field) => {
    if (field === 'login') {
      mapped.login = translate('employees.form.duplicateLogin');
    } else if (field === 'email') {
      mapped.email = translate('employees.form.duplicateEmail');
    } else {
      mapped[field] = fields[field];
    }
  });
  return mapped;
}

export function EmployeeFormModal({ employee, onClose, onSaved }: EmployeeFormModalProps) {
  const { t } = useTranslation();
  const isEdit = Boolean(employee);
  const [values, setValues] = useState<FormState>(() => initialState(employee));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const roleId = useId();
  const createMutation = useCreateEmployee();
  const updateMutation = useUpdateEmployee();
  const isSaving = createMutation.isPending || updateMutation.isPending;

  function setField(field: keyof FormState, value: string | boolean): void {
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
    const required = t('employees.form.required');
    const next: Record<string, string> = {};
    if (!values.firstName.trim()) {
      next.firstName = required;
    }
    if (!values.lastName.trim()) {
      next.lastName = required;
    }
    if (!isEdit && !values.login.trim()) {
      next.login = required;
    }
    if (!values.email.trim()) {
      next.email = required;
    } else if (!EMAIL_RE.test(values.email)) {
      next.email = t('employees.form.invalidEmail');
    }
    return next;
  }

  function handleServerError(error: unknown): void {
    const fields = getFieldErrors(error);
    const mapped = mapServerErrors(fields, t);
    if (Object.keys(mapped).length > 0) {
      setErrors(mapped);
    } else {
      setErrors({ form: t('employees.form.genericError') });
    }
  }

  function submitCreate(): void {
    const body: EmployeeCreate = {
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      login: values.login.trim(),
      email: values.email.trim(),
      department: values.department.trim() || undefined,
      mobilePhone: values.mobilePhone.trim() || undefined,
      licensePlate: values.licensePlate.trim() || undefined,
      isCorporate: values.isCorporate,
      role: values.role,
    };
    createMutation.mutate(body, { onSuccess: onSaved, onError: handleServerError });
  }

  function submitUpdate(): void {
    const body: EmployeeUpdate = {
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      email: values.email.trim(),
      department: values.department.trim() || undefined,
      mobilePhone: values.mobilePhone.trim() || undefined,
      licensePlate: values.licensePlate.trim() || undefined,
      isCorporate: values.isCorporate,
      role: values.role,
    };
    updateMutation.mutate(
      { id: employee!.id, body },
      { onSuccess: onSaved, onError: handleServerError },
    );
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    if (isEdit) {
      submitUpdate();
    } else {
      submitCreate();
    }
  }

  return (
    <Modal
      title={t(isEdit ? 'employees.form.editTitle' : 'employees.form.createTitle')}
      onClose={onClose}
    >
      <form id="employee-form" onSubmit={handleSubmit} noValidate>
        <Input
          label={t('employees.form.firstName')}
          value={values.firstName}
          error={Boolean(errors.firstName)}
          hint={errors.firstName}
          onChange={(event) => setField('firstName', event.target.value)}
        />
        <Input
          label={t('employees.form.lastName')}
          value={values.lastName}
          error={Boolean(errors.lastName)}
          hint={errors.lastName}
          onChange={(event) => setField('lastName', event.target.value)}
        />
        {!isEdit ? (
          <Input
            label={t('employees.form.login')}
            value={values.login}
            error={Boolean(errors.login)}
            hint={errors.login}
            onChange={(event) => setField('login', event.target.value)}
          />
        ) : null}
        <Input
          label={t('employees.form.email')}
          type="email"
          value={values.email}
          error={Boolean(errors.email)}
          hint={errors.email}
          onChange={(event) => setField('email', event.target.value)}
        />
        <Input
          label={t('employees.form.department')}
          value={values.department}
          onChange={(event) => setField('department', event.target.value)}
        />
        <Input
          label={t('employees.form.mobilePhone')}
          value={values.mobilePhone}
          onChange={(event) => setField('mobilePhone', event.target.value)}
        />
        <Input
          label={t('employees.form.licensePlate')}
          value={values.licensePlate}
          onChange={(event) => setField('licensePlate', event.target.value)}
        />
        <div className="auth-field">
          <label className="field-label" htmlFor={roleId}>
            {t('employees.form.role')}
          </label>
          <select
            id={roleId}
            className="field-input"
            value={values.role}
            onChange={(event) => setField('role', event.target.value as Role)}
          >
            <option value="EMPLOYEE">{t('employees.role.EMPLOYEE')}</option>
            <option value="ADMIN">{t('employees.role.ADMIN')}</option>
          </select>
        </div>
        <label className="checkbox-field">
          <input
            type="checkbox"
            checked={values.isCorporate}
            onChange={(event) => setField('isCorporate', event.target.checked)}
          />
          {t('employees.form.isCorporate')}
        </label>
        {errors.form ? (
          <p className="form-error" role="alert">
            {errors.form}
          </p>
        ) : null}
        <div className="modal-footer-inline">
          <Button variant="white" onClick={onClose}>
            {t('employees.form.cancel')}
          </Button>
          <Button variant="green" submit disabled={isSaving}>
            {t('employees.form.save')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
