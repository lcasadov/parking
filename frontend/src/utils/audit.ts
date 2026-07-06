// Utilidades de los paneles de auditoria / logs de login.

// Convierte una fecha YYYY-MM-DD al inicio del dia en UTC (date-time del contrato).
export function toIsoStart(date: string): string | undefined {
  return date ? `${date}T00:00:00Z` : undefined;
}

// Convierte una fecha YYYY-MM-DD al fin del dia en UTC (date-time del contrato).
export function toIsoEnd(date: string): string | undefined {
  return date ? `${date}T23:59:59Z` : undefined;
}

// La ventana [from, to] es valida si falta alguno de los extremos o from <= to.
export function isValidWindow(from: string, to: string): boolean {
  if (!from || !to) {
    return true;
  }
  return from <= to;
}

// Formatea un ISO date-time para mostrarlo; si es invalido, devuelve el original.
export function formatDateTime(iso: string): string {
  const parsed = new Date(iso);
  if (Number.isNaN(parsed.getTime())) {
    return iso;
  }
  return parsed.toLocaleString();
}

// Clases de pill de auditoria (design-system). Constantes para evitar literales
// repetidos (S1192).
const PILL_GREEN = 'pill-green';
const PILL_RED = 'pill-red';
const PILL_PINK = 'pill-pink';
const PILL_BLUE = 'pill-blue';
const PILL_AMBER = 'pill-amber';
const PILL_GRAY = 'pill-gray';

// Mapea la accion de auditoria a la variante de color de la pill (mockup 11).
// Se compara por palabra clave, no por el nombre exacto, para tolerar variantes
// (APPROVE_REQUEST / REQUEST_APPROVED). El orden importa: RELEASE va antes que
// CREATE para que CREATE_RELEASE resuelva a rosa.
export function auditPillClass(action: string): string {
  const value = action.toUpperCase();
  if (value.includes('APPROV')) {
    return PILL_GREEN;
  }
  if (value.includes('REJECT')) {
    return PILL_RED;
  }
  if (value.includes('RELEASE')) {
    return PILL_PINK;
  }
  if (value.includes('RESET')) {
    return PILL_AMBER;
  }
  if (value.includes('CREATE') || value.includes('UPDATE') || value.includes('DELETE')) {
    return PILL_BLUE;
  }
  return PILL_GRAY;
}
