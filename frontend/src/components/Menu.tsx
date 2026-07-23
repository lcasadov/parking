import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import { type ReactNode } from 'react';

export interface MenuItem {
  key: string;
  label: string;
  icon?: string;
  onSelect: () => void;
  // Marca la opción como destructiva (tinta roja).
  danger?: boolean;
  disabled?: boolean;
}

interface MenuProps {
  // Disparador: cualquier elemento (se compone con asChild).
  trigger: ReactNode;
  items: MenuItem[];
  align?: 'start' | 'center' | 'end';
  ariaLabel: string;
}

// Menú contextual accesible sobre Radix DropdownMenu (Ola A · primitivas).
// Radix gestiona navegación por teclado (flechas/Home/End/typeahead), foco y
// cierre. El panel escala desde su disparador (transform-origin provisto por
// Radix como var, ver .rx-menu en components.css) — emil-design-eng: los popovers
// nacen del trigger, no del centro. Reduced-motion se respeta en CSS.
export function Menu({ trigger, items, align = 'end', ariaLabel }: MenuProps) {
  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>{trigger}</DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          className="rx-menu"
          align={align}
          sideOffset={6}
          aria-label={ariaLabel}
        >
          {items.map((item) => (
            <DropdownMenu.Item
              key={item.key}
              className={`rx-menu-item${item.danger ? ' danger' : ''}`}
              disabled={item.disabled}
              onSelect={item.onSelect}
            >
              {item.icon ? <i className={`ti ti-${item.icon}`} aria-hidden="true" /> : null}
              {item.label}
            </DropdownMenu.Item>
          ))}
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
