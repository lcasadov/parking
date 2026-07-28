import { describe, expect, it } from 'vitest';
import { isLocationComplete, resolveBookingEntries, allModeAsDayChoice } from './wizardBooking';
import { buildSteps, emptyTypeLocation, PARKING_AUTO } from '../components/wizard/wizardTypes';
import type { TypeLocation } from '../components/wizard/wizardTypes';

const DATES = ['2026-08-03', '2026-08-04'];

function loc(partial: Partial<TypeLocation>): TypeLocation {
  return { ...emptyTypeLocation(), ...partial };
}

describe('buildSteps', () => {
  it('should_add_one_location_step_per_selected_type_in_order', () => {
    const both = buildSteps(['PARKING', 'DESK']).map((s) => s.kind);
    expect(both).toEqual(['RESOURCE', 'DATES', 'EMPLOYEE', 'LOCATION', 'LOCATION', 'SUMMARY']);
    const locations = buildSteps(['PARKING', 'DESK']).filter((s) => s.kind === 'LOCATION');
    expect(locations.map((s) => s.type)).toEqual(['PARKING', 'DESK']);
  });

  it('should_have_a_single_location_step_for_one_type', () => {
    expect(buildSteps(['DESK']).filter((s) => s.kind === 'LOCATION')).toHaveLength(1);
  });
});

describe('isLocationComplete (per type)', () => {
  it('should_require_a_concrete_desk_for_DESK', () => {
    expect(isLocationComplete(loc({ deskId: null }), 'DESK', DATES)).toBe(false);
    expect(isLocationComplete(loc({ deskId: 7 }), 'DESK', DATES)).toBe(true);
  });

  it('should_accept_auto_for_PARKING', () => {
    expect(isLocationComplete(loc({ parkingChoice: PARKING_AUTO }), 'PARKING', DATES)).toBe(true);
    expect(isLocationComplete(loc({ parkingChoice: 3 }), 'PARKING', DATES)).toBe(true);
    expect(isLocationComplete(loc({ parkingChoice: null }), 'PARKING', DATES)).toBe(false);
  });

  it('should_require_every_day_in_per_day_mode', () => {
    const location = loc({
      locationMode: 'PER_DAY',
      perDay: { '2026-08-03': { resourceId: 5, label: 'P-05', auto: false } },
    });
    expect(isLocationComplete(location, 'PARKING', DATES)).toBe(false);
  });
});

describe('resolveBookingEntries (per type)', () => {
  it('should_map_all_dates_to_the_same_resource_in_ALL_mode', () => {
    const entries = resolveBookingEntries(loc({ deskId: 9 }), 'DESK', DATES);
    expect(entries).toEqual([
      { date: DATES[0], resourceId: 9 },
      { date: DATES[1], resourceId: 9 },
    ]);
  });

  it('should_omit_resourceId_for_parking_auto', () => {
    const entries = resolveBookingEntries(loc({ parkingChoice: PARKING_AUTO }), 'PARKING', DATES);
    expect(entries.every((entry) => entry.resourceId === undefined)).toBe(true);
  });
});

describe('allModeAsDayChoice (per type)', () => {
  it('should_seed_auto_for_parking_auto', () => {
    expect(allModeAsDayChoice(loc({ parkingChoice: PARKING_AUTO }), 'PARKING')).toEqual({
      resourceId: null,
      label: null,
      auto: true,
    });
  });

  it('should_return_null_when_desk_not_chosen', () => {
    expect(allModeAsDayChoice(loc({ deskId: null }), 'DESK')).toBeNull();
  });
});
