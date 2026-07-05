import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { FloorPlanZoom } from './FloorPlanZoom';
import { renderWithProviders } from '../test/renderWithProviders';
import { ZOOM_MAX, ZOOM_MIN } from '../utils/floorPlan';

describe('FloorPlanZoom', () => {
  it('should_fire_zoom_and_reset_callbacks', async () => {
    const onZoomIn = vi.fn();
    const onZoomOut = vi.fn();
    const onReset = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(
      <FloorPlanZoom scale={1} onZoomIn={onZoomIn} onZoomOut={onZoomOut} onReset={onReset} />,
    );

    await user.click(screen.getByRole('button', { name: /acercar|zoom in/i }));
    await user.click(screen.getByRole('button', { name: /alejar|zoom out/i }));
    await user.click(screen.getByRole('button', { name: /restablecer zoom|reset zoom/i }));

    expect(onZoomIn).toHaveBeenCalledTimes(1);
    expect(onZoomOut).toHaveBeenCalledTimes(1);
    expect(onReset).toHaveBeenCalledTimes(1);
  });

  it('should_show_the_current_zoom_percentage', () => {
    renderWithProviders(
      <FloorPlanZoom scale={1.5} onZoomIn={vi.fn()} onZoomOut={vi.fn()} onReset={vi.fn()} />,
    );
    expect(screen.getByText(/zoom 150%/i)).toBeInTheDocument();
  });

  it('should_disable_zoom_out_at_min_and_zoom_in_at_max', () => {
    const { unmount } = renderWithProviders(
      <FloorPlanZoom scale={ZOOM_MIN} onZoomIn={vi.fn()} onZoomOut={vi.fn()} onReset={vi.fn()} />,
    );
    expect(screen.getByRole('button', { name: /alejar|zoom out/i })).toBeDisabled();
    unmount();

    renderWithProviders(
      <FloorPlanZoom scale={ZOOM_MAX} onZoomIn={vi.fn()} onZoomOut={vi.fn()} onReset={vi.fn()} />,
    );
    expect(screen.getByRole('button', { name: /acercar|zoom in/i })).toBeDisabled();
  });
});
