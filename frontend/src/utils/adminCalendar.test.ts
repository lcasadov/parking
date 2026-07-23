import { describe, expect, it } from 'vitest';
import { buildAdminCalendarCsv, summarizeAdminCalendar } from './adminCalendar';
import {
  adminCalendarFor,
  defaultAdminCalendar,
  emptyAdminCalendar,
} from '../mocks/calendarFixtures';
import type { CalendarCellState } from '../types/calendar';

describe('adminCalendar summary', () => {
  it('should_countStatesOnce_when_summarizingLoadedRows', () => {
    // P-01 cubre los 5 estados; P-02 es todo FREE. Solo datos ya cargados.
    const summary = summarizeAdminCalendar(defaultAdminCalendar.rows);
    expect(summary.spaces).toBe(2);
    // ASSIGNED (1) + REQUEST_APPROVED (1) cuentan como asignaciones.
    expect(summary.assignments).toBe(2);
    expect(summary.releases).toBe(1);
    expect(summary.requests).toBe(1);
  });

  it('should_returnZeroes_when_noRows', () => {
    const summary = summarizeAdminCalendar(emptyAdminCalendar.rows);
    expect(summary).toEqual({ spaces: 0, assignments: 0, releases: 0, requests: 0 });
  });
});

describe('adminCalendar CSV', () => {
  const stateLabel = (state: CalendarCellState): string => state;
  const dayLabel = (iso: string): string => iso;

  it('should_serializeGrid_when_buildingCsvFromLoadedData', () => {
    const csv = buildAdminCalendarCsv(
      adminCalendarFor('2026-05-11'),
      'Space',
      dayLabel,
      stateLabel,
    );
    const lines = csv.split('\r\n');
    expect(lines).toHaveLength(3); // cabecera + 2 filas
    expect(lines[0]).toBe('Space,2026-05-11,2026-05-12,2026-05-13,2026-05-14,2026-05-15');
    expect(lines[1]).toBe('P-01,ASSIGNED,RELEASED,REQUEST_PENDING,REQUEST_APPROVED,FREE');
    expect(lines[2]).toBe('P-02,FREE,FREE,FREE,FREE,FREE');
  });

  it('should_quoteValues_when_containingCommaOrQuote', () => {
    const csv = buildAdminCalendarCsv(
      adminCalendarFor('2026-05-11'),
      'Plaza, zona',
      dayLabel,
      () => 'a"b',
    );
    const header = csv.split('\r\n')[0];
    expect(header.startsWith('"Plaza, zona"')).toBe(true);
    expect(csv).toContain('"a""b"');
  });
});
