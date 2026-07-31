import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type MouseEvent as ReactMouseEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
  type RefObject,
} from 'react';
import { useTranslation } from 'react-i18next';
import { FloorPlanMarker } from './FloorPlanMarker';
import { FloorPlanMinimap } from './FloorPlanMinimap';
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

// Rectángulo de selección (marquee) en px del lienzo mientras se dibuja.
interface MarqueeState {
  x0: number;
  y0: number;
  x1: number;
  y1: number;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
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
  // Modo EXPLORAR (vista, no edición): el grande NO se arrastra; se navega con
  // zoom por rectángulo (marquee) + minimapa (panea) + Escape (encuadre). Requiere
  // que el viewport se cree con panEnabled=false. En edición se conserva el paneo.
  explore?: boolean;
  // Énfasis de disponibilidad: los puestos libres/elegibles laten y el resto se
  // atenúa, para que el ojo vaya directo a lo reservable (solo en modo vista).
  emphasizeFree?: boolean;
  // Acciones extra en la esquina superior derecha, apiladas bajo el botón de pantalla
  // completa (p. ej. el lápiz de "Editar posiciones"). Opcional.
  overlayActions?: ReactNode;
  // Si es `false`, no se pinta el minimapa interno (el contenedor lo renderiza aparte,
  // p. ej. en la columna lateral del Plano). Por defecto `true` (resto de usos intactos).
  renderMinimap?: boolean;
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
  explore = false,
  emphasizeFree = false,
  overlayActions,
  renderMinimap = true,
  onRequest,
  onDragStart,
}: FloorPlanSurfaceProps) {
  const { t } = useTranslation();
  const wrapRef = useRef<HTMLDivElement>(null);
  const [tip, setTip] = useState<TipState | null>(null);
  const [box, setBox] = useState({ w: 0, h: 0 });
  const [maximized, setMaximized] = useState(false);
  const [marquee, setMarquee] = useState<MarqueeState | null>(null);
  const marqueeStart = useRef<{ x: number; y: number } | null>(null);
  const placed = desks.filter(isPlaced);
  const unplaced = desks.filter((desk) => !isPlaced(desk));
  // Explorar sólo tiene sentido fuera de edición (en edición se arrastran marcadores).
  const exploring = explore && !editMode;

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

  const { zoomAtPoint, zoomToRect, scale, reset } = viewport;

  // Mide el lienzo (tamaño natural del "mundo" a escala 1): lo necesita el minimapa
  // para convertir sus coordenadas al mundo.
  useEffect(() => {
    const surface = surfaceRef.current;
    if (!surface || typeof ResizeObserver === 'undefined') {
      return undefined;
    }
    const measure = () => setBox({ w: surface.clientWidth, h: surface.clientHeight });
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(surface);
    return () => observer.disconnect();
  }, [surfaceRef]);

  // El zoom con rueda/trackpad está deshabilitado a propósito: capturaba el scroll y
  // dificultaba desplazarse por la página con el cursor sobre el plano. El zoom se
  // hace ahora con el rectángulo de selección (marquee), el minimapa y los botones
  // de zoom; la rueda desplaza la página con normalidad.

  // Maximizado IN-APP (no la Fullscreen API nativa, que sube el lienzo al "top
  // layer" del navegador por encima de los diálogos Radix e impide asignar desde el
  // plano): un overlay fijo con z-index por debajo de los modales, de modo que el
  // diálogo de confirmación de asignación siga apareciendo por encima del plano.
  const toggleMaximize = useCallback(() => setMaximized((prev) => !prev), []);

  // Escape: si el plano está maximizado, primero lo restaura; si no, en modo
  // explorar vuelve al encuadre. (Los modales ya no se cierran con Escape.)
  useEffect(() => {
    if (!maximized && !exploring) {
      return undefined;
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') {
        return;
      }
      if (maximized) {
        setMaximized(false);
      } else {
        reset();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [maximized, exploring, reset]);

  // Doble clic: acerca hacia el punto; si ya está ampliado, vuelve al encuadre.
  const handleDoubleClick = useCallback(
    (event: ReactMouseEvent<HTMLDivElement>) => {
      if (editMode) {
        return;
      }
      const rect = surfaceRef.current?.getBoundingClientRect();
      if (!rect) {
        return;
      }
      if (scale > 1.05) {
        reset();
      } else {
        zoomAtPoint(1.8, event.clientX - rect.left, event.clientY - rect.top);
      }
    },
    [editMode, surfaceRef, scale, reset, zoomAtPoint],
  );

  // --- Pointer: paneo (viewport) en edición/no-explorar; marquee-zoom en explorar ---
  const handlePointerDown = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      viewport.onPointerDown(event);
      if (!exploring) {
        return;
      }
      // Sobre un marcador no se dibuja rectángulo (el marcador gestiona su clic).
      if ((event.target as HTMLElement).closest('[data-testid="floor-marker"]')) {
        return;
      }
      // Segundo puntero ⇒ pinch: cancela cualquier marquee en curso.
      if (marqueeStart.current) {
        marqueeStart.current = null;
        setMarquee(null);
        return;
      }
      const rect = surfaceRef.current?.getBoundingClientRect();
      if (!rect) {
        return;
      }
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      marqueeStart.current = { x, y };
      setMarquee({ x0: x, y0: y, x1: x, y1: y });
    },
    [viewport, exploring, surfaceRef],
  );

  const handlePointerMove = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      viewport.onPointerMove(event);
      if (!exploring || !marqueeStart.current) {
        return;
      }
      const rect = surfaceRef.current?.getBoundingClientRect();
      if (!rect) {
        return;
      }
      const x = clamp(event.clientX - rect.left, 0, rect.width);
      const y = clamp(event.clientY - rect.top, 0, rect.height);
      setMarquee((prev) => (prev ? { ...prev, x1: x, y1: y } : null));
    },
    [viewport, exploring, surfaceRef],
  );

  const handlePointerUp = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      viewport.onPointerUp(event);
      if (!exploring || !marqueeStart.current) {
        return;
      }
      marqueeStart.current = null;
      const rect = surfaceRef.current?.getBoundingClientRect();
      setMarquee((prev) => {
        if (prev && rect) {
          const rx = Math.min(prev.x0, prev.x1);
          const ry = Math.min(prev.y0, prev.y1);
          zoomToRect(rx, ry, Math.abs(prev.x1 - prev.x0), Math.abs(prev.y1 - prev.y0), rect.width, rect.height);
        }
        return null;
      });
    },
    [viewport, exploring, surfaceRef, zoomToRect],
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

  const marqueeStyle = marquee
    ? {
        left: `${Math.min(marquee.x0, marquee.x1)}px`,
        top: `${Math.min(marquee.y0, marquee.y1)}px`,
        width: `${Math.abs(marquee.x1 - marquee.x0)}px`,
        height: `${Math.abs(marquee.y1 - marquee.y0)}px`,
      }
    : null;

  return (
    <div className={`floor-plan-surface-wrap${maximized ? ' is-maximized' : ''}`} ref={wrapRef}>
      <div
        ref={surfaceRef}
        data-testid="floor-plan-surface"
        className={`floor-plan-surface${editMode ? ' is-editing' : ''}${exploring ? ' is-explore' : ''}`}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
        onDoubleClick={handleDoubleClick}
      >
        <div className="plano-world" style={worldStyle}>
          <img src={floorPlanImage} alt={t('floorPlan.imageAlt')} className="floor-plan-image" />
          {placed.map((desk) => {
            const dragging = dragPos !== null && dragPos.deskId === desk.deskId;
            const selected = desk.deskId === selectedDeskId;
            const focused = focusDeskId !== null && desk.deskId === focusDeskId;
            let emphasis: 'free' | 'muted' | null = null;
            if (emphasizeFree && !editMode && !selected && !focused) {
              emphasis = desk.state === 'FREE' ? 'free' : 'muted';
            }
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
                emphasis={emphasis}
                left={dragging ? dragPos.x : (desk.coordX ?? 0)}
                top={dragging ? dragPos.y : (desk.coordY ?? 0)}
                onRequest={onRequest}
                onDragStart={onDragStart}
                onHover={handleHover}
              />
            );
          })}
        </div>

        {marqueeStyle ? <div className="floor-marquee" style={marqueeStyle} aria-hidden="true" /> : null}

        <SurfaceTools maximized={maximized} onToggle={toggleMaximize} actions={overlayActions} />
      </div>

      {exploring && renderMinimap ? <FloorPlanMinimap viewport={viewport} box={box} /> : null}

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

// Herramientas de la esquina superior derecha del lienzo: botón de pantalla completa +
// acciones opcionales (p. ej. el lápiz de editar). Extraído para no cargar la complejidad
// cognitiva de FloorPlanSurface (Sonar S3776).
function SurfaceTools({
  maximized,
  onToggle,
  actions,
}: {
  maximized: boolean;
  onToggle: () => void;
  actions?: ReactNode;
}) {
  const { t } = useTranslation();
  const label = t(maximized ? 'floorPlan.fullscreen.exit' : 'floorPlan.fullscreen.enter');
  return (
    <div className="floor-surface-tools">
      <button
        type="button"
        className="floor-fullscreen-btn"
        aria-label={label}
        title={label}
        onPointerDown={(event) => event.stopPropagation()}
        onClick={onToggle}
      >
        <i className={`ti ti-${maximized ? 'arrows-minimize' : 'arrows-maximize'}`} aria-hidden="true" />
      </button>
      {actions}
    </div>
  );
}
