import { useTranslation } from 'react-i18next';
import floorPlanImage from '../assets/floor-plan.png';
import type { FloorPlanDesk } from '../types/floorPlan';

interface FloorPlanThumbnailProps {
  desk: FloorPlanDesk;
}

// Miniatura del plano de la oficina con la posición de UN puesto marcada por un
// punto de acento (pulso). Ligera y sin lógica de viewport: la imagen se escala a
// su contenedor y el marcador se posiciona por coordX%/coordY% (mismo sistema de
// coordenadas que FloorPlanSurface). La usa el tooltip de "Ver en plano" en la
// rejilla de Ocupación. El pulso se anula bajo prefers-reduced-motion vía CSS.
export function FloorPlanThumbnail({ desk }: FloorPlanThumbnailProps) {
  const { t } = useTranslation();
  const hasPosition = desk.coordX !== null && desk.coordY !== null;

  return (
    <figure className="plan-mini">
      <img src={floorPlanImage} alt={t('floorPlan.imageAlt')} className="plan-mini-img" />
      {hasPosition ? (
        <span
          className="plan-mini-dot"
          style={{ left: `${desk.coordX}%`, top: `${desk.coordY}%` }}
          aria-hidden="true"
        >
          <span className="plan-mini-dot-num">{desk.deskNumber}</span>
        </span>
      ) : null}
    </figure>
  );
}
