import { type ReactNode } from 'react';

export interface SidebarItem {
  key: string;
  label: string;
  icon: string;
  active?: boolean;
}

// Sidebar base (vacio en bootstrap): se rellena en changes funcionales.
export function Sidebar({ items = [], children }: { items?: SidebarItem[]; children?: ReactNode }) {
  return (
    <nav className="sidebar" aria-label="primary">
      {items.map((item) => (
        <span key={item.key} className={`nav-item${item.active ? ' active' : ''}`}>
          <i className={`ti ti-${item.icon}`} aria-hidden="true" />
          {item.label}
        </span>
      ))}
      {children}
    </nav>
  );
}
