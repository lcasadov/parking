import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useSetFixedAssignments } from '../hooks/useFixedAssignments';
import { deskLabel } from '../utils/desks';
import { toggleDay, WEEK_DAYS } from '../utils/fixedAssignments';
import type { Desk } from '../types/desk';
import type { Employee } from '../types/employee';
import type { ParkingSpace } from '../types/parkingSpace';
import type { ResourceType } from '../types/request';
import type { FixedAssignmentGroup } from '../utils/fixedAssignments';

interface FixedAssignmentModalProps {
  employees: Employee[];
  spaces: ParkingSpace[];
  desks: Desk[];
  initial?: FixedAssignmentGroup | null;
  onClose: () => void;
  onSaved: () => void;
}

const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;

// Traduce el error del servidor (400 dias invalidos / 409 conflicto de unicidad)
// a la clave i18n del toast correspondiente (tasks §4.5).
function toastKeyForError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_CONFLICT) {
    return 'fixedAssignments.errors.conflict';
  }
  if (status === HTTP_BAD_REQUEST) {
    return 'fixedAssignments.errors.invalidDays';
  }
  return 'fixedAssignments.errors.generic';
}

// Opciones { value, label } del selector de recurso segun el tipo elegido.
function resourceOptions(
  resourceType: ResourceType,
  spaces: ParkingSpace[],
  desks: Desk[],
): Array<{ id: number; label: string }> {
  if (resourceType === 'DESK') {
    return desks.map((desk) => ({ id: desk.id, label: deskLabel(desk.number) }));
  }
  return spaces.map((space) => ({ id: space.id, label: space.label }));
}

// Modal ADMIN: asigna/modifica la asignacion fija de un empleado (recurso + dias).
// El selector de recurso (Plaza/Puesto) cambia la lista de recursos y envia el
// `resourceType` en PUT /fixed-assignments/employee/{id}. Un empleado puede tener
// plaza fija y puesto fijo (recursos independientes). En edicion el tipo es fijo.
export function FixedAssignmentModal({
  employees,
  spaces,
  desks,
  initial,
  onClose,
  onSaved,
}: FixedAssignmentModalProps) {
  const { t } = useTranslation();
  const isEdit = Boolean(initial);
  const [employeeId, setEmployeeId] = useState<number | ''>(initial?.employeeId ?? '');
  const [resourceType, setResourceType] = useState<ResourceType>(
    initial?.resourceType ?? 'PARKING',
  );
  const [resourceId, setResourceId] = useState<number | ''>(initial?.parkingSpaceId ?? '');
  const [days, setDays] = useState<number[]>(initial?.days ?? []);
  const [error, setError] = useState<string | null>(null);
  const setMutation = useSetFixedAssignments();

  const isDesk = resourceType === 'DESK';
  const options = resourceOptions(resourceType, spaces, desks);

  function changeResourceType(next: ResourceType): void {
    setResourceType(next);
    setResourceId('');
  }

  function validate(): string | null {
    if (employeeId === '') {
      return t('fixedAssignments.form.requiredEmployee');
    }
    if (resourceId === '') {
      return t(isDesk ? 'fixedAssignments.form.requiredDesk' : 'fixedAssignments.form.requiredSpace');
    }
    if (days.length === 0) {
      return t('fixedAssignments.form.requiredDays');
    }
    return null;
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    setMutation.mutate(
      {
        employeeId: Number(employeeId),
        body: { parkingSpaceId: Number(resourceId), daysOfWeek: days, resourceType },
      },
      {
        onSuccess: onSaved,
        onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
      },
    );
  }

  return (
    <Modal
      title={t(isEdit ? 'fixedAssignments.form.editTitle' : 'fixedAssignments.form.createTitle')}
      onClose={onClose}
    >
      <form id="fixed-assignment-form" onSubmit={handleSubmit} noValidate>
        <label className="field-label" htmlFor="fixed-assignment-employee">
          {t('fixedAssignments.form.employee')}
        </label>
        <select
          id="fixed-assignment-employee"
          className="field-input"
          value={employeeId}
          disabled={isEdit}
          onChange={(event) =>
            setEmployeeId(event.target.value === '' ? '' : Number(event.target.value))
          }
        >
          <option value="">{t('fixedAssignments.form.selectEmployee')}</option>
          {employees.map((employee) => (
            <option key={employee.id} value={employee.id}>
              {`${employee.firstName} ${employee.lastName}`}
            </option>
          ))}
        </select>

        <label className="field-label" htmlFor="fixed-assignment-resource-type">
          {t('fixedAssignments.form.resource')}
        </label>
        <select
          id="fixed-assignment-resource-type"
          className="field-input"
          value={resourceType}
          disabled={isEdit}
          onChange={(event) => changeResourceType(event.target.value as ResourceType)}
        >
          <option value="PARKING">{t('fixedAssignments.form.resourceParking')}</option>
          <option value="DESK">{t('fixedAssignments.form.resourceDesk')}</option>
        </select>

        <label className="field-label" htmlFor="fixed-assignment-space">
          {t(isDesk ? 'fixedAssignments.form.desk' : 'fixedAssignments.form.space')}
        </label>
        <select
          id="fixed-assignment-space"
          className="field-input"
          value={resourceId}
          onChange={(event) =>
            setResourceId(event.target.value === '' ? '' : Number(event.target.value))
          }
        >
          <option value="">
            {t(isDesk ? 'fixedAssignments.form.selectDesk' : 'fixedAssignments.form.selectSpace')}
          </option>
          {options.map((option) => (
            <option key={option.id} value={option.id}>
              {option.label}
            </option>
          ))}
        </select>

        <fieldset className="weekday-group">
          <legend className="field-label">{t('fixedAssignments.form.days')}</legend>
          {WEEK_DAYS.map((day) => (
            <label key={day} className="checkbox-field">
              <input
                type="checkbox"
                checked={days.includes(day)}
                onChange={() => setDays((previous) => toggleDay(previous, day))}
              />
              {t(`fixedAssignments.weekdays.${day}`)}
            </label>
          ))}
        </fieldset>

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}

        <div className="modal-footer-inline">
          <Button variant="white" onClick={onClose}>
            {t('fixedAssignments.form.cancel')}
          </Button>
          <Button variant="green" submit disabled={setMutation.isPending}>
            {t('fixedAssignments.form.save')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
