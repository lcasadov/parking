import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Dialog } from './Dialog';
import { Button } from './Button';
import { FloorPlanStatus } from './FloorPlanStatus';
import { FloorPlanSurface } from './FloorPlanSurface';
import { FloorPlanZoom } from './FloorPlanZoom';
import { useFloorPlanViewport } from '../hooks/useFloorPlanViewport';
import { useFloorPlanQuery } from '../hooks/useFloorPlan';
import { isValidIsoDate } from '../utils/calendar';
import type { FloorPlanDesk } from '../types/floorPlan';

// Duración del pulso de resalte al abrir el modal (ms). Después cesa el pulso y
// queda el anillo de acento estático. El pulso en sí se anula bajo
// prefers-reduced-motion vía CSS (queda solo el resalte).
const FOCUS_PULSE_MS = 3500;

interface FloorPlanFocusModalProps {
  // Puesto a resaltar (resuelto por la rejilla desde el plano); se usa su deskId
  // para el realce y sus coordenadas para centrar el viewport.
  desk: FloorPlanDesk;
  // Etiqueta legible del puesto ("D-03") para el título del modal.
  deskLabel: string;
  // Fecha del snapshot del plano (la rejilla pasa HOY): colorea la disponibilidad.
  date: string;
  onClose: () => void;
}

// Modal "Ver en plano" (solo lectura): reutiliza FloorPlanSurface (misma imagen,
// mismas coordenadas, mismo useFloorPlanQuery y el mismo realce focusDeskId/pulso
// que ya usaba la navegación) dentro de un Dialog. NO permite editar ni arrastrar
// ni solicitar: onRequest/onDragStart son no-ops. Centra el viewport en el puesto
// enfocado y lo pulsa unos segundos para captar la atención.
export function FloorPlanFocusModal({ desk, deskLabel, date, onClose }: FloorPlanFocusModalProps) {
  const { t } = useTranslation();
  const surfaceRef = useRef<HTMLDivElement>(null);
  const [pulsing, setPulsing] = useState(true);
  const focusedRef = useRef(false);

  const isDateValid = isValidIsoDate(date);
  const query = useFloorPlanQuery(date, isDateValid);
  const viewport = useFloorPlanViewport(true);

  const desks = query.data?.desks ?? [];
  // Puesto vivo del plano (por deskId); cae al recibido mientras el plano carga.
  const liveDesk = desks.find((item) => item.deskId === desk.deskId) ?? desk;
  const showPlan = isDateValid && !query.isLoading && !query.isError;

  // Centra el viewport en el puesto una vez cargado el plano (una sola vez).
  useEffect(() => {
    if (focusedRef.current || !showPlan) {
      return;
    }
    if (liveDesk.coordX === null || liveDesk.coordY === null) {
      return;
    }
    const rect = surfaceRef.current?.getBoundingClientRect();
    if (!rect || rect.width === 0) {
      return;
    }
    focusedRef.current = true;
    viewport.focusOn(liveDesk.coordX, liveDesk.coordY, rect.width, rect.height);
  }, [showPlan, liveDesk, viewport]);

  // Detiene el pulso pasados unos segundos (queda el anillo de acento estático).
  useEffect(() => {
    const timer = window.setTimeout(() => setPulsing(false), FOCUS_PULSE_MS);
    return () => window.clearTimeout(timer);
  }, []);

  const footer = (
    <Button variant="white" onClick={onClose}>
      {t('floorPlan.select.close')}
    </Button>
  );

  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open) {
          onClose();
        }
      }}
      title={t('occupancy.weekly.viewInPlanModalTitle', { resource: deskLabel })}
      icon="map-pin"
      footer={footer}
    >
      <FloorPlanStatus
        isDateValid={isDateValid}
        isLoading={query.isLoading}
        isError={query.isError}
        error={query.error}
      />

      {showPlan ? (
        <>
          <div className="floor-plan-toolbar">
            <FloorPlanZoom
              scale={viewport.scale}
              onZoomIn={viewport.zoomIn}
              onZoomOut={viewport.zoomOut}
              onReset={viewport.reset}
            />
          </div>
          <FloorPlanSurface
            desks={desks}
            editMode={false}
            dragPos={null}
            filter={null}
            viewport={viewport}
            surfaceRef={surfaceRef}
            focusDeskId={desk.deskId}
            focusPulsing={pulsing}
            onRequest={() => undefined}
            onDragStart={() => undefined}
          />
        </>
      ) : null}
    </Dialog>
  );
}
