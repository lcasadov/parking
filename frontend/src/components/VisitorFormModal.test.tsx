import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { VisitorFormModal } from './VisitorFormModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { visitorCarla } from '../mocks/visitorFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const VISITORS_URL = `${MSW_BASE}/visitors`;

function noop(): void {
  // sin efecto: callback de cierre en tests que no lo verifican.
}

describe('VisitorFormModal', () => {
  it('should_create_visitor_when_nationalId_is_unique', async () => {
    const user = userEvent.setup();
    let sentBody: Record<string, unknown> | null = null;
    let saved = false;
    server.use(
      http.post(VISITORS_URL, async ({ request }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...visitorCarla, ...sentBody, id: 99 }, { status: 201 });
      }),
    );
    renderWithProviders(<VisitorFormModal onClose={noop} onSaved={() => (saved = true)} />);

    await user.type(screen.getByLabelText(/^nombre$|^first name$/i), 'Nora');
    await user.type(screen.getByLabelText(/^apellidos$|^last name$/i), 'Nieto');
    await user.type(screen.getByLabelText(/dni|document/i), '99999999R');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => expect(saved).toBe(true));
    expect(sentBody).toMatchObject({
      firstName: 'Nora',
      lastName: 'Nieto',
      nationalId: '99999999R',
    });
  });

  it('should_show_inline_error_when_nationalId_is_duplicate_409', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(VISITORS_URL, () =>
        HttpResponse.json(
          {
            error: 'CONFLICT',
            message: 'duplicate',
            fields: { nationalId: 'exists' },
            timestamp: '2026-03-01T09:00:00Z',
          },
          { status: 409 },
        ),
      ),
    );
    renderWithProviders(<VisitorFormModal onClose={noop} onSaved={noop} />);

    await user.type(screen.getByLabelText(/^nombre$|^first name$/i), 'Nora');
    await user.type(screen.getByLabelText(/^apellidos$|^last name$/i), 'Nieto');
    await user.type(screen.getByLabelText(/dni|document/i), '12345678Z');
    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findByText(
        /ya existe un visitante con ese documento|a visitor with that document already exists/i,
      ),
    ).toBeInTheDocument();
  });

  it('should_show_required_errors_when_submitting_without_fields', async () => {
    const user = userEvent.setup();
    let posted = false;
    server.use(
      http.post(VISITORS_URL, () => {
        posted = true;
        return HttpResponse.json(visitorCarla, { status: 201 });
      }),
    );
    renderWithProviders(<VisitorFormModal onClose={noop} onSaved={noop} />);

    await user.click(screen.getByRole('button', { name: /guardar|save/i }));

    expect(
      await screen.findAllByText(/este campo es obligatorio|this field is required/i),
    ).toHaveLength(3);
    expect(posted).toBe(false);
  });

  it('should_update_visitor_when_editing_existing_card', async () => {
    const user = userEvent.setup();
    let putId: string | undefined;
    let saved = false;
    server.use(
      http.put(`${VISITORS_URL}/:id`, async ({ request, params }) => {
        putId = params.id as string;
        const body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...visitorCarla, ...body, id: Number(params.id) });
      }),
    );
    renderWithProviders(
      <VisitorFormModal visitor={visitorCarla} onClose={noop} onSaved={() => (saved = true)} />,
    );

    const dialog = within(screen.getByRole('dialog'));
    expect(dialog.getByLabelText(/^nombre$|^first name$/i)).toHaveValue('Carla');
    await user.clear(dialog.getByLabelText(/empresa|company/i));
    await user.type(dialog.getByLabelText(/empresa|company/i), 'Nueva Empresa');
    await user.click(dialog.getByRole('button', { name: /guardar|save/i }));

    await waitFor(() => expect(saved).toBe(true));
    expect(putId).toBe(String(visitorCarla.id));
  });
});
