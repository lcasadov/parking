import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DayBadges } from './DayBadges';
import { renderWithProviders } from '../test/renderWithProviders';

describe('DayBadges', () => {
  it('should_render_five_weekday_chips', () => {
    const { container } = renderWithProviders(<DayBadges days={[1, 2, 3, 4, 5]} />);
    expect(container.querySelectorAll('.day-chip')).toHaveLength(5);
  });

  it('should_mark_included_days_on_and_the_rest_off', () => {
    const { container } = renderWithProviders(<DayBadges days={[1, 3, 5]} />);
    expect(container.querySelectorAll('.day-chip.on')).toHaveLength(3);
    expect(container.querySelectorAll('.day-chip.off')).toHaveLength(2);
  });

  it('should_expose_accessible_day_name_and_state', () => {
    renderWithProviders(<DayBadges days={[1]} />);
    // Lunes incluido; Martes no incluido (texto sr-only).
    expect(screen.getByText(/lunes: incluido/i)).toBeInTheDocument();
    expect(screen.getByText(/martes: no incluido/i)).toBeInTheDocument();
  });
});
