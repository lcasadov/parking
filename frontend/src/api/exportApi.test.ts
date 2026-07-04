import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { EXPORT_PATHS, runExport } from './exportApi';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';

describe('runExport', () => {
  beforeEach(() => {
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:mock');
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should_request_with_format_param_when_exporting', async () => {
    let receivedFormat: string | null = null;
    server.use(
      http.get(`${MSW_BASE}${EXPORT_PATHS.employees}`, ({ request }) => {
        receivedFormat = new URL(request.url).searchParams.get('format');
        return HttpResponse.text('id,login\n10,aandersson', {
          headers: { 'Content-Type': 'text/csv' },
        });
      }),
    );
    const clickSpy = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);

    await runExport(EXPORT_PATHS.employees, 'csv', 'employees');

    expect(receivedFormat).toBe('csv');
    expect(clickSpy).toHaveBeenCalledTimes(1);
  });

  it('should_use_content_disposition_filename_when_present', async () => {
    server.use(
      http.get(`${MSW_BASE}${EXPORT_PATHS.audit}`, () =>
        HttpResponse.text('id,action', {
          headers: {
            'Content-Type': 'text/csv',
            'Content-Disposition': 'attachment; filename="audit-2026.csv"',
          },
        }),
      ),
    );
    let downloadName: string | undefined;
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      downloadName = this.download;
    });

    await runExport(EXPORT_PATHS.audit, 'csv', 'audit');

    expect(downloadName).toBe('audit-2026.csv');
  });

  it('should_fall_back_to_base_and_format_when_no_content_disposition', async () => {
    server.use(
      http.get(`${MSW_BASE}${EXPORT_PATHS.requests}`, () =>
        HttpResponse.text('id,status', { headers: { 'Content-Type': 'text/csv' } }),
      ),
    );
    let downloadName: string | undefined;
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      downloadName = this.download;
    });

    await runExport(EXPORT_PATHS.requests, 'xlsx', 'requests');

    expect(downloadName).toBe('requests.xlsx');
  });
});
