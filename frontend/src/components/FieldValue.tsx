import { type ReactNode } from 'react';

interface FieldValueProps {
  children: ReactNode;
  readOnly?: boolean;
  focused?: boolean;
  danger?: boolean;
  // Distribuye contenido e icono a los extremos (mockup .field-value.with-icon).
  withIcon?: boolean;
}

// Valor de campo de solo lectura con estilo de caja (mockup .field-value).
// Presentacional; los modificadores reflejan los estados visuales del mockup.
export function FieldValue({
  children,
  readOnly = false,
  focused = false,
  danger = false,
  withIcon = false,
}: FieldValueProps) {
  const classes = ['field-value'];
  if (readOnly) {
    classes.push('readonly');
  }
  if (focused) {
    classes.push('focused');
  }
  if (danger) {
    classes.push('danger');
  }
  if (withIcon) {
    classes.push('with-icon');
  }
  return <div className={classes.join(' ')}>{children}</div>;
}
