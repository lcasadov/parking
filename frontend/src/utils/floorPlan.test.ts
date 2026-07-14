import { AxiosError } from 'axios';
import { describe, expect, it } from 'vitest';
import {
  ZOOM_MAX,
  ZOOM_MIN,
  clampPercent,
  clampScale,
  countByState,
  isOutsideWindowError,
  isPlaced,
  markerColorClass,
  markerStateClass,
  matchesDeskSearch,
  nextCoord,
  SELECTED_MARKER_CLASS,
} from './floorPlan';
import type { DeskState, FloorPlanDesk } from '../types/floorPlan';

const desk = (coordX: number | null, coordY: number | null): FloorPlanDesk => ({
  deskId: 1,
  deskNumber: 1,
  category: 'STANDARD',
  coordX,
  coordY,
  state: 'FREE',
});

const deskWith = (deskNumber: number, state: DeskState): FloorPlanDesk => ({
  deskId: deskNumber,
  deskNumber,
  category: 'STANDARD',
  coordX: 10,
  coordY: 10,
  state,
});

describe('floorPlan utils', () => {
  it('should_build_kebab_state_class_when_given_a_state', () => {
    expect(markerStateClass('MINE')).toBe('floor-marker-mine');
  });

  it('should_use_selected_class_when_marker_is_chosen_otherwise_state_color', () => {
    expect(markerColorClass('FREE', true)).toBe(SELECTED_MARKER_CLASS);
    expect(markerColorClass('FREE', false)).toBe('floor-marker-free');
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

  it('should_count_desks_by_state', () => {
    const counts = countByState([
      deskWith(1, 'FREE'),
      deskWith(2, 'FREE'),
      deskWith(3, 'MINE'),
      deskWith(4, 'ASSIGNED'),
    ]);
    expect(counts.FREE).toBe(2);
    expect(counts.MINE).toBe(1);
    expect(counts.ASSIGNED).toBe(1);
    expect(counts.REQUESTED).toBe(0);
    expect(counts.RELEASED).toBe(0);
  });

  it('should_match_desk_search_by_number_substring', () => {
    expect(matchesDeskSearch(deskWith(12, 'FREE'), '')).toBe(true);
    expect(matchesDeskSearch(deskWith(12, 'FREE'), '1')).toBe(true);
    expect(matchesDeskSearch(deskWith(12, 'FREE'), '2')).toBe(true);
    expect(matchesDeskSearch(deskWith(12, 'FREE'), '3')).toBe(false);
    expect(matchesDeskSearch(deskWith(12, 'FREE'), '  ')).toBe(true);
  });

  it('should_clamp_scale_to_zoom_bounds', () => {
    expect(clampScale(0.1)).toBe(ZOOM_MIN);
    expect(clampScale(9)).toBe(ZOOM_MAX);
    expect(clampScale(1.234)).toBe(1.23);
  });
});
