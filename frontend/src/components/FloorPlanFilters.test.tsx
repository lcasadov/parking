import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { FloorPlanFilters } from './FloorPlanFilters';
import { renderWithProviders } from '../test/renderWithProviders';
import {
  floorDeskAssigned,
  floorDeskExecutive,
  floorDeskFree,
  floorDeskMine,
} from '../mocks/floorPlanFixtures';

const desks = [floorDeskFree, floorDeskExecutive, floorDeskMine, floorDeskAssigned];

describe('FloorPlanFilters', () => {
  it('should_render_a_count_per_state_chip', () => {
    renderWithProviders(<FloorPlanFilters desks={desks} active={null} onToggle={vi.fn()} />);
    // Two FREE desks (floorDeskFree + floorDeskExecutive is FREE).
    const free = screen.getByRole('button', { name: /libre|free/i });
    expect(free).toHaveTextContent('2');
    const mine = screen.getByRole('button', { name: /mi puesto|my desk/i });
    expect(mine).toHaveTextContent('1');
  });

  it('should_count_executive_desks_in_the_direction_chip', () => {
    renderWithProviders(<FloorPlanFilters desks={desks} active={null} onToggle={vi.fn()} />);
    const exec = screen.getByRole('button', { name: /dirección|executive/i });
    expect(exec).toHaveTextContent('1');
  });

  it('should_toggle_a_filter_when_a_chip_is_clicked', async () => {
    const onToggle = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanFilters desks={desks} active={null} onToggle={onToggle} />);

    await user.click(screen.getByRole('button', { name: /libre|free/i }));
    expect(onToggle).toHaveBeenCalledWith('FREE');
  });

  it('should_mark_the_active_chip_as_pressed', () => {
    renderWithProviders(<FloorPlanFilters desks={desks} active="MINE" onToggle={vi.fn()} />);
    expect(screen.getByRole('button', { name: /mi puesto|my desk/i })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });
});
