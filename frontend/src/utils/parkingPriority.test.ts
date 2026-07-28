import { describe, expect, it } from 'vitest';
import { floorOfLabel, groupEligibleByPriority, isHighCategory } from './parkingPriority';
import type { EligibleResource } from '../components/wizard/wizardTypes';

const eligible: EligibleResource[] = [
  { resourceId: 1, label: '1001' },
  { resourceId: 2, label: '1002' },
  { resourceId: 3, label: '3001' },
  { resourceId: 4, label: '4001' },
  { resourceId: 5, label: '4002' },
];

describe('parkingPriority', () => {
  it('deriva el millar (planta) desde la etiqueta', () => {
    expect(floorOfLabel('4001')).toBe(4);
    expect(floorOfLabel('P-1005')).toBe(1);
    expect(floorOfLabel('sin-numero')).toBe(0);
  });

  it('clasifica las categorías altas', () => {
    expect(isHighCategory('CEO')).toBe(true);
    expect(isHighCategory('DIRECTOR_N2')).toBe(true);
    expect(isHighCategory('EMPLEADO')).toBe(false);
    expect(isHighCategory(undefined)).toBe(false);
  });

  it('categoría BAJA sugiere la planta más profunda (millar mayor) primero', () => {
    const grouped = groupEligibleByPriority(eligible, 'EMPLEADO');
    expect(grouped.preferredFloor).toBe(4);
    expect(grouped.suggested.map((r) => r.label)).toEqual(['4001', '4002']);
    // Otras ordenadas por prioridad baja: millar 3 antes que millar 1.
    expect(grouped.others.map((r) => r.label)).toEqual(['3001', '1001', '1002']);
  });

  it('categoría ALTA sugiere la planta más alta (millar menor) primero', () => {
    const grouped = groupEligibleByPriority(eligible, 'CEO');
    expect(grouped.preferredFloor).toBe(1);
    expect(grouped.suggested.map((r) => r.label)).toEqual(['1001', '1002']);
    expect(grouped.others.map((r) => r.label)).toEqual(['3001', '4001', '4002']);
  });

  it('sin categoría: todo en otras, ordenado por número, sin sugeridas', () => {
    const grouped = groupEligibleByPriority(eligible, undefined);
    expect(grouped.preferredFloor).toBeNull();
    expect(grouped.suggested).toEqual([]);
    expect(grouped.others.map((r) => r.label)).toEqual(['1001', '1002', '3001', '4001', '4002']);
  });
});
