import { type ReactNode } from 'react';

// Tono semantico mapeado al color por estado del contrato §4 (mapa unico
// estado->color, tokens --state-*). Reutilizable por todas las vistas para
// unificar pills de estado/categoria sin hex sueltos.
export type StatusTone = 'occupied' | 'released' | 'pending' | 'request' | 'free';

interface StatusPillProps {
  tone: StatusTone;
  children: ReactNode;
  // Icono Tabler opcional (sin prefijo "ti-").
  icon?: string;
}

// Pill de estado (contrato §4/§5). Compone la forma de pill (.pill) con el color
// del estado (.pill-state-*), ambos derivados de tokens del design system.
// El significado no depende solo del color: el texto (children) lo explicita.
export function StatusPill({ tone, children, icon }: StatusPillProps) {
  return (
    <span className={`pill pill-state-${tone}`}>
      {icon ? <i className={`ti ti-${icon}`} aria-hidden="true" /> : null}
      {children}
    </span>
  );
}
