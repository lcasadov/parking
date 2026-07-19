import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Sidebar, SidebarSection, type SidebarItem } from './Sidebar';

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

  it('should_render_aleatica_wordmark_and_footer_slot', () => {
    const { container } = render(
      <Sidebar footer={<div>user-card</div>}>
        <SidebarSection label="Gestión" />
      </Sidebar>,
    );

    // Wordmark ALEATICA (fallback de texto) presente en la cabecera del sidebar.
    expect(container.querySelector('.sidebar-wordmark')).toHaveAttribute('aria-label', 'ALEATICA');
    // Etiqueta de seccion y slot de pie renderizados.
    expect(screen.getByText('Gestión')).toHaveClass('nav-section');
    expect(screen.getByText('user-card')).toBeInTheDocument();
  });
});
