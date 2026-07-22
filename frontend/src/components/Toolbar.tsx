import { type ReactNode } from 'react';

interface ToolbarProps {
  children: ReactNode;
  // Nombre accesible de la barra (buscador + filtros). Cadena ya traducida.
  ariaLabel?: string;
}

// Barra de herramientas reutilizable (contrato §6): fila flexible que agrupa el
// buscador (SearchBox), filtros (select/Tabs) y la accion primaria (Button). La
// clase .toolbar ya reparte el espacio (buscador a la derecha via .search-box).
// Presentacional: la composicion la decide cada pantalla.
export function Toolbar({ children, ariaLabel }: ToolbarProps) {
  return (
    <div className="toolbar" role="search" aria-label={ariaLabel}>
      {children}
    </div>
  );
}
