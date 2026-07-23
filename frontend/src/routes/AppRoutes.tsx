import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { AdminLayout } from '../layouts/AdminLayout';
import { AgencyLayout } from '../layouts/AgencyLayout';
import { EmployeeLayout } from '../layouts/EmployeeLayout';
import { ChangePasswordPage } from '../pages/ChangePasswordPage';
import { EmployeesPage } from '../pages/EmployeesPage';
import { ResourcesPage } from '../pages/ResourcesPage';
import { FloorPlanPage } from '../pages/FloorPlanPage';
import { MyResourcesPage } from '../pages/MyResourcesPage';
import { PendingRequestsPage } from '../pages/PendingRequestsPage';
import { MyRequestsPage } from '../pages/MyRequestsPage';
import { ReleaseHubPage } from '../pages/ReleaseHubPage';
import { VisitorsPage } from '../pages/VisitorsPage';
import { OccupancyPage } from '../pages/OccupancyPage';
import { RecordsPage } from '../pages/RecordsPage';
import { AdministrativeReleasesPage } from '../pages/AdministrativeReleasesPage';
import { SettingsPage } from '../pages/SettingsPage';
import { MyWeekPage } from '../pages/MyWeekPage';
import { LoginPage } from '../pages/LoginPage';
import { ROUTES } from './paths';

// Redirige una ruta antigua fusionada en pestañas a su destino nuevo,
// preseleccionando la pestaña correspondiente via `?tab=` (tasks §1.11: URLs
// viejas -> nuevas con redireccion donde sea barato, sin duplicar contenido).
function LegacyTabRedirect({ to, tab }: { to: string; tab?: string }) {
  return <Navigate to={tab ? `${to}?tab=${tab}` : to} replace />;
}

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
        {/* Indice ADMIN sin cambios (fuera de alcance de esta fase: solo el
            indice EMPLOYEE se mueve a "Mi Semana", app-shell spec). */}
        <Route index element={<Navigate to={ROUTES.adminEmployees} replace />} />

        {/* Operativa */}
        <Route path="requests" element={<PendingRequestsPage />} />
        <Route path="occupancy" element={<OccupancyPage />} />
        <Route path="floor-plan" element={<FloorPlanPage />} />
        <Route path="release" element={<ReleaseHubPage />} />
        <Route path="visitors" element={<VisitorsPage />} />

        {/* Gestión */}
        <Route path="employees" element={<EmployeesPage />} />
        <Route path="resources" element={<ResourcesPage />} />
        <Route path="records" element={<RecordsPage />} />
        <Route path="settings" element={<SettingsPage />} />

        {/* Rutas antiguas: redirigen al destino fusionado con la pestaña correcta. */}
        <Route
          path="parking-spaces"
          element={<LegacyTabRedirect to={ROUTES.adminResources} tab="parking" />}
        />
        <Route
          path="desks"
          element={<LegacyTabRedirect to={ROUTES.adminResources} tab="desks" />}
        />
        <Route
          path="calendar"
          element={<LegacyTabRedirect to={ROUTES.adminOccupancy} tab="weekly" />}
        />
        <Route
          path="availability"
          element={<LegacyTabRedirect to={ROUTES.adminOccupancy} tab="availability" />}
        />
        <Route
          path="releases"
          element={<LegacyTabRedirect to={ROUTES.adminReleaseHub} tab="byEmployee" />}
        />
        <Route
          path="release-by-date"
          element={<LegacyTabRedirect to={ROUTES.adminReleaseHub} tab="byDate" />}
        />
        <Route
          path="audit"
          element={<LegacyTabRedirect to={ROUTES.adminRecords} tab="audit" />}
        />
        <Route
          path="login-logs"
          element={<LegacyTabRedirect to={ROUTES.adminRecords} tab="loginLogs" />}
        />
      </Route>
      <Route
        path={ROUTES.employee}
        element={
          <ProtectedRoute requiredRole="EMPLOYEE">
            <EmployeeLayout />
          </ProtectedRoute>
        }
      >
        {/* "Mi Semana" es la ruta indice del empleado (app-shell spec). */}
        <Route index element={<Navigate to={ROUTES.employeeMyWeek} replace />} />
        <Route path="my-week" element={<MyWeekPage />} />
        <Route path="floor-plan" element={<FloorPlanPage />} />
        <Route path="requests" element={<MyRequestsPage />} />
        <Route path="my-resources" element={<MyResourcesPage />} />

        {/* Rutas antiguas: redirigen a "Mis plazas" con la pestaña correcta. */}
        <Route
          path="fixed-assignments"
          element={<LegacyTabRedirect to={ROUTES.employeeMyResources} tab="fixed" />}
        />
        <Route
          path="releases"
          element={<LegacyTabRedirect to={ROUTES.employeeMyResources} tab="releases" />}
        />
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
