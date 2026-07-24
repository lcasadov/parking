import { useRef, useState, type ReactElement } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { InfoBanner } from '../components/InfoBanner';
import { PageHeader } from '../components/PageHeader';
import { FloorPlanSurface } from '../components/FloorPlanSurface';
import { FloorPlanFeedback, type FloorPlanFeedbackKind } from '../components/FloorPlanFeedback';
import { FloorPlanStatus } from '../components/FloorPlanStatus';
import { FloorPlanDatebar } from '../components/FloorPlanDatebar';
import { FloorPlanFilters, type FloorPlanFilterValue } from '../components/FloorPlanFilters';
import { FloorPlanZoom } from '../components/FloorPlanZoom';
import { FloorPlanSidePanel } from '../components/FloorPlanSidePanel';
import { FloorPlanMobileList } from '../components/FloorPlanMobileList';
import { RequestDeskConfirmModal } from '../components/RequestDeskConfirmModal';
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

  const surfaceRef = useRef<HTMLDivElement>(null);

  const isDateValid = isValidIsoDate(date);
  const query = useFloorPlanQuery(date, isDateValid);
  const requestMutation = useRequestDeskFromFloorPlan();
  const positionMutation = useUpdateDeskPosition();
  const viewport = useFloorPlanViewport(!editMode);

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

  // Pinchar un marcador libre (o pulsar "Solicitar" en la lista movil) abre la
  // confirmacion; la solicitud solo se crea si el empleado confirma.
  function handleRequestClick(desk: FloorPlanDesk): void {
    setConfirmTarget(desk);
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

  // Modo vista: un botón "Editar posiciones". Modo edición: "Cancelar" (secundario)
  // + "Guardar cambios" (primario verde). `.page-actions` ya los separa con gap.
  function renderEditActions(): ReactElement {
    if (!editMode) {
      return (
        <Button variant="white" icon="drag-drop" onClick={handleEnterEdit}>
          {t('floorPlan.editPositions')}
        </Button>
      );
    }
    return (
      <>
        <Button variant="white" icon="x" onClick={handleCancelEdit} disabled={isSaving}>
          {t('common.cancel')}
        </Button>
        <Button
          variant="green"
          icon="device-floppy"
          onClick={handleSavePositions}
          disabled={isSaving}
        >
          {t('floorPlan.saveChanges')}
        </Button>
      </>
    );
  }

  return (
    <section className="floor-plan-page" aria-label={t('floorPlan.title')}>
      <PageHeader
        eyebrow={t('floorPlan.eyebrow')}
        title={t('floorPlan.title')}
        description={t('floorPlan.description')}
        actions={
          canEdit ? renderEditActions() : undefined
        }
      />

      <div className="floor-plan-controls">
        <FloorPlanDatebar date={date} onChange={handleDateChange} />
      </div>

      {editMode ? (
        <div className="floor-plan-editor-bar">
          <InfoBanner variant="blue" icon="drag-drop">
            {t('floorPlan.editHint')}
          </InfoBanner>
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
          <div className="floor-plan-toolbar">
            <FloorPlanFilters desks={desks} active={filter} onToggle={toggleFilter} />
            <FloorPlanZoom
              scale={viewport.scale}
              onZoomIn={viewport.zoomIn}
              onZoomOut={viewport.zoomOut}
              onReset={viewport.reset}
            />
          </div>

          <div className="floor-plan-layout">
            <FloorPlanSurface
              desks={effectiveDesks}
              editMode={editMode}
              dragPos={dragPos}
              filter={filter}
              viewport={viewport}
              surfaceRef={surfaceRef}
              onRequest={handleRequestClick}
              onDragStart={startDrag}
            />
            <FloorPlanSidePanel desks={desks} />
          </div>

          {canEdit ? null : (
            <FloorPlanMobileList
              desks={desks}
              pending={requestMutation.isPending}
              onRequest={handleRequestClick}
            />
          )}
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
    </section>
  );
}
