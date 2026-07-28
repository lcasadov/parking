import { useMemo } from 'react';
import { emitApiErrorToast, type ToastTone } from '../api/events';

export interface ToastApi {
  // Muestra un toast con el tono indicado (por defecto 'info'). `message` es
  // una clave i18n (el provider la traduce al pintarla, igual que el resto de
  // la app: nunca texto hardcodeado).
  show: (message: string, tone?: ToastTone) => void;
  success: (message: string) => void;
  error: (message: string) => void;
  info: (message: string) => void;
}

// Atajo ergonomico sobre el bus de eventos global (api/events.ts) para
// disparar confirmaciones de accion desde un componente React ("Solicitud
// creada", "Plaza asignada", "Liberada"...). El provider que renderiza la pila
// de toasts es <Toast/> (components/Toast.tsx), montado una vez cerca de la
// raiz (App.tsx); este hook solo emite el evento, no requiere Context porque
// el mismo bus ya lo usa codigo fuera del arbol de React (interceptor Axios).
export function useToast(): ToastApi {
  return useMemo(
    () => ({
      show: (message: string, tone: ToastTone = 'info') => emitApiErrorToast(message, tone),
      success: (message: string) => emitApiErrorToast(message, 'success'),
      error: (message: string) => emitApiErrorToast(message, 'error'),
      info: (message: string) => emitApiErrorToast(message, 'info'),
    }),
    [],
  );
}
