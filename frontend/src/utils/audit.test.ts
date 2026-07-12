import { describe, expect, it } from 'vitest';
import { auditPillClass, formatDateTime, isValidWindow, toIsoEnd, toIsoStart } from './audit';

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

  it('should_map_approve_actions_to_green_pill', () => {
    expect(auditPillClass('REQUEST_APPROVED')).toBe('pill-green');
    expect(auditPillClass('APPROVE_REQUEST')).toBe('pill-green');
  });

  it('should_map_reject_actions_to_red_pill', () => {
    expect(auditPillClass('REQUEST_REJECTED')).toBe('pill-red');
  });

  it('should_map_release_actions_to_pink_pill_even_when_they_start_with_create', () => {
    expect(auditPillClass('CREATE_RELEASE')).toBe('pill-pink');
    expect(auditPillClass('RELEASE_CANCELLED')).toBe('pill-pink');
  });

  it('should_map_reset_actions_to_amber_pill', () => {
    expect(auditPillClass('RESET_PASSWORD')).toBe('pill-amber');
  });

  it('should_map_create_update_delete_actions_to_blue_pill', () => {
    expect(auditPillClass('EMPLOYEE_CREATED')).toBe('pill-blue');
    expect(auditPillClass('UPDATE_FIXED_ASSIGNMENT')).toBe('pill-blue');
    expect(auditPillClass('EMPLOYEE_DELETED')).toBe('pill-blue');
  });

  it('should_map_unknown_actions_to_gray_pill', () => {
    expect(auditPillClass('RETENTION_PURGE')).toBe('pill-gray');
  });
});
