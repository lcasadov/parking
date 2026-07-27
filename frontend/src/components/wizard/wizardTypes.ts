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

// Beneficiario de la reserva: un empleado interno o un visitante externo. El
// visitante reserva por su propio endpoint (/visitor-reservations) y NO recibe email;
// no tiene categoría, así que no hay auto-asignación por planta.
export type BeneficiaryType = 'EMPLOYEE' | 'VISITOR';

// Estado de ubicación de UN tipo de recurso (plaza o puesto). Con varios tipos
// seleccionados, el asistente mantiene un slice por tipo (change admin-improvements,
// tarea 3: reservar plaza Y puesto en el mismo alta con un paso de ubicación por tipo).
export interface TypeLocation {
  // Modo de asignación de ubicación (relevante con varias fechas).
  locationMode: LocationMode;
  // --- Modo ALL (misma ubicación para todos los días) ---
  deskId: number | null;
  parkingChoice: ParkingChoice | null;
  // Etiqueta humana del recurso elegido (p. ej. "D-08"), para el resumen. null en
  // auto-asignación de plaza (aún sin recurso concreto).
  chosenLabel: string | null;
  // --- Modo PER_DAY (una elección por día) ---
  // Mapa fecha ISO → elección de ese día. Ausencia de clave = día sin asignar.
  perDay: Record<string, DayChoice>;
}

// Slice de ubicación vacío (estado inicial de cada tipo).
export function emptyTypeLocation(): TypeLocation {
  return { locationMode: 'ALL', deskId: null, parkingChoice: null, chosenLabel: null, perDay: {} };
}

export const RESOURCE_PARKING: ResourceType = 'PARKING';
export const RESOURCE_DESK: ResourceType = 'DESK';

// Orden canónico de los tipos en el asistente: primero Plaza, luego Puesto. Fija el
// orden de los pasos de ubicación cuando se piden ambos (tarea 3).
export const ORDERED_RESOURCE_TYPES: ResourceType[] = [RESOURCE_PARKING, RESOURCE_DESK];

// Estado completo del asistente. `dates` es la lista canónica y ordenada de
// fechas ISO derivada del modo activo; el resto de campos son la materia prima de
// cada modo para poder reconstruir la selección al volver atrás.
export interface WizardState {
  // Tipos de recurso a reservar (uno o ambos), en orden canónico.
  resourceTypes: ResourceType[];
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
  // Ubicación por tipo de recurso (ambas claves siempre presentes; solo se usan las
  // de los tipos seleccionados).
  locations: Record<ResourceType, TypeLocation>;
}

// Tipo de paso del asistente. Los pasos de ubicación son dinámicos: uno por cada
// tipo de recurso seleccionado (tarea 3).
export type StepKind = 'RESOURCE' | 'DATES' | 'EMPLOYEE' | 'LOCATION' | 'SUMMARY';

// Descriptor de un paso: su tipo y, para los pasos de ubicación, el recurso al que
// aplica (Plaza / Puesto).
export interface StepDescriptor {
  kind: StepKind;
  type?: ResourceType;
}

// Construye la secuencia de pasos según los tipos seleccionados: fijos (recurso,
// fechas, beneficiario), luego un paso de ubicación por tipo (en orden canónico), y
// el resumen al final.
export function buildSteps(resourceTypes: ResourceType[]): StepDescriptor[] {
  const steps: StepDescriptor[] = [
    { kind: 'RESOURCE' },
    { kind: 'DATES' },
    { kind: 'EMPLOYEE' },
  ];
  for (const type of resourceTypes) {
    steps.push({ kind: 'LOCATION', type });
  }
  steps.push({ kind: 'SUMMARY' });
  return steps;
}

// Resultado por fecha de la confirmación (loop de POST /requests/admin). Con varios
// tipos, cada resultado lleva su tipo para distinguir plaza de puesto en la misma fecha.
export interface BookingOutcome {
  date: string;
  ok: boolean;
  // Tipo de recurso al que corresponde el resultado (para agrupar cuando se piden ambos).
  resourceType?: ResourceType;
  // Clave i18n del motivo del fallo (duplicada / sin disponibilidad / genérico).
  reasonKey?: string;
}

// Recurso elegible para las fechas seleccionadas (libre en TODAS): id genérico
// del recurso (deskId o parkingSpaceId) + su etiqueta humana (p. ej. "P-08").
export interface EligibleResource {
  resourceId: number;
  label: string;
}
