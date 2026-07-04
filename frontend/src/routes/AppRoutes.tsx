import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { AdminLayout } from '../layouts/AdminLayout';
import { EmployeeLayout } from '../layouts/EmployeeLayout';
import { ChangePasswordPage } from '../pages/ChangePasswordPage';
import { EmployeesPage } from '../pages/EmployeesPage';
import { ParkingSpacesPage } from '../pages/ParkingSpacesPage';
import { FixedAssignmentsPage } from '../pages/FixedAssignmentsPage';
import { MyFixedAssignmentsPage } from '../pages/MyFixedAssignmentsPage';
import { PendingRequestsPage } from '../pages/PendingRequestsPage';
import { MyRequestsPage } from '../pages/MyRequestsPage';
import { AdministrativeReleasesPage } from '../pages/AdministrativeReleasesPage';
import { MyReleasesPage } from '../pages/MyReleasesPage';
import { VisitorsPage } from '../pages/VisitorsPage';
import { AdminCalendarPage } from '../pages/AdminCalendarPage';
import { AvailabilityPage } from '../pages/AvailabilityPage';
import { MyWeekPage } from '../pages/MyWeekPage';
import { LoginPage } from '../pages/LoginPage';
import { ROUTES } from './paths';

export function AppRoutes() {
  return (
    <Routes>
      <Route path={ROUTES.login} element={<LoginPage />} />
      <Route
        path={ROUTES.changePassword}
        element={
          <ProtectedRoute>
            <ChangePasswordPage />
          </ProtectedRoute>
        }
      />
      <Route
        path={ROUTES.admin}
        element={
          <ProtectedRoute requiredRole="ADMIN">
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to={ROUTES.adminEmployees} replace />} />
        <Route path="employees" element={<EmployeesPage />} />
        <Route path="parking-spaces" element={<ParkingSpacesPage />} />
        <Route path="fixed-assignments" element={<FixedAssignmentsPage />} />
        <Route path="requests" element={<PendingRequestsPage />} />
        <Route path="releases" element={<AdministrativeReleasesPage />} />
        <Route path="visitors" element={<VisitorsPage />} />
        <Route path="calendar" element={<AdminCalendarPage />} />
        <Route path="availability" element={<AvailabilityPage />} />
      </Route>
      <Route
        path={ROUTES.employee}
        element={
          <ProtectedRoute requiredRole="EMPLOYEE">
            <EmployeeLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to={ROUTES.employeeRequests} replace />} />
        <Route path="requests" element={<MyRequestsPage />} />
        <Route path="fixed-assignments" element={<MyFixedAssignmentsPage />} />
        <Route path="releases" element={<MyReleasesPage />} />
        <Route path="my-week" element={<MyWeekPage />} />
      </Route>
      <Route path="*" element={<Navigate to={ROUTES.login} replace />} />
    </Routes>
  );
}
