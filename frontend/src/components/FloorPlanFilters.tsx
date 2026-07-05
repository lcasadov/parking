import { useTranslation } from 'react-i18next';
import { DESK_STATES, EXECUTIVE_SYMBOL, countByState, markerStateClass } from '../utils/floorPlan';
import type { DeskState, FloorPlanDesk } from '../types/floorPlan';

// Valor de filtro activo: un estado de puesto o la categoría de dirección.
export type FloorPlanFilterValue = DeskState | 'EXECUTIVE';

interface FloorPlanFiltersProps {
  desks: FloorPlanDesk[];
  active: FloorPlanFilterValue | null;
  onToggle: (value: FloorPlanFilterValue) => void;
}

// Chips de filtro por estado (Libre, Liberado hoy, Mi puesto, Solicitado,
// Ocupado) con contadores + un chip de Dirección (EXECUTIVE). Actúan como toggle:
// pulsar el chip activo lo desactiva.
export function FloorPlanFilters({ desks, active, onToggle }: FloorPlanFiltersProps) {
  const { t } = useTranslation();
  const counts = countByState(desks);
  const executiveCount = desks.filter((desk) => desk.category === 'EXECUTIVE').length;

  function chip(value: FloorPlanFilterValue, dotClass: string, labelKey: string, count: number) {
    const isActive = active === value;
    return (
      <button
        key={value}
        type="button"
        className={`chip-filter${isActive ? ' is-active' : ''}`}
        aria-pressed={isActive}
        aria-label={t('floorPlan.filters.chip', { label: t(labelKey), count })}
        onClick={() => onToggle(value)}
      >
        <span className={`cf-dot ${dotClass}`} aria-hidden="true" />
        <span className="cf-label" aria-hidden="true">
          {t(labelKey)}
        </span>
        <span className="cf-count" aria-hidden="true">
          {count}
        </span>
      </button>
    );
  }

  return (
    <div className="chip-filters" role="group" aria-label={t('floorPlan.filters.label')}>
      {DESK_STATES.map((state) =>
        chip(state, markerStateClass(state), `floorPlan.states.${state}`, counts[state]),
      )}
      <button
        type="button"
        className={`chip-filter chip-filter-exec${active === 'EXECUTIVE' ? ' is-active' : ''}`}
        aria-pressed={active === 'EXECUTIVE'}
        aria-label={t('floorPlan.filters.chip', {
          label: t('floorPlan.legendExecutive'),
          count: executiveCount,
        })}
        onClick={() => onToggle('EXECUTIVE')}
      >
        <span className="cf-dot cf-dot-exec" aria-hidden="true">
          {EXECUTIVE_SYMBOL}
        </span>
        <span className="cf-label" aria-hidden="true">
          {t('floorPlan.legendExecutive')}
        </span>
        <span className="cf-count" aria-hidden="true">
          {executiveCount}
        </span>
      </button>
    </div>
  );
}
