import { describe, expect, it } from 'vitest';
import {
  addDaysIso,
  calendarStateKey,
  cellStateClass,
  dayMonth,
  isValidIsoDate,
  mondayOfWeek,
  myWeekStateKey,
  weekdayIndex,
} from './calendar';

describe('calendar utils', () => {
  it('should_acceptRealIsoDate_when_wellFormed', () => {
    expect(isValidIsoDate('2026-05-11')).toBe(true);
  });

  it('should_rejectDate_when_malformedOrImpossible', () => {
    expect(isValidIsoDate('')).toBe(false);
    expect(isValidIsoDate('2026-5-1')).toBe(false);
    expect(isValidIsoDate('2026-13-01')).toBe(false);
    expect(isValidIsoDate('2026-02-30')).toBe(false);
  });

  it('should_returnMonday_when_computingWeekStart', () => {
    // 2026-05-13 es miercoles -> lunes 2026-05-11.
    expect(mondayOfWeek(new Date(2026, 4, 13))).toBe('2026-05-11');
    // 2026-05-17 es domingo -> lunes de esa semana 2026-05-11.
    expect(mondayOfWeek(new Date(2026, 4, 17))).toBe('2026-05-11');
  });

  it('should_shiftWholeWeek_when_addingSevenDays', () => {
    expect(addDaysIso('2026-05-11', 7)).toBe('2026-05-18');
    expect(addDaysIso('2026-05-11', -7)).toBe('2026-05-04');
  });

  it('should_returnWeekdayIndex_when_givenIsoDate', () => {
    expect(weekdayIndex('2026-05-11')).toBe(1); // lunes
    expect(weekdayIndex('2026-05-17')).toBe(0); // domingo
  });

  it('should_formatDayMonth_when_givenIsoDate', () => {
    expect(dayMonth('2026-05-11')).toBe('11/05');
  });

  it('should_deriveClassAndKeys_when_mappingStates', () => {
    expect(cellStateClass('REQUEST_APPROVED')).toBe('cell-request-approved');
    expect(calendarStateKey('FREE')).toBe('calendar.states.FREE');
    expect(myWeekStateKey('ASSIGNED')).toBe('calendar.myWeek.states.ASSIGNED');
  });
});
