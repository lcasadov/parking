import { isPastDate, todayIso } from './releases';

// Utilidades de reservas de visita. Una reserva pasada ya no puede anularse
// (DELETE /visitor-reservations/{id} solo aplica a futuras -> 400 en pasada),
// por lo que la UI deshabilita el boton de cancelar y el date picker fija min=hoy.

// Reexporta el primer dia seleccionable (hoy) para el min de los date pickers.
export { todayIso };

// Una reserva es anulable solo si su fecha es hoy o futura.
export function canCancelReservation(reservationDate: string, now: Date = new Date()): boolean {
  return !isPastDate(reservationDate, now);
}
