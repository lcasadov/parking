import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Toolbar } from './Toolbar';

describe('Toolbar', () => {
  it('should_render_children_inside_a_labelled_search_region', () => {
    render(
      <Toolbar ariaLabel="Filtros de puestos">
        <button type="button">Nuevo</button>
      </Toolbar>,
    );
    const region = screen.getByRole('search', { name: 'Filtros de puestos' });
    expect(region).toHaveClass('toolbar');
    expect(screen.getByRole('button', { name: 'Nuevo' })).toBeInTheDocument();
  });
});
