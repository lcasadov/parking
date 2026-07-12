import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Toggle } from './Toggle';

describe('Toggle', () => {
  it('should_expose_switch_role_with_checked_state', () => {
    render(<Toggle checked onChange={() => {}} label="Notificaciones" />);
    const sw = screen.getByRole('switch', { name: 'Notificaciones' });
    expect(sw).toHaveAttribute('aria-checked', 'true');
    expect(sw).toHaveClass('on');
  });

  it('should_call_onChange_with_negated_value_when_clicked', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Toggle checked={false} onChange={onChange} label="Tema oscuro" />);

    await user.click(screen.getByRole('switch', { name: 'Tema oscuro' }));

    expect(onChange).toHaveBeenCalledWith(true);
  });

  it('should_toggle_via_keyboard_space', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Toggle checked onChange={onChange} label="Activo" />);

    screen.getByRole('switch').focus();
    await user.keyboard(' ');

    expect(onChange).toHaveBeenCalledWith(false);
  });

  it('should_not_fire_onChange_when_disabled', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Toggle checked={false} onChange={onChange} label="Bloqueado" disabled />);

    await user.click(screen.getByRole('switch'));

    expect(onChange).not.toHaveBeenCalled();
  });
});
