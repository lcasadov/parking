import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { parseContentDispositionFilename, triggerBlobDownload } from './download';

describe('parseContentDispositionFilename', () => {
  it('should_return_fallback_when_header_is_absent', () => {
    expect(parseContentDispositionFilename(undefined, 'fallback.csv')).toBe('fallback.csv');
  });

  it('should_extract_plain_filename_when_header_has_quoted_filename', () => {
    const header = 'attachment; filename="employees-2026.xlsx"';
    expect(parseContentDispositionFilename(header, 'fallback.xlsx')).toBe('employees-2026.xlsx');
  });

  it('should_extract_encoded_filename_when_header_has_filename_star', () => {
    const header = "attachment; filename*=UTF-8''audit%20log.csv";
    expect(parseContentDispositionFilename(header, 'fallback.csv')).toBe('audit log.csv');
  });

  it('should_return_fallback_when_header_has_no_filename', () => {
    expect(parseContentDispositionFilename('attachment', 'fallback.csv')).toBe('fallback.csv');
  });
});

describe('triggerBlobDownload', () => {
  beforeEach(() => {
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:mock');
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should_click_anchor_with_download_filename_when_triggered', () => {
    const clickSpy = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
    const blob = new Blob(['id,login'], { type: 'text/csv' });

    triggerBlobDownload(blob, 'report.csv');

    expect(globalThis.URL.createObjectURL).toHaveBeenCalledWith(blob);
    expect(clickSpy).toHaveBeenCalledTimes(1);
    expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock');
  });
});
