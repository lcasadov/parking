import { useTranslation } from 'react-i18next';
import { LocationDesk } from './LocationDesk';
import { LocationParking } from './LocationParking';
import { StepLocationPerDay } from './StepLocationPerDay';
import { allModeAsDayChoice } from '../../utils/wizardBooking';
import { PARKING_AUTO, RESOURCE_DESK } from './wizardTypes';
import type {
  BeneficiaryType,
  DayChoice,
  LocationMode,
  ParkingChoice,
  TypeLocation,
} from './wizardTypes';
import type { ResourceType } from '../../types/request';

interface StepLocationProps {
  // Tipo de recurso de este paso de ubicación (Plaza o Puesto).
  type: ResourceType;
  // Slice de ubicación de ese tipo.
  location: TypeLocation;
  dates: string[];
  beneficiaryType: BeneficiaryType;
  employeeId: number | null;
  // Aplica cambios al slice de ESTE tipo.
  patchLocation: (partial: Partial<TypeLocation>) => void;
}

// Paso de Ubicación (uno por tipo de recurso seleccionado). Con una sola fecha:
// elección única (puesto/plaza). Con varias fechas: segmentado "Misma para todos" o
// "Distinta por día" (una elección por fecha, vía StepLocationPerDay).
export function StepLocation({
  type,
  location,
  dates,
  beneficiaryType,
  employeeId,
  patchLocation,
}: StepLocationProps) {
  const { t } = useTranslation();
  const isDesk = type === RESOURCE_DESK;
  const multiDay = dates.length > 1;
  const perDay = multiDay && location.locationMode === 'PER_DAY';

  function setMode(mode: LocationMode): void {
    // Al pasar a "distinta por día" sin elecciones previas, siembra todos los días
    // con la elección de "misma para todos" (si la hay): empiezas igual y editas.
    if (mode === 'PER_DAY' && Object.keys(location.perDay).length === 0) {
      const seed = allModeAsDayChoice(location, type);
      if (seed) {
        const seeded: Record<string, DayChoice> = {};
        dates.forEach((date) => {
          seeded[date] = seed;
        });
        patchLocation({ locationMode: mode, perDay: seeded });
        return;
      }
    }
    patchLocation({ locationMode: mode });
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
            aria-selected={location.locationMode === 'ALL'}
            className={location.locationMode === 'ALL' ? 'is-active' : ''}
            onClick={() => setMode('ALL')}
          >
            <i className="ti ti-stack-2" aria-hidden="true" />
            {t('wizard.perday.modeAll')}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={location.locationMode === 'PER_DAY'}
            className={location.locationMode === 'PER_DAY' ? 'is-active' : ''}
            onClick={() => setMode('PER_DAY')}
          >
            <i className="ti ti-calendar-cog" aria-hidden="true" />
            {t('wizard.perday.modePerDay')}
          </button>
        </div>
      ) : null}

      {perDay ? (
        <StepLocationPerDay
          type={type}
          location={location}
          dates={dates}
          beneficiaryType={beneficiaryType}
          employeeId={employeeId}
          patchLocation={patchLocation}
        />
      ) : isDesk ? (
        <LocationDesk
          dates={dates}
          deskId={location.deskId}
          onChange={(deskId, label) => patchLocation({ deskId, chosenLabel: label })}
        />
      ) : (
        <LocationParking
          dates={dates}
          employeeId={employeeId}
          allowAuto={beneficiaryType === 'EMPLOYEE'}
          choice={location.parkingChoice}
          onChange={(choice: ParkingChoice, label) =>
            patchLocation({ parkingChoice: choice, chosenLabel: choice === PARKING_AUTO ? null : label })
          }
        />
      )}
    </div>
  );
}
