import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useTableSort } from './useTableSort';

describe('useTableSort', () => {
  it('should_cycle_none_asc_desc_none_and_expose_sortParam', () => {
    const { result } = renderHook(() => useTableSort());
    expect(result.current.sort).toBeNull();
    expect(result.current.sortParam).toBeUndefined();

    act(() => result.current.toggle('createdAt'));
    expect(result.current.sort).toEqual({ field: 'createdAt', dir: 'asc' });
    expect(result.current.sortParam).toBe('createdAt,asc');

    act(() => result.current.toggle('createdAt'));
    expect(result.current.sort).toEqual({ field: 'createdAt', dir: 'desc' });
    expect(result.current.sortParam).toBe('createdAt,desc');

    act(() => result.current.toggle('createdAt'));
    expect(result.current.sort).toBeNull();
    expect(result.current.sortParam).toBeUndefined();
  });

  it('should_reset_to_asc_when_switching_to_another_column', () => {
    const { result } = renderHook(() => useTableSort());
    act(() => result.current.toggle('createdAt'));
    act(() => result.current.toggle('createdAt')); // desc
    act(() => result.current.toggle('requestedDate'));
    expect(result.current.sort).toEqual({ field: 'requestedDate', dir: 'asc' });
    expect(result.current.sortParam).toBe('requestedDate,asc');
  });
});
