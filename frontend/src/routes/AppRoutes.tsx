import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { AdminLayout } from '../layouts/AdminLayout';
import { EmployeeLayout } from '../layouts/EmployeeLayout';
import { ChangePasswordPage } from '../pages/ChangePasswordPage';
import { EmployeesPage } from '../pages/EmployeesPage';
import { ParkingSpacesPage } from '../pages/ParkingSpacesPage';
import { FixedAssignmentsPage } from '../pages/FixedAssignmentsPage';
import { MyFixedAssignmentsPage } from '../pages/MyFixedAssignmentsPage';
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
      </Route>
      <Route
        path={ROUTES.employee}
        element={
          <ProtectedRoute requiredRole="EMPLOYEE">
            <EmployeeLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to={ROUTES.employeeFixedAssignments} replace />} />
        <Route path="fixed-assignments" element={<MyFixedAssignmentsPage />} />
      </Route>
      <Route path="*" element={<Navigate to={ROUTES.login} replace />} />
    </Routes>
  );
}
