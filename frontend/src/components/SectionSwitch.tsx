export interface SectionSwitchItem {
  id: string;
  label: string;
  // Icono Tabler opcional (sin prefijo "ti-").
  icon?: string;
}

// Conmutador GRANDE de sub-sección (mismo lenguaje visual que el selector
// plaza/puesto de Ocupación, clase .occ-mode): segmentos grandes con icono +
// etiqueta, imposibles de ignorar. Se usa en las secciones contenedoras
// (Recursos, Liberar, Registros) para cambiar de sub-vista de forma consistente,
// SIEMPRE debajo del título de la sección.
export function SectionSwitch({
  items,
  active,
  onChange,
  ariaLabel,
  className,
}: {
  items: SectionSwitchItem[];
  active: string;
  onChange: (id: string) => void;
  ariaLabel: string;
  // Clase extra opcional (p. ej. para anclar el conmutador a un lado del control-row).
  className?: string;
}) {
  return (
    <div
      className={`occ-mode section-switch${className ? ` ${className}` : ''}`}
      role="group"
      aria-label={ariaLabel}
    >
      {items.map((item) => {
        const isActive = item.id === active;
        return (
          <button
            key={item.id}
            type="button"
            className={`occ-mode-btn${isActive ? ' is-active' : ''}`}
            aria-pressed={isActive}
            onClick={() => onChange(item.id)}
          >
            {item.icon ? <i className={`ti ti-${item.icon}`} aria-hidden="true" /> : null}
            <span className="occ-mode-label">{item.label}</span>
          </button>
        );
      })}
    </div>
  );
}
