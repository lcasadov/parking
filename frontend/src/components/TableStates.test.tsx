import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { TableEmpty, TableError, TableSkeleton } from './TableStates';

describe('TableSkeleton', () => {
  it('should_announce_loading_and_render_rows_by_columns_bars', () => {
    const { container } = render(<TableSkeleton label="Cargando" rows={3} columns={4} />);
    const region = screen.getByRole('status');
    expect(region).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByText('Cargando')).toBeInTheDocument();
    expect(container.querySelectorAll('.skeleton-bar')).toHaveLength(12);
  });
});

describe('TableEmpty', () => {
  it('should_render_message_and_optional_action', () => {
    render(<TableEmpty message="Sin resultados" action={<button type="button">Crear</button>} />);
    expect(screen.getByText('Sin resultados')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Crear' })).toBeInTheDocument();
  });

  it('should_render_without_action', () => {
    const { container } = render(<TableEmpty message="Vacío" />);
    expect(container.querySelector('.data-state-action')).not.toBeInTheDocument();
  });
});

describe('TableError', () => {
  it('should_announce_error_and_retry_on_click', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    render(<TableError message="No se pudo cargar" retryLabel="Reintentar" onRetry={onRetry} />);
    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo cargar');
    await user.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});
