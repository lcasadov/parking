import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { FloorPlanSurface } from '../FloorPlanSurface';
import { FloorPlanZoom } from '../FloorPlanZoom';
import { Spinner } from '../Spinner';
import { useFloorPlanQuery } from '../../hooks/useFloorPlan';
import { useFloorPlanViewport } from '../../hooks/useFloorPlanViewport';
import { useWizardAvailability } from '../../hooks/useWizardAvailability';
import { RESOURCE_DESK } from './wizardTypes';
import type { FloorPlanDesk } from '../../types/floorPlan';

interface LocationDeskProps {
  dates: string[];
  deskId: number | null;
  onChange: (deskId: number, label: string) => void;
}

// Reproyecta cada puesto del plano según la disponibilidad en TODAS las fechas:
// elegible -> FREE (seleccionable); ocupado en alguna fecha -> ASSIGNED (bloqueado).
// El marcador del plano ya sólo permite pulsar los puestos FREE, así que basta con
// reescribir el estado para bloquear los no elegibles sin tocar la primitiva.
function projectDesks(desks: FloorPlanDesk[], eligibleIds: Set<number>): FloorPlanDesk[] {
  return desks.map((desk) => ({
    ...desk,
    state: eligibleIds.has(desk.deskId) ? 'FREE' : 'ASSIGNED',
  }));
}

// Ubicación DESK: el plano interactivo (solo lectura) para elegir el puesto pinchando
// su marcador, con disponibilidad en vivo, y una lista alternativa de puestos elegibles.
export function LocationDesk({ dates, deskId, onChange }: LocationDeskProps) {
  const { t } = useTranslation();
  const surfaceRef = useRef<HTMLDivElement>(null);
  const viewport = useFloorPlanViewport(true);

  // El plano se pide para la primera fecha (coordenadas y catálogo de puestos); la
  // elegibilidad real la aporta la intersección de disponibilidad de todas las fechas.
  const firstDate = dates[0] ?? '';
  const planQuery = useFloorPlanQuery(firstDate, firstDate !== '');
  const availability = useWizardAvailability(dates, RESOURCE_DESK, true);

  const loading = planQuery.isLoading || availability.isLoading;
  if (loading) {
    return (
      <div className="rzw-center">
        <Spinner />
        <p className="rzw-lead">{t('wizard.location.checking')}</p>
      </div>
    );
  }

  if (planQuery.isError || availability.isError) {
    return (
      <p className="form-error" role="alert">
        {t('wizard.location.error')}
      </p>
    );
  }

  const desks = projectDesks(planQuery.data?.desks ?? [], availability.eligibleIds);
  const eligible = availability.eligible;

  // Etiqueta humana del puesto elegido (por id); si no está en la lista elegible,
  // cae al número del puesto del plano.
  function pickDesk(id: number): void {
    const match = eligible.find((resource) => resource.resourceId === id);
    const fallback = desks.find((desk) => desk.deskId === id);
    onChange(id, match?.label ?? (fallback ? String(fallback.deskNumber) : String(id)));
  }

  return (
    <div className="rzw-loc">
      <span className="rzw-eyebrow">
        {t('wizard.location.eligibleCount', { count: eligible.length })}
      </span>

      <div className="rzw-plan-toolbar">
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
        selectedDeskId={deskId}
        onRequest={(desk) => pickDesk(desk.deskId)}
        onDragStart={() => undefined}
      />

      {eligible.length === 0 ? (
        <p className="rzw-empty">{t('wizard.location.noneDesk')}</p>
      ) : (
        <ul className="rzw-res-grid">
          {eligible.map((resource) => {
            const selected = deskId === resource.resourceId;
            return (
              <li key={resource.resourceId}>
                <button
                  type="button"
                  className={`rzw-res-chip${selected ? ' is-selected' : ''}`}
                  aria-pressed={selected}
                  onClick={() => pickDesk(resource.resourceId)}
                >
                  <i className="ti ti-armchair" aria-hidden="true" />
                  <span className="mono">{resource.label}</span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
