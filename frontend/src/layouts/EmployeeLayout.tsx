import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router-dom';
import { AppHeader } from '../components/AppHeader';
import { Sidebar, SidebarSection } from '../components/Sidebar';
import { SidebarUserCard } from '../components/SidebarUserCard';
import { ROUTES } from '../routes/paths';

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `nav-item${isActive ? ' active' : ''}`;

// Layout de empleado: header + sidebar ALEATICA (navegacion propia + tarjeta de
// usuario) + <Outlet/>.
export function EmployeeLayout() {
  const { t } = useTranslation();
  return (
    <div className="app-shell">
      <AppHeader pageTitle={t('layout.employeeArea')} />
      <div className="layout">
        <Sidebar footer={<SidebarUserCard />}>
          <SidebarSection label={t('layout.sections.navigation')} />
          <NavLink to={ROUTES.employeeMyWeek} className={navItemClass}>
            <i className="ti ti-calendar-event" aria-hidden="true" />
            {t('calendar.myWeek.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.employeeFloorPlan} className={navItemClass}>
            <i className="ti ti-map-2" aria-hidden="true" />
            {t('floorPlan.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.employeeRequests} className={navItemClass}>
            <i className="ti ti-inbox" aria-hidden="true" />
            {t('requests.mine.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.employeeFixedAssignments} className={navItemClass}>
            <i className="ti ti-pin" aria-hidden="true" />
            {t('fixedAssignments.mine.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.employeeReleases} className={navItemClass}>
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
