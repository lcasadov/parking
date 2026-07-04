import { describe, expect, it } from 'vitest';
import { formatDateTime, isValidWindow, toIsoEnd, toIsoStart } from './audit';

describe('audit utils', () => {
  it('should_return_start_of_day_iso_when_date_given', () => {
    expect(toIsoStart('2026-03-01')).toBe('2026-03-01T00:00:00Z');
  });

  it('should_return_undefined_start_when_date_empty', () => {
    expect(toIsoStart('')).toBeUndefined();
  });

  it('should_return_end_of_day_iso_when_date_given', () => {
    expect(toIsoEnd('2026-03-01')).toBe('2026-03-01T23:59:59Z');
  });

  it('should_return_undefined_end_when_date_empty', () => {
    expect(toIsoEnd('')).toBeUndefined();
  });

  it('should_be_valid_window_when_from_before_to', () => {
    expect(isValidWindow('2026-03-01', '2026-03-31')).toBe(true);
  });

  it('should_be_valid_window_when_an_endpoint_is_missing', () => {
    expect(isValidWindow('', '2026-03-31')).toBe(true);
    expect(isValidWindow('2026-03-01', '')).toBe(true);
  });

  it('should_be_invalid_window_when_from_after_to', () => {
    expect(isValidWindow('2026-03-31', '2026-03-01')).toBe(false);
  });

  it('should_format_iso_datetime_when_valid', () => {
    expect(formatDateTime('2026-03-01T09:15:00Z')).not.toBe('');
  });

  it('should_return_original_string_when_datetime_invalid', () => {
    expect(formatDateTime('not-a-date')).toBe('not-a-date');
  });
});
