import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { ViewInPlanTrigger } from './ViewInPlanTrigger';
import { VisitorDetailModal } from './VisitorDetailModal';
import { DeskMapButton } from './DeskMapButton';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';

describe('leaf components smoke', () => {
  it('ViewInPlanTrigger degrades to a disabled button without a resolved desk', () => {
    // Sin puesto resuelto, el disparador es un botón inerte (rama temprana, sin tooltip/modal).
    renderWithProviders(<ViewInPlanTrigger desk={null} deskLabel="D-03" date="2026-08-10" />);
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
  });

  it('VisitorDetailModal loads and renders the visitor record', async () => {
    server.use(
      http.get(`${MSW_BASE}/visitors/:id`, () =>
        HttpResponse.json({
          id: 7,
          firstName: 'Vera',
          lastName: 'Visit',
          nationalId: 'X1234567Z',
          company: 'ACME',
          licensePlate: '1234ABC',
          phone: null,
          email: null,
          notes: null,
        }),
      ),
    );

    renderWithProviders(<VisitorDetailModal visitorId={7} onClose={vi.fn()} />);

    expect(await screen.findByText('Vera')).toBeInTheDocument();
    expect(screen.getByText('Visit')).toBeInTheDocument();
  });

  it('DeskMapButton opens the plan modal and reports an unmatched desk', async () => {
    const user = userEvent.setup();
    // Plano sin el puesto buscado → el modal cae en la rama "puesto no encontrado".
    server.use(http.get(`${MSW_BASE}/floor-plan`, () => HttpResponse.json({ desks: [] })));

    renderWithProviders(<DeskMapButton deskLabel="D-99" date="2026-08-10" />);

    await user.click(screen.getByRole('button', { name: /plano|map/i }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
  });
});
