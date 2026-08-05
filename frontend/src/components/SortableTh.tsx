import { useTranslation } from 'react-i18next';
import type { SortState } from '../hooks/useTableSort';

interface SortableThProps {
  // Campo de orden (nombre de propiedad que entiende el backend, p. ej. `requestedDate`).
  field: string;
  // Texto visible de la cabecera (también usado en el aria-label "Ordenar por …").
  label: string;
  sort: SortState | null;
  onToggle: (field: string) => void;
  className?: string;
}

// Cabecera de columna ordenable: un `<th>` con un `<button>` (etiqueta + icono
// ▲/▼/neutro). `aria-sort` refleja el estado y el botón alterna el orden de esa columna
// (ciclo sin-orden → asc → desc → sin-orden, gestionado por useTableSort).
export function SortableTh({ field, label, sort, onToggle, className }: SortableThProps) {
  const { t } = useTranslation();
  const active = sort?.field === field;
  const dir = active ? sort?.dir : undefined;
  const ariaSort = active ? (dir === 'asc' ? 'ascending' : 'descending') : 'none';
  const icon = active
    ? dir === 'asc'
      ? 'arrow-narrow-up'
      : 'arrow-narrow-down'
    : 'arrows-sort';
  return (
    <th scope="col" className={className} aria-sort={ariaSort}>
      <button
        type="button"
        className={`th-sort${active ? ' is-active' : ''}`}
        aria-label={t('common.sortBy', { column: label })}
        onClick={() => onToggle(field)}
      >
        <span>{label}</span>
        <i className={`ti ti-${icon} th-sort-icon`} aria-hidden="true" />
      </button>
    </th>
  );
}
