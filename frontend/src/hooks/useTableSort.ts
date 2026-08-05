import { useState } from 'react';

export type SortDir = 'asc' | 'desc';

export interface SortState {
  field: string;
  dir: SortDir;
}

export interface TableSort {
  sort: SortState | null;
  // Cadena de orden en formato Spring `campo,dir` (o undefined si no hay orden), lista
  // para pasar como query param a un endpoint paginado.
  sortParam: string | undefined;
  toggle: (field: string) => void;
}

// Estado de orden de una tabla con ciclo de 3 estados por columna: sin-orden → asc →
// desc → sin-orden. Activar otra columna reinicia esa columna en ascendente. Sirve tanto
// para ordenación en servidor (usar `sortParam`) como en cliente (usar `sort`).
export function useTableSort(initial: SortState | null = null): TableSort {
  const [sort, setSort] = useState<SortState | null>(initial);

  function toggle(field: string): void {
    setSort((previous) => {
      if (!previous || previous.field !== field) {
        return { field, dir: 'asc' };
      }
      if (previous.dir === 'asc') {
        return { field, dir: 'desc' };
      }
      return null;
    });
  }

  const sortParam = sort ? `${sort.field},${sort.dir}` : undefined;
  return { sort, sortParam, toggle };
}
