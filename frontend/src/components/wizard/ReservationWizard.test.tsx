import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ReservationWizard } from './ReservationWizard';
import { renderWithProviders } from '../../test/renderWithProviders';

describe('ReservationWizard (multi-type)', () => {
  it('should_add_location_steps_for_each_selected_type', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReservationWizard onClose={vi.fn()} />);

    // Paso 1: seleccionar AMBOS recursos (plaza y puesto).
    await user.click(screen.getByRole('button', { name: /plaza de parking|parking space/i }));
    await user.click(screen.getByRole('button', { name: /puesto de oficina|office desk/i }));

    // El stepper dinámico muestra un paso de ubicación por tipo: Plaza y Puesto.
    await waitFor(() => {
      expect(screen.getAllByText(/ubicación · plaza|location · space/i).length).toBeGreaterThan(0);
      expect(screen.getAllByText(/ubicación · puesto|location · desk/i).length).toBeGreaterThan(0);
    });
  });

  it('should_show_a_single_location_step_for_one_type', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReservationWizard onClose={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: /plaza de parking|parking space/i }));

    expect(screen.getAllByText(/ubicación · plaza|location · space/i).length).toBeGreaterThan(0);
    expect(screen.queryByText(/ubicación · puesto|location · desk/i)).not.toBeInTheDocument();
  });
});
