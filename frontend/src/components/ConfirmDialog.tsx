import * as AlertDialog from '@radix-ui/react-alert-dialog';
import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  // 'green' acción neutra/positiva · 'red' acción destructiva.
  tone?: 'green' | 'red';
  // Deshabilita el botón de confirmar (p.ej. validación pendiente o mutación en curso).
  busy?: boolean;
  // Si se pasa, el confirmar NO auto-cierra el diálogo: se queda abierto mostrando un
  // spinner mientras `loading` es true (el caller cierra el diálogo en su onSuccess).
  // Sin esta prop, el confirmar mantiene el auto-cierre de Radix (comportamiento previo).
  loading?: boolean;
  // Icono Tabler opcional (sin prefijo "ti-").
  icon?: string;
}

// Diálogo de confirmación accesible sobre Radix AlertDialog (Ola A · primitivas).
// Radix aporta trap de foco, bloqueo de scroll, Escape y roles ARIA de alerta
// (aria-labelledby/aria-describedby) sin trabajo manual. Animación de entrada
// gobernada por CSS vía data-state (ver styles/components.css → .rx-*). Pensado
// para reemplazar los modales de confirmación existentes conservando su lógica:
// el handler onConfirm es el mismo callback de mutación que ya usan.
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  cancelLabel,
  onConfirm,
  tone = 'green',
  busy = false,
  loading,
  icon,
}: ConfirmDialogProps) {
  const { t } = useTranslation();
  // Modo "asíncrono con feedback": el confirmar no cierra el diálogo (lo cierra el
  // caller al terminar) y muestra un spinner mientras `loading` es true.
  const controlledClose = loading !== undefined;
  const confirmContent = (
    <>
      {loading ? <i className="ti ti-loader-2 btn-spin" aria-hidden="true" /> : null}
      {confirmLabel ?? t('common.confirm')}
    </>
  );
  return (
    <AlertDialog.Root open={open} onOpenChange={onOpenChange}>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="rx-overlay" />
        <AlertDialog.Content
          className="rx-dialog rx-dialog-narrow"
          // Los modales no se cierran con Escape (decisión de producto).
          onEscapeKeyDown={(event) => event.preventDefault()}
        >
          <div className={`rx-dialog-header ${tone}`}>
            <AlertDialog.Title className="rx-dialog-title">
              {icon ? <i className={`ti ti-${icon}`} aria-hidden="true" /> : null}
              {title}
            </AlertDialog.Title>
          </div>
          {description ? (
            <AlertDialog.Description asChild>
              <div className="rx-dialog-body">{description}</div>
            </AlertDialog.Description>
          ) : null}
          <div className="rx-dialog-footer">
            <AlertDialog.Cancel asChild>
              <button type="button" className="btn btn-white">
                {cancelLabel ?? t('common.cancel')}
              </button>
            </AlertDialog.Cancel>
            {controlledClose ? (
              <button
                type="button"
                className={`btn btn-${tone}`}
                disabled={busy || loading}
                aria-busy={loading || undefined}
                onClick={onConfirm}
              >
                {confirmContent}
              </button>
            ) : (
              <AlertDialog.Action asChild>
                <button
                  type="button"
                  className={`btn btn-${tone}`}
                  disabled={busy}
                  onClick={onConfirm}
                >
                  {confirmContent}
                </button>
              </AlertDialog.Action>
            )}
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
