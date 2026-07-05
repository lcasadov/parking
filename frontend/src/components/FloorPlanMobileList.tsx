import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import type { FloorPlanDesk } from '../types/floorPlan';

interface FloorPlanMobileListProps {
  desks: FloorPlanDesk[];
  pending: boolean;
  onRequest: (desk: FloorPlanDesk) => void;
}

// Lista móvil "Disponibles para solicitar": los puestos libres para la fecha con
// un botón Solicitar por fila. Es la vía principal de solicitud en móvil, sin
// depender de pulsar marcadores diminutos (init-floor-plan, mockup móvil).
export function FloorPlanMobileList({ desks, pending, onRequest }: FloorPlanMobileListProps) {
  const { t } = useTranslation();
  const free = desks.filter((desk) => desk.state === 'FREE');

  return (
    <section className="mlist" aria-label={t('floorPlan.mobile.title')}>
      <h2 className="mlist-title">{t('floorPlan.mobile.title')}</h2>
      {free.length === 0 ? (
        <p className="mlist-empty">{t('floorPlan.mobile.empty')}</p>
      ) : (
        <ul className="mlist-rows">
          {free.map((desk) => (
            <li key={desk.deskId} className="mlist-row">
              <span className="mlist-desk">{t('floorPlan.deskNumber', { number: desk.deskNumber })}</span>
              <Button
                variant="green"
                icon="calendar-plus"
                disabled={pending}
                onClick={() => onRequest(desk)}
              >
                {t('floorPlan.mobile.request')}
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
