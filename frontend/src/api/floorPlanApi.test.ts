import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { getFloorPlan, requestDeskFromFloorPlan, updateDeskPosition } from './floorPlanApi';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { defaultFloorPlan } from '../mocks/floorPlanFixtures';

const FLOOR_PLAN_URL = `${MSW_BASE}/floor-plan`;

describe('floorPlanApi', () => {
  it('should_send_date_query_param_when_getting_floor_plan', async () => {
    let receivedDate: string | null = null;
    server.use(
      http.get(FLOOR_PLAN_URL, ({ request }) => {
        receivedDate = new URL(request.url).searchParams.get('date');
        return HttpResponse.json(defaultFloorPlan);
      }),
    );

    const result = await getFloorPlan('2026-07-10');

    expect(receivedDate).toBe('2026-07-10');
    expect(result.desks).toHaveLength(defaultFloorPlan.desks.length);
  });

  it('should_post_date_to_desk_request_endpoint_when_requesting_from_plan', async () => {
    let body: Record<string, unknown> | null = null;
    let calledDeskId: string | undefined;
    server.use(
      http.post(`${FLOOR_PLAN_URL}/desks/:deskId/request`, async ({ request, params }) => {
        body = (await request.json()) as Record<string, unknown>;
        calledDeskId = params.deskId as string;
        return HttpResponse.json({ requestId: 42, state: 'REQUESTED' }, { status: 201 });
      }),
    );

    const result = await requestDeskFromFloorPlan(7, '2026-07-11');

    expect(calledDeskId).toBe('7');
    expect(body).toEqual({ date: '2026-07-11' });
    expect(result.requestId).toBe(42);
  });

  it('should_put_coordinates_when_updating_desk_position', async () => {
    let body: Record<string, unknown> | null = null;
    server.use(
      http.put(`${FLOOR_PLAN_URL}/desks/:deskId/position`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await updateDeskPosition(3, { coordX: 25, coordY: 75 });

    expect(body).toEqual({ coordX: 25, coordY: 75 });
  });
});
