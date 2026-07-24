import { useTranslation } from 'react-i18next';
import { deskInitials } from '../utils/floorPlan';
import type { FloorPlanDesk } from '../types/floorPlan';

interface FloorPlanOccupantPinProps {
  desk: FloorPlanDesk;
  // Posición (en %) heredada del marcador; el pin se eleva sobre él vía CSS.
  style: { left: string; top: string };
}

// Etiqueta accesible del pin según haya titular conocido o sea el puesto propio.
function pinLabel(desk: FloorPlanDesk, occupiedBy: (name: string) => string, mine: string, unknown: string): string {
  if (desk.state === 'MINE') {
    return mine;
  }
  return desk.occupantName ? occupiedBy(desk.occupantName) : unknown;
}

// Pin de ocupante sobre un marcador ocupado/asignado: avatar con iniciales del
// titular (o icono genérico si el nombre no está disponible). Puramente visual.
export function FloorPlanOccupantPin({ desk, style }: FloorPlanOccupantPinProps) {
  const { t } = useTranslation();
  const initials = deskInitials(desk.occupantName);
  const label = pinLabel(
    desk,
    (name) => t('floorPlan.pin.label', { name }),
    t('floorPlan.pin.mine'),
    t('floorPlan.pin.labelUnknown'),
  );

  return (
    <span
      className={`floor-pin${desk.state === 'MINE' ? ' floor-pin-mine' : ''}`}
      style={style}
      role="img"
      aria-label={label}
    >
      <span className="floor-pin-av" aria-hidden="true">
        {initials !== '' ? initials : <i className="ti ti-user" />}
      </span>
    </span>
  );
}
