import { useCallback, useEffect, useRef, useState, type PointerEvent as ReactPointerEvent, type RefObject } from 'react';
import { nextCoord } from '../utils/floorPlan';
import type { FloorPlanDesk } from '../types/floorPlan';

// Posición efectiva (en % del ancho/alto de la superficie) de un marcador durante
// un arrastre en curso.
export interface DragPosition {
  deskId: number;
  x: number;
  y: number;
}

// Estado interno de un arrastre (coordenadas en % relativas a la superficie).
interface DragState {
  deskId: number;
  startClientX: number;
  startClientY: number;
  baseX: number;
  baseY: number;
  rectWidth: number;
  rectHeight: number;
  x: number;
  y: number;
  moved: boolean;
}

// Arrastre de marcadores del ADMIN mediante Pointer Events (unifica ratón y
// táctil): al soltar, persiste la nueva posición vía `onPersist`. Devuelve la
// posición transitoria del marcador arrastrado y el manejador `startDrag`.
export function useDeskDrag(
  surfaceRef: RefObject<HTMLDivElement>,
  onPersist: (deskId: number, coordX: number, coordY: number) => void,
): { dragPos: DragPosition | null; startDrag: (desk: FloorPlanDesk, event: ReactPointerEvent) => void } {
  const [dragPos, setDragPos] = useState<DragPosition | null>(null);
  const dragRef = useRef<DragState | null>(null);
  const persistRef = useRef(onPersist);
  persistRef.current = onPersist;

  const handleMove = useCallback((event: PointerEvent) => {
    const drag = dragRef.current;
    if (!drag) {
      return;
    }
    const x = nextCoord(drag.baseX, event.clientX - drag.startClientX, drag.rectWidth);
    const y = nextCoord(drag.baseY, event.clientY - drag.startClientY, drag.rectHeight);
    drag.x = x;
    drag.y = y;
    drag.moved = true;
    setDragPos({ deskId: drag.deskId, x, y });
  }, []);

  const handleEnd = useCallback(() => {
    const drag = dragRef.current;
    window.removeEventListener('pointermove', handleMove);
    window.removeEventListener('pointerup', handleEnd);
    window.removeEventListener('pointercancel', handleEnd);
    if (drag?.moved) {
      persistRef.current(drag.deskId, drag.x, drag.y);
    }
    dragRef.current = null;
    setDragPos(null);
  }, [handleMove]);

  const startDrag = useCallback(
    (desk: FloorPlanDesk, event: ReactPointerEvent) => {
      const surface = surfaceRef.current;
      if (!surface) {
        return;
      }
      const rect = surface.getBoundingClientRect();
      dragRef.current = {
        deskId: desk.deskId,
        startClientX: event.clientX,
        startClientY: event.clientY,
        baseX: desk.coordX ?? 0,
        baseY: desk.coordY ?? 0,
        rectWidth: rect.width,
        rectHeight: rect.height,
        x: desk.coordX ?? 0,
        y: desk.coordY ?? 0,
        moved: false,
      };
      window.addEventListener('pointermove', handleMove);
      window.addEventListener('pointerup', handleEnd);
      window.addEventListener('pointercancel', handleEnd);
    },
    [surfaceRef, handleMove, handleEnd],
  );

  // Limpieza defensiva si el componente se desmonta a mitad de un arrastre.
  useEffect(() => {
    return () => {
      window.removeEventListener('pointermove', handleMove);
      window.removeEventListener('pointerup', handleEnd);
      window.removeEventListener('pointercancel', handleEnd);
    };
  }, [handleMove, handleEnd]);

  return { dragPos, startDrag };
}
