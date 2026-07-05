import { type PointerEvent as ReactPointerEvent, type RefObject } from 'react';
import { useTranslation } from 'react-i18next';
import { FloorPlanMarker } from './FloorPlanMarker';
import type { DeskState, FloorPlanDesk } from '../types/floorPlan';
import { EXECUTIVE_SYMBOL, isPlaced, markerStateClass, matchesFilter } from '../utils/floorPlan';
import type { FloorPlanFilterValue } from './FloorPlanFilters';
import type { FloorPlanViewport } from '../hooks/useFloorPlanViewport';
import type { DragPosition } from '../hooks/useDeskDrag';
import floorPlanImage from '../assets/floor-plan-neutral.svg';

// Orden de estados en la leyenda (sin literales repetidos, S1192).
const LEGEND_STATES: DeskState[] = ['FREE', 'MINE', 'ASSIGNED', 'REQUESTED', 'RELEASED'];

export type { DragPosition } from '../hooks/useDeskDrag';

interface FloorPlanSurfaceProps {
  desks: FloorPlanDesk[];
  editMode: boolean;
  dragPos: DragPosition | null;
  filter: FloorPlanFilterValue | null;
  viewport: FloorPlanViewport;
  surfaceRef: RefObject<HTMLDivElement>;
  onRequest: (desk: FloorPlanDesk) => void;
  onDragStart: (desk: FloorPlanDesk, event: ReactPointerEvent<HTMLButtonElement>) => void;
}

// Superficie del plano: leyenda + viewport con zoom/pan (transform CSS) que
// contiene la imagen de planta y un marcador por puesto colocado, más un listado
// aparte de los puestos sin posición.
export function FloorPlanSurface({
  desks,
  editMode,
  dragPos,
  filter,
  viewport,
  surfaceRef,
  onRequest,
  onDragStart,
}: FloorPlanSurfaceProps) {
  const { t } = useTranslation();
  const placed = desks.filter(isPlaced);
  const unplaced = desks.filter((desk) => !isPlaced(desk));

  function labelFor(desk: FloorPlanDesk): string {
    return t('floorPlan.markerLabel', {
      number: desk.deskNumber,
      state: t(`floorPlan.states.${desk.state}`),
    });
  }

  const worldStyle = {
    transform: `translate(${viewport.offsetX}px, ${viewport.offsetY}px) scale(${viewport.scale})`,
  };

  return (
    <div className="floor-plan-surface-wrap">
      <ul className="floor-plan-legend" aria-label={t('floorPlan.legendLabel')}>
        {LEGEND_STATES.map((state) => (
          <li key={state} className="floor-plan-legend-item">
            <span className={`floor-legend-swatch ${markerStateClass(state)}`} aria-hidden="true" />
            {t(`floorPlan.states.${state}`)}
          </li>
        ))}
        <li className="floor-plan-legend-item">
          <span className="floor-legend-swatch floor-marker-executive" aria-hidden="true">
            {EXECUTIVE_SYMBOL}
          </span>
          {t('floorPlan.legendExecutive')}
        </li>
      </ul>

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
            return (
              <FloorPlanMarker
                key={desk.deskId}
                desk={desk}
                label={labelFor(desk)}
                editMode={editMode}
                dimmed={!matchesFilter(desk, filter)}
                left={dragging ? dragPos.x : (desk.coordX ?? 0)}
                top={dragging ? dragPos.y : (desk.coordY ?? 0)}
                onRequest={onRequest}
                onDragStart={onDragStart}
              />
            );
          })}
        </div>
      </div>

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
