import { useTranslation } from 'react-i18next';
import { LocationDesk } from './LocationDesk';
import { LocationParking } from './LocationParking';
import { StepLocationPerDay } from './StepLocationPerDay';
import { allModeAsDayChoice } from '../../utils/wizardBooking';
import { PARKING_AUTO, RESOURCE_DESK } from './wizardTypes';
import type { DayChoice, LocationMode, ParkingChoice, WizardState } from './wizardTypes';

interface StepLocationProps {
  state: WizardState;
  dates: string[];
  patch: (partial: Partial<WizardState>) => void;
}

// Paso 4 — Ubicación. Con una sola fecha: elección única (puesto/plaza). Con varias
// fechas: segmentado "Misma para todos" (una elección para todos los días) o
// "Distinta por día" (una elección por fecha, vía StepLocationPerDay).
export function StepLocation({ state, dates, patch }: StepLocationProps) {
  const { t } = useTranslation();
  const isDesk = state.resourceType === RESOURCE_DESK;
  const multiDay = dates.length > 1;
  const perDay = multiDay && state.locationMode === 'PER_DAY';

  function setMode(mode: LocationMode): void {
    // Al pasar a "distinta por día" sin elecciones previas, siembra todos los días
    // con la elección de "misma para todos" (si la hay): empiezas igual y editas.
    if (mode === 'PER_DAY' && Object.keys(state.perDay).length === 0) {
      const seed = allModeAsDayChoice(state);
      if (seed) {
        const perDay: Record<string, DayChoice> = {};
        dates.forEach((date) => {
          perDay[date] = seed;
        });
        patch({ locationMode: mode, perDay });
        return;
      }
    }
    patch({ locationMode: mode });
  }

  return (
    <div className="rzw-step-body">
      <p className="rzw-lead">
        {isDesk ? t('wizard.location.leadDesk') : t('wizard.location.leadParking')}
      </p>

      {multiDay ? (
        <div className="rzw-segmented rzw-mode-toggle" role="tablist" aria-label={t('wizard.perday.modeLabel')}>
          <button
            type="button"
            role="tab"
            aria-selected={state.locationMode === 'ALL'}
            className={state.locationMode === 'ALL' ? 'is-active' : ''}
            onClick={() => setMode('ALL')}
          >
            <i className="ti ti-stack-2" aria-hidden="true" />
            {t('wizard.perday.modeAll')}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={state.locationMode === 'PER_DAY'}
            className={state.locationMode === 'PER_DAY' ? 'is-active' : ''}
            onClick={() => setMode('PER_DAY')}
          >
            <i className="ti ti-calendar-cog" aria-hidden="true" />
            {t('wizard.perday.modePerDay')}
          </button>
        </div>
      ) : null}

      {perDay ? (
        <StepLocationPerDay state={state} dates={dates} patch={patch} />
      ) : isDesk ? (
        <LocationDesk
          dates={dates}
          deskId={state.deskId}
          onChange={(deskId, label) => patch({ deskId, chosenLabel: label })}
        />
      ) : (
        <LocationParking
          dates={dates}
          employeeId={state.employeeId}
          choice={state.parkingChoice}
          onChange={(choice: ParkingChoice, label) =>
            patch({ parkingChoice: choice, chosenLabel: choice === PARKING_AUTO ? null : label })
          }
        />
      )}
    </div>
  );
}
