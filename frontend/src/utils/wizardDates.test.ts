import { describe, expect, it } from 'vitest';
import {
  buildMonthGrid,
  enumerateRange,
  firstOfMonth,
  hasValidSelection,
  isDateSelected,
  isPastDate,
  isSpecificParking,
  resolveWizardDates,
  shiftMonth,
  toggleScatterDate,
} from './wizardDates';
import type { WizardState } from '../components/wizard/wizardTypes';

// Estado mínimo del asistente para las utilidades de fechas (solo campos usados).
function stateWith(overrides: Partial<WizardState>): WizardState {
  return {
    dateMode: 'SINGLE',
    singleDate: '',
    rangeStart: '',
    rangeEnd: '',
    scatterDates: [],
    ...overrides,
  } as WizardState;
}

describe('wizardDates', () => {
  it('firstOfMonth returns the 1st of the given month', () => {
    expect(firstOfMonth('2026-07-28')).toBe('2026-07-01');
  });

  it('shiftMonth moves forward and backward across year boundaries', () => {
    expect(shiftMonth('2026-07-01', 1)).toBe('2026-08-01');
    expect(shiftMonth('2026-01-01', -1)).toBe('2025-12-01');
  });

  it('buildMonthGrid returns a 42-cell grid flagging in-month days', () => {
    const grid = buildMonthGrid('2026-07-15');
    expect(grid).toHaveLength(42);
    expect(grid.some((c) => c.iso === '2026-07-01' && c.inMonth)).toBe(true);
    // Los bordes pertenecen a meses adyacentes (inMonth false).
    expect(grid[0].inMonth).toBe(false);
    expect(grid[grid.length - 1].inMonth).toBe(false);
  });

  it('enumerateRange includes both ends and rejects inverted/empty ranges', () => {
    expect(enumerateRange('2026-07-01', '2026-07-03')).toEqual([
      '2026-07-01',
      '2026-07-02',
      '2026-07-03',
    ]);
    expect(enumerateRange('2026-07-03', '2026-07-01')).toEqual([]);
    expect(enumerateRange('', '2026-07-01')).toEqual([]);
  });

  it('resolveWizardDates derives dates from the active mode', () => {
    expect(resolveWizardDates(stateWith({ dateMode: 'SINGLE', singleDate: '2026-07-10' }))).toEqual([
      '2026-07-10',
    ]);
    expect(resolveWizardDates(stateWith({ dateMode: 'SINGLE', singleDate: '' }))).toEqual([]);
    expect(
      resolveWizardDates(
        stateWith({ dateMode: 'RANGE', rangeStart: '2026-07-01', rangeEnd: '2026-07-02' }),
      ),
    ).toEqual(['2026-07-01', '2026-07-02']);
    // SCATTER: se ordena.
    expect(
      resolveWizardDates(stateWith({ dateMode: 'SCATTER', scatterDates: ['2026-07-05', '2026-07-01'] })),
    ).toEqual(['2026-07-01', '2026-07-05']);
  });

  it('toggleScatterDate adds a missing date and removes an existing one', () => {
    expect(toggleScatterDate(['2026-07-01'], '2026-07-02')).toEqual(['2026-07-01', '2026-07-02']);
    expect(toggleScatterDate(['2026-07-01', '2026-07-02'], '2026-07-01')).toEqual(['2026-07-02']);
  });

  it('isDateSelected reflects the active mode', () => {
    expect(isDateSelected(stateWith({ dateMode: 'SINGLE', singleDate: '2026-07-10' }), '2026-07-10')).toBe(true);
    expect(isDateSelected(stateWith({ dateMode: 'SCATTER', scatterDates: ['2026-07-10'] }), '2026-07-10')).toBe(true);
    expect(
      isDateSelected(
        stateWith({ dateMode: 'RANGE', rangeStart: '2026-07-01', rangeEnd: '2026-07-05' }),
        '2026-07-03',
      ),
    ).toBe(true);
    expect(
      isDateSelected(
        stateWith({ dateMode: 'RANGE', rangeStart: '2026-07-01', rangeEnd: '2026-07-05' }),
        '2026-07-09',
      ),
    ).toBe(false);
  });

  it('isPastDate compares against the reference day', () => {
    const now = new Date('2026-07-28T09:00:00');
    expect(isPastDate('2026-07-27', now)).toBe(true);
    expect(isPastDate('2026-07-29', now)).toBe(false);
  });

  it('hasValidSelection requires a mode and at least one date', () => {
    expect(hasValidSelection('SINGLE', ['2026-07-01'])).toBe(true);
    expect(hasValidSelection('SINGLE', [])).toBe(false);
  });

  it('isSpecificParking distinguishes a concrete space from auto-assignment', () => {
    expect(isSpecificParking(12)).toBe(true);
    expect(isSpecificParking(null)).toBe(false);
    expect(isSpecificParking('AUTO' as never)).toBe(false);
  });
});
