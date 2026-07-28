import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { FloorPlanStatus } from './FloorPlanStatus';
import { FloorPlanSurface } from './FloorPlanSurface';
import { FloorPlanZoom } from './FloorPlanZoom';
import { useBackClose } from '../hooks/useBackClose';
import { useFloorPlanViewport } from '../hooks/useFloorPlanViewport';
import { useFloorPlanQuery } from '../hooks/useFloorPlan';
import { isValidIsoDate } from '../utils/calendar';
import type { FloorPlanDesk } from '../types/floorPlan';

// Puesto elegido devuelto al modal de solicitud: id (para el envio) + numero
// (para mostrar; el id nunca se muestra al usuario).
export interface PickedDesk {
  deskId: number;
  deskNumber: number;
}

interface DeskPickerModalProps {
  // Fecha ya elegida en el modal de solicitud: el selector no navega fecha, solo
  // la usa para colorear la disponibilidad de los puestos.
  date: string;
  onPick: (desk: PickedDesk) => void;
  onClose: () => void;
}

// Plano en modo SELECTOR (design D1/D6): reutiliza FloorPlanSurface (misma imagen,
// mismas coordenadas, mismo useFloorPlanQuery) dentro de un Modal. Al pinchar un
// puesto FREE NO crea la solicitud (no hay POST /floor-plan/.../request); solo
// devuelve el puesto elegido al modal y se cierra. Da feedback visual (SELECTED +
// mensaje role=status) para que la selección sea perceptible (bug de percepción).
export function DeskPickerModal({ date, onPick, onClose }: DeskPickerModalProps) {
  const { t } = useTranslation();
  // Anidado sobre el modal de solicitud: "atrás" cierra primero este selector.
  useBackClose(onClose);
  const surfaceRef = useRef<HTMLDivElement>(null);
  const [picked, setPicked] = useState<PickedDesk | null>(null);

  const isDateValid = isValidIsoDate(date);
  const query = useFloorPlanQuery(date, isDateValid);
  const viewport = useFloorPlanViewport(true);

  function handlePick(desk: FloorPlanDesk): void {
    const chosen: PickedDesk = { deskId: desk.deskId, deskNumber: desk.deskNumber };
    setPicked(chosen);
    onPick(chosen);
    onClose();
  }

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
      title={t('floorPlan.select.title')}
      icon="map-pin"
      footer={footer}
    >
      <p className="hint">{t('floorPlan.select.hint')}</p>

      {picked ? (
        <p className="floor-plan-feedback is-success" role="status" aria-live="polite">
          {t('floorPlan.select.confirmation', { number: picked.deskNumber })}
        </p>
      ) : null}

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
            selectedDeskId={picked?.deskId ?? null}
            onRequest={handlePick}
            onDragStart={() => undefined}
          />
        </>
      ) : null}
    </Dialog>
  );
}
