import { type InputHTMLAttributes } from 'react';

interface SearchBoxProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'value' | 'onChange'> {
  value: string;
  onValueChange: (next: string) => void;
  // Nombre accesible del campo (obligatorio: el icono es decorativo). El caller
  // pasa la cadena ya traducida (t('...')). Presentacional, sin i18n propia.
  label: string;
}

// Buscador reutilizable (design-system §6.8): caja con icono ti-search a la
// izquierda + input. El icono es decorativo (aria-hidden); el nombre accesible
// del input viene de `label`. Reutiliza la clase .search-box existente.
export function SearchBox({ value, onValueChange, label, ...rest }: SearchBoxProps) {
  return (
    <div className="search-box">
      <i className="ti ti-search" aria-hidden="true" />
      <input
        type="search"
        aria-label={label}
        value={value}
        onChange={(event) => onValueChange(event.target.value)}
        {...rest}
      />
    </div>
  );
}
