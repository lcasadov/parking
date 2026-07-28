import { getApiError } from '../api/apiError';

// Codigo del 409 que devuelve el backend cuando un recurso no puede desactivarse por
// tener asignaciones vigentes o futuras (guard ResourceDeactivationGuard, tarea 16).
const RESOURCE_HAS_FUTURE = 'RESOURCE_HAS_FUTURE_ASSIGNMENTS';

// Elige la clave i18n del toast al fallar la (des)activacion de un recurso: si el
// backend bloquea la desactivacion por asignaciones futuras, usa el aviso especifico
// que indica que afecta a empleados/visitantes; en cualquier otro error, el generico.
export function deactivationErrorKey(error: unknown, base: string): string {
  if (getApiError(error)?.error === RESOURCE_HAS_FUTURE) {
    return `${base}.errors.hasFutureAssignments`;
  }
  return `${base}.errors.toggle`;
}
