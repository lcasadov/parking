import { describe, expect, it } from 'vitest';
import {
  canCancelRequest,
  canResendRequest,
  isTodayOrFuture,
  REJECTION_REASON_CODES,
  resendCooldownRemainingMs,
  todayIso,
  toIsoDate,
} from './requests';
import type { Request, RequestStatus } from '../types/request';

const FIXED_NOW = new Date(2026, 6, 4); // 2026-07-04 (local)
const FIXED_NOW_ISO = FIXED_NOW.toISOString();
const HOUR_MS = 60 * 60 * 1000;

// Solicitud minima para las pruebas del predicado de cancelacion.
function buildRequest(status: RequestStatus, requestedDate: string): Request {
  return {
    id: 1,
    employeeId: 2,
    requestedDate,
    status,
    parkingSpaceId: null,
    approvalNote: null,
    rejectionReasonCode: null,
    rejectionReason: null,
    resolvedById: null,
    resolvedAt: null,
    createdAt: '2026-03-01T08:00:00Z',
  };
}

describe('requests window helpers', () => {
  it('should_format_local_date_when_toIsoDate_called', () => {
    expect(toIsoDate(new Date(2026, 0, 9))).toBe('2026-01-09');
  });

  it('should_return_today_when_todayIso_called', () => {
    expect(todayIso(FIXED_NOW)).toBe('2026-07-04');
  });

  it('should_accept_today_when_isTodayOrFuture_called', () => {
    expect(isTodayOrFuture('2026-07-04', FIXED_NOW)).toBe(true);
  });

  it('should_accept_any_future_date_without_upper_bound_when_isTodayOrFuture_called', () => {
    expect(isTodayOrFuture('2026-07-18', FIXED_NOW)).toBe(true);
    expect(isTodayOrFuture('2026-07-19', FIXED_NOW)).toBe(true);
    expect(isTodayOrFuture('2027-12-31', FIXED_NOW)).toBe(true);
  });

  it('should_reject_date_before_today_when_isTodayOrFuture_called', () => {
    expect(isTodayOrFuture('2026-07-03', FIXED_NOW)).toBe(false);
  });

  it('should_expose_reason_catalog_when_module_loaded', () => {
    expect(REJECTION_REASON_CODES).toEqual(['NO_AVAILABILITY', 'OUTSIDE_POLICY', 'OTHER']);
  });
});

describe('canCancelRequest', () => {
  it('should_allow_cancel_when_pending_any_date', () => {
    expect(canCancelRequest(buildRequest('PENDING', '2026-07-03'), FIXED_NOW)).toBe(true);
    expect(canCancelRequest(buildRequest('PENDING', '2026-07-20'), FIXED_NOW)).toBe(true);
  });

  it('should_allow_cancel_when_approved_today', () => {
    expect(canCancelRequest(buildRequest('APPROVED', '2026-07-04'), FIXED_NOW)).toBe(true);
  });

  it('should_allow_cancel_when_approved_future', () => {
    expect(canCancelRequest(buildRequest('APPROVED', '2026-07-05'), FIXED_NOW)).toBe(true);
  });

  it('should_reject_cancel_when_approved_past', () => {
    expect(canCancelRequest(buildRequest('APPROVED', '2026-07-03'), FIXED_NOW)).toBe(false);
  });

  it('should_reject_cancel_when_terminal_status', () => {
    expect(canCancelRequest(buildRequest('REJECTED', '2026-07-20'), FIXED_NOW)).toBe(false);
    expect(canCancelRequest(buildRequest('CANCELLED', '2026-07-20'), FIXED_NOW)).toBe(false);
  });
});

describe('canResendRequest', () => {
  it('should_be_eligible_when_24h_have_passed_since_creation_and_never_reminded', () => {
    const createdAt = new Date(FIXED_NOW.getTime() - 24 * HOUR_MS).toISOString();
    expect(canResendRequest(createdAt, null, FIXED_NOW)).toBe(true);
    expect(canResendRequest(createdAt, undefined, FIXED_NOW)).toBe(true);
  });

  it('should_not_be_eligible_before_24h_have_passed_since_creation', () => {
    const createdAt = new Date(FIXED_NOW.getTime() - 23 * HOUR_MS).toISOString();
    expect(canResendRequest(createdAt, null, FIXED_NOW)).toBe(false);
  });

  it('should_count_from_last_reminded_when_more_recent_than_creation', () => {
    const createdAt = new Date(FIXED_NOW.getTime() - 48 * HOUR_MS).toISOString();
    const lastRemindedAt = new Date(FIXED_NOW.getTime() - 1 * HOUR_MS).toISOString();
    // Aunque la creacion sea antigua, el ultimo reenvio fue hace solo 1h.
    expect(canResendRequest(createdAt, lastRemindedAt, FIXED_NOW)).toBe(false);
  });

  it('should_be_eligible_when_24h_have_passed_since_last_reminded', () => {
    const createdAt = new Date(FIXED_NOW.getTime() - 72 * HOUR_MS).toISOString();
    const lastRemindedAt = new Date(FIXED_NOW.getTime() - 25 * HOUR_MS).toISOString();
    expect(canResendRequest(createdAt, lastRemindedAt, FIXED_NOW)).toBe(true);
  });

  it('should_be_eligible_exactly_at_the_24h_boundary', () => {
    const createdAt = new Date(FIXED_NOW.getTime() - 24 * HOUR_MS).toISOString();
    expect(canResendRequest(createdAt, null, FIXED_NOW)).toBe(true);
  });
});

describe('resendCooldownRemainingMs', () => {
  it('should_return_zero_when_already_eligible', () => {
    const createdAt = new Date(FIXED_NOW.getTime() - 25 * HOUR_MS).toISOString();
    expect(resendCooldownRemainingMs(createdAt, null, FIXED_NOW)).toBe(0);
  });

  it('should_return_remaining_time_when_not_yet_eligible', () => {
    const createdAt = new Date(FIXED_NOW.getTime() - 20 * HOUR_MS).toISOString();
    expect(resendCooldownRemainingMs(createdAt, null, FIXED_NOW)).toBe(4 * HOUR_MS);
  });

  it('should_use_now_as_creation_reference_when_called_without_now', () => {
    expect(resendCooldownRemainingMs(FIXED_NOW_ISO, null)).toBeGreaterThanOrEqual(0);
  });
});
