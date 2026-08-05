import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PersonSelect, type PersonOption } from './PersonSelect';
import { renderWithProviders } from '../test/renderWithProviders';

const PEOPLE: PersonOption[] = [
  { id: 10, name: 'Alice Employee', sub: 'IT' },
  { id: 20, name: 'Bob Beck', sub: 'Ops' },
];

describe('PersonSelect', () => {
  it('should_show_placeholder_when_no_value', () => {
    renderWithProviders(
      <PersonSelect people={PEOPLE} value={null} onChange={vi.fn()} placeholder="Elegir" searchPlaceholder="Buscar…" emptyLabel="Sin resultados" ariaLabel="Persona" />,
    );
    expect(screen.getByRole('button', { name: /persona/i })).toHaveTextContent('Elegir');
  });

  it('should_open_filter_and_select_a_person', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    renderWithProviders(
      <PersonSelect people={PEOPLE} value={null} onChange={onChange} placeholder="Elegir" searchPlaceholder="Buscar…" emptyLabel="Sin resultados" ariaLabel="Persona" />,
    );

    await user.click(screen.getByRole('button', { name: /persona/i }));
    // Al abrir, el listado muestra las opciones; seleccionamos una.
    await user.click(await screen.findByRole('option', { name: /bob beck/i }));

    expect(onChange).toHaveBeenCalledWith(20);
  });

  it('should_render_selected_name_in_trigger', () => {
    renderWithProviders(
      <PersonSelect people={PEOPLE} value={10} onChange={vi.fn()} placeholder="Elegir" searchPlaceholder="Buscar…" emptyLabel="Sin resultados" ariaLabel="Persona" />,
    );
    expect(screen.getByRole('button', { name: /persona/i })).toHaveTextContent('Alice Employee');
  });
});
