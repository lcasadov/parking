import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { API_ERROR_TOAST, type ApiErrorToastDetail, type ToastTone } from '../api/events';
import { DUR, EASE } from '../theme/motion';

const AUTO_DISMISS_MS = 5000;

interface ToastEntry {
  id: number;
  message: string;
  tone: ToastTone;
}

// Icono por tono (S1192: sin literales repetidos).
const TOAST_ICON: Record<ToastTone, string> = {
  success: 'circle-check',
  error: 'alert-circle',
  info: 'info-circle',
};

// Provider de Toast GENERAL (Ola B4): antes solo existia para errores 403/5xx
// del interceptor Axios (siempre en rojo, un unico mensaje a la vez). Ahora
// apila varias entradas y soporta tono success/info/error, para poder mostrar
// tambien confirmaciones de accion ("Solicitud creada", "Plaza asignada",
// "Liberada"...). Sigue suscrito al mismo bus de eventos (parking:api-error-toast)
// que usa el interceptor Axios (fuera del arbol de React) y el hook
// hooks/useToast.ts (dentro de componentes) — un unico canal para ambos casos.
// Montar UNA vez cerca de la raiz (App.tsx).
export function Toast() {
  const { t } = useTranslation();
  const reduceMotion = useReducedMotion();
  const [toasts, setToasts] = useState<ToastEntry[]>([]);
  const nextId = useRef(0);

  useEffect(() => {
    function handle(event: Event) {
      const detail = (event as CustomEvent<ApiErrorToastDetail>).detail;
      const id = nextId.current;
      nextId.current += 1;
      const tone: ToastTone = detail.tone ?? 'error';
      setToasts((current) => [...current, { id, message: detail.message, tone }]);
      window.setTimeout(() => {
        setToasts((current) => current.filter((toast) => toast.id !== id));
      }, AUTO_DISMISS_MS);
    }
    window.addEventListener(API_ERROR_TOAST, handle);
    return () => window.removeEventListener(API_ERROR_TOAST, handle);
  }, []);

  function dismiss(id: number): void {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }

  if (toasts.length === 0) {
    return null;
  }

  return (
    <div className="toast-stack">
      <AnimatePresence initial={false}>
        {toasts.map((toast) => (
          <motion.div
            key={toast.id}
            className={`toast toast-${toast.tone}`}
            role={toast.tone === 'error' ? 'alert' : 'status'}
            initial={reduceMotion ? { opacity: 0 } : { opacity: 0, y: 12, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={reduceMotion ? { opacity: 0 } : { opacity: 0, y: 8, scale: 0.98 }}
            transition={{ duration: reduceMotion ? 0 : DUR.base, ease: EASE.out }}
          >
            <i className={`ti ti-${TOAST_ICON[toast.tone]}`} aria-hidden="true" />
            <span>{t(toast.message)}</span>
            <button
              type="button"
              className="toast-dismiss"
              aria-label={t('common.close')}
              onClick={() => dismiss(toast.id)}
            >
              <i className="ti ti-x" aria-hidden="true" />
            </button>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}
