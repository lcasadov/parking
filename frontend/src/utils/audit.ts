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
