// Bus de eventos minimalista para desacoplar el interceptor Axios de la UI.
// El interceptor 401 emite SESSION_EXPIRED; el SessionExpiredModal se suscribe.

export const SESSION_EXPIRED = 'parking:session-expired';
export const API_ERROR_TOAST = 'parking:api-error-toast';

export interface ApiErrorToastDetail {
  message: string;
}

export function emitSessionExpired(): void {
  window.dispatchEvent(new CustomEvent(SESSION_EXPIRED));
}

export function emitApiErrorToast(message: string): void {
  window.dispatchEvent(
    new CustomEvent<ApiErrorToastDetail>(API_ERROR_TOAST, { detail: { message } }),
  );
}
