import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { AdminLayout } from '../layouts/AdminLayout';
import { AgencyLayout } from '../layouts/AgencyLayout';
import { EmployeeLayout } from '../layouts/EmployeeLayout';
import { ChangePasswordPage } from '../pages/ChangePasswordPage';
import { EmployeesPage } from '../pages/EmployeesPage';
import { ParkingSpacesPage } from '../pages/ParkingSpacesPage';
import { DesksPage } from '../pages/DesksPage';
import { FloorPlanPage } from '../pages/FloorPlanPage';
import { MyFixedAssignmentsPage } from '../pages/MyFixedAssignmentsPage';
import { PendingRequestsPage } from '../pages/PendingRequestsPage';
import { MyRequestsPage } from '../pages/MyRequestsPage';
import { AdministrativeReleasesPage } from '../pages/AdministrativeReleasesPage';
import { MyReleasesPage } from '../pages/MyReleasesPage';
import { VisitorsPage } from '../pages/VisitorsPage';
import { AdminCalendarPage } from '../pages/AdminCalendarPage';
import { AvailabilityPage } from '../pages/AvailabilityPage';
import { AuditPage } from '../pages/AuditPage';
import { LoginLogsPage } from '../pages/LoginLogsPage';
import { SettingsPage } from '../pages/SettingsPage';
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
        <Route path="desks" element={<DesksPage />} />
        <Route path="floor-plan" element={<FloorPlanPage />} />
        <Route path="requests" element={<PendingRequestsPage />} />
        <Route path="releases" element={<AdministrativeReleasesPage />} />
        <Route path="visitors" element={<VisitorsPage />} />
        <Route path="calendar" element={<AdminCalendarPage />} />
        <Route path="availability" element={<AvailabilityPage />} />
        <Route path="audit" element={<AuditPage />} />
        <Route path="login-logs" element={<LoginLogsPage />} />
        <Route path="settings" element={<SettingsPage />} />
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
        <Route path="floor-plan" element={<FloorPlanPage />} />
      </Route>
      <Route
        path={ROUTES.agency}
        element={
          <ProtectedRoute requiredRole="AGENCIA">
            <AgencyLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to={ROUTES.agencyReleases} replace />} />
        <Route path="releases" element={<AdministrativeReleasesPage />} />
      </Route>
      <Route path="*" element={<Navigate to={ROUTES.login} replace />} />
    </Routes>
  );
}
