import { RESOURCE_DESK, PARKING_AUTO } from '../components/wizard/wizardTypes';
import { isSpecificParking } from './wizardDates';
import type { BookingEntry } from '../hooks/useReservationBooking';
import type { DayChoice, TypeLocation } from '../components/wizard/wizardTypes';
import type { ResourceType } from '../types/request';

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

// ¿Está completo el paso de ubicación de un tipo para poder avanzar/confirmar?
//  · ALL     → una única elección válida para todos los días.
//  · PER_DAY → cada día tiene una elección válida.
export function isLocationComplete(
  location: TypeLocation,
  type: ResourceType,
  dates: string[],
): boolean {
  const isDesk = type === RESOURCE_DESK;
  if (location.locationMode === 'PER_DAY') {
    return dates.length > 0 && dates.every((date) => isDayChoiceComplete(location.perDay[date], isDesk));
  }
  return isDesk ? location.deskId !== null : location.parkingChoice !== null;
}

// Recurso concreto (resourceId) de la elección ALL, o undefined para auto-asignación.
function allModeResourceId(location: TypeLocation, type: ResourceType): number | undefined {
  if (type === RESOURCE_DESK) {
    return location.deskId ?? undefined;
  }
  return isSpecificParking(location.parkingChoice) ? location.parkingChoice : undefined;
}

// Construye las entradas de reserva (una por fecha) de un tipo según su modo. En
// PER_DAY cada fecha toma su recurso del mapa `perDay` (undefined = auto). En ALL
// todas las fechas comparten el mismo recurso (o auto).
export function resolveBookingEntries(
  location: TypeLocation,
  type: ResourceType,
  dates: string[],
): BookingEntry[] {
  if (location.locationMode === 'PER_DAY') {
    return dates.map((date) => {
      const choice = location.perDay[date];
      return { date, resourceId: choice?.resourceId ?? undefined };
    });
  }
  const resourceId = allModeResourceId(location, type);
  return dates.map((date) => ({ date, resourceId }));
}

// Elección de día equivalente a la selección ALL (para "aplicar a todos" y para
// sembrar el modo PER_DAY al activarlo).
export function allModeAsDayChoice(location: TypeLocation, type: ResourceType): DayChoice | null {
  if (type === RESOURCE_DESK) {
    return location.deskId === null
      ? null
      : { resourceId: location.deskId, label: location.chosenLabel, auto: false };
  }
  if (location.parkingChoice === PARKING_AUTO) {
    return { resourceId: null, label: null, auto: true };
  }
  if (isSpecificParking(location.parkingChoice)) {
    return { resourceId: location.parkingChoice, label: location.chosenLabel, auto: false };
  }
  return null;
}
