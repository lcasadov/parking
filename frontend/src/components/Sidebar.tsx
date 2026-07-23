import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import aleaticaLogo from '../assets/aleatica-logo.png';

export interface SidebarItem {
  key: string;
  label: string;
  icon: string;
  active?: boolean;
}

// Sidebar ALEATICA (contrato §5 / prototipo `docs/design/prototipo-aleatica.html`):
// logo real de marca en la cabecera, navegacion desplazable (con secciones
// Gestión/Operativa aportadas por el layout via `children`) y tarjeta de usuario
// al pie (slot `footer`).
export function Sidebar({
  items = [],
  children,
  footer,
  className,
  ariaLabel = 'primary',
}: {
  items?: SidebarItem[];
  children?: ReactNode;
  footer?: ReactNode;
  className?: string;
  ariaLabel?: string;
}) {
  const { t } = useTranslation();
  return (
    <nav className={`sidebar${className ? ` ${className}` : ''}`} aria-label={ariaLabel}>
      {/* Logo oficial ALEATICA (extraido del prototipo aprobado, ver
          frontend/src/assets/aleatica-logo.png) + tagline "Gestión de parking". */}
      <div className="sidebar-brand">
        <img src={aleaticaLogo} alt="ALEATICA" className="sidebar-logo" />
        <span className="sidebar-tagline">{t('layout.brandTagline')}</span>
      </div>
      <div className="sidebar-nav">
        {items.map((item) => (
          <span key={item.key} className={`nav-item${item.active ? ' active' : ''}`}>
            <i className={`ti ti-${item.icon}`} aria-hidden="true" />
            {item.label}
          </span>
        ))}
        {children}
      </div>
      {footer}
    </nav>
  );
}

// Etiqueta de seccion del sidebar (Gestión / Operativa). Presentacional.
export function SidebarSection({ label }: { label: string }) {
  return <div className="nav-section">{label}</div>;
}
