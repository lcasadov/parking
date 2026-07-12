import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { DesksPage } from './DesksPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import {
  deskExecutive,
  deskInactive,
  deskStandard,
  pageOfDesks,
} from '../mocks/deskFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const DESKS_URL = `${MSW_BASE}/desks`;

describe('DesksPage', () => {
  it('should_render_desk_rows_when_list_loads', async () => {
    renderWithProviders(<DesksPage />);

    expect(await screen.findByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('should_show_executive_badge_when_desk_is_executive', async () => {
    server.use(http.get(DESKS_URL, () => HttpResponse.json(pageOfDesks([deskExecutive]))));
    renderWithProviders(<DesksPage />);

    const badge = await screen.findByText(/dirección|executive/i);
    expect(badge).toHaveClass('pill-blue');
  });

  it('should_show_standard_badge_when_desk_is_standard', async () => {
    server.use(http.get(DESKS_URL, () => HttpResponse.json(pageOfDesks([deskStandard]))));
    renderWithProviders(<DesksPage />);

    const badge = await screen.findByText(/estándar|standard/i);
    expect(badge).toHaveClass('pill-gray');
  });

  it('should_open_create_form_when_clicking_new', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DesksPage />);
    await screen.findByText('1');

    await user.click(screen.getByRole('button', { name: /nuevo puesto|new desk/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/nuevo puesto|new desk/i)).toBeInTheDocument();
  });

  it('should_open_edit_form_prefilled_when_clicking_edit', async () => {
    const user = userEvent.setup();
    renderWithProviders(<DesksPage />);
    const row = (await screen.findByText('1')).closest('tr') as HTMLElement;

    await user.click(within(row).getByRole('button', { name: /editar|edit/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByLabelText(/número del puesto|desk number/i)).toHaveValue(1);
  });

  it('should_deactivate_desk_when_toggling_active', async () => {
    let sentBody: Record<string, unknown> | null = null;
    server.use(
      http.get(DESKS_URL, () => HttpResponse.json(pageOfDesks([deskStandard]))),
      http.put(`${DESKS_URL}/:id`, async ({ request, params }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...deskStandard, ...sentBody, id: Number(params.id) });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<DesksPage />);
    const row = (await screen.findByText('1')).closest('tr') as HTMLElement;

    await user.click(within(row).getByRole('button', { name: /desactivar|deactivate/i }));

    await waitFor(() => expect(sentBody).not.toBeNull());
    expect(sentBody).toMatchObject({ active: false, number: 1 });
  });

  it('should_activate_desk_when_toggling_inactive', async () => {
    let sentBody: Record<string, unknown> | null = null;
    server.use(
      http.get(DESKS_URL, () => HttpResponse.json(pageOfDesks([deskInactive]))),
      http.put(`${DESKS_URL}/:id`, async ({ request, params }) => {
        sentBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ...deskInactive, ...sentBody, id: Number(params.id) });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<DesksPage />);
    const row = (await screen.findByText('3')).closest('tr') as HTMLElement;

    await user.click(within(row).getByRole('button', { name: /activar|activate/i }));

    await waitFor(() => expect(sentBody).not.toBeNull());
    expect(sentBody).toMatchObject({ active: true });
  });

  it('should_filter_visible_rows_when_typing_in_search_box', async () => {
    server.use(
      http.get(DESKS_URL, () => HttpResponse.json(pageOfDesks([deskStandard, deskExecutive]))),
    );
    const user = userEvent.setup();
    renderWithProviders(<DesksPage />);
    await screen.findByText('1');

    await user.type(screen.getByRole('searchbox'), String(deskExecutive.number));

    await waitFor(() => {
      expect(screen.queryByText('1')).not.toBeInTheDocument();
    });
    expect(screen.getByText(String(deskExecutive.number))).toBeInTheDocument();
  });

  it('should_request_inactive_filter_when_selecting_inactive', async () => {
    let receivedActive: string | null = null;
    server.use(
      http.get(DESKS_URL, ({ request }) => {
        receivedActive = new URL(request.url).searchParams.get('active');
        return HttpResponse.json(pageOfDesks([deskInactive]));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<DesksPage />);
    await screen.findByText('3');

    await user.selectOptions(
      screen.getByLabelText(/filtrar por estado|filter by status/i),
      'inactive',
    );

    await waitFor(() => expect(receivedActive).toBe('false'));
  });

  it('should_show_error_message_when_list_request_fails', async () => {
    server.use(
      http.get(DESKS_URL, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: new Date().toISOString() },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<DesksPage />);

    expect(
      await screen.findByText(/no se pudieron cargar los puestos|could not load desks/i),
    ).toBeInTheDocument();
  });
});
