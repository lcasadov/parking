import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { DayCards } from './DayCards';
import { renderWithProviders } from '../test/renderWithProviders';

describe('DayCards', () => {
  it('should_render_monday_to_friday_by_default', () => {
    renderWithProviders(<DayCards value={[]} onChange={() => {}} />);
    expect(screen.getAllByRole('button')).toHaveLength(5);
  });

  it('should_mark_selected_days_as_pressed', () => {
    renderWithProviders(<DayCards value={[2]} onChange={() => {}} />);
    const pressed = screen
      .getAllByRole('button')
      .filter((b) => b.getAttribute('aria-pressed') === 'true');
    expect(pressed).toHaveLength(1);
    expect(pressed[0]).toHaveClass('selected');
  });

  it('should_add_day_when_selecting_an_unselected_card', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    renderWithProviders(<DayCards value={[1]} onChange={onChange} />);

    await user.click(screen.getAllByRole('button')[2]);

    expect(onChange).toHaveBeenCalledWith([1, 3]);
  });

  it('should_remove_day_when_deselecting_a_selected_card', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    renderWithProviders(<DayCards value={[1, 3]} onChange={onChange} />);

    await user.click(screen.getAllByRole('button')[0]);

    expect(onChange).toHaveBeenCalledWith([3]);
  });

  it('should_honour_custom_day_set', () => {
    renderWithProviders(<DayCards value={[]} onChange={() => {}} days={[1, 2, 3]} />);
    expect(screen.getAllByRole('button')).toHaveLength(3);
  });
});
