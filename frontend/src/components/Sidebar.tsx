import { type ReactNode } from 'react';

export interface SidebarItem {
  key: string;
  label: string;
  icon: string;
  active?: boolean;
}

// Sidebar ALEATICA (contrato §5): wordmark de marca en la cabecera, navegacion
// desplazable (con secciones Gestión/Operativa aportadas por el layout via
// `children`) y tarjeta de usuario al pie (slot `footer`).
export function Sidebar({
  items = [],
  children,
  footer,
}: {
  items?: SidebarItem[];
  children?: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <nav className="sidebar" aria-label="primary">
      {/* Wordmark de texto estilizado como FALLBACK del logo. Cuando exista el
          asset real frontend/src/assets/aleatica-logo.png, sustituir este bloque
          por <img src={logo} alt="ALEATICA" className="sidebar-logo" />. */}
      <div className="sidebar-brand">
        <span className="sidebar-wordmark" aria-label="ALEATICA">
          ALE<span className="wordmark-accent">A</span>TICA
          <span className="sidebar-tagline">parking</span>
        </span>
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
