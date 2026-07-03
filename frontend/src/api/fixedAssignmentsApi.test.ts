import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import {
  getEmployeeFixedAssignments,
  listFixedAssignments,
  revokeEmployeeFixedAssignment,
  setEmployeeFixedAssignments,
} from './fixedAssignmentsApi';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';

describe('fixedAssignmentsApi', () => {
  it('should_send_page_and_size_when_listing_fixed_assignments', async () => {
    let requestUrl = '';
    server.use(
      http.get(`${MSW_BASE}/fixed-assignments`, ({ request }) => {
        requestUrl = request.url;
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 5,
          number: 2,
          first: false,
          last: true,
        });
      }),
    );

    const page = await listFixedAssignments({ page: 2, size: 5 });

    expect(requestUrl).toContain('page=2');
    expect(requestUrl).toContain('size=5');
    expect(page.size).toBe(5);
  });

  it('should_return_employee_assignments_when_getting_by_employee_id', async () => {
    const rows = await getEmployeeFixedAssignments(10);

    expect(rows).toHaveLength(3);
    expect(rows[0].employeeId).toBe(10);
    expect(rows[0].dayOfWeek).toBe(1);
    expect(rows[0].active).toBe(true);
  });

  it('should_put_parking_space_and_days_when_setting_assignments', async () => {
    let sentBody: unknown = null;
    server.use(
      http.put(`${MSW_BASE}/fixed-assignments/employee/:id`, async ({ request }) => {
        sentBody = await request.json();
        return HttpResponse.json([]);
      }),
    );

    await setEmployeeFixedAssignments(10, { parkingSpaceId: 1, daysOfWeek: [1, 2] });

    expect(sentBody).toEqual({ parkingSpaceId: 1, daysOfWeek: [1, 2] });
  });

  it('should_resolve_without_body_when_revoking_assignment', async () => {
    await expect(revokeEmployeeFixedAssignment(10)).resolves.toBeUndefined();
  });
});
