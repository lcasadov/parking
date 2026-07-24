import { type ReactNode } from 'react';
import { PageHeader } from './PageHeader';

// Cabecera de página que se adapta a si la página se monta suelta o EMBEBIDA dentro
// de una sección contenedora (Recursos/Liberar/Registros). Suelta: PageHeader
// completo (eyebrow + título + descripción + acciones). Embebida: el contenedor ya
// aporta el título de sección, así que solo se pinta la barra de acciones (a la
// derecha) para no duplicar títulos.
export function EmbeddablePageHeader({
  embedded,
  eyebrow,
  title,
  description,
  actions,
}: {
  embedded: boolean;
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  if (embedded) {
    return actions ? <div className="embedded-actions">{actions}</div> : null;
  }
  return <PageHeader eyebrow={eyebrow} title={title} description={description} actions={actions} />;
}
