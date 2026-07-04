import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ExportMyDataButton } from './ExportMyDataButton';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { EXPORT_PATHS } from '../api/exportApi';
import { adminUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const MY_DATA_URL = `${MSW_BASE}${EXPORT_PATHS.myData}`;

describe('ExportMyDataButton', () => {
  beforeEach(() => {
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:mock');
    globalThis.URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
  });

  it('should_export_own_data_when_employee_clicks', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
    let requested = false;
    server.use(
      http.get(MY_DATA_URL, () => {
        requested = true;
        return HttpResponse.text('field,value\nlogin,emp', {
          headers: { 'Content-Type': 'text/csv' },
        });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<ExportMyDataButton />);

    await user.click(
      await screen.findByRole('button', { name: /exportar mis datos|export my data/i }),
    );

    await waitFor(() => {
      expect(requested).toBe(true);
    });
    expect(HTMLAnchorElement.prototype.click).toHaveBeenCalled();
  });

  it('should_be_available_when_admin_authenticated', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(<ExportMyDataButton />);

    expect(
      await screen.findByRole('button', { name: /exportar mis datos|export my data/i }),
    ).toBeInTheDocument();
  });

  it('should_not_render_when_unauthenticated', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () =>
        HttpResponse.json(
          { error: 'unauthorized', message: 'no session', timestamp: new Date().toISOString() },
          { status: 401 },
        ),
      ),
    );
    renderWithProviders(<ExportMyDataButton />);

    await waitFor(() => {
      expect(
        screen.queryByRole('button', { name: /exportar mis datos|export my data/i }),
      ).not.toBeInTheDocument();
    });
  });
});
