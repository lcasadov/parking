import { describe, expect, it } from 'vitest';
import {
  canCancelRequest,
  isWithinWindow,
  maxRequestDateIso,
  REJECTION_REASON_CODES,
  todayIso,
  toIsoDate,
} from './requests';
import type { Request, RequestStatus } from '../types/request';

const FIXED_NOW = new Date(2026, 6, 4); // 2026-07-04 (local)

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

  it('should_return_today_plus_14_when_maxRequestDateIso_called', () => {
    expect(maxRequestDateIso(FIXED_NOW)).toBe('2026-07-18');
  });

  it('should_accept_date_inside_window_when_isWithinWindow_called', () => {
    expect(isWithinWindow('2026-07-04', FIXED_NOW)).toBe(true);
    expect(isWithinWindow('2026-07-18', FIXED_NOW)).toBe(true);
    expect(isWithinWindow('2026-07-10', FIXED_NOW)).toBe(true);
  });

  it('should_reject_date_before_today_when_isWithinWindow_called', () => {
    expect(isWithinWindow('2026-07-03', FIXED_NOW)).toBe(false);
  });

  it('should_reject_date_after_window_when_isWithinWindow_called', () => {
    expect(isWithinWindow('2026-07-19', FIXED_NOW)).toBe(false);
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
