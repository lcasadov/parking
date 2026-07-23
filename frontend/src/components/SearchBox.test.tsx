import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SearchBox } from './SearchBox';

describe('SearchBox', () => {
  it('should_render_search_box_with_decorative_icon_and_accessible_label', () => {
    const { container } = render(
      <SearchBox value="" onValueChange={() => {}} label="Buscar puestos" />,
    );
    expect(container.querySelector('.search-box')).toBeInTheDocument();
    expect(container.querySelector('i.ti.ti-search')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByRole('searchbox', { name: 'Buscar puestos' })).toBeInTheDocument();
  });

  it('should_emit_typed_value', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<SearchBox value="" onValueChange={onValueChange} label="Buscar" />);
    await user.type(screen.getByRole('searchbox'), 'a');
    expect(onValueChange).toHaveBeenCalledWith('a');
  });

  it('should_forward_placeholder', () => {
    render(<SearchBox value="" onValueChange={() => {}} label="Buscar" placeholder="Nombre…" />);
    expect(screen.getByPlaceholderText('Nombre…')).toBeInTheDocument();
  });
});
