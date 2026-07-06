import { useEffect, useRef, type ReactNode } from 'react';

interface PopoverProps {
  children: ReactNode;
  onClose: () => void;
  // Nombre accesible del panel.
  label: string;
  className?: string;
}

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

// Panel flotante anclado (mockup .popover). Se cierra al pulsar Escape o al
// hacer click fuera, y atrapa el foco con Tab dentro del panel.
export function Popover({ children, onClose, label, className }: PopoverProps) {
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const panel = panelRef.current;
    panel?.focus();

    function focusables(): HTMLElement[] {
      return Array.from(panel?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? []);
    }

    function handleKey(event: KeyboardEvent): void {
      if (event.key === 'Escape') {
        onClose();
        return;
      }
      if (event.key !== 'Tab') {
        return;
      }
      const items = focusables();
      if (items.length === 0) {
        event.preventDefault();
        return;
      }
      const first = items[0];
      const last = items[items.length - 1];
      const active = document.activeElement;
      if (event.shiftKey && active === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && active === last) {
        event.preventDefault();
        first.focus();
      }
    }

    function handleClickOutside(event: MouseEvent): void {
      if (panel && !panel.contains(event.target as Node)) {
        onClose();
      }
    }

    document.addEventListener('keydown', handleKey);
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('keydown', handleKey);
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [onClose]);

  return (
    <div
      ref={panelRef}
      className={`popover${className ? ` ${className}` : ''}`}
      role="dialog"
      aria-label={label}
      tabIndex={-1}
    >
      {children}
    </div>
  );
}
