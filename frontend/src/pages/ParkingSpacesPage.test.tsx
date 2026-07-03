import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { ParkingSpacesPage } from './ParkingSpacesPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { spaceP01, spaceP02, pageOfSpaces } from '../mocks/parkingSpaceFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const SPACES_URL = `${MSW_BASE}/parking-spaces`;

describe('ParkingSpacesPage', () => {
  it('should_render_space_rows_when_list_loads', async () => {
    renderWithProviders(<ParkingSpacesPage />);

    expect(await screen.findByText('P-01')).toBeInTheDocument();
    expect(screen.getByText('P-02')).toBeInTheDocument();
  });

  it('should_request_active_filter_when_selecting_active', async () => {
    let receivedActive: string | null = null;
    server.use(
      http.get(SPACES_URL, ({ request }) => {
        receivedActive = new URL(request.url).searchParams.get('active');
        const all = [spaceP01, spaceP02];
        const filtered =
          receivedActive === null ? all : all.filter((s) => String(s.active) === receivedActive);
        return HttpResponse.json(pageOfSpaces(filtered));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpacesPage />);
    await screen.findByText('P-01');

    await user.selectOptions(screen.getByLabelText(/filtrar por estado|filter by status/i), 'active');

    await waitFor(() => {
      expect(receivedActive).toBe('true');
    });
    await waitFor(() => {
      expect(screen.queryByText('P-02')).not.toBeInTheDocument();
    });
    expect(screen.getByText('P-01')).toBeInTheDocument();
  });

  it('should_open_edit_form_prefilled_when_clicking_edit', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpacesPage />);
    const row = (await screen.findByText('P-01')).closest('tr');
    expect(row).not.toBeNull();

    await user.click(
      within(row as HTMLElement).getByRole('button', { name: /editar|edit/i }),
    );

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/editar plaza|edit space/i)).toBeInTheDocument();
    expect(within(dialog).getByLabelText(/etiqueta de la plaza|space label/i)).toHaveValue('P-01');
  });

  it('should_show_error_message_when_list_request_fails', async () => {
    server.use(
      http.get(SPACES_URL, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: new Date().toISOString() },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<ParkingSpacesPage />);

    expect(
      await screen.findByText(/no se pudieron cargar las plazas|could not load parking spaces/i),
    ).toBeInTheDocument();
  });

  it('should_change_page_when_clicking_next_on_multipage_result', async () => {
    let requestedPage: string | null = null;
    server.use(
      http.get(SPACES_URL, ({ request }) => {
        requestedPage = new URL(request.url).searchParams.get('page');
        const isFirst = requestedPage === null || requestedPage === '0';
        return HttpResponse.json({
          content: [isFirst ? spaceP01 : spaceP02],
          totalElements: 2,
          totalPages: 2,
          size: 1,
          number: isFirst ? 0 : 1,
          first: isFirst,
          last: !isFirst,
        });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<ParkingSpacesPage />);
    await screen.findByText('P-01');

    await user.click(screen.getByRole('button', { name: /siguiente|next/i }));

    await waitFor(() => {
      expect(requestedPage).toBe('1');
    });
    expect(await screen.findByText('P-02')).toBeInTheDocument();
  });
});
