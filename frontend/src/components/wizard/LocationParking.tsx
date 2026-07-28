import { useTranslation } from 'react-i18next';
import { Spinner } from '../Spinner';
import { useWizardAvailability } from '../../hooks/useWizardAvailability';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import { groupEligibleByPriority, type RankedResource } from '../../utils/parkingPriority';
import { PARKING_AUTO, RESOURCE_PARKING } from './wizardTypes';
import type { ParkingChoice } from './wizardTypes';

interface LocationParkingProps {
  dates: string[];
  employeeId: number | null;
  // Auto-asignación por categoría: solo para empleados. Los visitantes no tienen
  // categoría, así que eligen SIEMPRE una plaza concreta (sin tarjeta de auto).
  allowAuto?: boolean;
  choice: ParkingChoice | null;
  onChange: (choice: ParkingChoice, label: string) => void;
}

// Ubicación PARKING: elegir una plaza concreta (libre en TODAS las fechas) o, para
// empleados, "asignación automática" por categoría. Las plazas manuales se ordenan
// por la prioridad de la categoría y se dividen en "sugeridas" y "otras".
export function LocationParking({ dates, employeeId, allowAuto = true, choice, onChange }: LocationParkingProps) {
  const { t } = useTranslation();
  const availability = useWizardAvailability(dates, RESOURCE_PARKING, true);
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const category = employeesQuery.data?.find((candidate) => candidate.id === employeeId)?.category;

  if (availability.isLoading) {
    return (
      <div className="rzw-center">
        <Spinner />
        <p className="rzw-lead">{t('wizard.location.checking')}</p>
      </div>
    );
  }

  if (availability.isError) {
    return (
      <p className="form-error" role="alert">
        {t('wizard.location.error')}
      </p>
    );
  }

  const autoSelected = choice === PARKING_AUTO;
  const { suggested, others, preferredFloor } = groupEligibleByPriority(
    availability.eligible,
    category,
  );

  // Rejilla de chips de plaza reutilizada por cada apartado.
  function renderGrid(resources: RankedResource[]) {
    return (
      <ul className="rzw-res-grid">
        {resources.map((resource) => {
          const selected = choice === resource.resourceId;
          return (
            <li key={resource.resourceId}>
              <button
                type="button"
                className={`rzw-res-chip${selected ? ' is-selected' : ''}`}
                aria-pressed={selected}
                onClick={() => onChange(resource.resourceId, resource.label)}
              >
                <i className="ti ti-car" aria-hidden="true" />
                <span className="rzw-res-num">{resource.label}</span>
              </button>
            </li>
          );
        })}
      </ul>
    );
  }

  return (
    <div className="rzw-loc">
      {allowAuto ? (
        <>
          <button
            type="button"
            className={`rzw-auto-card${autoSelected ? ' is-selected' : ''}`}
            aria-pressed={autoSelected}
            disabled={!availability.anyFreeSomeDate}
            onClick={() => onChange(PARKING_AUTO, '')}
          >
            <span className="rzw-auto-icon" aria-hidden="true">
              <i className="ti ti-wand" />
            </span>
            <span className="rzw-auto-text">
              <span className="rzw-auto-title">{t('wizard.location.anyFree')}</span>
              <span className="rzw-auto-desc">{t('wizard.location.anyFreeDesc')}</span>
            </span>
            <span className="rzw-choice-check" aria-hidden="true">
              <i className="ti ti-check" />
            </span>
          </button>

          <div className="rzw-loc-divider">
            <span>{t('wizard.location.orPick')}</span>
          </div>
        </>
      ) : null}

      {availability.eligible.length === 0 ? (
        <p className="rzw-empty">{t('wizard.location.noneParking')}</p>
      ) : (
        <div className="rzw-loc-groups">
          {suggested.length > 0 && preferredFloor !== null ? (
            <section className="rzw-loc-group">
              <span className="rzw-eyebrow rzw-eyebrow-accent">
                <i className="ti ti-sparkles" aria-hidden="true" />
                {t('wizard.location.suggestedForCategory', {
                  category: category ? t(`employees.category.${category}`) : '',
                  floor: -preferredFloor,
                })}
              </span>
              {renderGrid(suggested)}
            </section>
          ) : null}

          {others.length > 0 ? (
            <section className="rzw-loc-group">
              <span className="rzw-eyebrow">
                {suggested.length > 0
                  ? t('wizard.location.otherSpaces')
                  : t('wizard.location.eligibleCount', { count: others.length })}
              </span>
              {renderGrid(others)}
            </section>
          ) : null}
        </div>
      )}
    </div>
  );
}
