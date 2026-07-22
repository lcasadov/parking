import { type ReactNode } from 'react';

// Estados reutilizables de una vista de datos/tabla (contrato §6): carga
// (skeleton), vacio (mensaje + accion) y error (mensaje + reintentar). Se
// renderizan EN LUGAR de la tabla. Presentacionales: la copia llega traducida
// desde el caller (sin i18n propia, sin hex sueltos).

interface TableSkeletonProps {
  // Nombre accesible del estado de carga (cadena ya traducida). Ej: t('...loading').
  label: string;
  rows?: number;
  columns?: number;
}

// Skeleton de carga: filas/columnas de barras con shimmer. aria-busy + role
// status para que el lector de pantalla anuncie la carga sin leer el andamiaje.
export function TableSkeleton({ label, rows = 5, columns = 4 }: TableSkeletonProps) {
  return (
    <div className="table-skeleton" role="status" aria-busy="true">
      <span className="sr-only">{label}</span>
      {Array.from({ length: rows }, (_, rowIndex) => (
        <div className="table-skeleton-row" key={`row-${rowIndex}`} aria-hidden="true">
          {Array.from({ length: columns }, (_, colIndex) => (
            <span className="skeleton-bar" key={`cell-${rowIndex}-${colIndex}`} />
          ))}
        </div>
      ))}
    </div>
  );
}

interface TableEmptyProps {
  // Mensaje principal (cadena ya traducida).
  message: string;
  icon?: string;
  // Accion opcional (p. ej. un Button "Crear"). El caller la compone.
  action?: ReactNode;
}

// Estado vacio: icono + mensaje + accion opcional para guiar al usuario.
export function TableEmpty({ message, icon = 'inbox', action }: TableEmptyProps) {
  return (
    <div className="data-state" role="status">
      <i className={`ti ti-${icon} data-state-icon`} aria-hidden="true" />
      <p className="data-state-message">{message}</p>
      {action ? <div className="data-state-action">{action}</div> : null}
    </div>
  );
}

interface TableErrorProps {
  // Mensaje de error de usuario (cadena ya traducida, nunca el error crudo).
  message: string;
  // Etiqueta del boton reintentar (cadena ya traducida).
  retryLabel: string;
  onRetry: () => void;
  icon?: string;
}

// Estado de error: mensaje + boton reintentar. role=alert para anunciarlo.
export function TableError({
  message,
  retryLabel,
  onRetry,
  icon = 'alert-triangle',
}: TableErrorProps) {
  return (
    <div className="data-state data-state-error" role="alert">
      <i className={`ti ti-${icon} data-state-icon`} aria-hidden="true" />
      <p className="data-state-message">{message}</p>
      <div className="data-state-action">
        <button type="button" className="btn btn-white" onClick={onRetry}>
          <i className="ti ti-refresh" aria-hidden="true" />
          {retryLabel}
        </button>
      </div>
    </div>
  );
}
