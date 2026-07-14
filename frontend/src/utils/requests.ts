import type { RejectionReasonCode, Request } from '../types/request';

// Ventana de solicitud: hoy..hoy+14 dias naturales (README / ux-flows).
export const REQUEST_WINDOW_DAYS = 14;

// Catalogo de motivos de rechazo (contrato RejectionReasonCode). Usado para
// pintar el select de motivos traducido; el texto libre es obligatorio en OTHER.
export const REJECTION_REASON_CODES: RejectionReasonCode[] = [
  'NO_AVAILABILITY',
  'OUTSIDE_POLICY',
  'OTHER',
];

// Longitud minima del texto libre de rechazo cuando reasonCode = OTHER.
export const REJECTION_FREE_TEXT_MIN = 5;

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

// Formatea una fecha local a ISO (YYYY-MM-DD) sin componente horario ni TZ.
export function toIsoDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

// Primer dia seleccionable (hoy).
export function todayIso(now: Date = new Date()): string {
  return toIsoDate(now);
}

// Ultimo dia seleccionable (hoy + 14 dias).
export function maxRequestDateIso(now: Date = new Date()): string {
  const max = new Date(now);
  max.setDate(max.getDate() + REQUEST_WINDOW_DAYS);
  return toIsoDate(max);
}

// Comprueba si una fecha ISO cae dentro de la ventana hoy..hoy+14 (comparacion
// lexicografica valida por el formato YYYY-MM-DD).
export function isWithinWindow(dateIso: string, now: Date = new Date()): boolean {
  return dateIso >= todayIso(now) && dateIso <= maxRequestDateIso(now);
}

// Determina si el empleado dueño puede cancelar la solicitud (change
// cancel-approved-request, D1/D2): siempre en PENDING (no ocupa recurso); en
// APPROVED solo cuando la fecha es futura (>= hoy, hoy inclusive), lo que libera
// el recurso. REJECTED/CANCELLED son terminales. "Hoy" se deriva del mismo
// formato ISO local (YYYY-MM-DD) usado por la ventana de creacion, evitando
// desfases de zona; la comparacion lexicografica es valida por ese formato.
export function canCancelRequest(request: Request, now: Date = new Date()): boolean {
  if (request.status === 'PENDING') {
    return true;
  }
  if (request.status === 'APPROVED') {
    return request.requestedDate >= todayIso(now);
  }
  return false;
}
