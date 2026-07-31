import { useTranslation } from 'react-i18next';
import { occupancyCounts } from '../utils/floorPlan';
import type { DeskState, FloorPlanDesk } from '../types/floorPlan';

interface FloorPlanCountersProps {
  desks: FloorPlanDesk[];
}

// Celda de contador: numeral + etiqueta, con un punto del color del estado.
interface CounterCell {
  key: string;
  state: DeskState;
  value: number;
}

// Punto de color por estado (mismo lenguaje que los marcadores del plano y la tira de
// KPIs de Ocupación).
const STATE_DOT: Record<string, string> = {
  ASSIGNED: 'var(--accent)',
  FREE: 'var(--ink-faint)',
  RELEASED: 'var(--info)',
  REQUESTED: 'var(--pend)',
};

// Contadores de ocupación del día (Ocupado / Libre / Liberado / Solicitado) como TIRA
// compacta, con el mismo estilo que la tira de KPIs de Ocupación (occ-kpi-strip), para
// alojarlos arriba en la fila de la fecha (rediseño Plano, consistencia con Ocupación).
export function FloorPlanCounters({ desks }: FloorPlanCountersProps) {
  const { t } = useTranslation();
  const counts = occupancyCounts(desks);
  const counters: CounterCell[] = [
    { key: 'occupied', state: 'ASSIGNED', value: counts.occupied },
    { key: 'free', state: 'FREE', value: counts.free },
    { key: 'released', state: 'RELEASED', value: counts.released },
    { key: 'requested', state: 'REQUESTED', value: counts.requested },
  ];

  return (
    <div
      className="occ-kpi-strip plano-counters-strip"
      data-testid="occupancy-counters"
      role="group"
      aria-label={t('floorPlan.side.countersLabel')}
    >
      {counters.map((cell) => (
        <div key={cell.key} className="occ-kpi-stat">
          <span
            className="occ-kpi-stat-dot"
            style={{ background: STATE_DOT[cell.state] }}
            aria-hidden="true"
          />
          <span className="occ-kpi-stat-val mono">{cell.value}</span>
          <span className="occ-kpi-stat-text">
            <span className="occ-kpi-stat-lbl">{t(`floorPlan.states.${cell.state}`)}</span>
          </span>
        </div>
      ))}
    </div>
  );
}
