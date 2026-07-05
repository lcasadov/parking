import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { FloorPlanSidePanel } from './FloorPlanSidePanel';
import { renderWithProviders } from '../test/renderWithProviders';
import { defaultFloorPlan } from '../mocks/floorPlanFixtures';

describe('FloorPlanSidePanel', () => {
  it('should_list_every_desk_by_number', () => {
    renderWithProviders(<FloorPlanSidePanel desks={defaultFloorPlan.desks} />);
    const list = screen.getByRole('list');
    expect(within(list).getAllByRole('listitem')).toHaveLength(defaultFloorPlan.desks.length);
  });

  it('should_filter_the_list_by_desk_number_search', async () => {
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanSidePanel desks={defaultFloorPlan.desks} />);

    await user.type(screen.getByRole('searchbox', { name: /buscar puesto|search desk/i }), '3');

    const list = screen.getByRole('list');
    const items = within(list).getAllByRole('listitem');
    expect(items).toHaveLength(1);
    expect(items[0]).toHaveTextContent('3');
  });

  it('should_show_status_pills_in_admin_variant', () => {
    renderWithProviders(<FloorPlanSidePanel desks={defaultFloorPlan.desks} showStatus />);
    expect(screen.getByText(/ocupación del día|occupancy for the day/i)).toBeInTheDocument();
    // At least one occupancy pill rendered (assigned desk).
    expect(screen.getByText(/ocupado|occupied/i)).toBeInTheDocument();
  });

  it('should_show_empty_message_when_no_desk_matches', async () => {
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanSidePanel desks={defaultFloorPlan.desks} />);

    await user.type(screen.getByRole('searchbox', { name: /buscar puesto|search desk/i }), '999');

    expect(screen.getByText(/ningún puesto coincide|no desk matches/i)).toBeInTheDocument();
  });
});
