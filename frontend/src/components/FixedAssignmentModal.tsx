import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useSetFixedAssignments } from '../hooks/useFixedAssignments';
import { toggleDay, WEEK_DAYS } from '../utils/fixedAssignments';
import type { Employee } from '../types/employee';
import type { ParkingSpace } from '../types/parkingSpace';
import type { FixedAssignmentGroup } from '../utils/fixedAssignments';

interface FixedAssignmentModalProps {
  employees: Employee[];
  spaces: ParkingSpace[];
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

// Modal ADMIN: asigna/modifica la asignacion fija de un empleado (plaza + dias).
// Guarda via PUT /fixed-assignments/employee/{id} (reemplaza el conjunto).
export function FixedAssignmentModal({
  employees,
  spaces,
  initial,
  onClose,
  onSaved,
}: FixedAssignmentModalProps) {
  const { t } = useTranslation();
  const isEdit = Boolean(initial);
  const [employeeId, setEmployeeId] = useState<number | ''>(initial?.employeeId ?? '');
  const [spaceId, setSpaceId] = useState<number | ''>(initial?.parkingSpaceId ?? '');
  const [days, setDays] = useState<number[]>(initial?.days ?? []);
  const [error, setError] = useState<string | null>(null);
  const setMutation = useSetFixedAssignments();

  function validate(): string | null {
    if (employeeId === '') {
      return t('fixedAssignments.form.requiredEmployee');
    }
    if (spaceId === '') {
      return t('fixedAssignments.form.requiredSpace');
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
        body: { parkingSpaceId: Number(spaceId), daysOfWeek: days },
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

        <label className="field-label" htmlFor="fixed-assignment-space">
          {t('fixedAssignments.form.space')}
        </label>
        <select
          id="fixed-assignment-space"
          className="field-input"
          value={spaceId}
          onChange={(event) =>
            setSpaceId(event.target.value === '' ? '' : Number(event.target.value))
          }
        >
          <option value="">{t('fixedAssignments.form.selectSpace')}</option>
          {spaces.map((space) => (
            <option key={space.id} value={space.id}>
              {space.label}
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
