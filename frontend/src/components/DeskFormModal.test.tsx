import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { DeskFormModal } from './DeskFormModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { deskStandard } from '../mocks/deskFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const DESKS_URL = `${MSW_BASE}/desks`;

function apiError(error: string): Record<string, string> {
  return { error, message: error, timestamp: '2026-03-01T09:00:00Z' };
}

describe('DeskFormModal', () => {
  it('should_create_desk_when_number_in_range_and_unique', async () => {
    let sent: Record<string, unknown> | null = null;
    server.use(
      http.post(DESKS_URL, async ({ request }) => {
        sent = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...deskStandard, ...sent, id: 99 }, { status: 201 });
      }),
    );
    const onSaved = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<DeskFormModal onClose={vi.fn()} onSaved={onSaved} />);

    await user.type(screen.getByLabelText(/número del puesto|desk number/i), '7');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
    expect(sent).toMatchObject({ number: 7, category: 'STANDARD', active: true });
  });

  it('should_show_range_error_when_number_out_of_range', async () => {
    let posted = false;
    server.use(
      http.post(DESKS_URL, () => {
        posted = true;
        return HttpResponse.json({ ...deskStandard, id: 99 }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<DeskFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.type(screen.getByLabelText(/número del puesto|desk number/i), '70');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findByText(/debe estar entre 1 y 65|must be between 1 and 65/i),
    ).toBeInTheDocument();
    expect(posted).toBe(false);
  });

  it('should_show_duplicate_error_when_server_returns_409', async () => {
    server.use(
      http.post(DESKS_URL, () => HttpResponse.json(apiError('DESK_NUMBER_TAKEN'), { status: 409 })),
    );
    const user = userEvent.setup();
    renderWithProviders(<DeskFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.type(screen.getByLabelText(/número del puesto|desk number/i), '5');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findByText(/ya existe un puesto con ese número|desk with that number already exists/i),
    ).toBeInTheDocument();
  });

  it('should_return_400_when_desk_number_out_of_range_from_server', async () => {
    // El cliente valida el rango, pero además un 400 del servidor se mapea a rango.
    server.use(
      http.post(DESKS_URL, () => HttpResponse.json(apiError('OUT_OF_RANGE'), { status: 400 })),
    );
    const user = userEvent.setup();
    renderWithProviders(<DeskFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.type(screen.getByLabelText(/número del puesto|desk number/i), '10');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findByText(/debe estar entre 1 y 65|must be between 1 and 65/i),
    ).toBeInTheDocument();
  });

  it('should_send_executive_when_category_changed', async () => {
    let sent: Record<string, unknown> | null = null;
    server.use(
      http.post(DESKS_URL, async ({ request }) => {
        sent = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...deskStandard, ...sent, id: 99 }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<DeskFormModal onClose={vi.fn()} onSaved={vi.fn()} />);

    await user.type(screen.getByLabelText(/número del puesto|desk number/i), '9');
    await user.selectOptions(
      screen.getByLabelText(/categoría|category/i),
      'EXECUTIVE',
    );
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => expect(sent).not.toBeNull());
    expect(sent).toMatchObject({ number: 9, category: 'EXECUTIVE' });
  });

  it('should_update_category_when_admin_edits_existing_desk', async () => {
    let sentBody: Record<string, unknown> | null = null;
    server.use(
      http.put(`${DESKS_URL}/:id`, async ({ request, params }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...deskStandard, ...sentBody, id: Number(params.id) });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(
      <DeskFormModal desk={deskStandard} onClose={vi.fn()} onSaved={vi.fn()} />,
    );

    const dialog = within(screen.getByRole('dialog'));
    expect(dialog.getByLabelText(/número del puesto|desk number/i)).toHaveValue(1);
    await user.selectOptions(dialog.getByLabelText(/categoría|category/i), 'EXECUTIVE');
    await user.click(dialog.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => expect(sentBody).not.toBeNull());
    expect(sentBody).toMatchObject({ category: 'EXECUTIVE' });
  });
});
