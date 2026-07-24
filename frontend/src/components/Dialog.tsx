import * as RadixDialog from '@radix-ui/react-dialog';
import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

interface DialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  children: ReactNode;
  footer?: ReactNode;
  // 'green' | 'red' | 'amber' — color de la cabecera (paridad con Modal legacy).
  tone?: 'green' | 'red' | 'amber';
  narrow?: boolean;
  // Panel más ancho (flujos ricos como el asistente de reserva). Aditivo: el
  // resto de diálogos conservan el ancho base sin cambios.
  wide?: boolean;
  // Panel a pantalla completa (con inset en desktop, edge-to-edge en móvil).
  // Pensado para flujos multipaso (asistente de reserva) donde el contenido
  // interno necesita todo el alto de viewport disponible. Aditivo: el resto
  // de diálogos conservan el tamaño base sin cambios.
  fullScreen?: boolean;
  // Quita el padding por defecto de rx-dialog-body para que el contenido
  // gestione sus propias zonas (p.ej. cabecera sticky + área con scroll).
  flushBody?: boolean;
  // Icono Tabler opcional (sin prefijo "ti-").
  icon?: string;
  // Oculta el botón cerrar (x) para diálogos de decisión obligatoria.
  closeable?: boolean;
}

// Diálogo modal accesible sobre Radix Dialog (Ola A · primitivas). Alternativa de
// rollout al <Modal> legacy: mismo lenguaje visual (cabecera con tono + icono,
// cuerpo, footer) pero con trap de foco, bloqueo de scroll, Escape y aria-*
// gestionados por Radix. Migración sin cambiar lógica: el estado open/onOpenChange
// sustituye al montaje condicional; onOpenChange(false) equivale al antiguo onClose.
export function Dialog({
  open,
  onOpenChange,
  title,
  children,
  footer,
  tone = 'green',
  narrow = false,
  wide = false,
  fullScreen = false,
  flushBody = false,
  icon,
  closeable = true,
}: DialogProps) {
  const { t } = useTranslation();
  const widthClass = fullScreen
    ? ' rx-dialog-full'
    : narrow
      ? ' rx-dialog-narrow'
      : wide
        ? ' rx-dialog-wide'
        : '';
  const bodyClass = flushBody ? ' is-flush' : '';
  return (
    <RadixDialog.Root open={open} onOpenChange={onOpenChange}>
      <RadixDialog.Portal>
        <RadixDialog.Overlay className="rx-overlay" />
        <RadixDialog.Content
          className={`rx-dialog${widthClass}`}
          // Los modales NO se cierran con Escape (decisión de producto): evita
          // cierres accidentales, sobre todo en flujos largos como el asistente de
          // reserva. Se cierran con la (x), el footer o clic fuera.
          onEscapeKeyDown={(event) => event.preventDefault()}
        >
          <div className={`rx-dialog-header ${tone}`}>
            <RadixDialog.Title className="rx-dialog-title">
              {icon ? <i className={`ti ti-${icon}`} aria-hidden="true" /> : null}
              {title}
            </RadixDialog.Title>
            {closeable ? (
              <RadixDialog.Close className="rx-dialog-close" aria-label={t('common.close')}>
                <i className="ti ti-x" aria-hidden="true" />
              </RadixDialog.Close>
            ) : null}
          </div>
          <div className={`rx-dialog-body${bodyClass}`}>{children}</div>
          {footer ? <div className="rx-dialog-footer">{footer}</div> : null}
        </RadixDialog.Content>
      </RadixDialog.Portal>
    </RadixDialog.Root>
  );
}
