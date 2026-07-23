import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { SelectField, type SelectOption } from './SelectField';
import { getApiError, getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useSelectableReleaseEmployeesQuery } from '../hooks/useReleaseSelection';
import {
  employeeFixedAssignmentsQueryKey,
  useSetFixedAssignments,
} from '../hooks/useFixedAssignments';
import { useAdminAssignRequest } from '../hooks/useRequests';
import { getEmployeeFixedAssignments } from '../api/fixedAssignmentsApi';
import { mergeFixedAssignmentDays } from '../utils/fixedAssignments';
import { isoWeekday, longDate } from '../utils/calendar';
import { useQueryClient } from '@tanstack/react-query';
import type { ResourceType } from '../types/request';

// Modo de asignacion inline desde una celda libre de la rejilla de Ocupacion:
// puntual (una fecha concreta) o fija (todos los <dia> de la semana, indefinido).
type AssignMode = 'PUNCTUAL' | 'FIXED';

interface OccupancyAssignModalProps {
  // resource_id generico del recurso (plaza o puesto) de la fila de la celda.
  resourceId: number;
  // Etiqueta de negocio del recurso (p. ej. "P-08" / "D-12").
  resourceLabel: string;
  resourceType: ResourceType;
  // Fecha ISO de la celda seleccionada.
  date: string;
  onClose: () => void;
  onAssigned: () => void;
}

const FORM_ID = 'occupancy-assign-form';
const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;
const NO_AVAILABILITY = 'NO_AVAILABILITY';

// Traduce el error del servidor a la clave i18n del toast. El 409 distingue el
// recurso ya ocupado de la falta de disponibilidad en auto-asignacion.
function toastKeyForError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_CONFLICT) {
    return getApiError(error)?.error === NO_AVAILABILITY
      ? 'occupancy.assign.errors.noAvailability'
      : 'occupancy.assign.errors.conflict';
  }
  if (status === HTTP_BAD_REQUEST) {
    return 'occupancy.assign.errors.badRequest';
  }
  return 'occupancy.assign.errors.generic';
}

// Modal ADMIN de asignacion inline desde una celda FREE de la rejilla de Ocupacion
// (weekly-assignment spec, "Asignar desde una celda libre"). Elige un empleado y el
// tipo de asignacion:
//   - PUNTUAL: POST /requests/admin para esa fecha concreta (nace APPROVED).
//   - FIJA: PUT /fixed-assignments/employee/{id} para ese dia de la semana.
// CRITICO (design §Risk D): la asignacion FIJA PRECARGA los dias actuales del
// empleado (GET /fixed-assignments/employee/{id}) y reenvia el CONJUNTO COMPLETO con
// el dia añadido; nunca envia solo el dia nuevo, que borraria los demas dias.
export function OccupancyAssignModal({
  resourceId,
  resourceLabel,
  resourceType,
  date,
  onClose,
  onAssigned,
}: OccupancyAssignModalProps) {
  const { t, i18n } = useTranslation();
  const queryClient = useQueryClient();
  const [employeeId, setEmployeeId] = useState<number | null>(null);
  const [mode, setMode] = useState<AssignMode>('PUNCTUAL');
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const employees = employeesQuery.data ?? [];
  const employeeOptions: SelectOption[] = employees.map((employee) => ({
    value: String(employee.id),
    label: employee.fullName,
  }));
  const assignMutation = useAdminAssignRequest();
  const setFixedMutation = useSetFixedAssignments();

  const weekday = isoWeekday(date);
  const weekdayName = new Intl.DateTimeFormat(i18n.language, { weekday: 'long' }).format(
    new Date(`${date}T00:00:00`),
  );

  function onError(mutationError: unknown): void {
    emitApiErrorToast(toastKeyForError(mutationError));
  }

  // Asignacion FIJA: PRECARGA el conjunto actual de dias del empleado para este
  // tipo de recurso y reenvia la union con el dia de la celda (design §Risk D).
  async function submitFixed(targetEmployeeId: number): Promise<void> {
    const currentRows = await queryClient.fetchQuery({
      queryKey: employeeFixedAssignmentsQueryKey(targetEmployeeId),
      queryFn: () => getEmployeeFixedAssignments(targetEmployeeId),
    });
    const daysOfWeek = mergeFixedAssignmentDays(currentRows, resourceType, resourceId, weekday);
    await setFixedMutation.mutateAsync({
      employeeId: targetEmployeeId,
      body: { parkingSpaceId: resourceId, daysOfWeek, resourceType },
    });
  }

  // Asignacion PUNTUAL: crea un Request APPROVED para la fecha concreta.
  async function submitPunctual(targetEmployeeId: number): Promise<void> {
    await assignMutation.mutateAsync({
      employeeId: targetEmployeeId,
      requestedDate: date,
      resourceType,
      resourceId,
    });
  }

  async function handleSubmit(event: FormEvent): Promise<void> {
    event.preventDefault();
    if (employeeId === null) {
      setError(t('occupancy.assign.requiredEmployee'));
      return;
    }
    setError(null);
    setPending(true);
    try {
      if (mode === 'FIXED') {
        await submitFixed(employeeId);
      } else {
        await submitPunctual(employeeId);
      }
      onAssigned();
    } catch (mutationError) {
      onError(mutationError);
    } finally {
      setPending(false);
    }
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('occupancy.assign.cancel')}
      </Button>
      <Button variant="green" submit form={FORM_ID} disabled={pending}>
        {t('occupancy.assign.submit')}
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
      title={t('occupancy.assign.title')}
      icon="user-plus"
      tone="green"
      footer={footer}
    >
      <form id={FORM_ID} onSubmit={handleSubmit} noValidate>
        <dl className="release-prefill" aria-label={t('occupancy.assign.summary')}>
          <div className="release-prefill-row">
            <dt>{t(`occupancy.assign.resourceType.${resourceType}`)}</dt>
            <dd>{resourceLabel}</dd>
          </div>
          <div className="release-prefill-row">
            <dt>{t('occupancy.assign.date')}</dt>
            <dd>{longDate(date, i18n.language)}</dd>
          </div>
        </dl>

        <SelectField
          label={t('occupancy.assign.employee')}
          value={employeeId !== null ? String(employeeId) : ''}
          onValueChange={(value) => setEmployeeId(value === '' ? null : Number(value))}
          options={employeeOptions}
          placeholder={t('occupancy.assign.selectEmployee')}
        />

        <span className="field-label">{t('occupancy.assign.modeLabel')}</span>
        <div className="segmented" role="group" aria-label={t('occupancy.assign.modeLabel')}>
          <button
            type="button"
            className={mode === 'PUNCTUAL' ? 'active' : ''}
            aria-pressed={mode === 'PUNCTUAL'}
            onClick={() => setMode('PUNCTUAL')}
          >
            {t('occupancy.assign.mode.punctual')}
          </button>
          <button
            type="button"
            className={mode === 'FIXED' ? 'active' : ''}
            aria-pressed={mode === 'FIXED'}
            onClick={() => setMode('FIXED')}
          >
            {t('occupancy.assign.mode.fixed')}
          </button>
        </div>
        <p className="hint">
          {mode === 'FIXED'
            ? t('occupancy.assign.fixedHint', { weekday: weekdayName })
            : t('occupancy.assign.punctualHint')}
        </p>

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}
      </form>
    </Dialog>
  );
}
