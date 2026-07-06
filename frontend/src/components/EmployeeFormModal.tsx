import { useEffect, useId, useMemo, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { DayCards } from './DayCards';
import { FieldRow } from './FieldRow';
import { FieldValue } from './FieldValue';
import { InfoBanner } from './InfoBanner';
import { Input } from './Input';
import { Modal, type ModalTab } from './Modal';
import { ResetPasswordModal } from './ResetPasswordModal';
import { Toggle } from './Toggle';
import { getFieldErrors } from '../api/apiError';
import { useDesksQuery } from '../hooks/useDesks';
import { useCreateEmployee, useUpdateEmployee } from '../hooks/useEmployees';
import {
  useEmployeeFixedAssignmentsQuery,
  useRevokeFixedAssignment,
  useSetFixedAssignments,
} from '../hooks/useFixedAssignments';
import { useParkingSpacesQuery } from '../hooks/useParkingSpaces';
import { deskLabel } from '../utils/desks';
import {
  joinWithAnd,
  toEmployeeFixedResources,
  type FixedAssignmentGroup,
} from '../utils/fixedAssignments';
import type { Employee, EmployeeCreate, EmployeeUpdate, Role } from '../types/employee';
import type { ResourceType } from '../types/request';

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

// Estado de una pestaña de recurso (plaza o puesto): recurso elegido + dias.
interface ResourceState {
  resourceId: number | '';
  days: number[];
}

interface ResourceOption {
  id: number;
  label: string;
}

type TabId = 'details' | 'parking' | 'desk';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const LOOKUP_SIZE = 100;
const EMPTY_RESOURCE: ResourceState = { resourceId: '', days: [] };

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

function resourceStateFrom(group: FixedAssignmentGroup | null): ResourceState {
  if (!group) {
    return EMPTY_RESOURCE;
  }
  return { resourceId: group.parkingSpaceId, days: [...group.days] };
}

// Intencion de una pestaña de recurso: sin datos, completa o incompleta.
function resourceIntent(state: ResourceState): 'none' | 'set' | 'invalid' {
  const hasResource = state.resourceId !== '';
  const hasDays = state.days.length > 0;
  if (hasResource && hasDays) {
    return 'set';
  }
  if (!hasResource && !hasDays) {
    return 'none';
  }
  return 'invalid';
}

// Mapea ApiError.fields (409/400) a errores inline por campo (login/email).
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

// ---- Panel DETALLES ----
interface DetailsPanelProps {
  values: FormState;
  errors: Record<string, string>;
  isEdit: boolean;
  onField: (field: keyof FormState, value: string | boolean) => void;
  onReset: () => void;
}

function DetailsPanel({ values, errors, isEdit, onField, onReset }: DetailsPanelProps) {
  const { t } = useTranslation();
  const roleId = useId();
  const corporateLabel = values.isCorporate
    ? t('employees.form.corporateOn')
    : t('employees.form.corporateOff');

  return (
    <div role="tabpanel" aria-label={t('employees.form.tabs.details')}>
      <FieldRow>
        <Input
          label={t('employees.form.firstName')}
          value={values.firstName}
          error={Boolean(errors.firstName)}
          hint={errors.firstName}
          onChange={(event) => onField('firstName', event.target.value)}
        />
        <Input
          label={t('employees.form.lastName')}
          value={values.lastName}
          error={Boolean(errors.lastName)}
          hint={errors.lastName}
          onChange={(event) => onField('lastName', event.target.value)}
        />
      </FieldRow>
      <FieldRow>
        {isEdit ? (
          <div>
            <span className="field-label">{t('employees.form.login')}</span>
            <FieldValue readOnly>{values.login}</FieldValue>
          </div>
        ) : (
          <Input
            label={t('employees.form.login')}
            value={values.login}
            error={Boolean(errors.login)}
            hint={errors.login}
            onChange={(event) => onField('login', event.target.value)}
          />
        )}
        <Input
          label={t('employees.form.email')}
          type="email"
          value={values.email}
          error={Boolean(errors.email)}
          hint={errors.email}
          onChange={(event) => onField('email', event.target.value)}
        />
      </FieldRow>
      <FieldRow>
        <Input
          label={t('employees.form.department')}
          value={values.department}
          onChange={(event) => onField('department', event.target.value)}
        />
        <Input
          label={t('employees.form.mobilePhone')}
          value={values.mobilePhone}
          onChange={(event) => onField('mobilePhone', event.target.value)}
        />
      </FieldRow>
      <FieldRow>
        <Input
          label={t('employees.form.licensePlate')}
          value={values.licensePlate}
          onChange={(event) => onField('licensePlate', event.target.value)}
        />
        <div className="auth-field">
          <label className="field-label" htmlFor={roleId}>
            {t('employees.form.role')}
          </label>
          <select
            id={roleId}
            className="field-input"
            value={values.role}
            onChange={(event) => onField('role', event.target.value as Role)}
          >
            <option value="EMPLOYEE">{t('employees.role.EMPLOYEE')}</option>
            <option value="ADMIN">{t('employees.role.ADMIN')}</option>
          </select>
        </div>
      </FieldRow>
      <div className="auth-field">
        <span className="field-label">{t('employees.form.isCorporate')}</span>
        <Toggle
          checked={values.isCorporate}
          onChange={(next) => onField('isCorporate', next)}
          label={corporateLabel}
        />
      </div>
      {isEdit ? (
        <div className="account-security">
          <i className="ti ti-shield-lock green-icon" aria-hidden="true" />
          <div className="account-security-text">
            <span className="account-security-title">{t('employees.form.security.title')}</span>
            <span className="account-security-note">{t('employees.form.security.note')}</span>
          </div>
          <Button variant="white" icon="key" onClick={onReset}>
            {t('employees.form.security.reset')}
          </Button>
        </div>
      ) : null}
    </div>
  );
}

// ---- Panel de recurso fijo (plaza o puesto), reutilizable ----
interface ResourcePanelProps {
  prefix: 'parking' | 'desk';
  fullName: string;
  options: ResourceOption[];
  state: ResourceState;
  error?: string;
  onChange: (next: ResourceState) => void;
}

function ResourcePanel({ prefix, fullName, options, state, error, onChange }: ResourcePanelProps) {
  const { t } = useTranslation();
  const selectId = useId();
  const base = `employees.form.${prefix}`;
  const resourceName = options.find((option) => option.id === state.resourceId)?.label ?? '';

  return (
    <div role="tabpanel" aria-label={t(`${base}.title`)}>
      <p className="section-title-sm">{t(`${base}.title`)}</p>
      <p className="section-hint">{t(`${base}.hint`)}</p>
      <FieldRow>
        <div className="auth-field">
          <label className="field-label" htmlFor={selectId}>
            {t(`${base}.${prefix === 'parking' ? 'space' : 'desk'}`)}
          </label>
          <select
            id={selectId}
            className="field-input"
            value={state.resourceId}
            onChange={(event) =>
              onChange({
                ...state,
                resourceId: event.target.value === '' ? '' : Number(event.target.value),
              })
            }
          >
            <option value="">{t(`${base}.select`)}</option>
            {options.map((option) => (
              <option key={option.id} value={option.id}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <span className="field-label">{t('employees.form.validity')}</span>
          <FieldValue readOnly withIcon>
            <span>
              <i className="ti ti-infinity green-icon" aria-hidden="true" />{' '}
              {t('employees.form.validityIndefinite')}
            </span>
          </FieldValue>
        </div>
      </FieldRow>
      <span className="field-label">{t(`${base}.days`)}</span>
      <DayCards value={state.days} onChange={(next) => onChange({ ...state, days: next })} />
      {error ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}
      {resourceIntent(state) === 'set' ? (
        <InfoBanner variant="green" icon="info-circle">
          {t(`${base}.summary`, {
            name: fullName,
            space: resourceName,
            desk: resourceName,
            days: joinWithAnd(
              state.days.map((day) => t(`common.weekdayName.${day}`)),
              t('common.listAnd'),
            ),
          })}
        </InfoBanner>
      ) : null}
    </div>
  );
}

export function EmployeeFormModal({ employee, onClose, onSaved }: EmployeeFormModalProps) {
  const { t } = useTranslation();
  const isEdit = Boolean(employee);
  const [activeTab, setActiveTab] = useState<TabId>('details');
  const [values, setValues] = useState<FormState>(() => initialState(employee));
  const [parking, setParking] = useState<ResourceState>(EMPTY_RESOURCE);
  const [desk, setDesk] = useState<ResourceState>(EMPTY_RESOURCE);
  const [hadParking, setHadParking] = useState(false);
  const [hadDesk, setHadDesk] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isResetOpen, setIsResetOpen] = useState(false);

  const createMutation = useCreateEmployee();
  const updateMutation = useUpdateEmployee();
  const setAssignments = useSetFixedAssignments();
  const revokeAssignment = useRevokeFixedAssignment();
  const spacesQuery = useParkingSpacesQuery({ page: 0, size: LOOKUP_SIZE, active: true });
  const desksQuery = useDesksQuery({ page: 0, size: LOOKUP_SIZE, active: true });
  const assignmentsQuery = useEmployeeFixedAssignmentsQuery(employee?.id ?? null);

  const spaceOptions = useMemo<ResourceOption[]>(
    () => (spacesQuery.data?.content ?? []).map((space) => ({ id: space.id, label: space.label })),
    [spacesQuery.data],
  );
  const deskOptions = useMemo<ResourceOption[]>(
    () =>
      (desksQuery.data?.content ?? []).map((item) => ({ id: item.id, label: deskLabel(item.number) })),
    [desksQuery.data],
  );
  const mutations = [createMutation, updateMutation, setAssignments, revokeAssignment];
  const isSaving = mutations.some((mutation) => mutation.isPending);

  // Prefill de plaza/puesto cuando llegan las asignaciones del empleado (edicion).
  const assignmentsData = assignmentsQuery.data;
  useEffect(() => {
    if (!assignmentsData) {
      return;
    }
    const resources = toEmployeeFixedResources(assignmentsData);
    setParking(resourceStateFrom(resources.parking));
    setDesk(resourceStateFrom(resources.desk));
    setHadParking(resources.parking !== null);
    setHadDesk(resources.desk !== null);
  }, [assignmentsData]);

  const fullName = `${values.firstName} ${values.lastName}`.trim();

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
    if (resourceIntent(parking) === 'invalid') {
      next.parking = t('employees.form.resourceIncomplete');
    }
    if (resourceIntent(desk) === 'invalid') {
      next.desk = t('employees.form.resourceIncomplete');
    }
    return next;
  }

  function createBody(): EmployeeCreate {
    return {
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
  }

  function updateBody(): EmployeeUpdate {
    return {
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      email: values.email.trim(),
      department: values.department.trim() || undefined,
      mobilePhone: values.mobilePhone.trim() || undefined,
      licensePlate: values.licensePlate.trim() || undefined,
      isCorporate: values.isCorporate,
      role: values.role,
    };
  }

  async function saveEmployee(): Promise<number> {
    if (isEdit && employee) {
      await updateMutation.mutateAsync({ id: employee.id, body: updateBody() });
      return employee.id;
    }
    const created = await createMutation.mutateAsync(createBody());
    return created.id;
  }

  // Guarda (PUT) o revoca (DELETE) un recurso segun su intencion y si existia.
  async function syncResource(
    employeeId: number,
    resourceType: ResourceType,
    state: ResourceState,
    had: boolean,
  ): Promise<void> {
    if (resourceIntent(state) === 'set') {
      await setAssignments.mutateAsync({
        employeeId,
        body: {
          parkingSpaceId: Number(state.resourceId),
          daysOfWeek: state.days,
          resourceType,
        },
      });
    } else if (had) {
      await revokeAssignment.mutateAsync({ employeeId, resourceType });
    }
  }

  function handleServerError(error: unknown): void {
    const fields = getFieldErrors(error);
    const mapped = mapServerErrors(fields, t);
    setErrors(Object.keys(mapped).length > 0 ? mapped : { form: t('employees.form.genericError') });
  }

  async function persist(): Promise<void> {
    try {
      const employeeId = await saveEmployee();
      await syncResource(employeeId, 'PARKING', parking, hadParking);
      await syncResource(employeeId, 'DESK', desk, hadDesk);
      onSaved();
    } catch (error) {
      handleServerError(error);
    }
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    void persist();
  }

  const tabs: ModalTab[] = [
    { id: 'details', label: t('employees.form.tabs.details') },
    { id: 'parking', label: t('employees.form.tabs.parking') },
    { id: 'desk', label: t('employees.form.tabs.desk') },
  ];

  const footer = (
    <>
      <button type="button" className="btn-back" onClick={onClose}>
        {t('employees.form.close')}
      </button>
      <Button variant="green" submit form="employee-form" icon="device-floppy" disabled={isSaving}>
        {t('employees.form.save')}
      </Button>
    </>
  );

  return (
    <Modal
      title={t(isEdit ? 'employees.form.editTitle' : 'employees.form.createTitle')}
      icon="user"
      onClose={onClose}
      tabs={tabs}
      activeTab={activeTab}
      onTabChange={(id) => setActiveTab(id as TabId)}
      footer={footer}
    >
      <form id="employee-form" onSubmit={handleSubmit} noValidate>
        {activeTab === 'details' ? (
          <DetailsPanel
            values={values}
            errors={errors}
            isEdit={isEdit}
            onField={setField}
            onReset={() => setIsResetOpen(true)}
          />
        ) : null}
        {activeTab === 'parking' ? (
          <ResourcePanel
            prefix="parking"
            fullName={fullName}
            options={spaceOptions}
            state={parking}
            error={errors.parking}
            onChange={setParking}
          />
        ) : null}
        {activeTab === 'desk' ? (
          <ResourcePanel
            prefix="desk"
            fullName={fullName}
            options={deskOptions}
            state={desk}
            error={errors.desk}
            onChange={setDesk}
          />
        ) : null}
        {errors.form ? (
          <p className="form-error" role="alert">
            {errors.form}
          </p>
        ) : null}
      </form>

      {isResetOpen && employee ? (
        <ResetPasswordModal employee={employee} onClose={() => setIsResetOpen(false)} />
      ) : null}
    </Modal>
  );
}
