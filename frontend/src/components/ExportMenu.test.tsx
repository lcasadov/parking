import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ExportMenu } from './ExportMenu';
import { ExportMyDataButton } from './ExportMyDataButton';
import { Toast } from './Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { EXPORT_PATHS } from '../api/exportApi';
import { adminUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const EMPLOYEES_EXPORT_URL = `${MSW_BASE}${EXPORT_PATHS.employees}`;

function useAdminSession(): void {
  server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
}

function useEmployeeSession(): void {
  server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
}

describe('ExportMenu', () => {
  beforeEach(() => {
    // jsdom no implementa createObjectURL ni la navegacion de descarga.
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:mock');
    globalThis.URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
  });

  it('should_render_csv_and_xlsx_options_when_role_matches', async () => {
    useAdminSession();
    renderWithProviders(
      <ExportMenu path={EXPORT_PATHS.employees} fallbackBase="employees" requiredRole="ADMIN" />,
    );

    expect(
      await screen.findByRole('button', { name: /exportar csv|export csv/i }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /exportar xlsx|export xlsx/i })).toBeInTheDocument();
  });

  it('should_trigger_blob_download_when_clicking_a_format', async () => {
    useAdminSession();
    let receivedFormat: string | null = null;
    server.use(
      http.get(EMPLOYEES_EXPORT_URL, ({ request }) => {
        receivedFormat = new URL(request.url).searchParams.get('format');
        return HttpResponse.text('id,login\n10,aandersson', {
          headers: { 'Content-Type': 'text/csv' },
        });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(
      <ExportMenu path={EXPORT_PATHS.employees} fallbackBase="employees" requiredRole="ADMIN" />,
    );

    await user.click(await screen.findByRole('button', { name: /exportar csv|export csv/i }));

    await waitFor(() => {
      expect(receivedFormat).toBe('csv');
    });
    expect(globalThis.URL.createObjectURL).toHaveBeenCalled();
    expect(HTMLAnchorElement.prototype.click).toHaveBeenCalled();
  });

  it('should_show_rate_limit_toast_when_export_returns_429', async () => {
    useAdminSession();
    server.use(
      http.get(EMPLOYEES_EXPORT_URL, () =>
        HttpResponse.json(
          { error: 'rate_limited', message: 'too many', timestamp: new Date().toISOString() },
          { status: 429 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(
      <>
        <ExportMenu path={EXPORT_PATHS.employees} fallbackBase="employees" requiredRole="ADMIN" />
        <Toast />
      </>,
    );

    await user.click(await screen.findByRole('button', { name: /exportar csv|export csv/i }));

    expect(
      await screen.findByText(/límite de exportaciones|reached the export limit/i),
    ).toBeInTheDocument();
  });

  it('should_show_generic_error_toast_when_export_fails', async () => {
    useAdminSession();
    server.use(
      http.get(EMPLOYEES_EXPORT_URL, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: new Date().toISOString() },
          { status: 500 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(
      <>
        <ExportMenu path={EXPORT_PATHS.employees} fallbackBase="employees" requiredRole="ADMIN" />
        <Toast />
      </>,
    );

    await user.click(await screen.findByRole('button', { name: /exportar csv|export csv/i }));

    expect(
      await screen.findByText(/no se pudo generar la exportación|export could not be generated/i),
    ).toBeInTheDocument();
  });

  it('should_hide_admin_export_when_user_is_employee', async () => {
    useEmployeeSession();
    renderWithProviders(
      <>
        <ExportMenu path={EXPORT_PATHS.employees} fallbackBase="employees" requiredRole="ADMIN" />
        <ExportMyDataButton />
      </>,
    );

    // El boton "Exportar mis datos" confirma que la sesion EMPLOYEE ya cargo.
    expect(
      await screen.findByRole('button', { name: /exportar mis datos|export my data/i }),
    ).toBeInTheDocument();
    // El menu de exportacion ADMIN no debe renderizarse (defensa en profundidad).
    expect(screen.queryByRole('button', { name: /exportar csv|export csv/i })).not.toBeInTheDocument();
  });

  it('should_show_own_requests_export_when_user_is_employee', async () => {
    useEmployeeSession();
    renderWithProviders(
      <ExportMenu path={EXPORT_PATHS.myRequests} fallbackBase="my-requests" />,
    );

    expect(
      await screen.findByRole('button', { name: /exportar csv|export csv/i }),
    ).toBeInTheDocument();
  });
});
