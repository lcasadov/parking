import { describe, expect, it } from 'vitest';
import { distinctDays, groupFixedAssignments, toggleDay, WEEK_DAYS } from './fixedAssignments';
import { aliceAssignments, bobThursday } from '../mocks/fixedAssignmentFixtures';

describe('fixedAssignments utils', () => {
  it('should_expose_seven_iso_days_when_reading_week_days', () => {
    expect(WEEK_DAYS).toEqual([1, 2, 3, 4, 5, 6, 7]);
  });

  it('should_group_by_employee_and_space_with_sorted_days_when_grouping_rows', () => {
    const groups = groupFixedAssignments([...aliceAssignments, bobThursday]);

    expect(groups).toHaveLength(2);
    const alice = groups.find((group) => group.employeeId === 10);
    expect(alice?.parkingSpaceId).toBe(1);
    expect(alice?.days).toEqual([1, 2, 3]);
    const bob = groups.find((group) => group.employeeId === 11);
    expect(bob?.days).toEqual([4]);
  });

  it('should_return_empty_list_when_grouping_no_rows', () => {
    expect(groupFixedAssignments([])).toEqual([]);
  });

  it('should_return_sorted_distinct_days_when_computing_distinct', () => {
    expect(distinctDays(aliceAssignments)).toEqual([1, 2, 3]);
  });

  it('should_add_day_when_toggling_absent_day', () => {
    expect(toggleDay([1, 3], 2)).toEqual([1, 2, 3]);
  });

  it('should_remove_day_when_toggling_present_day', () => {
    expect(toggleDay([1, 2, 3], 2)).toEqual([1, 3]);
  });
});
