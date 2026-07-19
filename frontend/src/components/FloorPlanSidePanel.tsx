import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  deskStatePillClass,
  markerStateClass,
  matchesDeskSearch,
  occupancyCounts,
} from '../utils/floorPlan';
import type { DeskState, FloorPlanDesk } from '../types/floorPlan';

interface FloorPlanSidePanelProps {
  desks: FloorPlanDesk[];
}

// Celda de contador de la cuadrícula de ocupación: numeral serif + etiqueta, con
// el color de estado tomado del mapa único estado→color (contrato §4). `state` da
// la etiqueta i18n; `surfaceClass` (.state-*) tinta la celda; el valor lo aporta
// occupancyCounts.
interface CounterCell {
  key: string;
  state: DeskState;
  surfaceClass: string;
  value: number;
}

// Panel "Ocupación del día" (contrato §Plano del día): cuadrícula de contadores
// con numerales serif + listado buscable de puestos con dot de estado y pill. El
// contrato de floor-plan no incluye el titular del puesto, por lo que la fila usa
// el dato disponible (categoría) como subtítulo.
export function FloorPlanSidePanel({ desks }: FloorPlanSidePanelProps) {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const filtered = useMemo(
    () => desks.filter((desk) => matchesDeskSearch(desk, search)),
    [desks, search],
  );

  const counts = occupancyCounts(desks);
  // Cada contador se colorea con el estado representativo (contrato §4): ocupado→
  // ASSIGNED (verde), libre→FREE, liberado→RELEASED, solicitado→REQUESTED.
  const counters: CounterCell[] = [
    { key: 'occupied', state: 'ASSIGNED', surfaceClass: 'state-occupied', value: counts.occupied },
    { key: 'free', state: 'FREE', surfaceClass: 'state-free', value: counts.free },
    { key: 'released', state: 'RELEASED', surfaceClass: 'state-released', value: counts.released },
    {
      key: 'requested',
      state: 'REQUESTED',
      surfaceClass: 'state-request',
      value: counts.requested,
    },
  ];

  return (
    <aside className="plano-side" aria-label={t('floorPlan.side.label')}>
      <h2 className="plano-side-title">{t('floorPlan.side.occupancyTitle')}</h2>

      <dl
        className="plano-occ"
        data-testid="occupancy-counters"
        aria-label={t('floorPlan.side.countersLabel')}
      >
        {counters.map((cell) => (
          <div key={cell.key} className={`plano-occ-cell ${cell.surfaceClass}`}>
            <dt className="plano-occ-label">{t(`floorPlan.states.${cell.state}`)}</dt>
            <dd className="plano-occ-num">{cell.value}</dd>
          </div>
        ))}
      </dl>

      <div className="search-box plano-side-search">
        <i className="ti ti-search" aria-hidden="true" />
        <input
          type="search"
          className="field-input"
          value={search}
          aria-label={t('floorPlan.side.searchLabel')}
          placeholder={t('floorPlan.side.searchPlaceholder')}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      {filtered.length === 0 ? (
        <p className="plano-side-empty">{t('floorPlan.side.empty')}</p>
      ) : (
        <ul className="plano-side-list" aria-label={t('floorPlan.side.title')}>
          {filtered.map((desk) => (
            <li key={desk.deskId} className="plano-side-row">
              <button
                type="button"
                className="plano-side-row-btn"
                aria-label={t('floorPlan.side.rowAction', { number: desk.deskNumber })}
              >
                <span
                  className={`plano-side-dot ${markerStateClass(desk.state)}`}
                  aria-hidden="true"
                />
                <span className="side-meta">
                  <span className="plano-side-desk">
                    {t('floorPlan.deskNumber', { number: desk.deskNumber })}
                  </span>
                  <span className="sub">{t(`desks.category.${desk.category}`)}</span>
                </span>
                <span className={`pill ${deskStatePillClass(desk.state)}`}>
                  {t(`floorPlan.states.${desk.state}`)}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </aside>
  );
}
