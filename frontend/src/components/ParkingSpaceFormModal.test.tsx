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
const NUMBER_LABEL = /número de la plaza|space number/i;
const FLOOR_LABEL = /^planta$|^floor$/i;
const SAVE_BUTTON = /guardar|save/i;

function conflict() {
  return HttpResponse.json(
    {
      error: 'conflict',
      message: 'Duplicate number',
      fields: { number: 'already exists' },
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
      fields: { number: 'number out of range' },
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

    await user.type(screen.getByLabelText(NUMBER_LABEL), '1009');
    await user.click(screen.getByRole('button', { name: SAVE_BUTTON }));

    await waitFor(() => {
      expect(onSaved).toHaveBeenCalledTimes(1);
    });
    expect(sentBody).toMatchObject({ number: 1009, active: true });
  });

  it('should_show_derived_floor_readonly_and_recalculate_while_typing', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpaceFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    const floorField = screen.getByLabelText(FLOOR_LABEL);
    expect(floorField).toHaveAttribute('readonly');
    expect(floorField).toHaveValue('');

    await user.type(screen.getByLabelText(NUMBER_LABEL), '2005');
    expect(floorField).toHaveValue('2');

    await user.clear(screen.getByLabelText(NUMBER_LABEL));
    await user.type(screen.getByLabelText(NUMBER_LABEL), '3012');
    expect(floorField).toHaveValue('3');
  });

  it('should_show_error_when_number_is_below_minimum', async () => {
    let called = false;
    server.use(
      http.post(SPACES_URL, () => {
        called = true;
        return HttpResponse.json(spaceP01, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpaceFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.type(screen.getByLabelText(NUMBER_LABEL), '5');
    await user.click(screen.getByRole('button', { name: SAVE_BUTTON }));

    expect(
      await screen.findByText(/mayor o igual que 1000|greater than or equal to 1000/i),
    ).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it('should_show_number_error_when_create_returns_409_duplicate', async () => {
    server.use(http.post(SPACES_URL, () => conflict()));
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpaceFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.type(screen.getByLabelText(NUMBER_LABEL), '1001');
    await user.click(screen.getByRole('button', { name: SAVE_BUTTON }));

    expect(
      await screen.findByText(/ya existe una plaza con ese número|space with that number already exists/i),
    ).toBeInTheDocument();
  });

  it('should_show_required_error_when_submitting_empty_number', async () => {
    let called = false;
    server.use(
      http.post(SPACES_URL, () => {
        called = true;
        return HttpResponse.json(spaceP01, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpaceFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: SAVE_BUTTON }));

    expect(
      await screen.findByText(/este campo es obligatorio|this field is required/i),
    ).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it('should_show_field_error_when_create_returns_400_validation', async () => {
    server.use(http.post(SPACES_URL, () => badRequest()));
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpaceFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.type(screen.getByLabelText(NUMBER_LABEL), '1001');
    await user.click(screen.getByRole('button', { name: SAVE_BUTTON }));

    expect(await screen.findByText(/number out of range/i)).toBeInTheDocument();
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

    // El numero se precarga y la planta derivada se muestra de solo lectura.
    expect(screen.getByLabelText(NUMBER_LABEL)).toHaveValue(1001);
    expect(screen.getByLabelText(FLOOR_LABEL)).toHaveValue('1');

    // spaceP01.active === true; el toggle lo desactiva.
    await user.click(screen.getByRole('switch', { name: /plaza activa|active space/i }));
    await user.click(screen.getByRole('button', { name: SAVE_BUTTON }));

    await waitFor(() => {
      expect(onSaved).toHaveBeenCalledTimes(1);
    });
    expect(putId).toBe(String(spaceP01.id));
    expect(putBody).toMatchObject({ number: 1001, active: false });
  });
});
