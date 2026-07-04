import { describe, expect, it } from 'vitest';
import { canCancelReservation } from './visitors';

describe('canCancelReservation', () => {
  const now = new Date('2026-07-04T10:00:00Z');

  it('should_return_false_when_reservation_date_is_past', () => {
    expect(canCancelReservation('2026-07-03', now)).toBe(false);
  });

  it('should_return_true_when_reservation_date_is_today', () => {
    expect(canCancelReservation('2026-07-04', now)).toBe(true);
  });

  it('should_return_true_when_reservation_date_is_future', () => {
    expect(canCancelReservation('2026-07-05', now)).toBe(true);
  });
});
