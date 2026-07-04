import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AuditPage } from './AuditPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import {
  auditEmployeeCreated,
  auditPageOf,
  auditRequestApproved,
  auditRetentionPurge,
} from '../mocks/auditFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const AUDIT_URL = `${MSW_BASE}/audit`;

describe('AuditPage', () => {
  it('should_render_audit_rows_when_list_loads', async () => {
    renderWithProviders(<AuditPage />);

    expect(await screen.findByText('EMPLOYEE_CREATED')).toBeInTheDocument();
    expect(screen.getByText('REQUEST_APPROVED')).toBeInTheDocument();
  });

  it('should_show_system_actor_when_actor_is_null', async () => {
    server.use(http.get(AUDIT_URL, () => HttpResponse.json(auditPageOf([auditRetentionPurge]))));
    renderWithProviders(<AuditPage />);

    expect(await screen.findByText('RETENTION_PURGE')).toBeInTheDocument();
    expect(screen.getByText(/sistema|system/i)).toBeInTheDocument();
  });

  it('should_request_with_actor_filter_when_typing_actor_id', async () => {
    let receivedActor: string | null = null;
    server.use(
      http.get(AUDIT_URL, ({ request }) => {
        receivedActor = new URL(request.url).searchParams.get('actorEmployeeId');
        return HttpResponse.json(auditPageOf([auditEmployeeCreated]));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AuditPage />);
    await screen.findByText('EMPLOYEE_CREATED');

    await user.type(screen.getByLabelText(/id de empleado|employee id/i), '1');

    await waitFor(() => {
      expect(receivedActor).toBe('1');
    });
  });

  it('should_request_with_action_filter_when_typing_action', async () => {
    let receivedAction: string | null = null;
    server.use(
      http.get(AUDIT_URL, ({ request }) => {
        receivedAction = new URL(request.url).searchParams.get('action');
        return HttpResponse.json(auditPageOf([auditRequestApproved]));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AuditPage />);
    await screen.findByText('REQUEST_APPROVED');

    await user.type(screen.getByLabelText(/^acción$|^action$/i), 'REQUEST_APPROVED');

    await waitFor(() => {
      expect(receivedAction).toBe('REQUEST_APPROVED');
    });
  });

  it('should_request_with_date_window_when_selecting_from_and_to', async () => {
    let receivedFrom: string | null = null;
    let receivedTo: string | null = null;
    server.use(
      http.get(AUDIT_URL, ({ request }) => {
        const url = new URL(request.url);
        receivedFrom = url.searchParams.get('from');
        receivedTo = url.searchParams.get('to');
        return HttpResponse.json(auditPageOf([auditEmployeeCreated]));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AuditPage />);
    await screen.findByText('EMPLOYEE_CREATED');

    await user.type(screen.getByLabelText(/^desde$|^from$/i), '2026-03-01');
    await user.type(screen.getByLabelText(/^hasta$|^to$/i), '2026-03-31');

    await waitFor(() => {
      expect(receivedFrom).toBe('2026-03-01T00:00:00Z');
    });
    expect(receivedTo).toBe('2026-03-31T23:59:59Z');
  });

  it('should_show_window_error_and_skip_request_when_from_after_to', async () => {
    const receivedWindows: { from: string | null; to: string | null }[] = [];
    server.use(
      http.get(AUDIT_URL, ({ request }) => {
        const url = new URL(request.url);
        receivedWindows.push({
          from: url.searchParams.get('from'),
          to: url.searchParams.get('to'),
        });
        return HttpResponse.json(auditPageOf([auditEmployeeCreated]));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AuditPage />);
    await screen.findByText('EMPLOYEE_CREATED');

    await user.type(screen.getByLabelText(/^desde$|^from$/i), '2026-03-31');
    await user.type(screen.getByLabelText(/^hasta$|^to$/i), '2026-03-01');

    expect(
      await screen.findByText(/anterior o igual|before or equal/i),
    ).toBeInTheDocument();
    // La consulta con ventana invertida (from > to) nunca se dispara al backend.
    const invertedRequest = receivedWindows.some(
      (window) => window.from === '2026-03-31T00:00:00Z' && window.to === '2026-03-01T23:59:59Z',
    );
    expect(invertedRequest).toBe(false);
  });

  it('should_show_window_error_when_api_returns_400', async () => {
    server.use(
      http.get(AUDIT_URL, () =>
        HttpResponse.json(
          { error: 'validation', message: 'invalid window', timestamp: new Date().toISOString() },
          { status: 400 },
        ),
      ),
    );
    renderWithProviders(<AuditPage />);

    expect(
      await screen.findByText(/anterior o igual|before or equal/i),
    ).toBeInTheDocument();
  });

  it('should_show_error_message_when_list_request_fails', async () => {
    server.use(
      http.get(AUDIT_URL, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: new Date().toISOString() },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<AuditPage />);

    expect(
      await screen.findByText(/no se pudo cargar la auditoría|audit log could not be loaded/i),
    ).toBeInTheDocument();
  });

  it('should_change_page_when_clicking_next_on_multipage_result', async () => {
    let requestedPage: string | null = null;
    server.use(
      http.get(AUDIT_URL, ({ request }) => {
        requestedPage = new URL(request.url).searchParams.get('page');
        const isFirst = requestedPage === null || requestedPage === '0';
        return HttpResponse.json({
          content: [isFirst ? auditEmployeeCreated : auditRequestApproved],
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
    renderWithProviders(<AuditPage />);
    await screen.findByText('EMPLOYEE_CREATED');

    await user.click(screen.getByRole('button', { name: /siguiente|next/i }));

    await waitFor(() => {
      expect(requestedPage).toBe('1');
    });
    expect(await screen.findByText('REQUEST_APPROVED')).toBeInTheDocument();
  });
});
