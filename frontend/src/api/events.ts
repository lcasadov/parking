// Bus de eventos minimalista para desacoplar el interceptor Axios (y cualquier
// codigo fuera del arbol de React) de la UI. El interceptor 401 emite
// SESSION_EXPIRED; el SessionExpiredModal se suscribe. El interceptor 403/5xx
// y las confirmaciones de accion emiten API_ERROR_TOAST; el provider de Toast
// (components/Toast.tsx) se suscribe y apila las entradas.

export const SESSION_EXPIRED = 'parking:session-expired';
export const API_ERROR_TOAST = 'parking:api-error-toast';

// Tono visual del toast: 'error' (rojo, valor por defecto retrocompatible con
// todas las llamadas existentes) · 'success' (verde, confirmacion de accion) ·
// 'info' (azul, aviso neutro).
export type ToastTone = 'error' | 'success' | 'info';

export interface ApiErrorToastDetail {
  message: string;
  tone?: ToastTone;
}

export function emitSessionExpired(): void {
  window.dispatchEvent(new CustomEvent(SESSION_EXPIRED));
}

// Emite un toast global. `tone` por defecto 'error': todas las llamadas
// existentes (403/5xx, errores de mutacion) conservan el rojo de siempre sin
// tocar sus call-sites. Ver hooks/useToast.ts para el atajo ergonomico desde
// componentes React (toast.success/error/info).
export function emitApiErrorToast(message: string, tone: ToastTone = 'error'): void {
  window.dispatchEvent(
    new CustomEvent<ApiErrorToastDetail>(API_ERROR_TOAST, { detail: { message, tone } }),
  );
}
