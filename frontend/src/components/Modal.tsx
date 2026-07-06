import { useEffect, useId, useRef, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

export interface ModalTab {
  id: string;
  label: string;
}

interface ModalProps {
  title: string;
  children: ReactNode;
  footer?: ReactNode;
  onClose?: () => void;
  variant?: 'green' | 'red' | 'amber';
  narrow?: boolean;
  closeable?: boolean;
  // Icono Tabler (sin prefijo "ti-") a la izquierda del titulo.
  icon?: string;
  // Pestañas bajo la cabecera (mockup .modal-tabs).
  tabs?: ModalTab[];
  activeTab?: string;
  onTabChange?: (id: string) => void;
}

// Modal accesible: role=dialog + aria-labelledby; cierre con Esc; foco inicial.
// Cabecera con icono opcional, boton cerrar (x) y pestañas opcionales.
export function Modal({
  title,
  children,
  footer,
  onClose,
  variant = 'green',
  narrow = false,
  closeable = true,
  icon,
  tabs,
  activeTab,
  onTabChange,
}: ModalProps) {
  const { t } = useTranslation();
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const showClose = closeable && Boolean(onClose);

  useEffect(() => {
    dialogRef.current?.focus();
    if (!closeable || !onClose) {
      return;
    }
    function handleKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose?.();
      }
    }
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [onClose, closeable]);

  return (
    <div className="modal-overlay">
      <div
        ref={dialogRef}
        className={`modal${narrow ? ' narrow' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
      >
        <div className={`modal-header ${variant}`}>
          <span className="title" id={titleId}>
            {icon ? <i className={`ti ti-${icon}`} aria-hidden="true" /> : null}
            {title}
          </span>
          {showClose ? (
            <button
              type="button"
              className="modal-header-close"
              aria-label={t('common.close')}
              onClick={onClose}
            >
              <i className="ti ti-x" aria-hidden="true" />
            </button>
          ) : null}
        </div>
        {tabs && tabs.length > 0 ? (
          <div className="modal-tabs" role="tablist" aria-label={title}>
            {tabs.map((tab) => (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={activeTab === tab.id}
                className={`modal-tab${activeTab === tab.id ? ' active' : ''}`}
                onClick={() => onTabChange?.(tab.id)}
              >
                {tab.label}
              </button>
            ))}
          </div>
        ) : null}
        <div className="modal-body">{children}</div>
        {footer ? <div className="modal-footer">{footer}</div> : null}
      </div>
    </div>
  );
}
