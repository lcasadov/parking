// Curva decorativa corporativa (verde + naranja) anclada a la derecha del header.
// Decorativa -> aria-hidden.
export function BrandCurve({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 340 64"
      preserveAspectRatio="none"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M120 64 C200 64 200 0 340 0 L340 64 Z" fill="#639922" opacity="0.16" />
      <path d="M180 64 C260 64 260 8 340 8 L340 64 Z" fill="#f08a24" opacity="0.14" />
    </svg>
  );
}

export function BrandLogo({ subtitle = true }: { subtitle?: boolean }) {
  return (
    <span className="brand-block">
      <span className="logo-dot" aria-hidden="true" />
      <span>
        <span className="brand-name">parking</span>
        {subtitle ? <span className="brand-sub">ALEATICA</span> : null}
      </span>
    </span>
  );
}
