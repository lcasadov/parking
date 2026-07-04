import { AxiosError } from 'axios';
import { describe, expect, it } from 'vitest';
import { clampPercent, isOutsideWindowError, isPlaced, markerStateClass, nextCoord } from './floorPlan';
import type { FloorPlanDesk } from '../types/floorPlan';

const desk = (coordX: number | null, coordY: number | null): FloorPlanDesk => ({
  deskId: 1,
  deskNumber: 1,
  category: 'STANDARD',
  coordX,
  coordY,
  state: 'FREE',
});

describe('floorPlan utils', () => {
  it('should_build_kebab_state_class_when_given_a_state', () => {
    expect(markerStateClass('MINE')).toBe('floor-marker-mine');
  });

  it('should_flag_placed_only_when_both_coordinates_present', () => {
    expect(isPlaced(desk(10, 20))).toBe(true);
    expect(isPlaced(desk(null, 20))).toBe(false);
    expect(isPlaced(desk(10, null))).toBe(false);
  });

  it('should_clamp_percentage_to_range_0_100', () => {
    expect(clampPercent(-5)).toBe(0);
    expect(clampPercent(150)).toBe(100);
    expect(clampPercent(33.333)).toBe(33.33);
  });

  it('should_return_base_percentage_when_size_is_zero', () => {
    expect(nextCoord(40, 100, 0)).toBe(40);
  });

  it('should_shift_percentage_by_relative_delta_when_size_positive', () => {
    expect(nextCoord(20, 50, 1000)).toBe(25);
  });

  it('should_detect_outside_window_only_for_axios_400', () => {
    const err = new AxiosError('bad');
    err.response = { status: 400 } as AxiosError['response'];
    expect(isOutsideWindowError(err)).toBe(true);
    expect(isOutsideWindowError(new Error('other'))).toBe(false);
  });
});
