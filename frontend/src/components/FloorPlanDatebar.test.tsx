import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { FloorPlanDatebar } from './FloorPlanDatebar';
import { renderWithProviders } from '../test/renderWithProviders';
import { addDaysIso } from '../utils/calendar';
import { todayIso } from '../utils/requests';

describe('FloorPlanDatebar', () => {
  it('should_disable_previous_and_today_when_date_is_today', () => {
    renderWithProviders(<FloorPlanDatebar date={todayIso()} onChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: /día anterior|previous day/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /^hoy$|^today$/i })).toBeDisabled();
  });

  it('should_navigate_to_next_and_previous_day_within_window', async () => {
    const onChange = vi.fn();
    const start = addDaysIso(todayIso(), 2);
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanDatebar date={start} onChange={onChange} />);

    await user.click(screen.getByRole('button', { name: /día siguiente|next day/i }));
    expect(onChange).toHaveBeenCalledWith(addDaysIso(start, 1));

    await user.click(screen.getByRole('button', { name: /día anterior|previous day/i }));
    expect(onChange).toHaveBeenCalledWith(addDaysIso(start, -1));

    await user.click(screen.getByRole('button', { name: /^hoy$|^today$/i }));
    expect(onChange).toHaveBeenCalledWith(todayIso());
  });

  it('should_allow_navigating_to_any_far_future_date_without_upper_cap', async () => {
    const onChange = vi.fn();
    const farFuture = addDaysIso(todayIso(), 90);
    const user = userEvent.setup();
    renderWithProviders(<FloorPlanDatebar date={farFuture} onChange={onChange} />);

    const nextButton = screen.getByRole('button', { name: /día siguiente|next day/i });
    expect(nextButton).toBeEnabled();

    await user.click(nextButton);
    expect(onChange).toHaveBeenCalledWith(addDaysIso(farFuture, 1));
  });
});
