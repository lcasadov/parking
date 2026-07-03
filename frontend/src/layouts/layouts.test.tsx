import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { AdminLayout } from './AdminLayout';
import { EmployeeLayout } from './EmployeeLayout';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser, employeeUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

describe('layouts', () => {
  it('should_render_admin_layout_outlet_content', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));
    renderWithProviders(
      <Routes>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<span>admin-child</span>} />
        </Route>
      </Routes>,
      { route: '/admin' },
    );
    expect(await screen.findByText('admin-child')).toBeInTheDocument();
  });

  it('should_render_employee_layout_outlet_content', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(employeeUser)));
    renderWithProviders(
      <Routes>
        <Route path="/employee" element={<EmployeeLayout />}>
          <Route index element={<span>employee-child</span>} />
        </Route>
      </Routes>,
      { route: '/employee' },
    );
    expect(await screen.findByText('employee-child')).toBeInTheDocument();
  });
});
