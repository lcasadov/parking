import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { MyReleasesPage } from './MyReleasesPage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { pageOfReleases, releaseFuture, releasePast } from '../mocks/releaseFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

function rowFor(dateText: string): HTMLElement {
  return screen.getByText(dateText).closest('tr') as HTMLElement;
}

describe('MyReleasesPage (EMPLOYEE)', () => {
  it('should_list_only_own_releases_when_employee_requests_mine', async () => {
    renderWithProviders(<MyReleasesPage />);

    expect(await screen.findByText(releaseFuture.releaseDate)).toBeInTheDocument();
    const futureRow = rowFor(releaseFuture.releaseDate);
    expect(within(futureRow).getByText(/voluntaria|voluntary/i)).toBeInTheDocument();

    const pastRow = rowFor(releasePast.releaseDate);
    expect(within(pastRow).getByText(/administrativa|administrative/i)).toBeInTheDocument();
  });

  it('should_cancel_release_when_future_and_own', async () => {
    const user = userEvent.setup();
    let cancelledId: number | null = null;
    server.use(
      http.delete(`${MSW_BASE}/releases/:id`, ({ params }) => {
        cancelledId = Number(params.id);
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderWithProviders(<MyReleasesPage />);

    await screen.findByText(releaseFuture.releaseDate);
    const futureRow = rowFor(releaseFuture.releaseDate);
    await user.click(within(futureRow).getByRole('button', { name: /cancelar|cancel/i }));

    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /anular liberación|cancel release/i }));

    await waitFor(() => expect(cancelledId).toBe(releaseFuture.id));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('should_disable_cancel_when_release_is_past', async () => {
    renderWithProviders(<MyReleasesPage />);

    await screen.findByText(releasePast.releaseDate);
    const pastRow = rowFor(releasePast.releaseDate);
    expect(within(pastRow).getByRole('button', { name: /cancelar|cancel/i })).toBeDisabled();
  });

  it('should_reject_with_403_when_cancelling_release_of_another_employee', async () => {
    const user = userEvent.setup();
    server.use(
      http.delete(`${MSW_BASE}/releases/:id`, () =>
        HttpResponse.json(
          { error: 'RELEASE_FORBIDDEN', message: 'not owner', timestamp: '2026-03-01T09:00:00Z' },
          { status: 403 },
        ),
      ),
    );
    renderWithProviders(
      <>
        <MyReleasesPage />
        <Toast />
      </>,
    );

    await screen.findByText(releaseFuture.releaseDate);
    const futureRow = rowFor(releaseFuture.releaseDate);
    await user.click(within(futureRow).getByRole('button', { name: /cancelar|cancel/i }));
    const dialog = within(await screen.findByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: /anular liberación|cancel release/i }));

    expect(
      await screen.findByText(/no puedes anular una liberación que no es tuya|cannot cancel a release that is not yours/i),
    ).toBeInTheDocument();
  });

  it('should_render_pagination_when_more_than_one_page', async () => {
    server.use(
      http.get(`${MSW_BASE}/releases/mine`, () =>
        HttpResponse.json(
          pageOfReleases([releaseFuture], { totalPages: 2, first: true, last: false }),
        ),
      ),
    );
    renderWithProviders(<MyReleasesPage />);

    const next = await screen.findByRole('button', { name: /siguiente|next/i });
    expect(next).toBeEnabled();
    expect(screen.getByRole('button', { name: /anterior|previous/i })).toBeDisabled();
  });
});
