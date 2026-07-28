import type { EmployeeCategory } from '../types/employee';
import type { EligibleResource } from '../components/wizard/wizardTypes';

// Categorías de rango ALTO: en el garaje subterráneo prefieren las plantas físicas
// más altas (planta -1, millar 1xxx). Espejo de RequestService.HIGH_CATEGORIES.
const HIGH_CATEGORIES: ReadonlySet<EmployeeCategory> = new Set<EmployeeCategory>([
  'CEO',
  'CONSEJO',
  'DIRECTOR_N1',
  'DIRECTOR_N2',
]);

export function isHighCategory(category: EmployeeCategory | undefined): boolean {
  return category !== undefined && HIGH_CATEGORIES.has(category);
}

// Millar de la plaza a partir de su etiqueta numérica (p. ej. "4001" → 4). El
// millar N se corresponde con la planta física -N. Devuelve 0 si no es numérica.
export function floorOfLabel(label: string): number {
  const digits = label.replace(/\D/g, '');
  if (digits === '') {
    return 0;
  }
  return Math.trunc(Number.parseInt(digits, 10) / 1000);
}

// Clave de orden de planta según la preferencia de la categoría (misma regla que
// RequestService.floorPreferenceKey): ALTA = millar tal cual (1xxx primero); resto
// = millar negado (5xxx primero). Menor clave = más preferente.
function floorKey(floor: number, high: boolean): number {
  return high ? floor : -floor;
}

// Plaza elegible enriquecida con su número y millar, para ordenar/agrupar.
export interface RankedResource extends EligibleResource {
  number: number;
  floor: number;
}

export interface GroupedEligible {
  // Plazas de la planta preferente para la categoría (las que elegiría el auto),
  // ordenadas por prioridad. Vacío si no se conoce la categoría.
  suggested: RankedResource[];
  // Resto de plazas elegibles, ordenadas por prioridad.
  others: RankedResource[];
  // Millar de la planta preferente (1..5) o null si no hay categoría/plazas.
  preferredFloor: number | null;
}

// Ordena las plazas elegibles según la prioridad de la categoría del empleado y
// las divide en "sugeridas" (planta preferente = la que elegiría la auto-asignación)
// y "otras". Espejo cliente de autoAssignParkingSpace: la categoría solo fija el
// orden; dentro de una planta gana el número más bajo. Sin categoría, todo cae en
// `others` ordenado por número (sin sugeridas).
export function groupEligibleByPriority(
  eligible: EligibleResource[],
  category: EmployeeCategory | undefined,
): GroupedEligible {
  const ranked: RankedResource[] = eligible.map((resource) => {
    const number = numberOfLabel(resource.label);
    return { ...resource, number, floor: floorOfLabel(resource.label) };
  });

  // Sin categoría: fallback neutro por número ascendente, sin apartado de sugeridas.
  if (category === undefined || ranked.length === 0) {
    ranked.sort((a, b) => a.number - b.number);
    return { suggested: [], others: ranked, preferredFloor: null };
  }

  const high = isHighCategory(category);
  ranked.sort((a, b) => floorKey(a.floor, high) - floorKey(b.floor, high) || a.number - b.number);
  const preferredFloor = ranked[0].floor;
  return {
    suggested: ranked.filter((resource) => resource.floor === preferredFloor),
    others: ranked.filter((resource) => resource.floor !== preferredFloor),
    preferredFloor,
  };
}

// Número humano completo de la plaza (p. ej. "4001" → 4001), para el desempate.
function numberOfLabel(label: string): number {
  const digits = label.replace(/\D/g, '');
  return digits === '' ? 0 : Number.parseInt(digits, 10);
}
