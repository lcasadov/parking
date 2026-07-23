import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { InfoBanner } from '../components/InfoBanner';
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
import type { FloorPlanDesk } from '../types/floorPlan';

// Vista del plano interactivo de puestos (floor-plan). Ver plano = autenticado;
// arrastrar marcadores = solo ADMIN. Empleado pincha (o usa la lista móvil) un
// puesto libre para solicitarlo. Incluye barra de fecha, filtros, zoom y panel.
export function FloorPlanPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const canEdit = user?.role === 'ADMIN';

  const [date, setDate] = useState<string>(todayIso());
  const [editMode, setEditMode] = useState(false);
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

  const { dragPos, startDrag } = useDeskDrag(surfaceRef, (deskId, coordX, coordY) =>
    positionMutation.mutate({ deskId, body: { coordX, coordY } }),
  );

  function handleDateChange(value: string): void {
    setDate(value);
    setFeedback(null);
  }

  function handleToggleEdit(): void {
    setEditMode((previous) => !previous);
    setFeedback(null);
  }

  // Confirmación explícita del editor: las posiciones se auto-guardan al soltar
  // cada marcador, pero el botón da un feedback claro de que todo está guardado.
  function handleSavePositions(): void {
    setFeedback('saved');
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
  const showPlan = isDateValid && !query.isLoading && !query.isError;

  return (
    <section className="floor-plan-page" aria-labelledby="floor-plan-title">
      <header className="page-header">
        <h1 id="floor-plan-title" className="section-title">
          {t('floorPlan.title')}
        </h1>
        {canEdit ? (
          <div className="page-actions">
            <Button
              variant={editMode ? 'blue' : 'white'}
              icon="drag-drop"
              aria-pressed={editMode}
              onClick={handleToggleEdit}
            >
              {t('floorPlan.editPositions')}
            </Button>
          </div>
        ) : null}
      </header>

      <div className="floor-plan-controls">
        <FloorPlanDatebar date={date} onChange={handleDateChange} />
      </div>

      {editMode ? (
        <div className="floor-plan-editor-bar">
          <InfoBanner variant="blue" icon="drag-drop">
            {t('floorPlan.editHint')}
          </InfoBanner>
          <Button variant="green" icon="device-floppy" onClick={handleSavePositions}>
            {t('floorPlan.savePositions')}
          </Button>
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
              desks={desks}
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
