import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Modal } from './Modal';
import { renderWithProviders } from '../test/renderWithProviders';

const TABS = [
  { id: 'datos', label: 'Datos' },
  { id: 'dias', label: 'Días' },
];

describe('Modal', () => {
  it('should_render_dialog_with_icon_and_close_button', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    renderWithProviders(
      <Modal title="Editar empleado" icon="user" onClose={onClose}>
        <p>cuerpo</p>
      </Modal>,
    );
    expect(screen.getByRole('dialog', { name: 'Editar empleado' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /cerrar|close/i }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should_not_close_on_escape', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    renderWithProviders(
      <Modal title="Aviso" onClose={onClose}>
        <p>x</p>
      </Modal>,
    );

    await user.keyboard('{Escape}');

    // Decisión de producto: los modales no se cierran con Escape.
    expect(onClose).not.toHaveBeenCalled();
  });

  it('should_render_tabs_and_report_tab_changes', async () => {
    const user = userEvent.setup();
    const onTabChange = vi.fn();
    renderWithProviders(
      <Modal title="Empleado" tabs={TABS} activeTab="datos" onTabChange={onTabChange}>
        <p>x</p>
      </Modal>,
    );
    expect(screen.getByRole('tab', { name: 'Datos' })).toHaveAttribute('aria-selected', 'true');

    await user.click(screen.getByRole('tab', { name: 'Días' }));

    expect(onTabChange).toHaveBeenCalledWith('dias');
  });

  it('should_hide_close_button_when_not_closeable', () => {
    renderWithProviders(
      <Modal title="Bloqueante" variant="amber" closeable={false}>
        <p>x</p>
      </Modal>,
    );
    expect(screen.queryByRole('button', { name: /cerrar|close/i })).not.toBeInTheDocument();
    expect(screen.getByRole('dialog').querySelector('.modal-header')).toHaveClass('amber');
  });
});
