import {
  useCallback,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
  type RefObject,
} from 'react';
import { useTranslation } from 'react-i18next';
import { FloorPlanMarker } from './FloorPlanMarker';
import { FloorPlanTooltip } from './FloorPlanTooltip';
import type { FloorPlanDesk } from '../types/floorPlan';
import { isPlaced, matchesFilter } from '../utils/floorPlan';
import type { FloorPlanFilterValue } from './FloorPlanFilters';
import type { FloorPlanViewport } from '../hooks/useFloorPlanViewport';
import type { DragPosition } from '../hooks/useDeskDrag';
import floorPlanImage from '../assets/floor-plan.png';

export type { DragPosition } from '../hooks/useDeskDrag';

// Geometría del tooltip flotante (píxeles). Debe casar con el ancho de `.floor-tip`.
const TIP_WIDTH = 208;
const TIP_EDGE = 10; // margen mínimo respecto al borde horizontal del lienzo
const ARROW_INSET = 18; // la puntita nunca se acerca más de esto al borde del tooltip
const FLIP_BELOW_Y = 150; // si el marcador está a menos de esto del borde superior, abajo

// Estado del tooltip activo: id del puesto + posición ya resuelta en la capa no clipada.
interface TipState {
  deskId: number;
  left: number;
  top: number;
  below: boolean;
  arrow: number;
}

interface FloorPlanSurfaceProps {
  desks: FloorPlanDesk[];
  editMode: boolean;
  dragPos: DragPosition | null;
  filter: FloorPlanFilterValue | null;
  viewport: FloorPlanViewport;
  surfaceRef: RefObject<HTMLDivElement>;
  // Puesto elegido en el plano-selector: se realza (SELECTED) y se marca accesible.
  selectedDeskId?: number | null;
  // Puesto enfocado al llegar desde la rejilla ("Ver en plano"): anillo de acento
  // (`focusDeskId`) + pulso temporal mientras `focusPulsing` esté activo.
  focusDeskId?: number | null;
  focusPulsing?: boolean;
  onRequest: (desk: FloorPlanDesk) => void;
  onDragStart: (desk: FloorPlanDesk, event: ReactPointerEvent<HTMLButtonElement>) => void;
}

