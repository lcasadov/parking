import { type ReactNode } from 'react';

interface StatTileProps {
  // Etiqueta corta de la métrica (p.ej. "Inventario"). Cadena ya traducida.
  label: string;
  // Valor numérico grande (mono, tabular). Se combina con `unit` para que el
  // texto renderizado nunca sea un dígito suelto (evita colisiones de lectura).
  value: ReactNode;
  // Unidad/sufijo inline (p.ej. "plazas") mostrada en pequeño junto al valor.
  unit?: string;
  // Sub-línea atenuada opcional (contexto: "de 20", "hoy"…).
  sub?: string;
  // Color del punto indicador (token CSS, p.ej. "var(--accent)").
  dot?: string;
  // Icono Tabler opcional (nombre sin el prefijo "ti ti-").
  icon?: string;
}

// Tesela de KPI reutilizable (rediseño 2026): tarjeta con hairline + sombra en
// capas, etiqueta con punto/icono, numeral mono grande y sub-línea atenuada.
// Mismo lenguaje visual que las KPIs de Dashboard/Ocupación, generalizado para
// las pantallas de Gestión (Recursos, Registros).
export function StatTile({ label, value, unit, sub, dot, icon }: StatTileProps) {
  return (
    <div className="card stat-tile">
      <div className="stat-tile-lbl">
        {dot ? <span className="stat-tile-dot" style={{ background: dot }} /> : null}
        {icon ? <i className={`ti ti-${icon}`} aria-hidden="true" /> : null}
        {label}
      </div>
      <div className="stat-tile-val mono">
        {value}
        {unit ? <small>{unit}</small> : null}
      </div>
      {sub ? <div className="stat-tile-sub">{sub}</div> : null}
    </div>
  );
}
