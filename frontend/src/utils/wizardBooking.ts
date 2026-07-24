import { RESOURCE_DESK, PARKING_AUTO } from '../components/wizard/wizardTypes';
import { isSpecificParking } from './wizardDates';
import type { BookingEntry } from '../hooks/useReservationBooking';
import type { DayChoice, WizardState } from '../components/wizard/wizardTypes';

// ¿La elección de UN día está completa para el tipo de recurso?
//  · DESK    → exige un puesto concreto.
//  · PARKING → un recurso concreto o auto-asignación.
function isDayChoiceComplete(choice: DayChoice | undefined, isDesk: boolean): boolean {
  if (!choice) {
    return false;
  }
  if (isDesk) {
    return choice.resourceId !== null;
  }
  return choice.resourceId !== null || choice.auto;
}

// ¿Está completo el paso de ubicación para poder avanzar/confirmar?
//  · ALL     → una única elección válida para todos los días.
//  · PER_DAY → cada día tiene una elección válida.
export function isLocationComplete(state: WizardState, dates: string[]): boolean {
  const isDesk = state.resourceType === RESOURCE_DESK;
  if (state.locationMode === 'PER_DAY') {
    return dates.length > 0 && dates.every((date) => isDayChoiceComplete(state.perDay[date], isDesk));
  }
  return isDesk ? state.deskId !== null : state.parkingChoice !== null;
}

// Recurso concreto (resourceId) de la elección ALL, o undefined para auto-asignación.
function allModeResourceId(state: WizardState): number | undefined {
  if (state.resourceType === RESOURCE_DESK) {
    return state.deskId ?? undefined;
  }
  return isSpecificParking(state.parkingChoice) ? state.parkingChoice : undefined;
}

// Construye las entradas de reserva (una por fecha) según el modo. En PER_DAY cada
// fecha toma su recurso del mapa `perDay` (undefined = auto). En ALL todas las
// fechas comparten el mismo recurso (o auto).
export function resolveBookingEntries(state: WizardState, dates: string[]): BookingEntry[] {
  if (state.locationMode === 'PER_DAY') {
    return dates.map((date) => {
      const choice = state.perDay[date];
      return { date, resourceId: choice?.resourceId ?? undefined };
    });
  }
  const resourceId = allModeResourceId(state);
  return dates.map((date) => ({ date, resourceId }));
}

// Elección de día equivalente a la selección ALL (para "aplicar a todos" y para
// sembrar el modo PER_DAY al activarlo).
export function allModeAsDayChoice(state: WizardState): DayChoice | null {
  if (state.resourceType === RESOURCE_DESK) {
    return state.deskId === null ? null : { resourceId: state.deskId, label: state.chosenLabel, auto: false };
  }
  if (state.parkingChoice === PARKING_AUTO) {
    return { resourceId: null, label: null, auto: true };
  }
  if (isSpecificParking(state.parkingChoice)) {
    return { resourceId: state.parkingChoice, label: state.chosenLabel, auto: false };
  }
  return null;
}
