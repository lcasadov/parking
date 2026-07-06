export interface TabItem {
  id: string;
  label: string;
  // Contador opcional (badge rojo .count, p.ej. solicitudes pendientes).
  count?: number;
}

interface TabsProps {
  tabs: TabItem[];
  active: string;
  onChange: (id: string) => void;
  ariaLabel: string;
}

// Barra de pestañas estilo caja (mockup .tab) con badge .count opcional.
// role=tablist + aria-selected para navegacion accesible.
export function Tabs({ tabs, active, onChange, ariaLabel }: TabsProps) {
  return (
    <div className="tabs" role="tablist" aria-label={ariaLabel}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          id={`tab-${tab.id}`}
          aria-selected={active === tab.id}
          className={`tab${active === tab.id ? ' active' : ''}`}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
          {typeof tab.count === 'number' ? <span className="count">{tab.count}</span> : null}
        </button>
      ))}
    </div>
  );
}
