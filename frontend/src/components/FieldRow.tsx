import { type ReactNode } from 'react';

interface FieldRowProps {
  children: ReactNode;
  // Reparte las columnas 1.4fr / 1fr en lugar de 1fr / 1fr (mockup .field-row-1-4).
  wide?: boolean;
}

// Fila de formulario a dos columnas (mockup .field-row). Presentacional.
export function FieldRow({ children, wide = false }: FieldRowProps) {
  return <div className={`field-row${wide ? ' field-row-1-4' : ''}`}>{children}</div>;
}
