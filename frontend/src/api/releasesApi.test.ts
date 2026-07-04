import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import {
  cancelRelease,
  createAdministrativeRelease,
  createRelease,
  listMyReleases,
} from './releasesApi';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { releaseFuture } from '../mocks/releaseFixtures';

describe('releasesApi', () => {
  it('should_send_page_params_when_listMyReleases_called_with_paging', async () => {
    let receivedUrl = '';
    server.use(
      http.get(`${MSW_BASE}/releases/mine`, ({ request }) => {
        receivedUrl = request.url;
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 20,
          number: 0,
          first: true,
          last: true,
        });
      }),
    );

    await listMyReleases({ page: 2, size: 20 });

    expect(receivedUrl).toContain('page=2');
    expect(receivedUrl).toContain('size=20');
  });

  it('should_post_release_date_and_space_when_createRelease_called', async () => {
    let body: unknown = null;
    server.use(
      http.post(`${MSW_BASE}/releases`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(releaseFuture, { status: 201 });
      }),
    );

    const result = await createRelease({ releaseDate: '2026-07-10', parkingSpaceId: 3 });

    expect(body).toEqual({ releaseDate: '2026-07-10', parkingSpaceId: 3 });
    expect(result.type).toBe('VOLUNTARY');
  });

  it('should_delete_release_when_cancelRelease_called', async () => {
    let deletedId: number | null = null;
    server.use(
      http.delete(`${MSW_BASE}/releases/:id`, ({ params }) => {
        deletedId = Number(params.id);
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await cancelRelease(releaseFuture.id);

    expect(deletedId).toBe(releaseFuture.id);
  });

  it('should_send_reason_when_createAdministrativeRelease_called', async () => {
    let body: unknown = null;
    server.use(
      http.post(`${MSW_BASE}/releases/administrative`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          { ...releaseFuture, type: 'ADMINISTRATIVE', reason: 'no acude' },
          { status: 201 },
        );
      }),
    );

    const result = await createAdministrativeRelease({
      employeeId: 10,
      parkingSpaceId: 3,
      releaseDate: '2026-07-10',
      reason: 'no acude',
    });

    expect(body).toEqual({
      employeeId: 10,
      parkingSpaceId: 3,
      releaseDate: '2026-07-10',
      reason: 'no acude',
    });
    expect(result.type).toBe('ADMINISTRATIVE');
  });
});
