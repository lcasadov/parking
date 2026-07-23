import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import {
  getEmployeeWeekOccupancy,
  listSelectableReleaseEmployees,
} from './releaseSelectionApi';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';

describe('releaseSelectionApi', () => {
  it('should_list_selectable_employees', async () => {
    server.use(
      http.get(`${MSW_BASE}/releases/employees`, () =>
        HttpResponse.json([{ id: 7, fullName: 'Grace Hopper' }]),
      ),
    );

    const employees = await listSelectableReleaseEmployees();

    expect(employees).toEqual([{ id: 7, fullName: 'Grace Hopper' }]);
  });

  it('should_request_week_occupancy_with_weekStart_query', async () => {
    let seenWeekStart: string | null = null;
    server.use(
      http.get(`${MSW_BASE}/releases/employees/:id/occupancy`, ({ request, params }) => {
        seenWeekStart = new URL(request.url).searchParams.get('weekStart');
        return HttpResponse.json({
          employeeId: Number(params.id),
          employeeName: 'Grace Hopper',
          weekStart: seenWeekStart,
          days: [],
        });
      }),
    );

    const occupancy = await getEmployeeWeekOccupancy(7, '2026-07-06');

    expect(seenWeekStart).toBe('2026-07-06');
    expect(occupancy.employeeId).toBe(7);
  });
});
