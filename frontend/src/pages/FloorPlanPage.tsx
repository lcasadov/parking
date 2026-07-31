import { useEffect, useRef, useState, type ReactElement } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { PageFrame } from '../components/PageFrame';
import { FloorPlanSurface } from '../components/FloorPlanSurface';
import { FloorPlanFeedback, type FloorPlanFeedbackKind } from '../components/FloorPlanFeedback';
import { FloorPlanStatus } from '../components/FloorPlanStatus';
import { FloorPlanDatebar } from '../components/FloorPlanDatebar';
import { FloorPlanFilters, type FloorPlanFilterValue } from '../components/FloorPlanFilters';
import { FloorPlanMinimap } from '../components/FloorPlanMinimap';
import { FloorPlanZoom } from '../components/FloorPlanZoom';
import { FloorPlanDeskList } from '../components/FloorPlanDeskList';
import { RequestDeskConfirmModal } from '../components/RequestDeskConfirmModal';
import { OccupancyAssignModal } from '../components/OccupancyAssignModal';
import { useAuth } from '../auth/useAuth';
import { useDeskDrag } from '../hooks/useDeskDrag';
import { useFloorPlanViewport } from '../hooks/useFloorPlanViewport';
import {
  useFloorPlanQuery,
  useRequestDeskFromFloorPlan,
  useUpdateDeskPosition,
} from '../hooks/useFloorPlan';
import { isValidIsoDate } from '../utils/calendar';
import { todayIso } from '../utils/requests';
import type { DeskPositionUpdate, FloorPlanDesk } from '../types/floorPlan';

// Buffer local de posiciones editadas por el ADMIN durante el modo edición: id del
// puesto → coordenadas pendientes (aún sin persistir). Solo contiene los puestos
// que se han arrastrado respecto al snapshot que devuelve el backend.
type PositionOverrides = Map<number, DeskPositionUpdate>;

