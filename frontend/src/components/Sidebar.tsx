import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

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
      {/* Lockup compacto (estilo mockup): marca ALEATICA simple + nombre de
          producto "parking" y la empresa. Logo en public/. */}
      <div className="sidebar-brand">
        <img src="/logo-aleatica-mini.png" alt="ALEATICA" className="sidebar-mark" />
        <div className="sidebar-brand-text">
          <b className="sidebar-brand-name">{t('common.appName')}</b>
          <span className="sidebar-brand-sub">ALEATICA</span>
        </div>
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
