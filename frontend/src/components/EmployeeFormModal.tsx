import { useEffect, useId, useMemo, useRef, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { DayCards } from './DayCards';
import { Dialog } from './Dialog';
import { FieldRow } from './FieldRow';
import { FieldValue } from './FieldValue';
import { InfoBanner } from './InfoBanner';
import { Input } from './Input';
import { ResetPasswordModal } from './ResetPasswordModal';
import { Toggle } from './Toggle';
import { getFieldErrors, getStatus } from '../api/apiError';
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
  daysForResource,
  groupDaysByResource,
  isDayResourceSuperset,
  joinWithAnd,
  sameDayList,
  setResourceDays,
  toEmployeeDayResourceMaps,
  type DayResourceMap,
} from '../utils/fixedAssignments';
import {
  EMPLOYEE_CATEGORIES,
  type Employee,
  type EmployeeCategory,
  type EmployeeCreate,
  type EmployeeUpdate,
  type Role,
} from '../types/employee';
import type { FixedAssignment } from '../types/fixedAssignment';
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
  category: EmployeeCategory;
  isCorporate: boolean;
}

interface ResourceOption {
  id: number;
  label: string;
}

type TabId = 'details' | 'parking' | 'desk' | 'history';
type ResourcePrefix = 'parking' | 'desk';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const LOOKUP_SIZE = 100;
const WEEKDAYS: readonly number[] = [1, 2, 3, 4, 5];
const HTTP_CONFLICT = 409;

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
    category: employee?.category ?? 'EMPLEADO',
    isCorporate: employee?.isCorporate ?? false,
  };
}

