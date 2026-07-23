import { describe, expect, it } from 'vitest';
import {
  addDaysIso,
  calendarStateClass,
  calendarStateKey,
  cellStateClass,
  dayMonth,
  isoWeekNumber,
  isValidIsoDate,
  mondayOfWeek,
  myWeekStateKey,
  weekdayIndex,
  weekRangeLabel,
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

  it('should_mapStateToDesignSystemClass_when_stylingCells', () => {
    // Mapa estado->color del design system (contrato §4): liberado=azul (state-released).
    expect(calendarStateClass('ASSIGNED')).toBe('state-occupied');
    expect(calendarStateClass('RELEASED')).toBe('state-released');
    expect(calendarStateClass('REQUEST_PENDING')).toBe('state-pending');
    expect(calendarStateClass('REQUEST_APPROVED')).toBe('state-request');
    expect(calendarStateClass('FREE')).toBe('state-free');
  });

  it('should_computeIsoWeekNumber_when_givenIsoDate', () => {
    // Semana ISO 1 de 2026 = Lun 29-dic-2025 .. Dom 04-ene-2026.
    expect(isoWeekNumber('2026-01-05')).toBe(2);
    expect(isoWeekNumber('2026-05-11')).toBe(20);
  });

  it('should_formatWeekRange_when_givenBounds', () => {
    const label = weekRangeLabel('2026-05-11', '2026-05-15', 'en-US');
    expect(label).toMatch(/11/);
    expect(label).toMatch(/15/);
    expect(label).toContain('2026');
    expect(label).toContain('–');
  });
});
