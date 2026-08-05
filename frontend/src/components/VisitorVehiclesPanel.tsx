import { visitorVehiclesHooks } from '../hooks/useVisitorVehicles';
import type { VehiclesDraft } from '../types/vehicle';
import { VehiclesPanel } from './VehiclesPanel';

// Tab "Vehículos" del formulario de visitante (change visitor-vehicles): reutiliza el panel
// genérico VehiclesPanel con los hooks y textos del visitante. En el alta (sin id) opera en modo
// borrador (`draft`): acumula vehículos en memoria y el formulario los persiste al crear.
export function VisitorVehiclesPanel({
  visitorId,
  draft,
}: {
  visitorId: number | null;
  draft?: VehiclesDraft;
}) {
  return (
    <VehiclesPanel
      ownerId={visitorId}
      hooks={visitorVehiclesHooks}
      ns="visitors.vehicles"
      draft={draft}
    />
  );
}
