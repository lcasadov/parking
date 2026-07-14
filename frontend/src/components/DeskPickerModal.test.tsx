import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { DeskPickerModal } from './DeskPickerModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { floorPlanOf } from '../mocks/floorPlanFixtures';
import { renderWithProviders } from '../test/renderWithProviders';
import { todayIso } from '../utils/requests';
import type { FloorPlanDesk } from '../types/floorPlan';

const FLOOR_PLAN_URL = `${MSW_BASE}/floor-plan`;

// id (55) distinto de número (12) para verificar que se devuelve ambos por separado.
const freeDesk: FloorPlanDesk = {
  deskId: 55,
  deskNumber: 12,
  category: 'STANDARD',
  coordX: 20,
  coordY: 30,
  state: 'FREE',
};

const assignedDesk: FloorPlanDesk = {
  deskId: 56,
  deskNumber: 13,
  category: 'STANDARD',
  coordX: 40,
  coordY: 50,
  state: 'ASSIGNED',
};

function useFloorPlan(desks: FloorPlanDesk[]): void {
  server.use(http.get(FLOOR_PLAN_URL, () => HttpResponse.json(floorPlanOf(desks))));
}

describe('DeskPickerModal (floor plan as selector)', () => {
  it('should_invoke_onPick_and_close_without_posting_when_free_desk_clicked', async () => {
    useFloorPlan([freeDesk]);
    let postCalled = false;
    server.use(
      http.post(`${FLOOR_PLAN_URL}/desks/:deskId/request`, () => {
        postCalled = true;
        return HttpResponse.json({ requestId: 1, state: 'REQUESTED' }, { status: 201 });
      }),
    );
    const onPick = vi.fn();
    const onClose = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(
      <DeskPickerModal date={todayIso()} onPick={onPick} onClose={onClose} />,
    );

    const marker = await screen.findByRole('button', { name: /puesto 12|desk 12/i });
    await user.click(marker);

    expect(onPick).toHaveBeenCalledWith({ deskId: 55, deskNumber: 12 });
    expect(onClose).toHaveBeenCalledTimes(1);
    // En modo selector NO se crea la solicitud desde el plano (la crea el modal).
    expect(postCalled).toBe(false);
  });

  it('should_mark_selected_desk_with_class_aria_and_status_message', async () => {
    useFloorPlan([freeDesk]);
    const user = userEvent.setup();
    // onClose no desmonta (spy): el feedback visual permanece observable.
    renderWithProviders(
      <DeskPickerModal date={todayIso()} onPick={vi.fn()} onClose={vi.fn()} />,
    );

    const marker = await screen.findByRole('button', { name: /puesto 12|desk 12/i });
    await user.click(marker);

    await waitFor(() => expect(marker).toHaveClass('floor-marker-selected'));
    expect(marker).toHaveAttribute('aria-pressed', 'true');
    const status = screen.getByRole('status');
    expect(status).toHaveTextContent(/puesto 12 seleccionado|desk 12 selected/i);
  });

  it('should_not_select_a_non_free_desk', async () => {
    useFloorPlan([assignedDesk]);
    const onPick = vi.fn();
    renderWithProviders(
      <DeskPickerModal date={todayIso()} onPick={onPick} onClose={vi.fn()} />,
    );

    const marker = await screen.findByRole('button', { name: /puesto 13|desk 13/i });
    // Los puestos no libres están deshabilitados: no son elegibles.
    expect(marker).toBeDisabled();
    expect(onPick).not.toHaveBeenCalled();
  });
});
