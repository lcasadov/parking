import { useCallback, useEffect, useRef, useState, type MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { FloorPlanSurface, type DragPosition } from '../components/FloorPlanSurface';
import { FloorPlanFeedback, type FloorPlanFeedbackKind } from '../components/FloorPlanFeedback';
import { FloorPlanStatus } from '../components/FloorPlanStatus';
import { useAuth } from '../auth/useAuth';
import {
  useFloorPlanQuery,
  useRequestDeskFromFloorPlan,
  useUpdateDeskPosition,
} from '../hooks/useFloorPlan';
import { isValidIsoDate } from '../utils/calendar';
import { maxRequestDateIso, todayIso } from '../utils/requests';
import { nextCoord } from '../utils/floorPlan';
import type { FloorPlanDesk } from '../types/floorPlan';

// Estado interno de un arrastre en curso (coordenadas en % del ancho/alto).
interface DragState {
  deskId: number;
  startClientX: number;
  startClientY: number;
  baseX: number;
  baseY: number;
  rectWidth: number;
  rectHeight: number;
  x: number;
  y: number;
  moved: boolean;
}

// Vista del plano interactivo de puestos (floor-plan). Ver plano = autenticado;
// arrastrar marcadores = solo ADMIN. Empleado pincha un puesto libre para solicitarlo.
export function FloorPlanPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const canEdit = user?.role === 'ADMIN';

  const [date, setDate] = useState<string>(todayIso());
  const [editMode, setEditMode] = useState(false);
  const [feedback, setFeedback] = useState<FloorPlanFeedbackKind>(null);
  const [dragPos, setDragPos] = useState<DragPosition | null>(null);

  const surfaceRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef<DragState | null>(null);

  const isDateValid = isValidIsoDate(date);
  const query = useFloorPlanQuery(date, isDateValid);
  const requestMutation = useRequestDeskFromFloorPlan();
  const positionMutation = useUpdateDeskPosition();

  const mutatePositionRef = useRef(positionMutation.mutate);
  mutatePositionRef.current = positionMutation.mutate;

  const handleDragMove = useCallback((event: globalThis.MouseEvent) => {
    const drag = dragRef.current;
    if (!drag) {
      return;
    }
    const x = nextCoord(drag.baseX, event.clientX - drag.startClientX, drag.rectWidth);
    const y = nextCoord(drag.baseY, event.clientY - drag.startClientY, drag.rectHeight);
    drag.x = x;
    drag.y = y;
    drag.moved = true;
    setDragPos({ deskId: drag.deskId, x, y });
  }, []);

  const handleDragEnd = useCallback(() => {
    const drag = dragRef.current;
    window.removeEventListener('mousemove', handleDragMove);
    window.removeEventListener('mouseup', handleDragEnd);
    if (drag?.moved) {
      mutatePositionRef.current({
        deskId: drag.deskId,
        body: { coordX: drag.x, coordY: drag.y },
      });
    }
    dragRef.current = null;
    setDragPos(null);
  }, [handleDragMove]);

  const handleDragStart = useCallback(
    (desk: FloorPlanDesk, event: MouseEvent<HTMLButtonElement>) => {
      const surface = surfaceRef.current;
      if (!surface) {
        return;
      }
      const rect = surface.getBoundingClientRect();
      dragRef.current = {
        deskId: desk.deskId,
        startClientX: event.clientX,
        startClientY: event.clientY,
        baseX: desk.coordX ?? 0,
        baseY: desk.coordY ?? 0,
        rectWidth: rect.width,
        rectHeight: rect.height,
        x: desk.coordX ?? 0,
        y: desk.coordY ?? 0,
        moved: false,
      };
      window.addEventListener('mousemove', handleDragMove);
      window.addEventListener('mouseup', handleDragEnd);
    },
    [handleDragMove, handleDragEnd],
  );

  // Limpieza defensiva: si el componente se desmonta a mitad de un arrastre.
  useEffect(() => {
    return () => {
      window.removeEventListener('mousemove', handleDragMove);
      window.removeEventListener('mouseup', handleDragEnd);
    };
  }, [handleDragMove, handleDragEnd]);

  function handleDateChange(value: string): void {
    setDate(value);
    setFeedback(null);
  }

  function handleToggleEdit(): void {
    setEditMode((previous) => !previous);
    setFeedback(null);
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
        <FloorPlanSurface
          desks={desks}
          editMode={editMode}
          dragPos={dragPos}
          surfaceRef={surfaceRef}
          onRequest={handleRequest}
          onDragStart={handleDragStart}
        />
      ) : null}
    </section>
  );
}
