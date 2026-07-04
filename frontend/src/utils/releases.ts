import { todayIso } from './requests';

// Utilidades de liberacion. La ventana de liberacion es release_date >= hoy;
// una liberacion pasada ya no puede anularse (DELETE /releases/{id} solo aplica
// a futuras), por lo que la UI deshabilita el boton de cancelar.

// Reexporta el primer dia seleccionable (hoy) para el min de los date pickers.
export { todayIso };

// Una fecha ISO (YYYY-MM-DD) es pasada si es estrictamente anterior a hoy.
// Comparacion lexicografica valida por el formato YYYY-MM-DD.
export function isPastDate(dateIso: string, now: Date = new Date()): boolean {
  return dateIso < todayIso(now);
}

// Una liberacion es cancelable solo si su fecha es hoy o futura.
export function canCancelRelease(releaseDate: string, now: Date = new Date()): boolean {
  return !isPastDate(releaseDate, now);
}
