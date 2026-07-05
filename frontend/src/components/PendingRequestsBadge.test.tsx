import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { PendingRequestsBadge } from './PendingRequestsBadge';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';

const EMPTY_PAGE = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 20,
  number: 0,
  first: true,
  last: true,
};

describe('PendingRequestsBadge', () => {
  it('should_render_count_when_there_are_pending_requests', async () => {
    // Handler por defecto: 2 pendientes.
    renderWithProviders(<PendingRequestsBadge />);
    expect(await screen.findByText('2')).toBeInTheDocument();
  });

  it('should_render_nothing_when_no_pending_requests', async () => {
    let served = false;
    server.use(
      http.get(`${MSW_BASE}/requests/pending`, () => {
        served = true;
        return HttpResponse.json(EMPTY_PAGE);
      }),
    );
    const { container } = renderWithProviders(<PendingRequestsBadge />);
    // Espera a que la query resuelva; sin pendientes el badge no aparece.
    await waitFor(() => expect(served).toBe(true));
    expect(container.querySelector('.badge-red')).toBeNull();
  });
});
