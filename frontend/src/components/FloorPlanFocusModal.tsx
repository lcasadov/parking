import { useRef } from 'react';
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
// mismas coordenadas, mismo useFloorPlanQuery y el realce focusDeskId/pulso) dentro
// de un Dialog. NO permite editar ni arrastrar ni solicitar. NO hace zoom al puesto:
// muestra el plano COMPLETO (100%) con el asiento resaltado y parpadeando, para que
// el empleado vea dónde está en el conjunto de la oficina.
export function FloorPlanFocusModal({ desk, deskLabel, date, onClose }: FloorPlanFocusModalProps) {
  const { t } = useTranslation();
  const surfaceRef = useRef<HTMLDivElement>(null);
  const pulsing = true;

  const isDateValid = isValidIsoDate(date);
  const query = useFloorPlanQuery(date, isDateValid);
  const viewport = useFloorPlanViewport(true);

  const desks = query.data?.desks ?? [];
  const showPlan = isDateValid && !query.isLoading && !query.isError;


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
