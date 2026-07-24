import { useCallback, useRef, type PointerEvent as ReactPointerEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { FloorPlanViewport } from '../hooks/useFloorPlanViewport';
import floorPlanImage from '../assets/floor-plan.png';

interface FloorPlanMinimapProps {
  viewport: FloorPlanViewport;
  // Tamaño real del lienzo (px). Coincide con el tamaño natural del "mundo" a
  // escala 1, así que sirve para convertir coordenadas minimapa ↔ mundo.
  box: { w: number; h: number };
}

// Ancho del minimapa (px); el alto se deriva del ratio real del lienzo.
const MINI_W = 150;

// Minimapa del plano: vista general fija (no se transforma) con un recuadro que
// representa la porción visible del lienzo grande. Arrastrar (o pulsar) el recuadro
// PANEA el grande (el grande no se arrastra: solo zoom por rectángulo). El paneo se
// resuelve centrando la vista en el punto del mundo señalado en el minimapa.
export function FloorPlanMinimap({ viewport, box }: FloorPlanMinimapProps) {
  const { t } = useTranslation();
  const ref = useRef<HTMLDivElement>(null);
  const { scale, offsetX, offsetY, centerOnPoint } = viewport;

  if (box.w === 0 || box.h === 0) {
    return null;
  }

  const miniH = (MINI_W * box.h) / box.w;
  const miniScale = MINI_W / box.w;

  // Recuadro del viewport en coordenadas del minimapa (clamp a sus bordes).
  const rawLeft = (-offsetX / scale) * miniScale;
  const rawTop = (-offsetY / scale) * miniScale;
  const rectW = Math.min(MINI_W, MINI_W / scale);
  const rectH = Math.min(miniH, miniH / scale);
  const left = Math.min(Math.max(rawLeft, 0), MINI_W - rectW);
  const top = Math.min(Math.max(rawTop, 0), miniH - rectH);

  const panFromEvent = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      const rect = ref.current?.getBoundingClientRect();
      if (!rect) {
        return;
      }
      const mmx = event.clientX - rect.left;
      const mmy = event.clientY - rect.top;
      centerOnPoint(mmx / miniScale, mmy / miniScale, box.w, box.h);
    },
    [centerOnPoint, miniScale, box.w, box.h],
  );

  const dragging = useRef(false);

  return (
    <div
      ref={ref}
      className="floor-mini"
      style={{ width: `${MINI_W}px`, height: `${miniH}px` }}
      role="application"
      aria-label={t('floorPlan.minimap.label')}
      onPointerDown={(event) => {
        dragging.current = true;
        event.currentTarget.setPointerCapture(event.pointerId);
        panFromEvent(event);
      }}
      onPointerMove={(event) => {
        if (dragging.current) {
          panFromEvent(event);
        }
      }}
      onPointerUp={(event) => {
        dragging.current = false;
        event.currentTarget.releasePointerCapture(event.pointerId);
      }}
      onPointerCancel={() => {
        dragging.current = false;
      }}
    >
      <img src={floorPlanImage} alt="" className="floor-mini-img" draggable={false} />
      <div
        className="floor-mini-view"
        style={{ left: `${left}px`, top: `${top}px`, width: `${rectW}px`, height: `${rectH}px` }}
        aria-hidden="true"
      />
    </div>
  );
}
