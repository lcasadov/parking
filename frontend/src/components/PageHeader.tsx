import { type ReactNode } from 'react';

// Cabecera de pagina ALEATICA (contrato §5): eyebrow (etiqueta 11px uppercase)
// + titulo serif (Cormorant) + descripcion opcional, con zona de acciones a la
// derecha. Reutilizable por todas las pantallas autenticadas.
export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <header className="page-header">
      <div className="page-heading">
        {eyebrow ? <span className="page-eyebrow">{eyebrow}</span> : null}
        <h1 className="section-title">{title}</h1>
        {description ? <p className="page-description">{description}</p> : null}
      </div>
      {actions ? <div className="page-actions">{actions}</div> : null}
    </header>
  );
}
