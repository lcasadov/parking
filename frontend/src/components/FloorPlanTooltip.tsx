import { type CSSProperties } from 'react';
import { motion, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { deskInitials, deskStatePillClass, isOccupiedState } from '../utils/floorPlan';
import { DUR, EASE } from '../theme/motion';
import type { DeskState, FloorPlanDesk } from '../types/floorPlan';

interface FloorPlanTooltipProps {
  desk: FloorPlanDesk;
  // Posición en píxeles (centro horizontal / ancla vertical) relativa a la capa
  // NO clipada `.floor-plan-surface-wrap`, ya resuelta contra el pan/zoom del plano.
  left: number;
  top: number;
  // Voltea el tooltip debajo del marcador cuando está pegado al borde superior.
  below: boolean;
  // Desfase horizontal de la puntita respecto al centro del tooltip (px), para que
  // siga apuntando al marcador aunque el tooltip se haya clampado contra un borde.
  arrow: number;
}

// Línea de contexto del tooltip (icono Tabler + clave i18n) por estado. El estado
// ocupado ajeno (ASSIGNED) resuelve el nombre del titular aparte cuando existe.
const STATUS_LINE: Record<DeskState, { icon: string; key: string }> = {
  FREE: { icon: 'ti-hand-click', key: 'floorPlan.tooltip.free' },
  RELEASED: { icon: 'ti-calendar-heart', key: 'floorPlan.tooltip.released' },
  MINE: { icon: 'ti-user-check', key: 'floorPlan.tooltip.mine' },
  REQUESTED: { icon: 'ti-clock-hour-4', key: 'floorPlan.tooltip.requested' },
  ASSIGNED: { icon: 'ti-user', key: 'floorPlan.tooltip.occupied' },
};

// Tooltip que aparece al pasar/enfocar un marcador: nº de puesto + pill de estado,
// etiquetas (categoría) y una línea de contexto (disponibilidad o quién lo ocupa).
// Vive en la capa `.floor-plan-surface-wrap` (fuera del overflow y del transform del
// plano) para no recortarse ni escalar con el zoom; se posiciona en píxeles. El
// motion.div interno anima solo la ENTRADA (opacity/escala) sin pisar el transform.
export function FloorPlanTooltip({ desk, left, top, below, arrow }: FloorPlanTooltipProps) {
  const { t } = useTranslation();
  const reduceMotion = useReducedMotion();

  const line = STATUS_LINE[desk.state];
  const name = desk.occupantName ?? '';
  const withName = isOccupiedState(desk.state) && name !== '';
  const rise = below ? -4 : 4;
  const style = {
    left: `${left}px`,
    top: `${top}px`,
    '--tip-arrow-x': `${arrow}px`,
  } as CSSProperties;

  return (
    <div className={`floor-tip${below ? ' floor-tip-below' : ''}`} style={style} role="tooltip">
      <motion.div
        className="floor-tip-inner"
        initial={reduceMotion ? { opacity: 1 } : { opacity: 0, y: rise, scale: 0.96 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: reduceMotion ? 0 : DUR.fast, ease: EASE.out }}
      >
        <div className="floor-tip-head">
          <span className="floor-tip-desk">
            {t('floorPlan.deskNumber', { number: desk.deskNumber })}
          </span>
          <span className={`pill ${deskStatePillClass(desk.state)}`}>
            {t(`floorPlan.states.${desk.state}`)}
          </span>
        </div>

        <div className="floor-tip-tags">
          <span className="floor-tip-tag">{t(`desks.category.${desk.category}`)}</span>
        </div>

        {withName ? (
          <div className="floor-tip-occ">
            <span className="floor-tip-av" aria-hidden="true">
              {deskInitials(name)}
            </span>
            <span>
              {desk.state === 'MINE'
                ? t('floorPlan.tooltip.mine')
                : t('floorPlan.tooltip.occupiedBy', { name })}
            </span>
          </div>
        ) : (
          <div className="floor-tip-occ">
            <i className={`ti ${line.icon}`} aria-hidden="true" />
            <span>{t(line.key)}</span>
          </div>
        )}
      </motion.div>
    </div>
  );
}
