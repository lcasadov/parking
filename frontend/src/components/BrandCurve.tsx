// Curva decorativa corporativa (verde + naranja) anclada a la derecha del header.
// Paths COPIADOS de docs/mockups/shell.js (fuente de verdad); colores tokenizados
// (#c0dd97 -> --green-border, #ef9f27 -> --curve-orange). Decorativa -> aria-hidden.

// variant 'header': viewBox 340x64 (app-header). 'auth': viewBox 380x96 (auth-head),
// mismos trazos escalados (x1.118 / y1.5) manteniendo el estilo del mockup 12.
type CurveVariant = 'header' | 'auth';

const CURVES: Record<CurveVariant, { viewBox: string; green: string; orange: string }> = {
  header: {
    viewBox: '0 0 340 64',
    green: 'M0,40 Q120,5 220,30 T340,20 L340,0 L0,0 Z',
    orange: 'M120,55 Q200,25 280,45 T340,40 L340,55 L120,64 Z',
  },
  auth: {
    viewBox: '0 0 380 96',
    green: 'M0,60 Q134,7.5 246,45 T380,30 L380,0 L0,0 Z',
    orange: 'M134,82.5 Q224,37.5 313,67.5 T380,60 L380,82.5 L134,96 Z',
  },
};

export function BrandCurve({
  className,
  variant = 'header',
}: {
  className?: string;
  variant?: CurveVariant;
}) {
  const curve = CURVES[variant];
  return (
    <svg
      className={className}
      viewBox={curve.viewBox}
      preserveAspectRatio="none"
      aria-hidden="true"
      focusable="false"
    >
      <path d={curve.green} fill="var(--green-border)" opacity="0.85" />
      <path d={curve.orange} fill="var(--curve-orange)" opacity="0.9" />
    </svg>
  );
}

export function BrandLogo({ subtitle = true }: { subtitle?: boolean }) {
  return (
    <span className="brand-block">
      <span className="logo-dot" aria-hidden="true" />
      <span>
        <span className="brand-name">parking</span>
        {subtitle ? <span className="brand-sub">Nexo</span> : null}
      </span>
    </span>
  );
}
