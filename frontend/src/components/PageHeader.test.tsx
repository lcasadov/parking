import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { PageHeader } from './PageHeader';

describe('PageHeader', () => {
  it('should_render_eyebrow_title_description_and_actions', () => {
    render(
      <PageHeader
        eyebrow="Operativa"
        title="Solicitudes"
        description="Gestiona la cola de peticiones"
        actions={<button type="button">Nueva</button>}
      />,
    );

    expect(screen.getByRole('heading', { name: 'Solicitudes' })).toBeInTheDocument();
    expect(screen.getByText('Operativa')).toHaveClass('page-eyebrow');
    expect(screen.getByText('Gestiona la cola de peticiones')).toHaveClass('page-description');
    expect(screen.getByRole('button', { name: 'Nueva' })).toBeInTheDocument();
  });

  it('should_omit_optional_eyebrow_description_and_actions', () => {
    const { container } = render(<PageHeader title="Empleados" />);

    expect(screen.getByRole('heading', { name: 'Empleados' })).toBeInTheDocument();
    expect(container.querySelector('.page-eyebrow')).toBeNull();
    expect(container.querySelector('.page-description')).toBeNull();
    expect(container.querySelector('.page-actions')).toBeNull();
  });
});
