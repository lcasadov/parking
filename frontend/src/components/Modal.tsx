import { useEffect, useId, useRef, type ReactNode } from 'react';

interface ModalProps {
  title: string;
  children: ReactNode;
  footer?: ReactNode;
  onClose?: () => void;
  variant?: 'green' | 'red';
  narrow?: boolean;
  closeable?: boolean;
}

// Modal accesible: role=dialog + aria-labelledby; cierre con Esc; foco inicial.
export function Modal({
  title,
  children,
  footer,
  onClose,
  variant = 'green',
  narrow = false,
  closeable = true,
}: ModalProps) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);

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
            {title}
          </span>
        </div>
        <div className="modal-body">{children}</div>
        {footer ? <div className="modal-footer">{footer}</div> : null}
      </div>
    </div>
  );
}
