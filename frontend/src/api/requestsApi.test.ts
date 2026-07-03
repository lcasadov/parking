import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import {
  approveRequest,
  cancelRequest,
  createRequest,
  getRequest,
  listMyRequests,
  listPendingRequests,
  rejectRequest,
} from './requestsApi';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { requestPending1 } from '../mocks/requestFixtures';

describe('requestsApi', () => {
  it('should_send_status_filter_when_listMyRequests_called_with_status', async () => {
    let receivedUrl = '';
    server.use(
      http.get(`${MSW_BASE}/requests/mine`, ({ request }) => {
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

    await listMyRequests({ page: 1, size: 20, status: 'PENDING' });

    expect(receivedUrl).toContain('status=PENDING');
    expect(receivedUrl).toContain('page=1');
  });

  it('should_post_requested_date_when_createRequest_called', async () => {
    let body: unknown = null;
    server.use(
      http.post(`${MSW_BASE}/requests`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(requestPending1, { status: 201 });
      }),
    );

    const result = await createRequest({ requestedDate: '2026-07-10' });

    expect(body).toEqual({ requestedDate: '2026-07-10' });
    expect(result.status).toBe('PENDING');
  });

  it('should_request_pending_page_when_listPendingRequests_called', async () => {
    const page = await listPendingRequests({ page: 0, size: 20 });
    expect(Array.isArray(page.content)).toBe(true);
  });

  it('should_fetch_detail_when_getRequest_called', async () => {
    const detail = await getRequest(requestPending1.id);
    expect(detail.id).toBe(requestPending1.id);
  });

  it('should_return_cancelled_when_cancelRequest_called', async () => {
    const result = await cancelRequest(requestPending1.id);
    expect(result.status).toBe('CANCELLED');
  });

  it('should_send_space_when_approveRequest_called', async () => {
    const result = await approveRequest(requestPending1.id, { parkingSpaceId: 3, approvalNote: 'x' });
    expect(result.parkingSpaceId).toBe(3);
  });

  it('should_send_reason_when_rejectRequest_called', async () => {
    const result = await rejectRequest(requestPending1.id, { reasonCode: 'OTHER', rejectionReason: 'garaje' });
    expect(result.rejectionReasonCode).toBe('OTHER');
  });
});