// Recurso activo por defecto al precargar: el que cubre más días (para que el caso
// común —un mismo recurso toda la semana— muestre todos sus días encendidos en las
// day-cards). Empate → id más bajo. '' si el mapa está vacío.
function dominantResource(map: DayResourceMap): number | '' {
  const groups = groupDaysByResource(map);
  let best: number | '' = '';
  let bestDays = 0;
  for (const [resourceId, days] of groups) {
    if (days.length > bestDays || (days.length === bestDays && best !== '' && resourceId < best)) {
      best = resourceId;
      bestDays = days.length;
    }
  }
  return best;
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

// ---- Tablist interno del diálogo (mantiene role="tab" que localizan los tests) ----
interface TabBarProps {
  tabs: { id: TabId; label: string }[];
  active: TabId;
  onChange: (id: TabId) => void;
  ariaLabel: string;
}

function TabBar({ tabs, active, onChange, ariaLabel }: TabBarProps) {
  return (
    <div className="modal-tabs" role="tablist" aria-label={ariaLabel}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={active === tab.id}
          className={`modal-tab${active === tab.id ? ' active' : ''}`}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
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
  const categoryId = useId();
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
            <option value="AGENCIA">{t('employees.role.AGENCIA')}</option>
          </select>
        </div>
      </FieldRow>
      <FieldRow>
        <div className="auth-field">
          <label className="field-label" htmlFor={categoryId}>
            {t('employees.form.category')}
          </label>
          <select
            id={categoryId}
            className="field-input"
            value={values.category}
            onChange={(event) => onField('category', event.target.value as EmployeeCategory)}
          >
            {EMPLOYEE_CATEGORIES.map((category) => (
              <option key={category} value={category}>
                {t(`employees.category.${category}`)}
              </option>
            ))}
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

// ---- Detalle día→recurso: qué recurso queda asignado cada día de la semana ----
interface DayResourceListProps {
  prefix: ResourcePrefix;
  map: DayResourceMap;
  options: ResourceOption[];
  onClearDay: (day: number) => void;
}

function DayResourceList({ prefix, map, options, onClearDay }: DayResourceListProps) {
  const { t } = useTranslation();
  const labelFor = (id: number): string =>
    options.find((option) => option.id === id)?.label ?? `#${id}`;

  return (
    <ul className="day-resource-list" aria-label={t(`employees.form.${prefix}.perDayTitle`)}>
      {WEEKDAYS.map((day) => {
        const resourceId = map[day];
        const assigned = resourceId !== undefined;
        return (
          <li key={day} className={`day-resource-row${assigned ? ' assigned' : ''}`}>
            <span className="day-resource-day">{t(`common.weekdayName.${day}`)}</span>
            {assigned ? (
              <>
                <span className="day-resource-value">{labelFor(resourceId)}</span>
                <button
                  type="button"
                  className="day-resource-clear"
                  aria-label={t('employees.form.clearDay', {
                    day: t(`common.weekdayName.${day}`),
                  })}
                  onClick={() => onClearDay(day)}
                >
                  <i className="ti ti-x" aria-hidden="true" />
                </button>
              </>
            ) : (
              <span className="day-resource-value muted">{t('employees.form.dayUnassigned')}</span>
            )}
          </li>
        );
      })}
    </ul>
  );
}

// ---- Panel de recurso fijo (plaza o puesto): mapa día→recurso ----
interface ResourcePanelProps {
  prefix: ResourcePrefix;
  fullName: string;
  options: ResourceOption[];
  map: DayResourceMap;
  active: number | '';
  error?: string;
  onActiveChange: (value: number | '') => void;
  onDaysChange: (days: number[]) => void;
  onApplyAll: () => void;
  onClearDay: (day: number) => void;
}

function ResourcePanel({
  prefix,
  fullName,
  options,
  map,
  active,
  error,
  onActiveChange,
  onDaysChange,
  onApplyAll,
  onClearDay,
}: ResourcePanelProps) {
  const { t } = useTranslation();
  const selectId = useId();
  const base = `employees.form.${prefix}`;
  const activeDays = active === '' ? [] : daysForResource(map, active);
  const groups = groupDaysByResource(map);
  const hasAssignments = groups.size > 0;

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
            value={active}
            onChange={(event) =>
              onActiveChange(event.target.value === '' ? '' : Number(event.target.value))
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

      <div className="day-assign-head">
        <span className="field-label">{t(`${base}.days`)}</span>
        <button
          type="button"
          className="link-btn"
          disabled={active === ''}
          onClick={onApplyAll}
        >
          <i className="ti ti-calendar-check" aria-hidden="true" /> {t('employees.form.applyAll')}
        </button>
      </div>
      <DayCards value={activeDays} onChange={onDaysChange} />
      <p className="hint">{t('employees.form.dayResourceHint')}</p>

      <p className="section-title-sm">{t(`${base}.perDayTitle`)}</p>
      <DayResourceList prefix={prefix} map={map} options={options} onClearDay={onClearDay} />

      {error ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}
      {hasAssignments ? (
        <InfoBanner variant="green" icon="info-circle">
          {groups.size > 1
            ? t('employees.form.multiResourceSummary', { name: fullName, count: groups.size })
            : t(`${base}.summary`, {
                name: fullName,
                space: options.find((o) => o.id === [...groups.keys()][0])?.label ?? '',
                desk: options.find((o) => o.id === [...groups.keys()][0])?.label ?? '',
                days: joinWithAnd(
                  [...groups.values()][0].map((day) => t(`common.weekdayName.${day}`)),
                  t('common.listAnd'),
                ),
              })}
        </InfoBanner>
      ) : null}
    </div>
  );
}

// ---- Panel HISTÓRICO: resumen de solo lectura de las asignaciones vigentes ----
interface HistoryPanelProps {
  employee: Employee;
  rows: FixedAssignment[];
  spaceOptions: ResourceOption[];
  deskOptions: ResourceOption[];
}

function HistoryResourceBlock({
  prefix,
  map,
  options,
}: {
  prefix: ResourcePrefix;
  map: DayResourceMap;
  options: ResourceOption[];
}) {
  const { t } = useTranslation();
  const groups = groupDaysByResource(map);
  const base = `employees.form.${prefix}`;
  return (
    <div className="history-block">
      <span className="field-label">{t(`${base}.title`)}</span>
      {groups.size === 0 ? (
        <p className="muted">{t('employees.form.history.none')}</p>
      ) : (
        <ul className="history-list">
          {[...groups.entries()].map(([resourceId, days]) => (
            <li key={resourceId} className="history-item">
              <span className="fixed-cell-label">
                {options.find((o) => o.id === resourceId)?.label ?? `#${resourceId}`}
              </span>
              <span className="muted">
                {joinWithAnd(
                  days.map((day) => t(`common.weekdayName.${day}`)),
                  t('common.listAnd'),
                )}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function HistoryPanel({ employee, rows, spaceOptions, deskOptions }: HistoryPanelProps) {
  const { t, i18n } = useTranslation();
  const maps = toEmployeeDayResourceMaps(rows);
  const formatDate = (iso?: string | null): string =>
    iso ? new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium' }).format(new Date(iso)) : '—';

  return (
    <div role="tabpanel" aria-label={t('employees.form.tabs.history')}>
      <p className="section-title-sm">{t('employees.form.history.title')}</p>
      <p className="section-hint">{t('employees.form.history.hint')}</p>
      <HistoryResourceBlock prefix="parking" map={maps.parking} options={spaceOptions} />
      <HistoryResourceBlock prefix="desk" map={maps.desk} options={deskOptions} />
      <dl className="release-prefill" aria-label={t('employees.form.history.account')}>
        <div className="release-prefill-row">
          <dt>{t('employees.form.history.authOrigin')}</dt>
          <dd>{employee.authOrigin}</dd>
        </div>
        <div className="release-prefill-row">
          <dt>{t('employees.form.history.createdAt')}</dt>
          <dd>{formatDate(employee.createdAt)}</dd>
        </div>
        <div className="release-prefill-row">
          <dt>{t('employees.form.history.updatedAt')}</dt>
          <dd>{formatDate(employee.updatedAt)}</dd>
        </div>
      </dl>
    </div>
  );
}

export function EmployeeFormModal({ employee, onClose, onSaved }: EmployeeFormModalProps) {
  const { t } = useTranslation();
  const isEdit = Boolean(employee);
  const [activeTab, setActiveTab] = useState<TabId>('details');
  const [values, setValues] = useState<FormState>(() => initialState(employee));
  const [parkingMap, setParkingMap] = useState<DayResourceMap>({});
  const [deskMap, setDeskMap] = useState<DayResourceMap>({});
  const [activeParking, setActiveParking] = useState<number | ''>('');
  const [activeDesk, setActiveDesk] = useState<number | ''>('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isResetOpen, setIsResetOpen] = useState(false);
  // Instantánea del prefill (estado previo por tipo) para reconciliar el guardado.
  // Se fija una sola vez y no vuelve a cambiar, así que el guardado lee el estado
  // del servidor al abrir aunque el usuario edite los mapas de trabajo.
  const [prevParkingMap, setPrevParkingMap] = useState<DayResourceMap>({});
  const [prevDeskMap, setPrevDeskMap] = useState<DayResourceMap>({});
  const prefilledRef = useRef(false);

  const createMutation = useCreateEmployee();
  const updateMutation = useUpdateEmployee();
  const setAssignments = useSetFixedAssignments();
  const revokeAssignment = useRevokeFixedAssignment();
  const spacesQuery = useParkingSpacesQuery({ page: 0, size: LOOKUP_SIZE, active: true });
  const desksQuery = useDesksQuery({ page: 0, size: LOOKUP_SIZE, active: true });
  const assignmentsQuery = useEmployeeFixedAssignmentsQuery(employee?.id ?? null);

  // Incluye los recursos ya precargados aunque estén inactivos (no vendrían en el
  // lookup de activos), para no perder su etiqueta en el selector.
  const spaceOptions = useMemo<ResourceOption[]>(() => {
    const base = (spacesQuery.data?.content ?? []).map((space) => ({
      id: space.id,
      label: space.label,
    }));
    return withPrefilledIds(base, prevParkingMap);
  }, [spacesQuery.data, prevParkingMap]);
  const deskOptions = useMemo<ResourceOption[]>(() => {
    const base = (desksQuery.data?.content ?? []).map((item) => ({
      id: item.id,
      label: deskLabel(item.number),
    }));
    return withPrefilledIds(base, prevDeskMap);
  }, [desksQuery.data, prevDeskMap]);

  const mutations = [createMutation, updateMutation, setAssignments, revokeAssignment];
  const isSaving = mutations.some((mutation) => mutation.isPending);

  // Prefill de plaza/puesto: una sola vez por empleado, para no pisar las ediciones
  // en curso si la query se invalida (p. ej. tras un guardado parcial).
  const assignmentsData = assignmentsQuery.data;
  useEffect(() => {
    if (!assignmentsData || prefilledRef.current) {
      return;
    }
    prefilledRef.current = true;
    const maps = toEmployeeDayResourceMaps(assignmentsData);
    setPrevParkingMap(maps.parking);
    setPrevDeskMap(maps.desk);
    setParkingMap(maps.parking);
    setDeskMap(maps.desk);
    setActiveParking(dominantResource(maps.parking));
    setActiveDesk(dominantResource(maps.desk));
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
      category: values.category,
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
      category: values.category,
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

  // Reconcilia un tipo de recurso (PARKING o DESK) día→recurso:
  //  - target vacío + había algo previo → DELETE por tipo (revoca todo el tipo).
  //  - si hubo reasignaciones/retiradas (target no es superset del previo) → limpia
  //    el tipo (DELETE) antes de recrear, para no chocar con el índice único al mover
  //    un día de un recurso a otro (evita 409 espurio).
  //  - un PUT por recurso con sus días; se omiten los recursos cuyos días no cambian.
  async function syncType(
    employeeId: number,
    resourceType: ResourceType,
    prev: DayResourceMap,
    target: DayResourceMap,
  ): Promise<void> {
    const prevEmpty = Object.keys(prev).length === 0;
    const targetGroups = groupDaysByResource(target);
    if (targetGroups.size === 0) {
      if (!prevEmpty) {
        await revokeAssignment.mutateAsync({ employeeId, resourceType });
      }
      return;
    }
    const cleanSlate = !prevEmpty && !isDayResourceSuperset(target, prev);
    if (cleanSlate) {
      await revokeAssignment.mutateAsync({ employeeId, resourceType });
    }
    const prevGroups = groupDaysByResource(prev);
    for (const [resourceId, days] of targetGroups) {
      if (!cleanSlate && sameDayList(prevGroups.get(resourceId), days)) {
        continue;
      }
      await setAssignments.mutateAsync({
        employeeId,
        body: { parkingSpaceId: resourceId, daysOfWeek: days, resourceType },
      });
    }
  }

  function resourceErrorMessage(error: unknown): string {
    return getStatus(error) === HTTP_CONFLICT
      ? t('employees.form.resourceConflict')
      : t('employees.form.resourceSaveError');
  }

  // Guardado de un tipo con captura de error por recurso (409 traducido). Devuelve
  // false si falló, para no cerrar el modal y señalar la pestaña afectada.
  async function syncTypeSafe(
    employeeId: number,
    resourceType: ResourceType,
    prev: DayResourceMap,
    target: DayResourceMap,
    field: 'parking' | 'desk',
  ): Promise<boolean> {
    try {
      await syncType(employeeId, resourceType, prev, target);
      return true;
    } catch (error) {
      setErrors((previous) => ({ ...previous, [field]: resourceErrorMessage(error) }));
      return false;
    }
  }

  function handleServerError(error: unknown): void {
    const fields = getFieldErrors(error);
    const mapped = mapServerErrors(fields, t);
    setErrors(Object.keys(mapped).length > 0 ? mapped : { form: t('employees.form.genericError') });
  }

  async function persist(): Promise<void> {
    let employeeId: number;
    try {
      employeeId = await saveEmployee();
    } catch (error) {
      handleServerError(error);
      return;
    }
    const parkingOk = await syncTypeSafe(
      employeeId,
      'PARKING',
      prevParkingMap,
      parkingMap,
      'parking',
    );
    const deskOk = await syncTypeSafe(employeeId, 'DESK', prevDeskMap, deskMap, 'desk');
    if (!parkingOk) {
      setActiveTab('parking');
    } else if (!deskOk) {
      setActiveTab('desk');
    } else {
      onSaved();
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

  // Fija los días del recurso activo dentro del mapa del tipo (preserva otros recursos).
  function changeDays(prefix: ResourcePrefix, days: number[]): void {
    const active = prefix === 'parking' ? activeParking : activeDesk;
    if (active === '') {
      return;
    }
    const setter = prefix === 'parking' ? setParkingMap : setDeskMap;
    setter((current) => setResourceDays(current, active, days));
  }

  function applyAll(prefix: ResourcePrefix): void {
    changeDays(prefix, [...WEEKDAYS]);
  }

  function clearDay(prefix: ResourcePrefix, day: number): void {
    const setter = prefix === 'parking' ? setParkingMap : setDeskMap;
    setter((current) => {
      if (current[day] === undefined) {
        return current;
      }
      const next = { ...current };
      delete next[day];
      return next;
    });
  }

  const tabs: { id: TabId; label: string }[] = [
    { id: 'details', label: t('employees.form.tabs.details') },
    { id: 'parking', label: t('employees.form.tabs.parking') },
    { id: 'desk', label: t('employees.form.tabs.desk') },
  ];
  if (isEdit) {
    tabs.push({ id: 'history', label: t('employees.form.tabs.history') });
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('employees.form.close')}
      </Button>
      <Button variant="green" submit form="employee-form" icon="device-floppy" disabled={isSaving}>
        {t('employees.form.save')}
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
      title={t(isEdit ? 'employees.form.editTitle' : 'employees.form.createTitle')}
      icon="user"
      tone="green"
      footer={footer}
    >
      <TabBar
        tabs={tabs}
        active={activeTab}
        onChange={setActiveTab}
        ariaLabel={t(isEdit ? 'employees.form.editTitle' : 'employees.form.createTitle')}
      />
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
            map={parkingMap}
            active={activeParking}
            error={errors.parking}
            onActiveChange={setActiveParking}
            onDaysChange={(days) => changeDays('parking', days)}
            onApplyAll={() => applyAll('parking')}
            onClearDay={(day) => clearDay('parking', day)}
          />
        ) : null}
        {activeTab === 'desk' ? (
          <ResourcePanel
            prefix="desk"
            fullName={fullName}
            options={deskOptions}
            map={deskMap}
            active={activeDesk}
            error={errors.desk}
            onActiveChange={setActiveDesk}
            onDaysChange={(days) => changeDays('desk', days)}
            onApplyAll={() => applyAll('desk')}
            onClearDay={(day) => clearDay('desk', day)}
          />
        ) : null}
        {activeTab === 'history' && employee ? (
          <HistoryPanel
            employee={employee}
            rows={assignmentsData ?? []}
            spaceOptions={spaceOptions}
            deskOptions={deskOptions}
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
    </Dialog>
  );
}

// Añade a las opciones cualquier resource_id presente en el prefill que no esté ya
// (recurso inactivo): se muestra como #id para no perder la referencia.
function withPrefilledIds(options: ResourceOption[], map: DayResourceMap): ResourceOption[] {
  const known = new Set(options.map((option) => option.id));
  const extra: ResourceOption[] = [];
  for (const id of new Set(Object.values(map))) {
    if (!known.has(id)) {
      extra.push({ id, label: `#${id}` });
    }
  }
  return extra.length > 0 ? [...options, ...extra] : options;
}
