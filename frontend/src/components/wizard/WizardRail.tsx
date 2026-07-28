import { useTranslation } from 'react-i18next';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import { useVisitorsQuery } from '../../hooks/useVisitors';
import { longDate } from '../../utils/calendar';
import { PARKING_AUTO, RESOURCE_DESK } from './wizardTypes';
import type { StepDescriptor, TypeLocation, WizardState } from './wizardTypes';
import type { ResourceType } from '../../types/request';

interface WizardRailProps {
  steps: StepDescriptor[];
  labels: string[];
  current: number;
  reachable: number;
  state: WizardState;
  dates: string[];
  onStepClick: (index: number) => void;
}

// Riel lateral del asistente (desktop): pasos en vertical, cada uno con el valor ya
// seleccionado (resumen en vivo), y clicable para saltar a un paso ya alcanzado.
export function WizardRail({
  steps,
  labels,
  current,
  reachable,
  state,
  dates,
  onStepClick,
}: WizardRailProps) {
  const { t, i18n } = useTranslation();
  const isVisitor = state.beneficiaryType === 'VISITOR';
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const visitorsQuery = useVisitorsQuery({ page: 0, size: 100 });
  const employee = employeesQuery.data?.find((candidate) => candidate.id === state.employeeId);
  const visitor = visitorsQuery.data?.content.find((candidate) => candidate.id === state.visitorId);

  function resourceValue(): string | null {
    if (state.resourceTypes.length === 0) {
      return null;
    }
    return state.resourceTypes
      .map((type) => t(type === RESOURCE_DESK ? 'wizard.resource.desk' : 'wizard.resource.parking'))
      .join(' · ');
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
    if (isVisitor) {
      return visitor ? `${visitor.firstName} ${visitor.lastName}`.trim() : null;
    }
    if (!employee) {
      return null;
    }
    if (employee.category) {
      return `${employee.fullName} · ${t(`employees.category.${employee.category}`)}`;
    }
    return employee.fullName;
  }

  function locationValue(type: ResourceType): string | null {
    const location: TypeLocation = state.locations[type];
    if (dates.length > 1 && location.locationMode === 'PER_DAY') {
      return t('wizard.perday.modePerDay');
    }
    if (type !== RESOURCE_DESK && location.parkingChoice === PARKING_AUTO) {
      return t('wizard.location.anyFree');
    }
    return location.chosenLabel;
  }

  function valueFor(descriptor: StepDescriptor): string | null {
    switch (descriptor.kind) {
      case 'RESOURCE':
        return resourceValue();
      case 'DATES':
        return datesValue();
      case 'EMPLOYEE':
        return employeeValue();
      case 'LOCATION':
        return descriptor.type ? locationValue(descriptor.type) : null;
      default:
        return null;
    }
  }

  return (
    <nav className="rzw-rail" aria-label={t('wizard.rail.label')}>
      <p className="rzw-rail-title">{t('wizard.rail.title')}</p>
      <ol className="rzw-rail-steps">
        {steps.map((descriptor, index) => {
          const isActive = index === current;
          const isDone = index < current;
          const clickable = index !== current && index <= reachable;
          const value = valueFor(descriptor);
          const status = isActive ? ' is-active' : isDone ? ' is-done' : '';
          return (
            <li key={labels[index]}>
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
                  <span className="rzw-rail-label">{labels[index]}</span>
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
