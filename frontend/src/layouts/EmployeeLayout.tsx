import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router-dom';
import { AppHeader } from '../components/AppHeader';
import { Sidebar } from '../components/Sidebar';
import { ROUTES } from '../routes/paths';

// Layout de empleado: header + sidebar (navegacion propia) + <Outlet/>.
export function EmployeeLayout() {
  const { t } = useTranslation();
  return (
    <div className="app-shell">
      <AppHeader pageTitle={t('layout.employeeArea')} />
      <div className="layout">
        <Sidebar>
          <NavLink
            to={ROUTES.employeeMyWeek}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-calendar-event" aria-hidden="true" />
            {t('calendar.myWeek.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.employeeFloorPlan}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-map-2" aria-hidden="true" />
            {t('floorPlan.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.employeeRequests}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-inbox" aria-hidden="true" />
            {t('requests.mine.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.employeeFixedAssignments}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-pin" aria-hidden="true" />
            {t('fixedAssignments.mine.navLabel')}
          </NavLink>
          <NavLink
            to={ROUTES.employeeReleases}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-arrow-back-up" aria-hidden="true" />
            {t('releases.mine.navLabel')}
          </NavLink>
        </Sidebar>
        <main className="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
