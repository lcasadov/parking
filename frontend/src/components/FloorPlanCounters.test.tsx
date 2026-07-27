import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FloorPlanCounters } from './FloorPlanCounters';
import { renderWithProviders } from '../test/renderWithProviders';
import {
  floorDeskAssigned,
  floorDeskFree,
  floorDeskReleased,
  floorDeskRequested,
} from '../mocks/floorPlanFixtures';

describe('FloorPlanCounters', () => {
  it('should_count_desks_by_state', () => {
    renderWithProviders(
      <FloorPlanCounters
        desks={[floorDeskFree, floorDeskAssigned, floorDeskReleased, floorDeskRequested]}
      />,
    );

    const counters = within(screen.getByTestId('occupancy-counters'));
    // Cada estado aporta 1: ocupado, libre, liberado, solicitado.
    expect(counters.getAllByText('1')).toHaveLength(4);
  });

  it('should_label_each_counter_by_state', () => {
    renderWithProviders(<FloorPlanCounters desks={[floorDeskFree]} />);

    const counters = within(screen.getByTestId('occupancy-counters'));
    expect(counters.getByText(/libre|free/i)).toBeInTheDocument();
    expect(counters.getByText(/ocupad|occupied/i)).toBeInTheDocument();
  });
});
