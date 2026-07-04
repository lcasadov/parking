import { type MouseEvent } from 'react';
import type { FloorPlanDesk } from '../types/floorPlan';
import { markerStateClass } from '../utils/floorPlan';

interface FloorPlanMarkerProps {
  desk: FloorPlanDesk;
  label: string;
  editMode: boolean;
  // Posición efectiva en % (puede diferir de desk.coord* durante el arrastre).
  left: number;
  top: number;
  onRequest: (desk: FloorPlanDesk) => void;
  onDragStart: (desk: FloorPlanDesk, event: MouseEvent<HTMLButtonElement>) => void;
}

// Un marcador de puesto sobre el plano: botón accesible posicionado por %.
// En modo edición inicia el arrastre; fuera de él, un puesto FREE se solicita.
export function FloorPlanMarker({
  desk,
  label,
  editMode,
  left,
  top,
  onRequest,
  onDragStart,
}: FloorPlanMarkerProps) {
  const isExecutive = desk.category === 'EXECUTIVE';
  const classes = [
    'floor-marker',
    markerStateClass(desk.state),
    isExecutive ? 'floor-marker-executive' : '',
    editMode ? 'floor-marker-editing' : '',
  ]
    .filter(Boolean)
    .join(' ');

  const canRequest = !editMode && desk.state === 'FREE';

  function handleClick(): void {
    if (canRequest) {
      onRequest(desk);
    }
  }

  function handleMouseDown(event: MouseEvent<HTMLButtonElement>): void {
    if (editMode) {
      onDragStart(desk, event);
    }
  }

  return (
    <button
      type="button"
      data-testid="floor-marker"
      className={classes}
      style={{ left: `${left}%`, top: `${top}%` }}
      aria-label={label}
      disabled={!editMode && desk.state !== 'FREE'}
      onClick={handleClick}
      onMouseDown={handleMouseDown}
    >
      <span aria-hidden="true" className="floor-marker-number">
        {desk.deskNumber}
      </span>
    </button>
  );
}
