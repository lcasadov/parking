import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { FloorPlanSurface } from '../components/FloorPlanSurface';
import { FloorPlanFeedback, type FloorPlanFeedbackKind } from '../components/FloorPlanFeedback';
import { FloorPlanStatus } from '../components/FloorPlanStatus';
import { FloorPlanDatebar } from '../components/FloorPlanDatebar';
import { FloorPlanFilters, type FloorPlanFilterValue } from '../components/FloorPlanFilters';
import { FloorPlanZoom } from '../components/FloorPlanZoom';
import { FloorPlanSidePanel } from '../components/FloorPlanSidePanel';
import { FloorPlanMobileList } from '../components/FloorPlanMobileList';
import { useAuth } from '../auth/useAuth';
import { useDeskDrag } from '../hooks/useDeskDrag';
import { useFloorPlanViewport } from '../hooks/useFloorPlanViewport';
import {
  useFloorPlanQuery,
  useRequestDeskFromFloorPlan,
  useUpdateDeskPosition,
} from '../hooks/useFloorPlan';
import { isValidIsoDate } from '../utils/calendar';
import { maxRequestDateIso, todayIso } from '../utils/requests';
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

  function toggleFilter(value: FloorPlanFilterValue): void {
    setFilter((previous) => (previous === value ? null : value));
  }

  function handleRequest(desk: FloorPlanDesk): void {
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
        <Input
          id="floor-plan-date"
          type="date"
          label={t('floorPlan.dateLabel')}
          value={date}
          min={todayIso()}
          max={maxRequestDateIso()}
          onChange={(event) => handleDateChange(event.target.value)}
        />
        {isDateValid ? <FloorPlanDatebar date={date} onChange={handleDateChange} /> : null}
      </div>

      {editMode ? (
        <p className="form-hint" role="status">
          {t('floorPlan.editHint')}
        </p>
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
              onRequest={handleRequest}
              onDragStart={startDrag}
            />
            <FloorPlanSidePanel desks={desks} showStatus={canEdit} />
          </div>

          {canEdit ? null : (
            <FloorPlanMobileList
              desks={desks}
              pending={requestMutation.isPending}
              onRequest={handleRequest}
            />
          )}
        </>
      ) : null}
    </section>
  );
}
