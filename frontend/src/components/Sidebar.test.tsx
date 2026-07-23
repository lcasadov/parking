import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Sidebar, SidebarSection, type SidebarItem } from './Sidebar';
import { renderWithProviders } from '../test/renderWithProviders';

describe('Sidebar', () => {
  it('should_render_nav_items_with_active_state', () => {
    const items: SidebarItem[] = [
      { key: 'home', label: 'Inicio', icon: 'home', active: true },
      { key: 'list', label: 'Listado', icon: 'list' },
    ];
    renderWithProviders(<Sidebar items={items} />);

    expect(screen.getByText('Inicio')).toBeInTheDocument();
    expect(screen.getByText('Listado')).toBeInTheDocument();
    expect(screen.getByRole('navigation')).toBeInTheDocument();
  });

  it('should_render_aleatica_logo_and_footer_slot', () => {
    const { container } = renderWithProviders(
      <Sidebar footer={<div>user-card</div>}>
        <SidebarSection label="Gestión" />
      </Sidebar>,
    );

    // Logo oficial ALEATICA (imagen real) en la cabecera del sidebar.
    const logo = screen.getByRole('img', { name: 'ALEATICA' });
    expect(logo).toHaveClass('sidebar-logo');
    expect(logo.tagName).toBe('IMG');
    expect(container.querySelector('.sidebar-tagline')).toBeInTheDocument();
    // Etiqueta de seccion y slot de pie renderizados.
    expect(screen.getByText('Gestión')).toHaveClass('nav-section');
    expect(screen.getByText('user-card')).toBeInTheDocument();
  });
});
