import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { apiClient } from './apiClient';
import { SESSION_EXPIRED } from './events';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';

describe('apiClient interceptor', () => {
  it('should_emit_session_expired_when_response_401', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () =>
        HttpResponse.json({ error: 'unauthorized', message: 'no' }, { status: 401 }),
      ),
    );
    const listener = vi.fn();
    window.addEventListener(SESSION_EXPIRED, listener);

    await expect(apiClient.get('/auth/me')).rejects.toBeDefined();

    expect(listener).toHaveBeenCalledTimes(1);
    window.removeEventListener(SESSION_EXPIRED, listener);
  });
});
