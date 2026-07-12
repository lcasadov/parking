export interface LegendItem {
  // Color CSS del punto (idealmente un token, p.ej. 'var(--green)').
  color: string;
  label: string;
}

interface LegendProps {
  items: LegendItem[];
}

// Leyenda de colores (mockup .legend): lista de puntos con etiqueta.
export function Legend({ items }: LegendProps) {
  return (
    <ul className="legend">
      {items.map((item) => (
        <li key={item.label} className="legend-item">
          <span className="legend-dot" style={{ background: item.color }} aria-hidden="true" />
          {item.label}
        </li>
      ))}
    </ul>
  );
}
