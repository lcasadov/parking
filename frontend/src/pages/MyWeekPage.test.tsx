import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { MyWeekPage } from './MyWeekPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { defaultMyWeek } from '../mocks/calendarFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

describe('MyWeekPage (EMPLOYEE)', () => {
  it('should_renderOwnWeek_when_myWeekLoaded', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/my-week`, () => HttpResponse.json(defaultMyWeek)),
    );
    renderWithProviders(<MyWeekPage />);

    expect(await screen.findByText(/plaza asignada|space assigned/i)).toBeInTheDocument();
    expect(screen.getByText(/plaza liberada|space released/i)).toBeInTheDocument();
    expect(screen.getByText(/solicitud pendiente|request pending/i)).toBeInTheDocument();
    // Muestra la etiqueta de la plaza propia (asignada y liberada la misma semana).
    expect(screen.getAllByText(/plaza P-12|space P-12/i).length).toBeGreaterThan(0);
  });

  it('should_notRenderThirdPartyNames_when_showingMyWeek', async () => {
    // El contrato de my-week omite nombres; incluso si el backend enviara un
    // campo extra, la vista nunca lo renderiza.
    server.use(
      http.get(`${MSW_BASE}/calendar/my-week`, () =>
        HttpResponse.json({
          weekStart: '2026-05-11',
          days: [
            {
              date: '2026-05-11',
              state: 'ASSIGNED',
              parkingSpaceLabel: 'P-12',
              requestStatus: null,
              employeeName: 'Alice Andersson',
            },
          ],
        }),
      ),
    );
    renderWithProviders(<MyWeekPage />);

    await screen.findByText(/plaza asignada|space assigned/i);
    expect(screen.queryByText('Alice Andersson')).not.toBeInTheDocument();
  });

  it('should_showError_when_myWeekRequestFails', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/my-week`, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: '2026-05-11T09:00:00Z' },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<MyWeekPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/no se pudo cargar tu semana|your week could not be loaded/i),
      ).toBeInTheDocument();
    });
  });

  it('should_showEmptyState_when_noDaysReturned', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/my-week`, () =>
        HttpResponse.json({ weekStart: '2026-05-11', days: [] }),
      ),
    );
    renderWithProviders(<MyWeekPage />);

    expect(
      await screen.findByText(/no hay días que mostrar|no days to show/i),
    ).toBeInTheDocument();
  });
});