// Superficie del plano: viewport con zoom/pan (transform CSS) que contiene la
// imagen de planta y un marcador por puesto colocado, más un listado aparte de
// los puestos sin posición. La leyenda de estados la aportan los chips de filtro.
export function FloorPlanSurface({
  desks,
  editMode,
  dragPos,
  filter,
  viewport,
  surfaceRef,
  selectedDeskId = null,
  focusDeskId = null,
  focusPulsing = false,
  onRequest,
  onDragStart,
}: FloorPlanSurfaceProps) {
  const { t } = useTranslation();
  const wrapRef = useRef<HTMLDivElement>(null);
  const [tip, setTip] = useState<TipState | null>(null);
  const placed = desks.filter(isPlaced);
  const unplaced = desks.filter((desk) => !isPlaced(desk));

  // Al pasar/enfocar un marcador medimos su rectángulo REAL en pantalla (ya refleja
  // el pan/zoom del plano) y colocamos el tooltip en `.floor-plan-surface-wrap`, una
  // capa sin overflow ni transform ⇒ no se recorta ni escala. Clamp horizontal para
  // que no sobresalga del lienzo, con la puntita reapuntada al marcador.
  const handleHover = useCallback(
    (desk: FloorPlanDesk, button: HTMLButtonElement | null) => {
      const wrap = wrapRef.current;
      const surface = surfaceRef.current;
      if (editMode || !button || !wrap || !surface) {
        setTip(null);
        return;
      }
      const wrapRect = wrap.getBoundingClientRect();
      const surfaceRect = surface.getBoundingClientRect();
      const rect = button.getBoundingClientRect();
      const cx = rect.left + rect.width / 2 - wrapRect.left;
      const cy = rect.top + rect.height / 2 - wrapRect.top;
      const surfaceLeft = surfaceRect.left - wrapRect.left;
      const surfaceRight = surfaceRect.right - wrapRect.left;
      const surfaceTop = surfaceRect.top - wrapRect.top;
      const half = TIP_WIDTH / 2;
      const minCenter = surfaceLeft + TIP_EDGE + half;
      const maxCenter = surfaceRight - TIP_EDGE - half;
      const center =
        maxCenter >= minCenter
          ? Math.min(Math.max(cx, minCenter), maxCenter)
          : (surfaceLeft + surfaceRight) / 2;
      const arrowMax = half - ARROW_INSET;
      const arrow = Math.min(Math.max(cx - center, -arrowMax), arrowMax);
      setTip({ deskId: desk.deskId, left: center, top: cy, below: cy - surfaceTop < FLIP_BELOW_Y, arrow });
    },
    [editMode, surfaceRef],
  );

  const tipDesk = tip ? placed.find((desk) => desk.deskId === tip.deskId) : undefined;

  function labelFor(desk: FloorPlanDesk, selected: boolean, focused: boolean): string {
    let key = 'floorPlan.markerLabel';
    if (focused) {
      key = 'floorPlan.markerLabelFocused';
    } else if (selected) {
      key = 'floorPlan.markerLabelSelected';
    }
    return t(key, {
      number: desk.deskNumber,
      state: t(`floorPlan.states.${desk.state}`),
    });
  }

  const worldStyle = {
    transform: `translate(${viewport.offsetX}px, ${viewport.offsetY}px) scale(${viewport.scale})`,
  };

  return (
    <div className="floor-plan-surface-wrap" ref={wrapRef}>
      <div
        ref={surfaceRef}
        data-testid="floor-plan-surface"
        className={`floor-plan-surface${editMode ? ' is-editing' : ''}`}
        onPointerDown={viewport.onPointerDown}
        onPointerMove={viewport.onPointerMove}
        onPointerUp={viewport.onPointerUp}
        onPointerCancel={viewport.onPointerUp}
      >
        <div className="plano-world" style={worldStyle}>
          <img src={floorPlanImage} alt={t('floorPlan.imageAlt')} className="floor-plan-image" />
          {placed.map((desk) => {
            const dragging = dragPos !== null && dragPos.deskId === desk.deskId;
            const selected = desk.deskId === selectedDeskId;
            const focused = focusDeskId !== null && desk.deskId === focusDeskId;
            return (
              <FloorPlanMarker
                key={desk.deskId}
                desk={desk}
                label={labelFor(desk, selected, focused)}
                editMode={editMode}
                dimmed={!matchesFilter(desk, filter)}
                selected={selected}
                focused={focused}
                pulsing={focused && focusPulsing}
                left={dragging ? dragPos.x : (desk.coordX ?? 0)}
                top={dragging ? dragPos.y : (desk.coordY ?? 0)}
                onRequest={onRequest}
                onDragStart={onDragStart}
                onHover={handleHover}
              />
            );
          })}
        </div>
      </div>

      {tip && tipDesk ? (
        <FloorPlanTooltip
          desk={tipDesk}
          left={tip.left}
          top={tip.top}
          below={tip.below}
          arrow={tip.arrow}
        />
      ) : null}

      {unplaced.length > 0 ? (
        <aside data-testid="floor-unplaced" className="floor-unplaced">
          <h2 className="floor-unplaced-title">{t('floorPlan.unplacedTitle')}</h2>
          <p className="floor-unplaced-hint">{t('floorPlan.unplacedHint')}</p>
          <ul className="floor-unplaced-list">
            {unplaced.map((desk) => (
              <li key={desk.deskId} className="floor-unplaced-item">
                {t('floorPlan.deskNumber', { number: desk.deskNumber })}
              </li>
            ))}
          </ul>
        </aside>
      ) : null}
    </div>
  );
}
