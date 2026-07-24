import { useTranslation } from 'react-i18next';
import type { ResourceType } from '../types/request';

// Orden fijo del conmutador (S1192: sin literales repetidos).
const RESOURCE_TYPES: ResourceType[] = ['PARKING', 'DESK'];

// Icono Tabler por tipo de recurso (coherente con el resto de la app: plaza =
// ti-parking, puesto = ti-armchair).
const RESOURCE_ICON: Record<ResourceType, string> = {
  PARKING: 'parking',
  DESK: 'armchair',
};

// Conmutador GRANDE plaza/puesto de la pantalla de Ocupación: dos segmentos con
// icono + etiqueta, imposible de ignorar. Al cambiarlo cambian titulo, KPIs y
// rejilla (el estado vive en la pagina; este componente es solo presentacion).
// Reutilizable por la vista Semanal y por Disponibilidad para un lenguaje unico.
export function ResourceModeSwitch({
  value,
  onChange,
  ariaLabel,
}: {
  value: ResourceType;
  onChange: (type: ResourceType) => void;
  ariaLabel: string;
}) {
  const { t } = useTranslation();
  return (
    <div className="occ-mode" role="group" aria-label={ariaLabel}>
      {RESOURCE_TYPES.map((type) => {
        const active = value === type;
        return (
          <button
            key={type}
            type="button"
            className={`occ-mode-btn${active ? ' is-active' : ''}`}
            aria-pressed={active}
            onClick={() => onChange(type)}
          >
            <i className={`ti ti-${RESOURCE_ICON[type]}`} aria-hidden="true" />
            <span className="occ-mode-label">{t(`occupancy.weekly.resourceType.${type}`)}</span>
          </button>
        );
      })}
    </div>
  );
}
