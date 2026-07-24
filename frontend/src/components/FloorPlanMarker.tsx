import { useState, type PointerEvent as ReactPointerEvent } from 'react';
import { FloorPlanOccupantPin } from './FloorPlanOccupantPin';
import { FloorPlanTooltip } from './FloorPlanTooltip';
import type { FloorPlanDesk } from '../types/floorPlan';
import { EXECUTIVE_SYMBOL, isOccupiedState, markerColorClass } from '../utils/floorPlan';

interface FloorPlanMarkerProps {
  desk: FloorPlanDesk;
  label: string;
  editMode: boolean;
  // Posición efectiva en % (puede diferir de desk.coord* durante el arrastre).
  left: number;
  top: number;
  // Resalte por filtro activo (atenúa los que no coinciden).
  dimmed?: boolean;
  // Realce "puesto elegido" en el plano-selector (estado visual SELECTED, solo UI).
  selected?: boolean;
  onRequest: (desk: FloorPlanDesk) => void;
  onDragStart: (desk: FloorPlanDesk, event: ReactPointerEvent<HTMLButtonElement>) => void;
}

// Un marcador de puesto sobre el plano: botón accesible posicionado por %.
// En modo edición inicia el arrastre (Pointer Events, ratón + táctil); fuera de
// él, un puesto FREE se solicita. Al pasar/enfocar muestra un tooltip con el
// detalle y, si está ocupado, un pin de ocupante. EXECUTIVE se distingue con ◆.
export function FloorPlanMarker({
  desk,
  label,
  editMode,
  left,
  top,
  dimmed = false,
  selected = false,
  onRequest,
  onDragStart,
}: FloorPlanMarkerProps) {
  const [active, setActive] = useState(false);

  const isExecutive = desk.category === 'EXECUTIVE';
  // En modo edición los marcadores se pintan neutros (gris uniforme) para
  // enfocar el reposicionamiento; fuera de él, el color semántico de estado, o el
  // realce de selección (SELECTED) cuando el puesto está elegido en el selector.
  const colorClass = editMode ? 'floor-marker-neutral' : markerColorClass(desk.state, selected);
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
  const showPin = !editMode && !dimmed && isOccupiedState(desk.state);
  const pos = { left: `${left}%`, top: `${top}%` };

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
    <>
      <button
        type="button"
        data-testid="floor-marker"
        className={classes}
        style={pos}
        aria-label={label}
        aria-pressed={selected ? true : undefined}
        disabled={!editMode && desk.state !== 'FREE'}
        onClick={handleClick}
        onPointerDown={handlePointerDown}
        onPointerEnter={() => setActive(true)}
        onPointerLeave={() => setActive(false)}
        onFocus={() => setActive(true)}
        onBlur={() => setActive(false)}
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

      {showPin ? <FloorPlanOccupantPin desk={desk} style={pos} /> : null}
      <FloorPlanTooltip desk={desk} show={!editMode && active} style={pos} below={top < 18} />
    </>
  );
}
