import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import {
  getVapidPublicKey,
  subscribeApi,
  unsubscribeApi,
  updateNotificationChannels,
} from './pushApi';

describe('pushApi', () => {
  it('getVapidPublicKey returns the public key', async () => {
    server.use(
      http.get(`${MSW_BASE}/push/vapid-public-key`, () => HttpResponse.json({ publicKey: 'pk.test' })),
    );
    expect(await getVapidPublicKey()).toBe('pk.test');
  });

  it('subscribeApi posts endpoint and keys', async () => {
    let body: unknown = null;
    server.use(
      http.post(`${MSW_BASE}/push/subscriptions`, async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    await subscribeApi({ endpoint: 'e', keys: { p256dh: 'p', auth: 'a' } } as PushSubscriptionJSON);
    expect(body).toEqual({ endpoint: 'e', keys: { p256dh: 'p', auth: 'a' } });
  });

  it('unsubscribeApi deletes with the endpoint in the body', async () => {
    let body: unknown = null;
    server.use(
      http.delete(`${MSW_BASE}/push/subscriptions`, async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    await unsubscribeApi('e');
    expect(body).toEqual({ endpoint: 'e' });
  });

  it('updateNotificationChannels PUTs the two flags', async () => {
    let body: unknown = null;
    server.use(
      http.put(`${MSW_BASE}/admin/settings/notification-channels`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({
          approvalMode: 'MANUAL',
          emailNotificationsEnabled: false,
          pushNotificationsEnabled: true,
        });
      }),
    );
    const result = await updateNotificationChannels(false, true);
    expect(body).toEqual({ emailNotificationsEnabled: false, pushNotificationsEnabled: true });
    expect(result.pushNotificationsEnabled).toBe(true);
  });
});
