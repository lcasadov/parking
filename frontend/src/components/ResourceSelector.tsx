import { RESOURCE_ICON } from '../utils/resourceIcon';
import type { ResourceType } from '../types/request';

// Valor del selector: un tipo de recurso concreto o "ALL" (solo en modo filtro).
export type ResourceValue = ResourceType | 'ALL';

const RESOURCE_TYPES: ResourceType[] = ['PARKING', 'DESK'];

interface ResourceSelectorProps {
  value: ResourceValue;
  onChange: (value: ResourceValue) => void;
  // `lg` = conmutador de DOMINIO (cambia qué se ve: título/KPIs/contenido) — Ocupación, Recursos.
  // `sm` = FILTRO de una lista (incluye "Todas") — Solicitudes, Liberar.
  size?: 'lg' | 'sm';
  // Añade el segmento "Todas" (solo tiene sentido en modo filtro `sm`).
  withAll?: boolean;
  // Etiquetas resueltas por la pantalla (i18n) para no acoplar el componente a un namespace.
  labels: { all?: string; parking: string; desk: string };
  ariaLabel: string;
}

// Selector plaza/puesto UNIFICADO (change sticky-admin-frame). Un único control y una
// única posición (siempre leading del control-row del PageFrame). El tamaño comunica el
// rol: `lg` reutiliza el conmutador grande `.occ-mode` (idéntico a Ocupación/Recursos);
// `sm` es un segmentado compacto con opción "Todas" para los filtros de lista.
export function ResourceSelector({
  value,
  onChange,
  size = 'lg',
  withAll = false,
  labels,
  ariaLabel,
}: ResourceSelectorProps) {
  const labelFor = (type: ResourceType): string =>
    type === 'PARKING' ? labels.parking : labels.desk;

  if (size === 'lg') {
    // Variante grande: reutiliza las clases existentes de ResourceModeSwitch/SectionSwitch.
    // Con `withAll` añade el segmento "Todas/Todos" (para filtros que también quieren el
    // estilo grande, p. ej. Solicitudes) manteniendo la misma apariencia que Ocupación.
    const values: ResourceValue[] = withAll ? ['ALL', ...RESOURCE_TYPES] : [...RESOURCE_TYPES];
    return (
      <div className="occ-mode section-switch resource-selector-lg" role="group" aria-label={ariaLabel}>
        {values.map((option) => {
          const active = value === option;
          const isAll = option === 'ALL';
          return (
            <button
              key={option}
              type="button"
              className={`occ-mode-btn${active ? ' is-active' : ''}`}
              aria-pressed={active}
              onClick={() => onChange(option)}
            >
              {!isAll ? (
                <i className={`ti ti-${RESOURCE_ICON[option as ResourceType]}`} aria-hidden="true" />
              ) : null}
              <span className="occ-mode-label">
                {isAll ? (labels.all ?? 'Todas') : labelFor(option as ResourceType)}
              </span>
            </button>
          );
        })}
      </div>
    );
  }

  // Variante compacta (filtro): [Todas · Plazas · Puestos].
  const values: ResourceValue[] = withAll ? ['ALL', ...RESOURCE_TYPES] : [...RESOURCE_TYPES];
  return (
    <div className="resource-selector-sm" role="group" aria-label={ariaLabel}>
      {values.map((option) => {
        const active = value === option;
        const label =
          option === 'ALL' ? (labels.all ?? 'Todas') : labelFor(option as ResourceType);
        return (
          <button
            key={option}
            type="button"
            className={`rs-sm-btn${active ? ' is-active' : ''}`}
            aria-pressed={active}
            onClick={() => onChange(option)}
          >
            {option !== 'ALL' ? (
              <i className={`ti ti-${RESOURCE_ICON[option as ResourceType]}`} aria-hidden="true" />
            ) : null}
            {label}
          </button>
        );
      })}
    </div>
  );
}
