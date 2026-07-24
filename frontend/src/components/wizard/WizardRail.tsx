import { useTranslation } from 'react-i18next';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import { longDate } from '../../utils/calendar';
import {
  PARKING_AUTO,
  RESOURCE_DESK,
  STEP_DATES,
  STEP_EMPLOYEE,
  STEP_LOCATION,
  STEP_RESOURCE,
} from './wizardTypes';
import type { WizardState } from './wizardTypes';

interface WizardRailProps {
  steps: string[];
  current: number;
  reachable: number;
  state: WizardState;
  dates: string[];
  onStepClick: (index: number) => void;
}

// Riel lateral del asistente (desktop): pasos en vertical, cada uno con el valor ya
// seleccionado (resumen en vivo), y clicable para saltar a un paso ya alcanzado.
// Sustituye en ancho al stepper horizontal (que queda para móvil) y aprovecha el
// espacio sobrante del modal a pantalla completa.
export function WizardRail({ steps, current, reachable, state, dates, onStepClick }: WizardRailProps) {
  const { t, i18n } = useTranslation();
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const employee = employeesQuery.data?.find((candidate) => candidate.id === state.employeeId);
  const isDesk = state.resourceType === RESOURCE_DESK;

  function resourceValue(): string | null {
    if (state.resourceType === null) {
      return null;
    }
    return t(isDesk ? 'wizard.resource.desk' : 'wizard.resource.parking');
  }

  function datesValue(): string | null {
    if (dates.length === 0) {
      return null;
    }
    if (dates.length === 1) {
      return longDate(dates[0], i18n.language);
    }
    return t('wizard.summary.dates', { count: dates.length });
  }

  function employeeValue(): string | null {
    if (!employee) {
      return null;
    }
    if (employee.category) {
      return `${employee.fullName} · ${t(`employees.category.${employee.category}`)}`;
    }
    return employee.fullName;
  }

  function locationValue(): string | null {
    if (dates.length > 1 && state.locationMode === 'PER_DAY') {
      return t('wizard.perday.modePerDay');
    }
    if (!isDesk && state.parkingChoice === PARKING_AUTO) {
      return t('wizard.location.anyFree');
    }
    return state.chosenLabel;
  }

  const valueByStep: Record<number, string | null> = {
    [STEP_RESOURCE]: resourceValue(),
    [STEP_DATES]: datesValue(),
    [STEP_EMPLOYEE]: employeeValue(),
    [STEP_LOCATION]: locationValue(),
  };

  return (
    <nav className="rzw-rail" aria-label={t('wizard.rail.label')}>
      <p className="rzw-rail-title">{t('wizard.rail.title')}</p>
      <ol className="rzw-rail-steps">
        {steps.map((label, index) => {
          const isActive = index === current;
          const isDone = index < current;
          const clickable = index !== current && index <= reachable;
          const value = valueByStep[index] ?? null;
          const status = isActive ? ' is-active' : isDone ? ' is-done' : '';
          return (
            <li key={label}>
              <button
                type="button"
                className={`rzw-rail-step${status}${clickable ? ' is-clickable' : ''}`}
                aria-current={isActive ? 'step' : undefined}
                disabled={!clickable}
                onClick={() => (clickable ? onStepClick(index) : undefined)}
              >
                <span className="rzw-rail-mark" aria-hidden="true">
                  {isDone ? <i className="ti ti-check" /> : index + 1}
                </span>
                <span className="rzw-rail-text">
                  <span className="rzw-rail-label">{label}</span>
                  {value ? <span className="rzw-rail-value">{value}</span> : null}
                </span>
              </button>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
