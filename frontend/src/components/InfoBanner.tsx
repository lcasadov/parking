import { type ReactNode } from 'react';

type InfoBannerVariant = 'blue' | 'green' | 'amber' | 'red';

interface InfoBannerProps {
  variant: InfoBannerVariant;
  children: ReactNode;
  // Nombre del icono Tabler (sin el prefijo "ti-"). Opcional.
  icon?: string;
}

// Banner informativo con color semantico (mockup .info-banner). role=status
// para que los lectores de pantalla anuncien el contenido.
export function InfoBanner({ variant, children, icon }: InfoBannerProps) {
  return (
    <div className={`info-banner ${variant}`} role="status">
      {icon ? <i className={`ti ti-${icon}`} aria-hidden="true" /> : null}
      <span>{children}</span>
    </div>
  );
}
