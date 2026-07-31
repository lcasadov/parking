// KPI compacto en tira (marco sticky): punto de color + numeral Geist Mono tabular +
// etiqueta y sublínea (opcional) apiladas. Una fila fina que conserva la información de
// las tarjetas altas sin robar alto. Compartido por Ocupación y Recursos.
export function KpiStat({
  dot,
  value,
  label,
  sub,
}: {
  dot: string;
  value: number | string;
  label: string;
  sub?: string;
}) {
  return (
    <div className="occ-kpi-stat">
      <span className="occ-kpi-stat-dot" style={{ background: dot }} aria-hidden="true" />
      <span className="occ-kpi-stat-val mono">{value}</span>
      <span className="occ-kpi-stat-text">
        <span className="occ-kpi-stat-lbl">{label}</span>
        {sub ? <span className="occ-kpi-stat-sub">{sub}</span> : null}
      </span>
    </div>
  );
}
