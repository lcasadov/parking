import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { deskStatePillClass, matchesDeskSearch } from '../utils/floorPlan';
import type { FloorPlanDesk } from '../types/floorPlan';

interface FloorPlanSidePanelProps {
  desks: FloorPlanDesk[];
  // Variante admin: muestra la "ocupación del día" con pills de estado por fila.
  showStatus?: boolean;
}

// Panel lateral (escritorio): lista de puestos buscable por número. En la
// variante admin cada fila muestra el estado del día con una pill.
export function FloorPlanSidePanel({ desks, showStatus = false }: FloorPlanSidePanelProps) {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const filtered = useMemo(
    () => desks.filter((desk) => matchesDeskSearch(desk, search)),
    [desks, search],
  );

  return (
    <aside className="plano-side" aria-label={t('floorPlan.side.label')}>
      <h2 className="plano-side-title">
        {showStatus ? t('floorPlan.side.occupancyTitle') : t('floorPlan.side.title')}
      </h2>
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
        <ul className="plano-side-list">
          {filtered.map((desk) => (
            <li key={desk.deskId} className="plano-side-row">
              <span className="plano-side-desk">{t('floorPlan.deskNumber', { number: desk.deskNumber })}</span>
              {showStatus ? (
                <span className={`pill ${deskStatePillClass(desk.state)}`}>
                  {t(`floorPlan.states.${desk.state}`)}
                </span>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </aside>
  );
}
