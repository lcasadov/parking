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
  icon,
  closeable = true,
}: DialogProps) {
  const { t } = useTranslation();
  return (
    <RadixDialog.Root open={open} onOpenChange={onOpenChange}>
      <RadixDialog.Portal>
        <RadixDialog.Overlay className="rx-overlay" />
        <RadixDialog.Content className={`rx-dialog${narrow ? ' rx-dialog-narrow' : ''}`}>
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
          <div className="rx-dialog-body">{children}</div>
          {footer ? <div className="rx-dialog-footer">{footer}</div> : null}
        </RadixDialog.Content>
      </RadixDialog.Portal>
    </RadixDialog.Root>
  );
}
