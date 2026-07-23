import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { FloorPlanSidePanel } from './FloorPlanSidePanel';
import { renderWithProviders } from '../test/renderWithProviders';
import { defaultFloorPlan, floorDeskExecutive } from '../mocks/floorPlanFixtures';

// El listado de puestos es el único role="list" (la cuadrícula de contadores usa
// <dl>, sin role list); se resuelve por su aria-label.
function deskList(): HTMLElement {
  return screen.getByRole('list', { name: /^puestos$|^desks$/i });
}

describe('FloorPlanSidePanel', () => {
  it('should_list_every_desk_by_number', () => {
    renderWithProviders(<FloorPlanSidePanel desks={defaultFloorPlan.desks} />);
    expect(within(deskList()).getAllByRole('listitem')).toHaveLength(
      defaultFloorPlan.desks.length,
    );
  });

  it('should_filter_the_list_by_desk_number_search', async () => {
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanSidePanel desks={defaultFloorPlan.desks} />);

    await user.type(screen.getByRole('searchbox', { name: /buscar puesto|search desk/i }), '3');

    const items = within(deskList()).getAllByRole('listitem');
    expect(items).toHaveLength(1);
    expect(items[0]).toHaveTextContent('3');
  });

  it('should_render_the_occupancy_title', () => {
    renderWithProviders(<FloorPlanSidePanel desks={defaultFloorPlan.desks} />);
    expect(
      screen.getByRole('heading', { name: /ocupación del día|occupancy for the day/i }),
    ).toBeInTheDocument();
  });

  it('should_render_a_serif_counters_grid_by_state', () => {
    renderWithProviders(<FloorPlanSidePanel desks={defaultFloorPlan.desks} />);
    const counters = screen.getByTestId('occupancy-counters');
    // defaultFloorPlan: 1 ASSIGNED + 1 MINE ⇒ ocupado = 2.
    const occupied = within(counters)
      .getByText(/ocupado|occupied/i)
      .closest('.plano-occ-cell');
    expect(occupied).not.toBeNull();
    expect(occupied).toHaveTextContent('2');
    expect(within(counters).getByText('2')).toHaveClass('plano-occ-num');
  });

  it('should_show_a_state_pill_and_dot_on_every_row', () => {
    renderWithProviders(<FloorPlanSidePanel desks={[floorDeskExecutive]} />);
    const row = within(deskList()).getByRole('listitem');
    // Subtítulo = categoría (el origen de datos de floor-plan no incluye titular).
    expect(within(row).getByText(/dirección|executive/i)).toBeInTheDocument();
    // Pill de estado a la derecha de la fila.
    expect(within(row).getByText(/libre|free/i)).toBeInTheDocument();
    // Dot de estado con el color del marcador correspondiente.
    expect(row.querySelector('.plano-side-dot')).toHaveClass('floor-marker-free');
  });

  it('should_render_focusable_rows', () => {
    renderWithProviders(<FloorPlanSidePanel desks={defaultFloorPlan.desks} />);
    const rows = within(deskList()).getAllByRole('button');
    expect(rows).toHaveLength(defaultFloorPlan.desks.length);
  });

  it('should_show_empty_message_when_no_desk_matches', async () => {
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanSidePanel desks={defaultFloorPlan.desks} />);

    await user.type(screen.getByRole('searchbox', { name: /buscar puesto|search desk/i }), '999');

    expect(screen.getByText(/ningún puesto coincide|no desk matches/i)).toBeInTheDocument();
  });
});
