import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { CreateRequestModal } from './CreateRequestModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { requestPending1 } from '../mocks/requestFixtures';
import { renderWithProviders } from '../test/renderWithProviders';
import { todayIso } from '../utils/requests';

const REQUESTS_URL = `${MSW_BASE}/requests`;

// Captura los cuerpos de cada POST /requests para comprobar los resourceType.
function captureRequestBodies(): { bodies: Array<{ resourceType?: string }> } {
  const captured = { bodies: [] as Array<{ resourceType?: string }> };
  server.use(
    http.post(REQUESTS_URL, async ({ request }) => {
      const body = (await request.json()) as { requestedDate: string; resourceType?: string };
      captured.bodies.push(body);
      return HttpResponse.json(
        { ...requestPending1, id: 999, requestedDate: body.requestedDate },
        { status: 201 },
      );
    }),
  );
  return captured;
}

async function fillDate(): Promise<void> {
  const user = userEvent.setup();
  await user.clear(screen.getByLabelText(/fecha de la solicitud|request date/i));
  await user.type(screen.getByLabelText(/fecha de la solicitud|request date/i), todayIso());
}

async function submit(): Promise<void> {
  const user = userEvent.setup();
  const dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('button', { name: /enviar solicitud|submit request/i }));
}

describe('CreateRequestModal (unified request)', () => {
  it('should_post_only_parking_request_when_only_parking_selected', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(1);
    expect(captured.bodies[0].resourceType).toBe('PARKING');
  });

  it('should_post_desk_request_when_only_desk_selected', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await user.click(screen.getByLabelText(/plaza de parking|parking space/i));
    await user.click(screen.getByLabelText(/puesto de oficina|office desk/i));
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(1);
    expect(captured.bodies[0].resourceType).toBe('DESK');
  });

  it('should_post_both_requests_when_parking_and_desk_selected', async () => {
    const captured = captureRequestBodies();
    const onCreated = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={onCreated} />);

    await fillDate();
    await user.click(screen.getByLabelText(/puesto de oficina|office desk/i));
    await submit();

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1));
    expect(captured.bodies).toHaveLength(2);
    const types = captured.bodies.map((b) => b.resourceType).sort();
    expect(types).toEqual(['DESK', 'PARKING']);
  });

  it('should_show_error_when_no_resource_selected', async () => {
    const captured = captureRequestBodies();
    const user = userEvent.setup();
    renderWithProviders(<CreateRequestModal onClose={vi.fn()} onCreated={vi.fn()} />);

    await fillDate();
    await user.click(screen.getByLabelText(/plaza de parking|parking space/i));
    await submit();

    expect(
      await screen.findByText(/al menos un recurso|at least one resource/i),
    ).toBeInTheDocument();
    expect(captured.bodies).toHaveLength(0);
  });
});
