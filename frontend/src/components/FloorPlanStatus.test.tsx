import { screen } from '@testing-library/react';
import { AxiosError } from 'axios';
import { describe, expect, it } from 'vitest';
import { FloorPlanStatus } from './FloorPlanStatus';
import { renderWithProviders } from '../test/renderWithProviders';

// Error 400 con la forma ApiError de ventana fuera de rango (init-floor-plan).
function outsideWindowError(): AxiosError {
  const error = new AxiosError('bad request');
  error.response = {
    status: 400,
    data: { error: 'OUTSIDE_REQUEST_WINDOW', message: 'out of range' },
    statusText: 'Bad Request',
    headers: {},
    config: {} as never,
  };
  return error;
}

describe('FloorPlanStatus', () => {
  it('should_prompt_for_a_valid_date_when_date_is_invalid', () => {
    renderWithProviders(
      <FloorPlanStatus isDateValid={false} isLoading={false} isError={false} error={null} />,
    );
    expect(
      screen.getByText(/introduce una fecha válida|enter a valid date/i),
    ).toBeInTheDocument();
  });

  it('should_render_a_spinner_while_loading', () => {
    renderWithProviders(
      <FloorPlanStatus isDateValid isLoading isError={false} error={null} />,
    );
    expect(screen.getByText(/cargando|loading/i)).toBeInTheDocument();
  });

  it('should_show_the_outside_window_message_on_a_window_error', () => {
    renderWithProviders(
      <FloorPlanStatus isDateValid isLoading={false} isError error={outsideWindowError()} />,
    );
    expect(
      screen.getByText(/no puede ser anterior a hoy|cannot be earlier than today/i),
    ).toBeInTheDocument();
  });

  it('should_show_the_generic_load_error_on_any_other_error', () => {
    renderWithProviders(
      <FloorPlanStatus isDateValid isLoading={false} isError error={new Error('boom')} />,
    );
    expect(
      screen.getByText(/no se pudo cargar el plano|the floor plan could not be loaded/i),
    ).toBeInTheDocument();
  });

  it('should_render_nothing_when_the_plan_can_be_shown', () => {
    const { container } = renderWithProviders(
      <FloorPlanStatus isDateValid isLoading={false} isError={false} error={null} />,
    );
    expect(container).toBeEmptyDOMElement();
  });
});
