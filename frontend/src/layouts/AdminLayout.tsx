import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router-dom';
import { AppHeader } from '../components/AppHeader';
import { PendingRequestsBadge } from '../components/PendingRequestsBadge';
import { Sidebar } from '../components/Sidebar';
import { ROUTES } from '../routes/paths';

// Layout de administracion: header + sidebar (navegacion) + <Outlet/>.
export function AdminLayout() {
  const { t } = useTranslation();
  return (
    <div className="app-shell">
      <AppHeader pageTitle={t('layout.adminArea')} />
      <div className="layout">
        <Sidebar>
          <NavLink
            to={ROUTES.adminCalendar}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-calendar-event" aria-hidden="true" />
            {t('calendar.admin.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminAvailability}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-calendar-stats" aria-hidden="true" />
            {t('availability.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminEmployees}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-users" aria-hidden="true" />
            {t('employees.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminParkingSpaces}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-parking" aria-hidden="true" />
            {t('parkingSpaces.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminDesks}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-armchair" aria-hidden="true" />
            {t('desks.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminFloorPlan}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-map-2" aria-hidden="true" />
            {t('floorPlan.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminFixedAssignments}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-pin" aria-hidden="true" />
            {t('fixedAssignments.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminRequests}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-inbox" aria-hidden="true" />
            {t('requests.inbox.navLabel')}
            <PendingRequestsBadge />
          </NavLink>
          <NavLink
            to={ROUTES.adminReleases}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-arrow-back-up" aria-hidden="true" />
            {t('releases.admin.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminVisitors}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-user-plus" aria-hidden="true" />
            {t('visitors.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminAudit}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-history" aria-hidden="true" />
            {t('audit.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminLoginLogs}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-login" aria-hidden="true" />
            {t('loginLogs.navLabel')}
          </NavLink>
        </Sidebar>
        <main className="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
