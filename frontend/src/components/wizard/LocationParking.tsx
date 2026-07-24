import { useTranslation } from 'react-i18next';
import { Spinner } from '../Spinner';
import { useWizardAvailability } from '../../hooks/useWizardAvailability';
import { PARKING_AUTO, RESOURCE_PARKING } from './wizardTypes';
import type { ParkingChoice } from './wizardTypes';

interface LocationParkingProps {
  dates: string[];
  choice: ParkingChoice | null;
  onChange: (choice: ParkingChoice, label: string) => void;
}

// Ubicación PARKING: elegir una plaza concreta (libre en TODAS las fechas) o
// "cualquier plaza libre" (auto-asignación por fecha, omitiendo resourceId).
export function LocationParking({ dates, choice, onChange }: LocationParkingProps) {
  const { t } = useTranslation();
  const availability = useWizardAvailability(dates, RESOURCE_PARKING, true);

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

  return (
    <div className="rzw-loc">
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

      <span className="rzw-eyebrow">
        {t('wizard.location.eligibleCount', { count: availability.eligible.length })}
      </span>
      {availability.eligible.length === 0 ? (
        <p className="rzw-empty">{t('wizard.location.noneParking')}</p>
      ) : (
        <ul className="rzw-res-grid">
          {availability.eligible.map((resource) => {
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
                  <span className="mono">{resource.label}</span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
