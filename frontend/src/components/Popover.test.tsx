import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Popover } from './Popover';

describe('Popover', () => {
  it('should_render_children_inside_labelled_dialog', () => {
    render(
      <Popover onClose={() => {}} label="Preferencias">
        <button type="button">Cerrar sesión</button>
      </Popover>,
    );
    expect(screen.getByRole('dialog', { name: 'Preferencias' })).toBeInTheDocument();
  });

  it('should_close_on_escape', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(
      <Popover onClose={onClose} label="Menu">
        <button type="button">Acción</button>
      </Popover>,
    );

    await user.keyboard('{Escape}');

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should_close_when_clicking_outside', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(
      <div>
        <button type="button">fuera</button>
        <Popover onClose={onClose} label="Menu">
          <button type="button">dentro</button>
        </Popover>
      </div>,
    );

    await user.click(screen.getByRole('button', { name: 'fuera' }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should_not_close_when_clicking_inside', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(
      <Popover onClose={onClose} label="Menu">
        <button type="button">dentro</button>
      </Popover>,
    );

    await user.click(screen.getByRole('button', { name: 'dentro' }));

    expect(onClose).not.toHaveBeenCalled();
  });

  it('should_wrap_focus_from_last_to_first_on_tab', () => {
    render(
      <Popover onClose={() => {}} label="Menu">
        <button type="button">uno</button>
        <button type="button">dos</button>
      </Popover>,
    );
    const first = screen.getByRole('button', { name: 'uno' });
    const last = screen.getByRole('button', { name: 'dos' });
    last.focus();

    fireEvent.keyDown(last, { key: 'Tab' });

    expect(document.activeElement).toBe(first);
  });

  it('should_wrap_focus_from_first_to_last_on_shift_tab', () => {
    render(
      <Popover onClose={() => {}} label="Menu">
        <button type="button">uno</button>
        <button type="button">dos</button>
      </Popover>,
    );
    const first = screen.getByRole('button', { name: 'uno' });
    const last = screen.getByRole('button', { name: 'dos' });
    first.focus();

    fireEvent.keyDown(first, { key: 'Tab', shiftKey: true });

    expect(document.activeElement).toBe(last);
  });

  it('should_swallow_tab_when_there_is_nothing_focusable', () => {
    const onClose = vi.fn();
    render(
      <Popover onClose={onClose} label="Vacio">
        <span>solo texto</span>
      </Popover>,
    );

    fireEvent.keyDown(document, { key: 'Tab' });

    expect(onClose).not.toHaveBeenCalled();
  });
});
