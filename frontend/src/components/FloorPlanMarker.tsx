import { type PointerEvent as ReactPointerEvent } from 'react';
import type { FloorPlanDesk } from '../types/floorPlan';
import { EXECUTIVE_SYMBOL, markerStateClass } from '../utils/floorPlan';

interface FloorPlanMarkerProps {
  desk: FloorPlanDesk;
  label: string;
  editMode: boolean;
  // Posición efectiva en % (puede diferir de desk.coord* durante el arrastre).
  left: number;
  top: number;
  // Resalte por filtro activo (atenúa los que no coinciden).
  dimmed?: boolean;
  onRequest: (desk: FloorPlanDesk) => void;
  onDragStart: (desk: FloorPlanDesk, event: ReactPointerEvent<HTMLButtonElement>) => void;
}

// Un marcador de puesto sobre el plano: botón accesible posicionado por %.
// En modo edición inicia el arrastre (Pointer Events, ratón + táctil); fuera de
// él, un puesto FREE se solicita. EXECUTIVE se distingue con anillo ámbar + ◆.
export function FloorPlanMarker({
  desk,
  label,
  editMode,
  left,
  top,
  dimmed = false,
  onRequest,
  onDragStart,
}: FloorPlanMarkerProps) {
  const isExecutive = desk.category === 'EXECUTIVE';
  // En modo edición los marcadores se pintan neutros (gris uniforme) para
  // enfocar el reposicionamiento; fuera de él, el color semántico de estado.
  const colorClass = editMode ? 'floor-marker-neutral' : markerStateClass(desk.state);
  const classes = [
    'floor-marker',
    colorClass,
    isExecutive ? 'floor-marker-executive' : '',
    editMode ? 'floor-marker-editing' : '',
    dimmed ? 'floor-marker-dimmed' : '',
  ]
    .filter(Boolean)
    .join(' ');

  const canRequest = !editMode && desk.state === 'FREE';

  function handleClick(): void {
    if (canRequest) {
      onRequest(desk);
    }
  }

  function handlePointerDown(event: ReactPointerEvent<HTMLButtonElement>): void {
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
      onPointerDown={handlePointerDown}
    >
      <span aria-hidden="true" className="floor-marker-number">
        {desk.deskNumber}
      </span>
      {isExecutive ? (
        <span aria-hidden="true" className="floor-marker-exec-badge">
          {EXECUTIVE_SYMBOL}
        </span>
      ) : null}
    </button>
  );
}
