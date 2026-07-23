import { describe, expect, it } from 'vitest';
import {
  distinctDays,
  groupFixedAssignments,
  mergeFixedAssignmentDays,
  toggleDay,
  WEEK_DAYS,
} from './fixedAssignments';
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

  // CRITICO (design §Risk D): la asignacion inline fija debe PRESERVAR los dias
  // existentes; mergeFixedAssignmentDays reenvia el conjunto completo + el nuevo dia.
  it('should_preserve_existing_days_when_merging_new_day', () => {
    // Alice tiene PARKING (plaza 1) los dias 1,2,3; añadir el 5 debe devolver 1,2,3,5.
    expect(mergeFixedAssignmentDays(aliceAssignments, 'PARKING', 1, 5)).toEqual([1, 2, 3, 5]);
  });

  it('should_not_duplicate_day_when_merging_present_day', () => {
    expect(mergeFixedAssignmentDays(aliceAssignments, 'PARKING', 1, 2)).toEqual([1, 2, 3]);
  });

  it('should_return_only_new_day_when_no_existing_assignment_for_type', () => {
    // Sin puesto fijo previo, el DESK arranca solo con el dia añadido.
    expect(mergeFixedAssignmentDays(aliceAssignments, 'DESK', 1, 4)).toEqual([4]);
  });

  // Fix 7.1: emparejar solo por resourceType consolidaba mal cuando el empleado
  // tiene recursos DISTINTOS del mismo tipo en dias distintos (p.ej. puesto 1 el
  // lunes y puesto 3 el miercoles). Debe emparejar tambien por resourceId: asignar
  // el puesto 3 el miercoles no debe heredar ni alterar los dias del puesto 1.
  it('should_not_mix_days_from_a_different_resource_of_the_same_type', () => {
    expect(mergeFixedAssignmentDays(aliceAssignments, 'PARKING', 3, 3)).toEqual([3]);
  });
});
