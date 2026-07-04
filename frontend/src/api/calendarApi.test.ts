import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { getAdminCalendar, getAvailability, getMyWeek } from './calendarApi';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';

describe('calendarApi', () => {
  it('should_sendDateParam_when_fetchingAvailability', async () => {
    const result = await getAvailability('2026-05-11');
    expect(result.date).toBe('2026-05-11');
    expect(Array.isArray(result.availableResources)).toBe(true);
  });

  it('should_sendWeekStartParam_when_fetchingAdminCalendar', async () => {
    const result = await getAdminCalendar('2026-05-11');
    expect(result.weekStart).toBe('2026-05-11');
    expect(result.rows.length).toBeGreaterThan(0);
  });

  it('should_omitWeekStart_when_myWeekCalledWithoutArgument', async () => {
    let receivedUrl = '';
    server.use(
      http.get(`${MSW_BASE}/calendar/my-week`, ({ request }) => {
        receivedUrl = request.url;
        return HttpResponse.json({ weekStart: '2026-05-11', days: [] });
      }),
    );

    await getMyWeek();

    expect(receivedUrl).not.toContain('weekStart');
  });

  it('should_includeWeekStart_when_myWeekCalledWithArgument', async () => {
    let receivedUrl = '';
    server.use(
      http.get(`${MSW_BASE}/calendar/my-week`, ({ request }) => {
        receivedUrl = request.url;
        return HttpResponse.json({ weekStart: '2026-05-11', days: [] });
      }),
    );

    await getMyWeek('2026-05-11');

    expect(receivedUrl).toContain('weekStart=2026-05-11');
  });
});
