import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { EmployeesPage } from './EmployeesPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import {
  employeeAlice,
  employeeBob,
  pageOf,
} from '../mocks/employeeFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const EMPLOYEES_URL = `${MSW_BASE}/employees`;

describe('EmployeesPage', () => {
  beforeEach(() => {
    // jsdom no implementa createObjectURL ni la navegacion de descarga.
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:mock');
    globalThis.URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
  });

  it('should_render_employee_rows_when_list_loads', async () => {
    renderWithProviders(<EmployeesPage />);

    expect(await screen.findByText('Alice Andersson')).toBeInTheDocument();
    expect(screen.getByText('Bob Brown')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /dar de baja|deactivate/i })).toBeInTheDocument();
  });

  it('should_request_with_q_and_filter_when_searching', async () => {
    let receivedQ: string | null = null;
    server.use(
      http.get(EMPLOYEES_URL, ({ request }) => {
        const q = new URL(request.url).searchParams.get('q');
        receivedQ = q;
        const all = [employeeAlice, employeeBob];
        const filtered = q
          ? all.filter((e) => `${e.firstName} ${e.lastName}`.toLowerCase().includes(q.toLowerCase()))
          : all;
        return HttpResponse.json(pageOf(filtered));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<EmployeesPage />);
    await screen.findByText('Alice Andersson');

    await user.type(screen.getByRole('searchbox'), 'Bob');

    await waitFor(() => {
      expect(receivedQ).toBe('Bob');
    });
    await waitFor(() => {
      expect(screen.queryByText('Alice Andersson')).not.toBeInTheDocument();
    });
    expect(screen.getByText('Bob Brown')).toBeInTheDocument();
  });

  it('should_call_deactivate_when_deactivating_active_employee', async () => {
    let deletedId: string | undefined;
    server.use(
      http.delete(`${EMPLOYEES_URL}/:id`, ({ params }) => {
        deletedId = params.id as string;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<EmployeesPage />);
    await screen.findByText('Alice Andersson');

    await user.click(screen.getByRole('button', { name: /dar de baja|deactivate/i }));

    await waitFor(() => {
      expect(deletedId).toBe(String(employeeAlice.id));
    });
  });

  it('should_call_reactivate_when_reactivating_inactive_employee', async () => {
    let reactivatedId: string | undefined;
    server.use(
      http.post(`${EMPLOYEES_URL}/:id/reactivate`, ({ params }) => {
        reactivatedId = params.id as string;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<EmployeesPage />);
    await screen.findByText('Bob Brown');

    await user.click(screen.getByRole('button', { name: /reactivar|reactivate/i }));

    await waitFor(() => {
      expect(reactivatedId).toBe(String(employeeBob.id));
    });
  });

  it('should_request_csv_export_when_clicking_export', async () => {
    let exportFormat: string | null = null;
    server.use(
      http.get(`${EMPLOYEES_URL}/export`, ({ request }) => {
        exportFormat = new URL(request.url).searchParams.get('format');
        return HttpResponse.text('id,login\n10,aandersson', {
          headers: { 'Content-Type': 'text/csv' },
        });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<EmployeesPage />);
    await screen.findByText('Alice Andersson');

    await user.click(screen.getByRole('button', { name: /exportar csv|export csv/i }));

    await waitFor(() => {
      expect(exportFormat).toBe('csv');
    });
  });

  it('should_open_reset_password_modal_when_clicking_reset', async () => {
    const user = userEvent.setup();
    renderWithProviders(<EmployeesPage />);
    const aliceRow = (await screen.findByText('Alice Andersson')).closest('tr');
    expect(aliceRow).not.toBeNull();

    await user.click(
      within(aliceRow as HTMLElement).getByRole('button', {
        name: /restablecer contraseña|reset password/i,
      }),
    );

    expect(
      await screen.findByRole('button', { name: /generar contraseña temporal|generate temporary/i }),
    ).toBeInTheDocument();
  });

  it('should_open_edit_form_prefilled_when_clicking_edit', async () => {
    const user = userEvent.setup();
    renderWithProviders(<EmployeesPage />);
    const aliceRow = (await screen.findByText('Alice Andersson')).closest('tr');

    await user.click(
      within(aliceRow as HTMLElement).getByRole('button', { name: /editar|edit/i }),
    );

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/editar empleado|edit employee/i)).toBeInTheDocument();
    expect(within(dialog).getByLabelText(/^nombre$|^first name$/i)).toHaveValue('Alice');
  });

  it('should_show_error_message_when_list_request_fails', async () => {
    server.use(
      http.get(EMPLOYEES_URL, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: new Date().toISOString() },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<EmployeesPage />);

    expect(
      await screen.findByText(/no se pudieron cargar los empleados|could not load employees/i),
    ).toBeInTheDocument();
  });

  it('should_change_page_when_clicking_next_on_multipage_result', async () => {
    let requestedPage: string | null = null;
    server.use(
      http.get(EMPLOYEES_URL, ({ request }) => {
        requestedPage = new URL(request.url).searchParams.get('page');
        const isFirst = requestedPage === null || requestedPage === '0';
        return HttpResponse.json({
          content: [isFirst ? employeeAlice : employeeBob],
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
    renderWithProviders(<EmployeesPage />);
    await screen.findByText('Alice Andersson');

    await user.click(screen.getByRole('button', { name: /siguiente|next/i }));

    await waitFor(() => {
      expect(requestedPage).toBe('1');
    });
    expect(await screen.findByText('Bob Brown')).toBeInTheDocument();
  });
});
