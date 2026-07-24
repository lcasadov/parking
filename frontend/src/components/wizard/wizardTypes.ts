import type { ResourceType } from '../../types/request';

// Modo de selección de fechas del asistente de reserva:
//  · SINGLE  — un día concreto.
//  · RANGE   — un intervalo continuo (desde–hasta), ambos incluidos.
//  · SCATTER — días sueltos, multiselección de fechas dispersas.
export type DateMode = 'SINGLE' | 'RANGE' | 'SCATTER';

// Opción de plaza de parking: una plaza concreta (id) o auto-asignación (la
// primera libre por fecha, omitiendo resourceId en el POST /requests/admin).
export const PARKING_AUTO = 'AUTO' as const;
export type ParkingChoice = number | typeof PARKING_AUTO;

// Modo de asignación de ubicación cuando hay varias fechas:
//  · ALL     — la misma plaza/puesto para todos los días (flujo simple).
//  · PER_DAY — una elección independiente por cada día.
export type LocationMode = 'ALL' | 'PER_DAY';

// Elección de ubicación de UN día concreto (modo PER_DAY). `auto` (solo parking)
// = auto-asignación por categoría ese día (resourceId viaja null). Un recurso
// concreto lleva `resourceId` + `label` y `auto=false`. Sin elegir = ausencia de
// entrada en el mapa `perDay`.
export interface DayChoice {
  resourceId: number | null;
  label: string | null;
  auto: boolean;
}

// Índice de cada paso del asistente (modo admin, con paso de empleado).
export const STEP_RESOURCE = 0;
export const STEP_DATES = 1;
export const STEP_EMPLOYEE = 2;
export const STEP_LOCATION = 3;
export const STEP_SUMMARY = 4;

// Estado completo del asistente. `dates` es la lista canónica y ordenada de
// fechas ISO (YYYY-MM-DD) derivada del modo activo; el resto de campos son la
// materia prima de cada modo para poder reconstruir la selección al volver atrás.
// Beneficiario de la reserva: un empleado interno o un visitante externo. El
// visitante reserva por su propio endpoint (/visitor-reservations) y NO recibe email;
// no tiene categoría, así que no hay auto-asignación por planta.
export type BeneficiaryType = 'EMPLOYEE' | 'VISITOR';

export interface WizardState {
  resourceType: ResourceType | null;
  dateMode: DateMode;
  // SINGLE
  singleDate: string;
  // RANGE
  rangeStart: string;
  rangeEnd: string;
  // SCATTER
  scatterDates: string[];
  // Beneficiario: empleado o visitante.
  beneficiaryType: BeneficiaryType;
  employeeId: number | null;
  visitorId: number | null;
  // Modo de asignación de ubicación (relevante con varias fechas).
  locationMode: LocationMode;
  // --- Modo ALL (misma ubicación para todos los días) ---
  // Ubicación: puesto elegido (deskId) o plaza elegida / auto.
  deskId: number | null;
  parkingChoice: ParkingChoice | null;
  // Etiqueta humana del recurso elegido (p. ej. "D-08"), para el resumen. null en
  // auto-asignación de plaza (aún sin recurso concreto).
  chosenLabel: string | null;
  // --- Modo PER_DAY (una elección por día) ---
  // Mapa fecha ISO → elección de ese día. Ausencia de clave = día sin asignar.
  perDay: Record<string, DayChoice>;
}

// Resultado por fecha de la confirmación (loop de POST /requests/admin).
export interface BookingOutcome {
  date: string;
  ok: boolean;
  // Clave i18n del motivo del fallo (duplicada / sin disponibilidad / genérico).
  reasonKey?: string;
}

// Recurso elegible para las fechas seleccionadas (libre en TODAS): id genérico
// del recurso (deskId o parkingSpaceId) + su etiqueta humana (p. ej. "P-08").
export interface EligibleResource {
  resourceId: number;
  label: string;
}

export const RESOURCE_PARKING: ResourceType = 'PARKING';
export const RESOURCE_DESK: ResourceType = 'DESK';
