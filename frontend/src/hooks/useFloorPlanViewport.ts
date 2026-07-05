import { useCallback, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react';
import { ZOOM_STEP, clampScale } from '../utils/floorPlan';

export interface ViewportState {
  scale: number;
  offsetX: number;
  offsetY: number;
}

export interface FloorPlanViewport extends ViewportState {
  zoomIn: () => void;
  zoomOut: () => void;
  reset: () => void;
  panEnabled: boolean;
  onPointerDown: (event: ReactPointerEvent) => void;
  onPointerMove: (event: ReactPointerEvent) => void;
  onPointerUp: (event: ReactPointerEvent) => void;
}

interface PanStart {
  pointerId: number;
  clientX: number;
  clientY: number;
  offsetX: number;
  offsetY: number;
}

interface PinchStart {
  distance: number;
  scale: number;
}

const INITIAL: ViewportState = { scale: 1, offsetX: 0, offsetY: 0 };

function distanceBetween(a: { clientX: number; clientY: number }, b: { clientX: number; clientY: number }): number {
  return Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
}

// Zoom (botones + pinch de 2 dedos) y pan (arrastre de 1 puntero) del plano
// mediante Pointer Events. `panEnabled` desactiva el pan cuando el ADMIN está en
// modo edición (para no competir con el arrastre de marcadores).
export function useFloorPlanViewport(panEnabled: boolean): FloorPlanViewport {
  const [state, setState] = useState<ViewportState>(INITIAL);
  const pointers = useRef<Map<number, ReactPointerEvent>>(new Map());
  const panStart = useRef<PanStart | null>(null);
  const pinchStart = useRef<PinchStart | null>(null);

  const zoomTo = useCallback((next: number) => {
    setState((prev) => ({ ...prev, scale: clampScale(next) }));
  }, []);

  const zoomIn = useCallback(() => zoomTo(state.scale + ZOOM_STEP), [zoomTo, state.scale]);
  const zoomOut = useCallback(() => zoomTo(state.scale - ZOOM_STEP), [zoomTo, state.scale]);
  const reset = useCallback(() => setState(INITIAL), []);

  const beginPan = useCallback(
    (event: ReactPointerEvent) => {
      panStart.current = {
        pointerId: event.pointerId,
        clientX: event.clientX,
        clientY: event.clientY,
        offsetX: state.offsetX,
        offsetY: state.offsetY,
      };
    },
    [state.offsetX, state.offsetY],
  );

  const onPointerDown = useCallback(
    (event: ReactPointerEvent) => {
      pointers.current.set(event.pointerId, event);
      const points = [...pointers.current.values()];
      if (points.length === 2) {
        panStart.current = null;
        pinchStart.current = { distance: distanceBetween(points[0], points[1]), scale: state.scale };
      } else if (points.length === 1 && panEnabled) {
        beginPan(event);
      }
    },
    [panEnabled, state.scale, beginPan],
  );

  const applyPinch = useCallback((points: ReactPointerEvent[]) => {
    const start = pinchStart.current;
    if (!start || start.distance === 0) {
      return;
    }
    const ratio = distanceBetween(points[0], points[1]) / start.distance;
    setState((prev) => ({ ...prev, scale: clampScale(start.scale * ratio) }));
  }, []);

  const applyPan = useCallback((event: ReactPointerEvent) => {
    const start = panStart.current;
    if (!start || start.pointerId !== event.pointerId) {
      return;
    }
    setState((prev) => ({
      ...prev,
      offsetX: start.offsetX + (event.clientX - start.clientX),
      offsetY: start.offsetY + (event.clientY - start.clientY),
    }));
  }, []);

  const onPointerMove = useCallback(
    (event: ReactPointerEvent) => {
      if (pointers.current.has(event.pointerId)) {
        pointers.current.set(event.pointerId, event);
      }
      const points = [...pointers.current.values()];
      if (points.length === 2) {
        applyPinch(points);
      } else if (points.length === 1) {
        applyPan(event);
      }
    },
    [applyPinch, applyPan],
  );

  const onPointerUp = useCallback((event: ReactPointerEvent) => {
    pointers.current.delete(event.pointerId);
    if (pointers.current.size < 2) {
      pinchStart.current = null;
    }
    if (panStart.current?.pointerId === event.pointerId) {
      panStart.current = null;
    }
  }, []);

  return {
    ...state,
    zoomIn,
    zoomOut,
    reset,
    panEnabled,
    onPointerDown,
    onPointerMove,
    onPointerUp,
  };
}
