import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AvailabilityPage } from './AvailabilityPage';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { defaultAvailability, emptyAvailability } from '../mocks/calendarFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

describe('AvailabilityPage (ADMIN)', () => {
  it('should_listAvailableSpaces_when_dateSelected', async () => {
    server.use(
      http.get(`${MSW_BASE}/availability`, () => HttpResponse.json(defaultAvailability)),
    );
    renderWithProviders(<AvailabilityPage />);

    expect(await screen.findByText('P-01')).toBeInTheDocument();
    expect(screen.getByText('P-03')).toBeInTheDocument();
    expect(screen.getByText(/2 plaza|2 space/i)).toBeInTheDocument();
  });

  it('should_showEmptyState_when_noSpacesAvailable', async () => {
    server.use(
      http.get(`${MSW_BASE}/availability`, () => HttpResponse.json(emptyAvailability)),
    );
    renderWithProviders(<AvailabilityPage />);

    expect(
      await screen.findByText(/no hay plazas disponibles|no spaces available/i),
    ).toBeInTheDocument();
  });

  it('should_showValidationHint_when_dateClearedOrInvalid', async () => {
    renderWithProviders(<AvailabilityPage />);

    const dateInput = screen.getByLabelText(/^fecha$|^date$/i) as HTMLInputElement;
    fireEvent.change(dateInput, { target: { value: '' } });

    expect(
      await screen.findByText(/elige una fecha|pick a date|introduce una fecha|enter a valid/i),
    ).toBeInTheDocument();
  });

  it('should_showError_when_availabilityRequestFails', async () => {
    server.use(
      http.get(`${MSW_BASE}/availability`, () =>
        HttpResponse.json(
          { error: 'server', message: 'boom', timestamp: '2026-05-11T09:00:00Z' },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<AvailabilityPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/no se pudo cargar la disponibilidad|availability could not be loaded/i),
      ).toBeInTheDocument();
    });
  });
});
