import { type ReactNode } from 'react';

interface PageFrameProps {
  // `contained` = pantalla de datos: cabecera/controles FIJOS y una única región
  // scrollable (el body). `page` = Panel/Ajustes: el marco scrollea como documento
  // (la cabecera queda sticky arriba). En ≤768px ambos degradan a scroll de documento.
  scroll?: 'contained' | 'page';
  eyebrow?: string;
  title: ReactNode;
  titleId?: string;
  // Ocupación anuncia el cambio de modo (plaza/puesto) con aria-live sobre el título.
  titleAriaLive?: 'polite' | 'off';
  // Zona derecha de la barra de título (acciones de página: Export, "Nuevo…", etc.).
  actions?: ReactNode;
  // SLOT ANCLADO: el selector plaza/puesto SIEMPRE se renderiza aquí (leading del
  // control-row), de modo que su posición la fija el marco, no cada pantalla.
  resourceSelector?: ReactNode;
  // Resto de controles de la fila (tabs, buscador, filtros), tras el selector.
  toolbar?: ReactNode;
  // Filas fijas adicionales (Ocupación: tira de KPIs, nav de semana, filtros rápidos).
  subbar?: ReactNode;
  // Barra inferior fija (paginación).
  footer?: ReactNode;
  // Etiqueta accesible de la región scrollable (obligatoria en modo `contained`).
  bodyLabel?: string;
  children: ReactNode;
}

// Marco de pantalla ADMIN (change sticky-admin-frame). Estructura común a todas las
// pantallas de datos: barra de título + control-row (selector + toolbar) + subbar
// opcional, todo FIJO; y un `pf-body` que es la ÚNICA región con scroll. La disciplina
// de alturas (min-height:0 en la cadena flex) vive en el CSS (.pf / .main / .app-shell).
export function PageFrame({
  scroll = 'contained',
  eyebrow,
  title,
  titleId,
  titleAriaLive,
  actions,
  resourceSelector,
  toolbar,
  subbar,
  footer,
  bodyLabel,
  children,
}: PageFrameProps) {
  const contained = scroll === 'contained';
  return (
    <section className="pf" data-scroll={scroll}>
      <header className="pf-titlebar">
        <div className="pf-titlebar-text">
          {eyebrow ? <span className="page-eyebrow">{eyebrow}</span> : null}
          <h1 id={titleId} className="section-title pf-title" aria-live={titleAriaLive}>
            {title}
          </h1>
        </div>
        {actions ? <div className="pf-actions">{actions}</div> : null}
      </header>

      {resourceSelector || toolbar ? (
        <div className="pf-controls">
          {resourceSelector ? <div className="pf-resource">{resourceSelector}</div> : null}
          {toolbar ? <div className="pf-toolbar">{toolbar}</div> : null}
        </div>
      ) : null}

      {subbar ? <div className="pf-subbar">{subbar}</div> : null}

      <div
        className="pf-body"
        role={contained ? 'region' : undefined}
        tabIndex={contained ? 0 : undefined}
        aria-label={bodyLabel}
      >
        {children}
      </div>

      {footer ? <div className="pf-footer">{footer}</div> : null}
    </section>
  );
}
