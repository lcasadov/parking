import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { MyWeekPage } from './MyWeekPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser } from '../mocks/fixtures';
import { defaultMyWeek } from '../mocks/calendarFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const ME_URL = `${MSW_BASE}/auth/me`;

describe('MyWeekPage (EMPLOYEE)', () => {
  it('should_renderOwnWeek_when_myWeekLoaded', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/my-week`, () => HttpResponse.json(defaultMyWeek)),
    );
    renderWithProviders(<MyWeekPage />);

    // El estado aparece en la celda del día y (parcialmente) en la leyenda inferior.
    expect((await screen.findAllByText(/plaza asignada|space assigned/i)).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/plaza liberada|space released/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/solicitud pendiente|request pending/i).length).toBeGreaterThan(0);
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

    await screen.findAllByText(/plaza asignada|space assigned/i);
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

  it('should_openRequestModal_when_requestActionClicked', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/my-week`, () => HttpResponse.json(defaultMyWeek)),
    );
    const user = userEvent.setup();
    renderWithProviders(<MyWeekPage />);
    await screen.findAllByText(/plaza asignada|space assigned/i);

    await user.click(screen.getByRole('button', { name: /^solicitar$|^request$/i }));

    // El modal de solicitud unificada aparece (diálogo + botón de envío).
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /enviar solicitud|submit request/i }),
    ).toBeInTheDocument();
  });

  it('should_enableRelease_when_employeeHasFixedParking', async () => {
    // El empleado 10 (Alice) tiene plaza fija en los handlers por defecto.
    server.use(
      http.get(ME_URL, () => HttpResponse.json({ ...adminUser, employeeId: 10, role: 'EMPLOYEE' })),
      http.get(`${MSW_BASE}/calendar/my-week`, () => HttpResponse.json(defaultMyWeek)),
    );
    renderWithProviders(<MyWeekPage />);
    await screen.findAllByText(/plaza asignada|space assigned/i);

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /^liberar$|^release$/i })).toBeEnabled(),
    );
  });

  it('should_disableRelease_when_employeeHasNoFixedParking', async () => {
    server.use(
      http.get(`${MSW_BASE}/calendar/my-week`, () => HttpResponse.json(defaultMyWeek)),
    );
    renderWithProviders(<MyWeekPage />);
    await screen.findAllByText(/plaza asignada|space assigned/i);

    // adminUser por defecto (employeeId 1) no tiene asignaciones fijas → deshabilitado.
    expect(screen.getByRole('button', { name: /^liberar$|^release$/i })).toBeDisabled();
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
