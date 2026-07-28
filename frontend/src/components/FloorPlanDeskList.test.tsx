import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { FloorPlanDeskList } from './FloorPlanDeskList';
import { renderWithProviders } from '../test/renderWithProviders';
import {
  defaultFloorPlan,
  floorDeskAssigned,
  floorDeskFree,
} from '../mocks/floorPlanFixtures';

function deskList(): HTMLElement {
  return screen.getByRole('list', { name: /^puestos$|^desks$/i });
}

describe('FloorPlanDeskList', () => {
  it('should_list_every_desk_by_number', () => {
    renderWithProviders(<FloorPlanDeskList desks={defaultFloorPlan.desks} />);
    expect(within(deskList()).getAllByRole('listitem')).toHaveLength(
      defaultFloorPlan.desks.length,
    );
  });

  it('should_filter_the_list_by_desk_number_search', async () => {
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanDeskList desks={defaultFloorPlan.desks} />);

    await user.type(screen.getByRole('searchbox', { name: /buscar puesto|search desk/i }), '3');

    const items = within(deskList()).getAllByRole('listitem');
    expect(items).toHaveLength(1);
    expect(items[0]).toHaveTextContent('3');
  });

  it('should_select_a_free_desk_when_actionable', async () => {
    const onSelect = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(
      <FloorPlanDeskList
        desks={[floorDeskFree, floorDeskAssigned]}
        onSelect={onSelect}
        actionLabelKey="floorPlan.side.assignAction"
      />,
    );

    // Solo el puesto libre (nº 1) es un botón accionable.
    await user.click(screen.getByRole('button', { name: /asignar el nº 1|assign no\. 1/i }));
    expect(onSelect).toHaveBeenCalledWith(floorDeskFree);
  });

  it('should_not_make_non_free_desks_actionable', () => {
    const onSelect = vi.fn();
    renderWithProviders(
      <FloorPlanDeskList desks={[floorDeskAssigned]} onSelect={onSelect} />,
    );

    // El puesto ocupado (nº 2) no se expone como botón accionable.
    expect(
      screen.queryByRole('button', { name: /solicitar el nº 2|request no\. 2/i }),
    ).not.toBeInTheDocument();
  });
});
