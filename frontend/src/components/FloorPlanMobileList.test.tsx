import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { FloorPlanMobileList } from './FloorPlanMobileList';
import { renderWithProviders } from '../test/renderWithProviders';
import {
  floorDeskAssigned,
  floorDeskExecutive,
  floorDeskFree,
} from '../mocks/floorPlanFixtures';

describe('FloorPlanMobileList', () => {
  it('should_list_only_free_desks_with_a_request_button', () => {
    renderWithProviders(
      <FloorPlanMobileList
        desks={[floorDeskFree, floorDeskAssigned, floorDeskExecutive]}
        pending={false}
        onRequest={vi.fn()}
      />,
    );
    // Two FREE desks (floorDeskFree + executive is FREE); assigned excluded.
    const buttons = screen.getAllByRole('button', { name: /solicitar|request/i });
    expect(buttons).toHaveLength(2);
  });

  it('should_call_onRequest_with_the_desk_when_row_button_clicked', async () => {
    const onRequest = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(
      <FloorPlanMobileList desks={[floorDeskFree]} pending={false} onRequest={onRequest} />,
    );

    await user.click(screen.getByRole('button', { name: /solicitar|request/i }));
    expect(onRequest).toHaveBeenCalledWith(floorDeskFree);
  });

  it('should_disable_buttons_while_a_request_is_pending', () => {
    renderWithProviders(
      <FloorPlanMobileList desks={[floorDeskFree]} pending onRequest={vi.fn()} />,
    );
    expect(screen.getByRole('button', { name: /solicitar|request/i })).toBeDisabled();
  });

  it('should_show_empty_message_when_no_free_desks', () => {
    renderWithProviders(
      <FloorPlanMobileList desks={[floorDeskAssigned]} pending={false} onRequest={vi.fn()} />,
    );
    expect(screen.getByText(/no hay puestos libres|no free desks/i)).toBeInTheDocument();
  });
});
