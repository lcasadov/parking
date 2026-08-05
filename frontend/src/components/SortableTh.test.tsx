import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../test/renderWithProviders';
import { SortableTh } from './SortableTh';
import type { SortState } from '../hooks/useTableSort';

function renderTh(sort: SortState | null, onToggle: (field: string) => void = () => {}) {
  return renderWithProviders(
    <table>
      <thead>
        <tr>
          <SortableTh field="createdAt" label="Creada" sort={sort} onToggle={onToggle} />
        </tr>
      </thead>
    </table>,
  );
}

describe('SortableTh', () => {
  it('should_expose_aria_sort_none_when_inactive', () => {
    renderTh(null);
    expect(screen.getByRole('columnheader')).toHaveAttribute('aria-sort', 'none');
  });

  it('should_expose_aria_sort_ascending_when_active_asc', () => {
    renderTh({ field: 'createdAt', dir: 'asc' });
    expect(screen.getByRole('columnheader')).toHaveAttribute('aria-sort', 'ascending');
  });

  it('should_expose_aria_sort_descending_when_active_desc', () => {
    renderTh({ field: 'createdAt', dir: 'desc' });
    expect(screen.getByRole('columnheader')).toHaveAttribute('aria-sort', 'descending');
  });

  it('should_call_onToggle_with_field_on_click', () => {
    const onToggle = vi.fn();
    renderTh(null, onToggle);
    fireEvent.click(screen.getByRole('button'));
    expect(onToggle).toHaveBeenCalledWith('createdAt');
  });
});
