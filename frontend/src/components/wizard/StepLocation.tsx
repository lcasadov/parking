import { useTranslation } from 'react-i18next';
import { LocationDesk } from './LocationDesk';
import { LocationParking } from './LocationParking';
import { PARKING_AUTO, RESOURCE_DESK } from './wizardTypes';
import type { ParkingChoice, WizardState } from './wizardTypes';

interface StepLocationProps {
  state: WizardState;
  dates: string[];
  patch: (partial: Partial<WizardState>) => void;
}

// Paso 4 — Ubicación: delega en la variante de puesto (plano) o de plaza (grid +
// "cualquier plaza libre") según el tipo de recurso elegido en el paso 1.
export function StepLocation({ state, dates, patch }: StepLocationProps) {
  const { t } = useTranslation();
  const isDesk = state.resourceType === RESOURCE_DESK;

  return (
    <div className="rzw-step-body">
      <p className="rzw-lead">
        {isDesk ? t('wizard.location.leadDesk') : t('wizard.location.leadParking')}
      </p>
      {isDesk ? (
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
