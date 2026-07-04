import { describe, expect, it } from 'vitest';
import { canCancelRelease, isPastDate, todayIso } from './releases';

describe('releases utils', () => {
  it('should_return_true_when_date_is_before_today', () => {
    const now = new Date('2026-07-04T10:00:00Z');
    expect(isPastDate('2026-07-03', now)).toBe(true);
  });

  it('should_return_false_when_date_is_today_or_future', () => {
    const now = new Date('2026-07-04T10:00:00Z');
    expect(isPastDate(todayIso(now), now)).toBe(false);
    expect(isPastDate('2026-07-05', now)).toBe(false);
  });

  it('should_allow_cancel_when_release_is_today_or_future', () => {
    const now = new Date('2026-07-04T10:00:00Z');
    expect(canCancelRelease('2026-07-04', now)).toBe(true);
    expect(canCancelRelease('2026-07-10', now)).toBe(true);
  });

  it('should_block_cancel_when_release_is_past', () => {
    const now = new Date('2026-07-04T10:00:00Z');
    expect(canCancelRelease('2026-07-01', now)).toBe(false);
  });
});
