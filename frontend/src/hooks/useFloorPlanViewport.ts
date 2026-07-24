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
  // Zoom relativo (factor) manteniendo fijo el punto (px,py) bajo el cursor —
  // coordenadas en píxeles relativas al lienzo. Es el zoom de rueda/trackpad y
  // de doble clic: el sitio señalado no se mueve al acercar/alejar.
  zoomAtPoint: (factor: number, px: number, py: number) => void;
  // Centra el viewport en un punto (xPercent/yPercent = coordenadas 0-100 del
  // marcador) con un ligero acercamiento, midiendo el lienzo con width/height (px).
  focusOn: (xPercent: number, yPercent: number, width: number, height: number) => void;
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

// Acercamiento al centrar en un puesto (enlace "Ver en plano" desde la rejilla):
// destaca el marcador sin perder el contexto del plano.
const FOCUS_SCALE = 1.6;

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

  // Zoom hacia un punto: el punto del "mundo" bajo (px,py) se mantiene en (px,py)
  // tras reescalar, ajustando el offset. Base del zoom con rueda/trackpad/doble clic.
  const zoomAtPoint = useCallback((factor: number, px: number, py: number) => {
    setState((prev) => {
      const nextScale = clampScale(prev.scale * factor);
      if (nextScale === prev.scale) {
        return prev;
      }
      const worldX = (px - prev.offsetX) / prev.scale;
      const worldY = (py - prev.offsetY) / prev.scale;
      return {
        scale: nextScale,
        offsetX: px - worldX * nextScale,
        offsetY: py - worldY * nextScale,
      };
    });
  }, []);

  // Centra el marcador (xPercent/yPercent) en el lienzo de tamaño width x height,
  // aplicando FOCUS_SCALE. El layout del "mundo" no depende del transform, así que
  // width/height (rect del lienzo, ~alto de la imagen a escala 1) son estables.
  const focusOn = useCallback(
    (xPercent: number, yPercent: number, width: number, height: number) => {
      const scale = clampScale(FOCUS_SCALE);
      const targetX = (xPercent / 100) * width;
      const targetY = (yPercent / 100) * height;
      setState({
        scale,
        offsetX: width / 2 - scale * targetX,
        offsetY: height / 2 - scale * targetY,
      });
    },
    [],
  );

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
    zoomAtPoint,
    focusOn,
    panEnabled,
    onPointerDown,
    onPointerMove,
    onPointerUp,
  };
}
