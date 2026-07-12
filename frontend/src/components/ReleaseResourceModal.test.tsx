import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { ReleaseResourceModal } from './ReleaseResourceModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';
import { todayIso } from '../utils/releases';

const RELEASES_URL = `${MSW_BASE}/releases`;

function renderModal(overrides: Partial<Parameters<typeof ReleaseResourceModal>[0]> = {}) {
  const onClose = vi.fn();
  const onReleased = vi.fn();
  renderWithProviders(
    <ReleaseResourceModal
      parkingSpaceId={12}
      spaceLabel="P-12"
      onClose={onClose}
      onReleased={onReleased}
      {...overrides}
    />,
  );
  return { onClose, onReleased };
}

describe('ReleaseResourceModal', () => {
  it('should_show_the_fixed_resource_and_confirmation_summary', () => {
    renderModal();
    // Banner verde con el recurso fijo.
    expect(screen.getByText(/tu recurso fijo es|your fixed resource is/i)).toBeInTheDocument();
    // Resumen: pill de tipo VOLUNTARIA y nota azul de disponibilidad.
    expect(screen.getByText(/voluntaria|voluntary/i)).toBeInTheDocument();
    expect(
      screen.getByText(/otro empleado lo solicite|another employee to request/i),
    ).toBeInTheDocument();
    // La etiqueta del recurso aparece en el banner y en el resumen.
    expect(screen.getAllByText(/P-12/).length).toBeGreaterThan(0);
  });

  it('should_require_a_date_before_releasing', async () => {
    const user = userEvent.setup();
    const { onReleased } = renderModal();

    await user.click(screen.getByRole('button', { name: /^liberar$|^release$/i }));

    expect(screen.getByRole('alert')).toHaveTextContent(/selecciona una fecha|select a date/i);
    expect(onReleased).not.toHaveBeenCalled();
  });

  it('should_release_the_space_for_a_valid_date', async () => {
    let body: Record<string, unknown> | null = null;
    server.use(
      http.post(RELEASES_URL, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 1 }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    const { onReleased } = renderModal();

    const dateInput = screen.getByLabelText(/fecha a liberar|date to release/i);
    await user.type(dateInput, todayIso());
    await user.click(screen.getByRole('button', { name: /^liberar$|^release$/i }));

    await waitFor(() => expect(onReleased).toHaveBeenCalled());
    expect(body).toEqual({ releaseDate: todayIso(), parkingSpaceId: 12 });
  });
});
