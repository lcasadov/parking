import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useFloorPlanViewport } from './useFloorPlanViewport';
import { ZOOM_MAX, ZOOM_MIN, ZOOM_STEP } from '../utils/floorPlan';
import type { PointerEvent as ReactPointerEvent } from 'react';

// Construye un evento de puntero mínimo para el hook (solo se usan id/coords).
function pointer(pointerId: number, clientX: number, clientY: number): ReactPointerEvent {
  return { pointerId, clientX, clientY } as ReactPointerEvent;
}

describe('useFloorPlanViewport', () => {
  it('should_zoom_in_and_out_by_step_and_reset', () => {
    const { result } = renderHook(() => useFloorPlanViewport(true));
    expect(result.current.scale).toBe(1);

    act(() => result.current.zoomIn());
    expect(result.current.scale).toBeCloseTo(1 + ZOOM_STEP);

    act(() => result.current.zoomOut());
    expect(result.current.scale).toBeCloseTo(1);

    act(() => result.current.zoomIn());
    act(() => result.current.reset());
    expect(result.current.scale).toBe(1);
    expect(result.current.offsetX).toBe(0);
  });

  it('should_clamp_zoom_to_bounds', () => {
    const { result } = renderHook(() => useFloorPlanViewport(true));
    for (let i = 0; i < 20; i += 1) {
      act(() => result.current.zoomIn());
    }
    expect(result.current.scale).toBe(ZOOM_MAX);
    for (let i = 0; i < 40; i += 1) {
      act(() => result.current.zoomOut());
    }
    expect(result.current.scale).toBe(ZOOM_MIN);
  });

  it('should_pan_with_a_single_pointer_when_enabled', () => {
    const { result } = renderHook(() => useFloorPlanViewport(true));
    act(() => result.current.onPointerDown(pointer(1, 100, 100)));
    act(() => result.current.onPointerMove(pointer(1, 130, 150)));
    expect(result.current.offsetX).toBe(30);
    expect(result.current.offsetY).toBe(50);
    act(() => result.current.onPointerUp(pointer(1, 130, 150)));
  });

  it('should_not_pan_when_disabled_in_edit_mode', () => {
    const { result } = renderHook(() => useFloorPlanViewport(false));
    act(() => result.current.onPointerDown(pointer(1, 100, 100)));
    act(() => result.current.onPointerMove(pointer(1, 130, 150)));
    expect(result.current.offsetX).toBe(0);
    expect(result.current.offsetY).toBe(0);
  });

  it('should_zoom_by_pinch_with_two_pointers', () => {
    const { result } = renderHook(() => useFloorPlanViewport(true));
    act(() => result.current.onPointerDown(pointer(1, 0, 0)));
    act(() => result.current.onPointerDown(pointer(2, 100, 0)));
    // Separate the fingers to double the distance → scale doubles (clamped).
    act(() => result.current.onPointerMove(pointer(2, 200, 0)));
    expect(result.current.scale).toBeGreaterThan(1);
    act(() => result.current.onPointerUp(pointer(1, 0, 0)));
    act(() => result.current.onPointerUp(pointer(2, 200, 0)));
  });
});
