import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Sidebar, type SidebarItem } from './Sidebar';

describe('Sidebar', () => {
  it('should_render_nav_items_with_active_state', () => {
    const items: SidebarItem[] = [
      { key: 'home', label: 'Inicio', icon: 'home', active: true },
      { key: 'list', label: 'Listado', icon: 'list' },
    ];
    render(<Sidebar items={items} />);

    expect(screen.getByText('Inicio')).toBeInTheDocument();
    expect(screen.getByText('Listado')).toBeInTheDocument();
    expect(screen.getByRole('navigation')).toBeInTheDocument();
  });
});
