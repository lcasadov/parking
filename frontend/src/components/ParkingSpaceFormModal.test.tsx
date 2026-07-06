import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { ParkingSpaceFormModal } from './ParkingSpaceFormModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { spaceP01 } from '../mocks/parkingSpaceFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const SPACES_URL = `${MSW_BASE}/parking-spaces`;

function conflict() {
  return HttpResponse.json(
    {
      error: 'conflict',
      message: 'Duplicate label',
      fields: { label: 'already exists' },
      timestamp: new Date().toISOString(),
    },
    { status: 409 },
  );
}

function badRequest() {
  return HttpResponse.json(
    {
      error: 'validation',
      message: 'Invalid',
      fields: { label: 'label too long' },
      timestamp: new Date().toISOString(),
    },
    { status: 400 },
  );
}

describe('ParkingSpaceFormModal', () => {
  it('should_create_space_when_form_is_valid', async () => {
    let sentBody: Record<string, unknown> | null = null;
    server.use(
      http.post(SPACES_URL, async ({ request }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...spaceP01, id: 50 }, { status: 201 });
      }),
    );
    const onSaved = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpaceFormModal onClose={vi.fn()} onSaved={onSaved} />);

    await user.type(screen.getByLabelText(/etiqueta de la plaza|space label/i), 'P-09');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => {
      expect(onSaved).toHaveBeenCalledTimes(1);
    });
    expect(sentBody).toMatchObject({ label: 'P-09', active: true });
  });

  it('should_show_label_error_when_create_returns_409_duplicate_label', async () => {
    server.use(http.post(SPACES_URL, () => conflict()));
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpaceFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.type(screen.getByLabelText(/etiqueta de la plaza|space label/i), 'P-01');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findByText(/ya existe una plaza con esa etiqueta|space with that label already exists/i),
    ).toBeInTheDocument();
  });

  it('should_show_required_error_when_submitting_empty_label', async () => {
    let called = false;
    server.use(
      http.post(SPACES_URL, () => {
        called = true;
        return HttpResponse.json(spaceP01, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpaceFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findByText(/este campo es obligatorio|this field is required/i),
    ).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it('should_show_field_error_when_create_returns_400_validation', async () => {
    server.use(http.post(SPACES_URL, () => badRequest()));
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpaceFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.type(screen.getByLabelText(/etiqueta de la plaza|space label/i), 'P-TOOLONG-LABEL-9999');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(await screen.findByText(/label too long/i)).toBeInTheDocument();
  });

  it('should_update_space_and_toggle_active_when_editing', async () => {
    let putId: string | undefined;
    let putBody: Record<string, unknown> | null = null;
    server.use(
      http.put(`${SPACES_URL}/:id`, async ({ request, params }) => {
        putId = params.id as string;
        putBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...spaceP01, ...putBody });
      }),
    );
    const onSaved = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(
      <ParkingSpaceFormModal space={spaceP01} onClose={vi.fn()} onSaved={onSaved} />,
    );

    // spaceP01.active === true; el toggle lo desactiva.
    await user.click(screen.getByRole('switch', { name: /plaza activa|active space/i }));
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => {
      expect(onSaved).toHaveBeenCalledTimes(1);
    });
    expect(putId).toBe(String(spaceP01.id));
    expect(putBody).toMatchObject({ label: 'P-01', active: false });
  });
});
