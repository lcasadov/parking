import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AdminCalendarPage } from './AdminCalendarPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminCalendarFor, emptyAdminCalendar } from '../mocks/calendarFixtures';
import { addDaysIso, mondayOfWeek } from '../utils/calendar';
import { renderWithProviders } from '../test/renderWithProviders';

describe('AdminCalendarPage (ADMIN grid)', () => {
  it('should_renderCellsPerState_when_calendarLoaded', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/admin`, ({ request }) =>
        HttpResponse.json(
          adminCalendarFor(new URL(request.url).searchParams.get('weekStart') ?? ''),
        ),
      ),
    );
    renderWithProviders(<AdminCalendarPage />);

    const grid = await screen.findByRole('table');
    // Fila P-01 cubre los cinco estados de CalendarCellState.
    expect(within(grid).getByText(/asignada|assigned/i)).toBeInTheDocument();
    expect(within(grid).getByText(/liberada|released/i)).toBeInTheDocument();
    expect(within(grid).getByText(/solicitud pendiente|request pending/i)).toBeInTheDocument();
    expect(within(grid).getByText(/solicitud aprobada|request approved/i)).toBeInTheDocument();
    // El admin sí ve los nombres de titulares (Alice aparece en ASSIGNED y RELEASED).
    expect(within(grid).getAllByText('Alice Andersson').length).toBeGreaterThan(0);
    expect(within(grid).getByText('Bob Beck')).toBeInTheDocument();
    // Cabeceras de fila = etiquetas de plaza.
    expect(within(grid).getByRole('rowheader', { name: 'P-01' })).toBeInTheDocument();
  });

  it('should_requestAdjacentWeeks_when_navigating', async () => {
    const requested: string[] = [];
    server.use(
      http.get(`${MSW_BASE}/calendar/admin`, ({ request }) => {
        const weekStart = new URL(request.url).searchParams.get('weekStart') ?? '';
        requested.push(weekStart);
        return HttpResponse.json(adminCalendarFor(weekStart));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminCalendarPage />);

    await screen.findByRole('table');
    const thisWeek = mondayOfWeek();
    await waitFor(() => expect(requested).toContain(thisWeek));

    await user.click(screen.getByRole('button', { name: /semana siguiente|next week/i }));
    await waitFor(() => expect(requested).toContain(addDaysIso(thisWeek, 7)));

    await user.click(screen.getByRole('button', { name: /semana anterior|previous week/i }));
    await waitFor(() => expect(requested).toContain(thisWeek));
  });

  it('should_alignReleasedToBlueStateToken_when_rendered', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/admin`, ({ request }) =>
        HttpResponse.json(
          adminCalendarFor(new URL(request.url).searchParams.get('weekStart') ?? ''),
        ),
      ),
    );
    renderWithProviders(<AdminCalendarPage />);

    const grid = await screen.findByRole('table');
    const released = within(grid).getByText(/liberada|released/i);
    const cell = released.closest('td');
    // Liberado usa el token de estado del contrato (azul), no el pink previo.
    expect(cell).toHaveClass('state-released');
    expect(cell).not.toHaveClass('cell-released');
  });

  it('should_showSummaryCards_when_calendarLoaded', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/admin`, ({ request }) =>
        HttpResponse.json(
          adminCalendarFor(new URL(request.url).searchParams.get('weekStart') ?? ''),
        ),
      ),
    );
    renderWithProviders(<AdminCalendarPage />);

    await screen.findByRole('table');
    const assignments = screen.getByText(/asignaciones|assignments/i).closest('.summary-card');
    expect(assignments).not.toBeNull();
    // ASSIGNED (1) + REQUEST_APPROVED (1) = 2 asignaciones en la semana fixture.
    expect(within(assignments as HTMLElement).getByText('2')).toBeInTheDocument();
  });

  it('should_toggleStateFilter_when_filterButtonClicked', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/admin`, ({ request }) =>
        HttpResponse.json(
          adminCalendarFor(new URL(request.url).searchParams.get('weekStart') ?? ''),
        ),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminCalendarPage />);

    await screen.findByRole('table');
    expect(screen.queryByRole('group', { name: /filtrar|filter/i })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /filtrar|filter/i }));
    const group = screen.getByRole('group', { name: /filtrar|filter/i });
    const freeChip = within(group).getByRole('button', { name: /libre|free/i });
    expect(freeChip).toHaveAttribute('aria-pressed', 'true');

    await user.click(freeChip);
    expect(freeChip).toHaveAttribute('aria-pressed', 'false');
  });

  it('should_enableExport_when_rowsPresent', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/admin`, ({ request }) =>
        HttpResponse.json(
          adminCalendarFor(new URL(request.url).searchParams.get('weekStart') ?? ''),
        ),
      ),
    );
    renderWithProviders(<AdminCalendarPage />);

    await screen.findByRole('table');
    expect(screen.getByRole('button', { name: /exportar|export/i })).toBeEnabled();
  });

  it('should_showEmptyState_when_noSpacesConfigured', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/admin`, () => HttpResponse.json(emptyAdminCalendar)),
    );
    renderWithProviders(<AdminCalendarPage />);

    expect(
      await screen.findByText(/no hay plazas configuradas|no spaces configured/i),
    ).toBeInTheDocument();
  });

  it('should_showError_when_calendarRequestFails', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/admin`, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: '2026-05-11T09:00:00Z' },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<AdminCalendarPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/no se pudo cargar el calendario|calendar could not be loaded/i),
      ).toBeInTheDocument();
    });
  });
});
