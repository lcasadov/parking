import { useTranslation } from 'react-i18next';
import { occupancyCounts } from '../utils/floorPlan';
import type { DeskState, FloorPlanDesk } from '../types/floorPlan';

interface FloorPlanCountersProps {
  desks: FloorPlanDesk[];
}

// Celda de contador: numeral grande + etiqueta, tintada con el estado representativo
// (contrato §4, mismo mapa estado→color que los marcadores del plano).
interface CounterCell {
  key: string;
  state: DeskState;
  surfaceClass: string;
  value: number;
}

// Contadores de ocupación del día (Ocupado / Libre / Liberado / Solicitado) en una
// columna a la DERECHA del mapa, con numerales grandes (rediseño Plano §6.2). Extrae
// la cuadrícula que antes vivía en el panel lateral y la agranda.
export function FloorPlanCounters({ desks }: FloorPlanCountersProps) {
  const { t } = useTranslation();
  const counts = occupancyCounts(desks);
  const counters: CounterCell[] = [
    { key: 'occupied', state: 'ASSIGNED', surfaceClass: 'state-occupied', value: counts.occupied },
    { key: 'free', state: 'FREE', surfaceClass: 'state-free', value: counts.free },
    { key: 'released', state: 'RELEASED', surfaceClass: 'state-released', value: counts.released },
    { key: 'requested', state: 'REQUESTED', surfaceClass: 'state-request', value: counts.requested },
  ];

  return (
    <dl
      className="plano-counters"
      data-testid="occupancy-counters"
      aria-label={t('floorPlan.side.countersLabel')}
    >
      {counters.map((cell) => (
        <div key={cell.key} className={`plano-counter ${cell.surfaceClass}`}>
          <dd className="plano-counter-num">{cell.value}</dd>
          <dt className="plano-counter-label">{t(`floorPlan.states.${cell.state}`)}</dt>
        </div>
      ))}
    </dl>
  );
}
