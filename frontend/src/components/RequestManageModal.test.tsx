import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import {
  RequestManageModal,
  type ReassignTarget,
  type RequestManagePrefill,
  type SwapTarget,
} from './RequestManageModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';

const prefill: RequestManagePrefill = {
  requestId: 701,
  employeeName: 'Bob Beck',
  resourceLabel: 'P-01',
  date: '2026-05-14',
  resourceType: 'PARKING',
};

const reassignTargets: ReassignTarget[] = [{ resourceId: 2, label: 'P-02' }];
const swapTargets: SwapTarget[] = [
  { requestId: 900, resourceLabel: 'P-03', employeeName: 'Alice Andersson' },
];

// El botón de envío comparte texto con su botón segmentado ("Reasignar" /
// "Intercambiar"); se distingue por estar asociado al form del modal.
function submitButton(name: RegExp): HTMLElement {
  const button = screen
    .getAllByRole('button', { name })
    .find((el) => el.getAttribute('form') === 'request-manage-form');
  if (!button) {
    throw new Error(`No submit button found for ${name}`);
  }
  return button;
}

function renderModal(overrides: Partial<Parameters<typeof RequestManageModal>[0]> = {}) {
  const props = {
    prefill,
    reassignTargets,
    swapTargets,
    onDone: vi.fn(),
    onRequestCancel: vi.fn(),
    onClose: vi.fn(),
    ...overrides,
  };
  renderWithProviders(<RequestManageModal {...props} />);
  return props;
}

describe('RequestManageModal', () => {
  it('should_reassign_to_free_resource_when_submitting', async () => {
    let sentBody: Record<string, unknown> | null = null;
    server.use(
      http.post(`${MSW_BASE}/requests/admin/reassign`, async ({ request }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 701 });
      }),
    );
    const user = userEvent.setup();
    const props = renderModal();

    // Reasignar es la acción por defecto: se elige el recurso libre y se envía.
    await user.click(screen.getByRole('combobox', { name: /nuevo recurso|new resource/i }));
    await user.click(await screen.findByRole('option', { name: 'P-02' }));
    await user.click(submitButton(/^reasignar$|^reassign$/i));

    await waitFor(() => expect(props.onDone).toHaveBeenCalledTimes(1));
    expect(sentBody).toEqual({ requestId: 701, newResourceId: 2 });
  });

  it('should_swap_with_other_reservation_when_selected', async () => {
    let sentBody: Record<string, unknown> | null = null;
    server.use(
      http.post(`${MSW_BASE}/requests/admin/swap`, async ({ request }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ requestA: { id: 701 }, requestB: { id: 900 } });
      }),
    );
    const user = userEvent.setup();
    const props = renderModal();

    await user.click(screen.getByRole('button', { name: /intercambiar|swap/i }));
    await user.click(screen.getByRole('combobox', { name: /intercambiar con|swap with/i }));
    await user.click(await screen.findByRole('option', { name: /P-03 · Alice Andersson/i }));
    await user.click(submitButton(/^intercambiar$|^swap$/i));

    await waitFor(() => expect(props.onDone).toHaveBeenCalledTimes(1));
    expect(sentBody).toEqual({ requestIdA: 701, requestIdB: 900 });
  });

  it('should_delegate_to_cancel_flow_when_releasing', async () => {
    const user = userEvent.setup();
    const props = renderModal();

    await user.click(screen.getByRole('button', { name: /^liberar$|^release$/i }));
    await user.click(screen.getByRole('button', { name: /liberar reserva|release reservation/i }));

    expect(props.onRequestCancel).toHaveBeenCalledTimes(1);
  });

  it('should_show_empty_hint_when_no_free_resources', async () => {
    renderModal({ reassignTargets: [] });

    expect(
      screen.getByText(/no hay recursos libres|no free resources/i),
    ).toBeInTheDocument();
  });
});
