import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router-dom';
import { AppHeader } from '../components/AppHeader';
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
            to={ROUTES.adminFixedAssignments}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-calendar-check" aria-hidden="true" />
            {t('fixedAssignments.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminRequests}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-inbox" aria-hidden="true" />
            {t('requests.inbox.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminReleases}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-calendar-off" aria-hidden="true" />
            {t('releases.admin.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.adminVisitors}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-user-plus" aria-hidden="true" />
            {t('visitors.navLabel')}
          </NavLink>
        </Sidebar>
        <main className="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
