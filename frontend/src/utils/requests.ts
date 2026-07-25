import type { RejectionReasonCode, Request } from '../types/request';

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

// Comprueba si una fecha ISO es hoy o cualquier fecha futura (sin limite
// superior): solo se rechazan fechas anteriores a hoy. La comparacion
// lexicografica es valida por el formato YYYY-MM-DD.
export function isTodayOrFuture(dateIso: string, now: Date = new Date()): boolean {
  return dateIso >= todayIso(now);
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

// Ventana minima entre reenvios de aviso a los admins (PendingConfirmationBanner,
// POST /requests/{id}/resend): 24h desde la creacion o desde el ultimo reenvio,
// lo que sea mas reciente.
const RESEND_COOLDOWN_MS = 24 * 60 * 60 * 1000;

// Instante base (mas reciente entre creacion y ultimo reenvio) a partir del cual
// se cuenta la ventana de 24h.
function resendBaselineMs(createdAt: string, lastRemindedAt?: string | null): number {
  const createdMs = new Date(createdAt).getTime();
  const remindedMs = lastRemindedAt ? new Date(lastRemindedAt).getTime() : createdMs;
  return Math.max(createdMs, remindedMs);
}

// Elegible para reenviar la solicitud (POST /requests/{id}/resend) cuando han
// pasado >= 24h desde max(createdAt, lastRemindedAt).
export function canResendRequest(
  createdAt: string,
  lastRemindedAt: string | null | undefined,
  now: Date = new Date(),
): boolean {
  return now.getTime() - resendBaselineMs(createdAt, lastRemindedAt) >= RESEND_COOLDOWN_MS;
}

// Milisegundos restantes hasta que el reenvio sea elegible (0 si ya lo es). Usado
// para el texto "podrás reavisar en Xh" cuando aun no se puede reenviar.
export function resendCooldownRemainingMs(
  createdAt: string,
  lastRemindedAt: string | null | undefined,
  now: Date = new Date(),
): number {
  const elapsed = now.getTime() - resendBaselineMs(createdAt, lastRemindedAt);
  return Math.max(0, RESEND_COOLDOWN_MS - elapsed);
}
