import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { LoginLogsPage } from './LoginLogsPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import {
  loginInvalid,
  loginLogPageOf,
  loginOk,
} from '../mocks/auditFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const LOGIN_LOGS_URL = `${MSW_BASE}/login-logs`;

describe('LoginLogsPage', () => {
  it('should_render_login_rows_when_list_loads', async () => {
    renderWithProviders(<LoginLogsPage />);

    expect(await screen.findByText('admin')).toBeInTheDocument();
    expect(screen.getByText('hacker')).toBeInTheDocument();
  });

  it('should_return_only_invalid_credentials_when_login_log_filtered_by_result', async () => {
    let receivedResult: string | null = null;
    server.use(
      http.get(LOGIN_LOGS_URL, ({ request }) => {
        receivedResult = new URL(request.url).searchParams.get('result');
        const all = [loginOk, loginInvalid];
        const filtered = receivedResult
          ? all.filter((entry) => entry.result === receivedResult)
          : all;
        return HttpResponse.json(loginLogPageOf(filtered));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<LoginLogsPage />);
    await screen.findByText('admin');

    await user.selectOptions(
      screen.getByLabelText(/^resultado$|^result$/i),
      'INVALID_CREDENTIALS',
    );

    await waitFor(() => {
      expect(receivedResult).toBe('INVALID_CREDENTIALS');
    });
    await waitFor(() => {
      expect(screen.queryByText('admin')).not.toBeInTheDocument();
    });
    expect(screen.getByText('hacker')).toBeInTheDocument();
  });

  it('should_request_with_date_window_when_selecting_from_and_to', async () => {
    let receivedFrom: string | null = null;
    let receivedTo: string | null = null;
    server.use(
      http.get(LOGIN_LOGS_URL, ({ request }) => {
        const url = new URL(request.url);
        receivedFrom = url.searchParams.get('from');
        receivedTo = url.searchParams.get('to');
        return HttpResponse.json(loginLogPageOf([loginOk]));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<LoginLogsPage />);
    await screen.findByText('admin');

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
      http.get(LOGIN_LOGS_URL, ({ request }) => {
        const url = new URL(request.url);
        receivedWindows.push({
          from: url.searchParams.get('from'),
          to: url.searchParams.get('to'),
        });
        return HttpResponse.json(loginLogPageOf([loginOk]));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<LoginLogsPage />);
    await screen.findByText('admin');

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

  it('should_show_window_error_when_api_returns_400_for_invalid_enum', async () => {
    server.use(
      http.get(LOGIN_LOGS_URL, () =>
        HttpResponse.json(
          { error: 'validation', message: 'invalid result', timestamp: new Date().toISOString() },
          { status: 400 },
        ),
      ),
    );
    renderWithProviders(<LoginLogsPage />);

    expect(
      await screen.findByText(/anterior o igual|before or equal/i),
    ).toBeInTheDocument();
  });

  it('should_show_error_message_when_list_request_fails', async () => {
    server.use(
      http.get(LOGIN_LOGS_URL, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: new Date().toISOString() },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<LoginLogsPage />);

    expect(
      await screen.findByText(/no se pudieron cargar los accesos|sign-ins could not be loaded/i),
    ).toBeInTheDocument();
  });

  it('should_change_page_when_clicking_next_on_multipage_result', async () => {
    let requestedPage: string | null = null;
    server.use(
      http.get(LOGIN_LOGS_URL, ({ request }) => {
        requestedPage = new URL(request.url).searchParams.get('page');
        const isFirst = requestedPage === null || requestedPage === '0';
        return HttpResponse.json({
          content: [isFirst ? loginOk : loginInvalid],
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
    renderWithProviders(<LoginLogsPage />);
    await screen.findByText('admin');

    await user.click(screen.getByRole('button', { name: /siguiente|next/i }));

    await waitFor(() => {
      expect(requestedPage).toBe('1');
    });
    expect(await screen.findByText('hacker')).toBeInTheDocument();
  });
});
