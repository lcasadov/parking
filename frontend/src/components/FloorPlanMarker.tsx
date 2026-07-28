import { type PointerEvent as ReactPointerEvent } from 'react';
import { FloorPlanOccupantPin } from './FloorPlanOccupantPin';
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
  // Realce "puesto enfocado" al llegar desde la rejilla ("Ver en plano"): anillo de
  // acento persistente (`focused`) + animación de pulso temporal (`pulsing`, ~3-4s).
  focused?: boolean;
  pulsing?: boolean;
  // Énfasis de disponibilidad (modo explorar): 'free' hace latir suave los puestos
  // libres/elegibles; 'muted' atenúa los no disponibles para que el ojo vaya a lo libre.
  emphasis?: 'free' | 'muted' | null;
  onRequest: (desk: FloorPlanDesk) => void;
  onDragStart: (desk: FloorPlanDesk, event: ReactPointerEvent<HTMLButtonElement>) => void;
  // Notifica al plano el hover/focus para renderizar el tooltip en una capa no
  // clipada: el botón (para medir su posición real) o null al salir.
  onHover: (desk: FloorPlanDesk, button: HTMLButtonElement | null) => void;
}

// Construye la lista de clases del marcador según su estado visual. Extraído del
// componente para mantener su complejidad cognitiva baja (S3776). En modo edición
// los marcadores se pintan neutros (gris) para enfocar el reposicionamiento; fuera
// de él, el color semántico de estado o el realce de selección (SELECTED).
function markerClasses(args: {
  desk: FloorPlanDesk;
  editMode: boolean;
  selected: boolean;
  isExecutive: boolean;
  dimmed: boolean;
  focused: boolean;
  pulsing: boolean;
  emphasis: 'free' | 'muted' | null;
}): string {
  const { desk, editMode, selected, isExecutive, dimmed, focused, pulsing, emphasis } = args;
  const colorClass = editMode ? 'floor-marker-neutral' : markerColorClass(desk.state, selected);
  return [
    'floor-marker',
    colorClass,
    isExecutive ? 'floor-marker-executive' : '',
    editMode ? 'floor-marker-editing' : '',
    dimmed ? 'floor-marker-dimmed' : '',
    focused ? 'floor-marker-focused' : '',
    pulsing ? 'floor-marker-pulse' : '',
    emphasis === 'free' ? 'floor-marker-free-pulse' : '',
    emphasis === 'muted' ? 'floor-marker-muted' : '',
  ]
    .filter(Boolean)
    .join(' ');
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
  focused = false,
  pulsing = false,
  emphasis = null,
  onRequest,
  onDragStart,
  onHover,
}: FloorPlanMarkerProps) {
  const isExecutive = desk.category === 'EXECUTIVE';
  const classes = markerClasses({ desk, editMode, selected, isExecutive, dimmed, focused, pulsing, emphasis });

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
        aria-current={focused ? 'location' : undefined}
        disabled={!editMode && desk.state !== 'FREE'}
        onClick={handleClick}
        onPointerDown={handlePointerDown}
        onPointerEnter={(event) => onHover(desk, event.currentTarget)}
        onPointerLeave={() => onHover(desk, null)}
        onFocus={(event) => onHover(desk, event.currentTarget)}
        onBlur={() => onHover(desk, null)}
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
    </>
  );
}
