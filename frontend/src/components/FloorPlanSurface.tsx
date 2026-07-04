import { type MouseEvent, type RefObject } from 'react';
import { useTranslation } from 'react-i18next';
import { FloorPlanMarker } from './FloorPlanMarker';
import type { DeskState, FloorPlanDesk } from '../types/floorPlan';
import { isPlaced, markerStateClass } from '../utils/floorPlan';
import floorPlanImage from '../assets/floor-plan-placeholder.svg';

// Orden de estados en la leyenda (sin literales repetidos, S1192).
const LEGEND_STATES: DeskState[] = ['FREE', 'MINE', 'ASSIGNED', 'REQUESTED', 'RELEASED'];

export interface DragPosition {
  deskId: number;
  x: number;
  y: number;
}

interface FloorPlanSurfaceProps {
  desks: FloorPlanDesk[];
  editMode: boolean;
  dragPos: DragPosition | null;
  surfaceRef: RefObject<HTMLDivElement>;
  onRequest: (desk: FloorPlanDesk) => void;
  onDragStart: (desk: FloorPlanDesk, event: MouseEvent<HTMLButtonElement>) => void;
}

// Superficie del plano: imagen de planta + un marcador por puesto colocado, más
// un listado aparte de los puestos sin posición (atenuados, no clicables en el plano).
export function FloorPlanSurface({
  desks,
  editMode,
  dragPos,
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

  return (
    <div className="floor-plan-layout">
      <ul className="floor-plan-legend" aria-label={t('floorPlan.legendLabel')}>
        {LEGEND_STATES.map((state) => (
          <li key={state} className="floor-plan-legend-item">
            <span className={`floor-legend-swatch ${markerStateClass(state)}`} aria-hidden="true" />
            {t(`floorPlan.states.${state}`)}
          </li>
        ))}
        <li className="floor-plan-legend-item">
          <span className="floor-legend-swatch floor-marker-executive" aria-hidden="true" />
          {t('floorPlan.legendExecutive')}
        </li>
      </ul>

      <div ref={surfaceRef} data-testid="floor-plan-surface" className="floor-plan-surface">
        <img src={floorPlanImage} alt={t('floorPlan.imageAlt')} className="floor-plan-image" />
        {placed.map((desk) => {
          const dragging = dragPos !== null && dragPos.deskId === desk.deskId;
          return (
            <FloorPlanMarker
              key={desk.deskId}
              desk={desk}
              label={labelFor(desk)}
              editMode={editMode}
              left={dragging ? dragPos.x : (desk.coordX ?? 0)}
              top={dragging ? dragPos.y : (desk.coordY ?? 0)}
              onRequest={onRequest}
              onDragStart={onDragStart}
            />
          );
        })}
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