// Vista del plano interactivo de puestos (floor-plan). Ver plano = autenticado;
// arrastrar marcadores = solo ADMIN. Empleado pincha (o usa la lista móvil) un
// puesto libre para solicitarlo. Incluye barra de fecha, filtros, zoom y panel.
export function FloorPlanPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const canEdit = user?.role === 'ADMIN';

  const [date, setDate] = useState<string>(todayIso());
  const [editMode, setEditMode] = useState(false);
  // Posiciones arrastradas pero aún NO guardadas (buffer local). El snapshot es la
  // posición del backend: al Cancelar se vacía este buffer y los marcadores vuelven
  // al snapshot; al Guardar se persiste cada entrada.
  const [overrides, setOverrides] = useState<PositionOverrides>(new Map());
  const [isSaving, setIsSaving] = useState(false);
  const [feedback, setFeedback] = useState<FloorPlanFeedbackKind>(null);
  const [filter, setFilter] = useState<FloorPlanFilterValue | null>(null);
  // Puesto pendiente de confirmar antes de crear la solicitud (marcador o lista
  // movil): evita altas accidentales por toques/zoom en tactil (requests spec).
  const [confirmTarget, setConfirmTarget] = useState<FloorPlanDesk | null>(null);
  // Puesto LIBRE que el ADMIN va a asignar (empleado o visitante) para el día
  // seleccionado (rediseño Plano §6.4): reutiliza el modal de asignación de Ocupación.
  const [assignTarget, setAssignTarget] = useState<FloorPlanDesk | null>(null);

  const surfaceRef = useRef<HTMLDivElement>(null);
  // Dimensiones del lienzo, para el minimapa que ahora vive en la columna lateral.
  const [box, setBox] = useState({ w: 0, h: 0 });

  const isDateValid = isValidIsoDate(date);
  const query = useFloorPlanQuery(date, isDateValid);
  const requestMutation = useRequestDeskFromFloorPlan();
  const positionMutation = useUpdateDeskPosition();
  // panEnabled=false siempre: en vista se navega en modo EXPLORAR (marquee +
  // minimapa) y en edición se arrastran marcadores (nunca se panea el lienzo).
  const viewport = useFloorPlanViewport(false);

  // En edición, soltar un marcador NO auto-guarda: solo actualiza el buffer local.
  const { dragPos, startDrag } = useDeskDrag(surfaceRef, (deskId, coordX, coordY) =>
    setOverrides((previous) => {
      const next = new Map(previous);
      next.set(deskId, { coordX, coordY });
      return next;
    }),
  );

  function handleDateChange(value: string): void {
    setDate(value);
    setFeedback(null);
  }

  // Entra en edición partiendo de un buffer vacío (snapshot = datos del backend).
  function handleEnterEdit(): void {
    setOverrides(new Map());
    setEditMode(true);
    setFeedback(null);
  }

  // Cancelar: descarta el buffer y revierte los marcadores al snapshot sin guardar.
  function handleCancelEdit(): void {
    setOverrides(new Map());
    setEditMode(false);
    setFeedback(null);
  }

  // Guardar: persiste cada posición modificada (secuencial, abortando al primer
  // error), refresca vía invalidación de la mutación y sale del modo edición.
  async function persistOverrides(): Promise<void> {
    if (overrides.size === 0) {
      setEditMode(false);
      return;
    }
    setIsSaving(true);
    setFeedback(null);
    try {
      for (const [deskId, body] of overrides) {
        await positionMutation.mutateAsync({ deskId, body });
      }
      setOverrides(new Map());
      setEditMode(false);
      setFeedback('saved');
    } catch {
      setFeedback('saveError');
    } finally {
      setIsSaving(false);
    }
  }

  function handleSavePositions(): void {
    void persistOverrides();
  }

  function toggleFilter(value: FloorPlanFilterValue): void {
    setFilter((previous) => (previous === value ? null : value));
  }

  // Activar un puesto LIBRE (marcador o fila del listado). El ADMIN abre el modal de
  // asignación (empleado/visitante); el empleado, la confirmación de solicitud. Los
  // puestos no libres no son accionables.
  function handleDeskActivate(desk: FloorPlanDesk): void {
    if (desk.state !== 'FREE') {
      return;
    }
    if (canEdit) {
      setAssignTarget(desk);
    } else {
      setConfirmTarget(desk);
    }
  }

  // Éxito de la asignación admin desde el plano: cierra el modal y refresca el
  // snapshot del plano (la mutación no invalida la query de floor-plan).
  function handleAssigned(): void {
    setAssignTarget(null);
    setFeedback(null);
    void query.refetch();
  }

  function handleConfirmRequest(): void {
    if (!confirmTarget) {
      return;
    }
    const desk = confirmTarget;
    setConfirmTarget(null);
    setFeedback(null);
    requestMutation.mutate(
      { deskId: desk.deskId, date },
      {
        onSuccess: () => setFeedback('success'),
        onError: () => setFeedback('conflict'),
      },
    );
  }

  const desks = query.data?.desks ?? [];

  // Posiciones efectivas para pintar: override local si el puesto se ha arrastrado,
  // si no las del backend. Los estados/estilo del puesto no cambian, solo coord.
  const effectiveDesks =
    overrides.size === 0
      ? desks
      : desks.map((desk) => {
          const override = overrides.get(desk.deskId);
          return override ? { ...desk, coordX: override.coordX, coordY: override.coordY } : desk;
        });
  const showPlan = isDateValid && !query.isLoading && !query.isError;

  // Mide el lienzo (ancho/alto) para el minimapa, que ahora se renderiza en la columna
  // lateral (fuera de FloorPlanSurface). Se re-observa cuando aparece el plano.
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
  }, [showPlan]);

  // En modo VISTA, un lápiz bajo el botón de pantalla completa para entrar en edición.
  // En edición, las acciones (Guardar/Cancelar) viven en la barra azul superior, no aquí.
  // `stopPropagation` evita iniciar paneo/marquee al pulsar el lápiz.
  function renderEditOverlay(): ReactElement | null {
    if (!canEdit || editMode) {
      return null;
    }
    return (
      <button
        type="button"
        className="floor-fullscreen-btn"
        aria-label={t('floorPlan.editPositions')}
        title={t('floorPlan.editPositions')}
        onPointerDown={(event) => event.stopPropagation()}
        onClick={handleEnterEdit}
      >
        <i className="ti ti-pencil" aria-hidden="true" />
      </button>
    );
  }

  return (
    <PageFrame
      eyebrow={t('floorPlan.eyebrow')}
      title={t('floorPlan.title')}
      bodyLabel={t('floorPlan.title')}
      subbar={
        <div className="plano-topbar">
          <div className="floor-plan-controls">
            <FloorPlanDatebar date={date} onChange={handleDateChange} />
          </div>
        </div>
      }
    >
      {editMode ? (
        <div className="floor-plan-editbar">
          <span className="floor-plan-editbar-hint">
            <i className="ti ti-drag-drop" aria-hidden="true" />
            {t('floorPlan.editHint')}
          </span>
          <div className="floor-plan-editbar-actions">
            <Button variant="white" onClick={handleCancelEdit} disabled={isSaving}>
              {t('common.cancel')}
            </Button>
            <Button variant="green" icon="check" onClick={handleSavePositions} disabled={isSaving}>
              {t('floorPlan.saveChanges')}
            </Button>
          </div>
        </div>
      ) : null}

      <FloorPlanFeedback feedback={feedback} />

      <FloorPlanStatus
        isDateValid={isDateValid}
        isLoading={query.isLoading}
        isError={query.isError}
        error={query.error}
      />

      {showPlan ? (
        <>
          {/* Fila principal: mapa a la izquierda; a la derecha (donde estaban los
              contadores) el botón de editar, los filtros de estado y el zoom. */}
          <div className="plano-main">
            <div className="plano-map-col">
              <FloorPlanSurface
                desks={effectiveDesks}
                editMode={editMode}
                dragPos={dragPos}
                filter={filter}
                viewport={viewport}
                surfaceRef={surfaceRef}
                explore={!editMode}
                renderMinimap={false}
                overlayActions={renderEditOverlay()}
                onRequest={handleDeskActivate}
                onDragStart={startDrag}
              />
            </div>
            <aside className="plano-aside" aria-label={t('floorPlan.title')}>
              <FloorPlanFilters desks={desks} active={filter} onToggle={toggleFilter} />
              <FloorPlanZoom
                scale={viewport.scale}
                onZoomIn={viewport.zoomIn}
                onZoomOut={viewport.zoomOut}
                onReset={viewport.reset}
              />
              {!editMode ? <FloorPlanMinimap viewport={viewport} box={box} /> : null}
            </aside>
          </div>

          {/* Listado de puestos a todo el ancho bajo el mapa (§6.3). En edición no se
              asigna: la lista queda de solo lectura para no interferir con el arrastre. */}
          <FloorPlanDeskList
            desks={desks}
            onSelect={editMode ? undefined : handleDeskActivate}
            actionLabelKey={canEdit ? 'floorPlan.side.assignAction' : 'floorPlan.side.rowAction'}
          />
        </>
      ) : null}

      {confirmTarget ? (
        <RequestDeskConfirmModal
          desk={confirmTarget}
          date={date}
          onConfirm={handleConfirmRequest}
          onClose={() => setConfirmTarget(null)}
        />
      ) : null}

      {assignTarget ? (
        <OccupancyAssignModal
          resourceId={assignTarget.deskId}
          resourceLabel={t('floorPlan.deskNumber', { number: assignTarget.deskNumber })}
          resourceType="DESK"
          date={date}
          onClose={() => setAssignTarget(null)}
          onAssigned={handleAssigned}
        />
      ) : null}
    </PageFrame>
  );
}
